package com.memory.xiaohua.ai.tool;

import com.memory.xiaohua.ai.config.AiProperties;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.utils.file.MinioOSSUtil;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 网页生成 Agent 工具集（Code Agent，纯读）
 *
 * <p>核心设计：<strong>占位符替换法</strong>。
 *
 * <p>照片访问地址是 MinIO 预签名 URL，长度常达 600+ 字符
 * （含 {@code X-Amz-Signature} 等参数）。若让 LLM 自行把完整 URL 写进
 * {@code <img src>}，极易出现截断、转义、漏参数等问题，导致图片全部裂图。
 * 因此改为：
 * <ol>
 *   <li>给 LLM 的上下文里只出现短占位符 {@code {{IMG_1}}...}；</li>
 *   <li>LLM 只需写 {@code <img src="{{IMG_1}}">}；</li>
 *   <li>生成后由本类用真实 URL 精确字符串替换。</li>
 * </ol>
 * 这样 URL 完整性与模型能力解耦，100% 可控。
 *
 * <p>URL 新鲜度：优先使用对象存储重新签名的地址（避免 DB 中存量 URL 过期导致历史网页裂图），
 * 签名失败时回退 DB 中已存的 URL。
 *
 * @author xiaohua
 */
@Slf4j
@Component
public class WebpageTools {

    private final JdbcTemplate jdbcTemplate;
    private final AiProperties properties;

    /** 网页生成专用模型（max-tokens 更大，避免 HTML 被截断）；未配置时回退主模型 */
    @Autowired(required = false)
    @Qualifier("webpageModel")
    private ChatModel webpageModel;

    @Autowired(required = false)
    @Qualifier("mainModel")
    private ChatModel mainModel;

    /** 对象存储签名工具（可能未装配，缺失时回退 DB 中已存 URL） */
    @Autowired(required = false)
    private MinioOSSUtil minioOSSUtil;

    public WebpageTools(JdbcTemplate jdbcTemplate, AiProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    /**
     * 根据照片上下文 + 创意提示生成 HTML 网页。
     *
     * @param fileIds 照片ID列表
     * @param prompt   创意提示
     * @param userId   当前用户
     * @return 完整 HTML 字符串（占位符已被替换为真实图片地址）
     */
    public String generateHtml(List<String> fileIds, String prompt, Long userId) {
        PhotoContext ctx = loadPhotoContext(fileIds, userId);
        ChatModel model = webpageModel != null ? webpageModel : mainModel;
        if (model == null) {
            throw new IllegalStateException("网页生成模型未配置");
        }
        String userPrompt = ctx.contextText + "\n\n创意提示：" + prompt;
        String html = model.chat(buildSystemPrompt() + "\n\n" + userPrompt);
        html = extractHtml(html);

        // 关键：token 上限导致 HTML 在 CSS/标签中途被截断时，先修复结构，否则浏览器无法渲染
        boolean truncated = isTruncated(html);
        if (truncated) {
            log.warn("[AI][WebpageTools] 生成的 HTML 被截断（长度={}），执行结构修复", html.length());
            html = repairTruncatedHtml(html);
        }

        String result = replacePlaceholders(html, ctx.urlByPlaceholder);

        // 兜底：模型漏写 <img>（纯文字排版或截断导致照片区未生成）时，自动注入图片网格
        if (!ctx.urlByPlaceholder.isEmpty() && !containsImgTag(result)) {
            log.warn("[AI][WebpageTools] 生成的 HTML 未包含任何 <img>，自动注入图片网格");
            result = injectImageGallery(result, ctx.urlByPlaceholder.values());
        }
        long leftover = ctx.urlByPlaceholder.keySet().stream().filter(result::contains).count();
        log.info("[AI][WebpageTools] 网页生成完成，照片数={}，截断={}，未替换占位符残留={}",
                ctx.urlByPlaceholder.size(), truncated, leftover);
        return result;
    }

    /** 判断 HTML 是否被截断：缺少 </html>，或 style/script 标签未闭合 */
    private boolean isTruncated(String html) {
        if (html == null || html.isBlank()) {
            return true;
        }
        String lower = html.toLowerCase();
        if (!lower.contains("</html>")) {
            return true;
        }
        // <style> 未闭合
        int styleOpen = lower.lastIndexOf("<style");
        int styleClose = lower.lastIndexOf("</style>");
        if (styleOpen > styleClose) {
            return true;
        }
        return lower.lastIndexOf("<script") > lower.lastIndexOf("</script>");
    }

    /**
     * 修复被截断的 HTML：闭合未结束的 style/body/html，使浏览器能正常解析。
     *
     * <p>截断常发生在 CSS 声明中途（如 "border-top:"），补充闭合标签后，
     * 浏览器会忽略最后那条不完整的声明，页面其余部分仍可正常渲染。
     */
    private String repairTruncatedHtml(String html) {
        String lower = html.toLowerCase();
        StringBuilder sb = new StringBuilder(html);

        // 截断在 CSS 声明中途时，补一个分号结束该声明，避免吞掉后续注入内容
        String tail = html.trim();
        if (!tail.endsWith("}") && !tail.endsWith(">") && !tail.endsWith(";")) {
            sb.append(';');
        }
        int styleOpen = lower.lastIndexOf("<style");
        int styleClose = lower.lastIndexOf("</style>");
        if (styleOpen > styleClose) {
            sb.append("\n</style>\n");
        }
        int scriptOpen = lower.lastIndexOf("<script");
        int scriptClose = lower.lastIndexOf("</script>");
        if (scriptOpen > scriptClose) {
            sb.append("\n</script>\n");
        }
        if (!lower.contains("<body")) {
            sb.append("<body>\n");
        }
        sb.append("\n</body>\n</html>");
        return sb.toString();
    }

    private boolean containsImgTag(String html) {
        return html != null && html.toLowerCase().contains("<img");
    }

    /** 在 </body> 前注入响应式图片网格（无 body 标签则追加到末尾） */
    private String injectImageGallery(String html, java.util.Collection<String> urls) {
        StringBuilder gallery = new StringBuilder();
        gallery.append("\n<section style=\"max-width:1200px;margin:2rem auto;padding:0 1rem;\">")
                .append("<div style=\"display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:1.5rem;\">");
        for (String url : urls) {
            if (url == null || url.isEmpty()) {
                continue;
            }
            gallery.append("<div style=\"aspect-ratio:1/1.2;border-radius:20px;overflow:hidden;")
                    .append("background:#f5f5f5;box-shadow:0 10px 24px rgba(0,0,0,.12);\">")
                    .append("<img src=\"").append(url)
                    .append("\" alt=\"照片\" loading=\"lazy\" style=\"width:100%;height:100%;object-fit:cover;display:block;\" ")
                    .append("onerror=\"this.style.display='none'\"/></div>");
        }
        gallery.append("</div></section>\n");

        String src = html == null ? "" : html;
        String lower = src.toLowerCase();
        int bodyEnd = lower.lastIndexOf("</body>");
        if (bodyEnd >= 0) {
            return src.substring(0, bodyEnd) + gallery + src.substring(bodyEnd);
        }
        // 无 </body>：插在 <body> 起始标签之后，避免内容落进 <head>/破损 CSS 中
        int bodyStart = lower.indexOf("<body");
        if (bodyStart >= 0) {
            int gt = src.indexOf('>', bodyStart);
            if (gt > 0) {
                return src.substring(0, gt + 1) + gallery + src.substring(gt + 1);
            }
        }
        return src + gallery;
    }

    private String buildSystemPrompt() {
        return """
                你是网页设计专家。根据给定的照片清单和用户创意提示，生成一个完整的、可直接在浏览器打开的 HTML 网页。

                要求：
                1. 内联 CSS，美观现代，移动端适配。
                2. 【最重要】每张照片用占位符作为图片地址，格式严格为 <img src="{{IMG_1}}">。
                   占位符必须原样照抄清单中给出的写法（含两侧双花括号和数字序号）。
                   **严禁**自行编造图片地址，也严禁把 fileId、文件名当作 src。
                3. 清单里有几张照片就嵌入几张，不要多也不要少，序号与清单一一对应。
                4. 为每张图加 onerror 兜底（加载失败时显示占位块）。
                5. 若清单提示"未指定照片"，则不要插入任何 <img>，用 CSS 占位块或纯文字排版。
                6. 只输出 HTML 代码本身（以 <!DOCTYPE html> 开头），不要任何解释或 markdown 代码块。
                7. 【输出顺序】CSS 尽量精简紧凑（可写在一行内），先输出 <!DOCTYPE>、<head>、<style>，
                   再输出 <body> 内的照片区块，最后才是页脚等次要内容。
                   这样即使输出长度受限，照片部分也已经生成完毕。
                8. 必须以 </body></html> 正常结尾，保证文档结构完整。
                """;
    }

    // ==================== 照片上下文（占位符化） ====================

    /** 照片上下文：给 LLM 的占位符文本 + 占位符→真实URL 映射 */
    private static class PhotoContext {
        String contextText;
        Map<String, String> urlByPlaceholder = new LinkedHashMap<>();
    }

    private PhotoContext loadPhotoContext(List<String> fileIds, Long userId) {
        PhotoContext ctx = new PhotoContext();
        if (fileIds == null || fileIds.isEmpty()) {
            ctx.contextText = "（未指定照片：请不要插入任何 <img> 图片，用 CSS 占位块或纯文字排版完成网页）";
            return ctx;
        }
        // 校验 fileId 为安全标识（仅字母数字/连字符），防 IN 拼接注入
        for (String id : fileIds) {
            if (id == null || id.isBlank() || !id.matches("[0-9a-zA-Z-]{1,64}")) {
                throw new BusinessException(StatusCode.PARAMS_ERROR, "照片ID不合法");
            }
        }
        String in = fileIds.stream()
                .map(id -> "'" + id + "'")
                .reduce((a, b) -> a + "," + b)
                .orElse("''");
        String sql = "SELECT f.file_id, f.origin_file_name, f.file_object_name, f.thumbnail_object_name, " +
                "f.file_url, f.thumbnail_url, f.date_time_original, l.city, l.province " +
                "FROM file f " +
                "LEFT JOIN location l ON f.file_id = l.file_id " +
                "JOIN user_file uf ON f.file_id = uf.file_id " +
                "WHERE uf.user_id = ? AND f.file_id IN (" + in + ") " +
                "AND uf.is_deleted = 0 LIMIT " + properties.getQueryMaxRows();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
        if (rows.isEmpty()) {
            ctx.contextText = "（指定的照片不存在或不属于当前用户：请不要插入任何 <img> 图片）";
            return ctx;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 可用照片清单（共 ").append(rows.size()).append(" 张）\n");
        sb.append("用法：把占位符原样写进 <img src=\"...\">，系统会自动替换成真实图片地址。\n\n");
        int idx = 1;
        for (Map<String, Object> row : rows) {
            String placeholder = "{{IMG_" + idx + "}}";
            String url = resolveImageUrl(row);
            ctx.urlByPlaceholder.put(placeholder, url);
            sb.append(idx).append(". 占位符 ").append(placeholder)
              .append(" | 名称=").append(row.get("origin_file_name"))
              .append(" | 时间=").append(row.get("date_time_original"))
              .append(" | 地点=").append(firstNonBlank(row.get("province"), row.get("city")))
              .append("\n   用法示例: <img src=\"").append(placeholder).append("\" alt=\"照片\">\n\n");
            idx++;
        }
        ctx.contextText = sb.toString();
        return ctx;
    }

    /**
     * 解析图片地址：优先用对象存储重新签名（保证新鲜可用），失败则回退 DB 中已存 URL。
     */
    private String resolveImageUrl(Map<String, Object> row) {
        String dbThumb = nvl(row.get("thumbnail_url"));
        String dbFull = nvl(row.get("file_url"));
        if (minioOSSUtil != null) {
            String thumbObj = nvl(row.get("thumbnail_object_name"));
            String fullObj = nvl(row.get("file_object_name"));
            try {
                if (!fullObj.isEmpty()) {
                    return minioOSSUtil.getFileUrl(fullObj);
                }
                if (!thumbObj.isEmpty()) {
                    return minioOSSUtil.getFileUrl(thumbObj);
                }
            } catch (Exception e) {
                log.warn("[AI][WebpageTools] 图片重新签名失败，回退 DB 中已存 URL: {}", e.getMessage());
            }
        }
        return !dbFull.isEmpty() ? dbFull : dbThumb;
    }

    /**
     * 用真实 URL 替换占位符。
     *
     * <p>容错处理模型可能产生的写法偏差：花括号被省略、被 HTML 实体化等。
     */
    private String replacePlaceholders(String html, Map<String, String> urlByPlaceholder) {
        if (html == null || html.isBlank() || urlByPlaceholder.isEmpty()) {
            return html;
        }
        String result = html;
        for (Map.Entry<String, String> e : urlByPlaceholder.entrySet()) {
            String token = e.getKey().replace("{{", "").replace("}}", ""); // 如 IMG_1
            String url = e.getValue();
            if (url == null || url.isEmpty()) {
                continue;
            }
            // 依次替换：标准写法 → 单花括号 → 裸标识符（仅当出现在 src 属性中）
            result = result.replace(e.getKey(), url);
            result = result.replace("{" + token + "}", url);
            result = result.replace(token + "_URL", url);
        }
        return result;
    }

    private String extractHtml(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("```(html)?", "").trim();
        }
        return s;
    }

    private String firstNonBlank(Object a, Object b) {
        String av = nvl(a);
        return !av.isEmpty() ? av : nvl(b);
    }

    private String nvl(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
