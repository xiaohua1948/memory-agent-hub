package com.memory.xiaohua.ai.tool;

import com.memory.xiaohua.ai.config.AiProperties;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 网页生成 Agent 工具集（Code Agent，纯读）
 *
 * <p>将选中照片作为上下文注入 LLM，生成完整 HTML 网页。
 *
 * @author xiaohua
 */
@Component
public class WebpageTools {

    private final JdbcTemplate jdbcTemplate;
    private final AiProperties properties;

    @Autowired(required = false)
    @Qualifier("mainModel")
    private ChatModel mainModel;

    public WebpageTools(JdbcTemplate jdbcTemplate, AiProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    /**
     * 根据照片上下文 + 创意提示生成 HTML 网页。
     *
     * @param fileIds  照片ID列表
     * @param prompt   创意提示（如"旅行日记网页"）
     * @param userId   当前用户
     * @return 完整 HTML 字符串
     */
    public String generateHtml(List<String> fileIds, String prompt, Long userId) {
        String photoContext = loadPhotoContext(fileIds, userId);
        if (mainModel == null) {
            throw new IllegalStateException("主任务模型未配置");
        }
        String sysPrompt = """
                你是网页设计专家。根据给定的照片信息和用户创意提示，生成一个完整的、可直接在浏览器打开的 HTML 网页。
                要求：
                1. 内联 CSS，美观现代，移动端适配。
                2. 照片用 <img> 标签引用缩略图 URL，加载失败时显示占位。
                3. 只输出 HTML 代码本身（以 <!DOCTYPE html> 开头），不要任何解释或 markdown 代码块。
                """;
        String userPrompt = "照片信息：\n" + photoContext + "\n\n创意提示：" + prompt;
        String html = mainModel.chat(sysPrompt + "\n\n" + userPrompt);
        return extractHtml(html);
    }

    private String loadPhotoContext(List<String> fileIds, Long userId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return "（无选中照片，请根据创意提示生成通用网页）";
        }
        // 校验 fileId 为安全标识（仅字母数字/连字符），防 IN 拼接注入
        for (String id : fileIds) {
            if (id == null || id.isBlank() || !id.matches("[0-9a-zA-Z-]{1,64}")) {
                throw new BusinessException(StatusCode.PARAMS_ERROR, "照片ID不合法");
            }
        }
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < fileIds.size(); i++) {
            in.append("'").append(fileIds.get(i)).append("'");
            if (i < fileIds.size() - 1) in.append(",");
        }
        String sql = "SELECT f.file_id, f.thumbnail_url, f.date_time_original, l.city, l.province " +
                "FROM file f " +
                "LEFT JOIN location l ON f.file_id = l.file_id " +
                "JOIN user_file uf ON f.file_id = uf.file_id " +
                "WHERE uf.user_id = ? AND f.file_id IN (" + in + ") " +
                "AND uf.is_deleted = 0 LIMIT " + properties.getQueryMaxRows();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> row : rows) {
            sb.append("- fileId=").append(row.get("file_id"))
              .append(", thumb=").append(row.get("thumbnail_url"))
              .append(", time=").append(row.get("date_time_original"))
              .append(", city=").append(row.get("city")).append("\n");
        }
        return sb.toString();
    }

    private String extractHtml(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("```(html)?", "").trim();
        }
        return s;
    }
}
