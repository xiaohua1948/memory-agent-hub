package com.memory.xiaohua.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memory.xiaohua.ai.action.PendingActionService;
import com.memory.xiaohua.ai.agent.AgentToolContext;
import com.memory.xiaohua.ai.agent.Artifact;
import com.memory.xiaohua.ai.sql.query.AskDataService;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.mapper.AlbumMapper;
import com.memory.xiaohua.model.entity.Album;
import com.memory.xiaohua.model.vo.ai.PendingActionVO;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 主 Agent 工具集 —— 真正暴露给大模型 function calling 的「子 Agent」能力入口
 *
 * <p>架构：主 Agent（{@code MainAgent}）通过 LLM tool-calling 循环自主决定
 * 调用哪个工具、传什么参数，实现复杂任务的自由编排；每个 @Tool 方法背后
 * 组合一个子 Agent / 业务能力：
 * <ul>
 *   <li>searchPhotos —— 照片查询子 Agent（text2sql，纯读）</li>
 *   <li>listAlbums / createAlbum / addPicturesToAlbum —— 相册管理子 Agent
 *       （写操作只生成「草稿 + 确认卡片」，用户确认后才真正执行，human-in-the-loop）</li>
 *   <li>generateHtmlPage —— 网页生成子 Agent（Code，纯读）</li>
 * </ul>
 *
 * <p>安全设计：
 * <ol>
 *   <li>工具方法签名不含 userId —— 身份由 {@link AgentToolContext}（ThreadLocal）注入；</li>
 *   <li>写操作不直接落库，只创建 PendingAction 草稿，推送确认卡片；</li>
 *   <li>照片 fileId 做归属校验（user_file 表），防止模型幻觉出不存在的照片ID；</li>
 *   <li>相册解析强制 user_id 条件，杜绝越权操作他人相册。</li>
 * </ol>
 *
 * @author xiaohua
 */
@Slf4j
@Component
public class AgentTools {

    private final QueryTools queryTools;
    private final AlbumTools albumTools;
    private final WebpageTools webpageTools;
    private final PendingActionService pendingActionService;
    private final AskDataService askDataService;
    private final AlbumMapper albumMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AgentTools(QueryTools queryTools,
                      AlbumTools albumTools,
                      WebpageTools webpageTools,
                      PendingActionService pendingActionService,
                      AskDataService askDataService,
                      AlbumMapper albumMapper,
                      JdbcTemplate jdbcTemplate,
                      ObjectMapper objectMapper) {
        this.queryTools = queryTools;
        this.albumTools = albumTools;
        this.webpageTools = webpageTools;
        this.pendingActionService = pendingActionService;
        this.askDataService = askDataService;
        this.albumMapper = albumMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    // ==================== 读操作（直接执行） ====================

    /**
     * 照片查询子 Agent：自然语言 → text2sql → 结构化照片列表。
     */
    @Tool("搜索当前用户的照片，返回照片列表。支持按地点、拍摄时间、文件大小、标签、设备等条件筛选")
    public String searchPhotos(@P("查询条件的自然语言描述，例如：地点在上海且文件大小大于2MB") String question) {
        AgentToolContext ctx = AgentToolContext.current();
        if (question == null || question.isBlank()) {
            return "查询条件为空，请补充筛选条件。";
        }
        List<Map<String, Object>> photos = queryTools.searchPhotosStructured(question, ctx.getUserId());
        // 前端网格展示（photos 事件，与既有 SSE 契约一致）
        ctx.pushEvent("photos", photos);
        if (photos.isEmpty()) {
            // 空结果必须带诊断：否则模型会把「条件没匹配上」直接断言成「你没有这些照片」，
            // 用户无法分辨是"真的没有"还是"检索方式不对"，直接被误导。
            return "未找到符合条件的照片。" + buildEmptyResultDiagnostic(ctx.getUserId());
        }
        List<String> fileIds = photos.stream()
                .map(p -> String.valueOf(p.get("fileId")))
                .toList();
        // 注册为独立工件：返回句柄而非裸 ID 列表——省 token，且指代无歧义
        String ref = ctx.registerPhotos(fileIds, photos.size() + "张·" + abbreviate(question, 24));
        return "找到 " + photos.size() + " 张照片，已记为 " + ref
                + "。后续工具要用这批照片时，fileIdsJson 传 \"" + ref + "\"；"
                + "若指的就是它，传 [] 也可以（系统默认使用最近一次查询的结果）。";
    }

    /**
     * 空结果诊断：回传该用户的照片总量与地理分布，让主 Agent 能区分三种「查不到」：
     * <ol>
     *   <li><b>条件写法不一致</b>：目标地区出现在分布里，但本次筛选条件与库中取值不同
     *       （如库里是「山西省」而条件写成了别的形式）→ 应改用库中实际写法重查；</li>
     *   <li><b>确实没有</b>：目标地区不在分布里 → 才可以告知用户没有该地区的照片；</li>
     *   <li><b>没有任何地点数据</b>：分布为空（照片普遍不含 GPS EXIF，如翻拍件、证书、截图）
     *       → 按地点查必然为 0，要如实说明原因，而不是回答"没有该地区的照片"。</li>
     * </ol>
     *
     * <p>没有这段诊断时，模型会把三种情况一律说成"没有"，这是最容易被用户察觉的误导
     * （明明有山西的照片，只因条件写法不一致就被答成 0 张）。
     */
    private String buildEmptyResultDiagnostic(Long userId) {
        try {
            Integer total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user_file WHERE user_id = ? AND is_deleted = 0",
                    Integer.class, userId);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT l.province, l.city, COUNT(*) AS cnt "
                            + "FROM location l "
                            + "JOIN user_file uf ON uf.file_id = l.file_id "
                            + "WHERE uf.user_id = ? AND uf.is_deleted = 0 "
                            + "AND ((l.province IS NOT NULL AND l.province <> '') "
                            + "  OR (l.city IS NOT NULL AND l.city <> '')) "
                            + "GROUP BY l.province, l.city ORDER BY cnt DESC LIMIT 10",
                    userId);

            int totalCount = total == null ? 0 : total;
            if (rows.isEmpty()) {
                return "【诊断】该用户共有 " + totalCount + " 张照片，但没有任何带地理位置信息的记录"
                        + "（照片普遍不含 GPS EXIF，如翻拍件、证书、截图）。"
                        + "因此按地点查不出结果是正常的，请如实向用户说明这一点，"
                        + "不要回答成「没有该地点的照片」。";
            }
            StringBuilder sb = new StringBuilder("【诊断】该用户共有 ").append(totalCount)
                    .append(" 张照片，其中带地理位置信息的分布如下（省/市/数量）：");
            for (Map<String, Object> row : rows) {
                sb.append("\n- ")
                  .append(blankTo(row.get("province"), "省未知"))
                  .append(" / ")
                  .append(blankTo(row.get("city"), "市未知"))
                  .append(" / ")
                  .append(row.get("cnt"));
            }
            sb.append("\n判断规则：若目标地区在上面的分布中出现，说明是本次筛选条件写法与库中取值不一致，"
                    + "必须改用上面列出的实际写法重新调用 searchPhotos；"
                    + "只有目标地区确实不在上面时，才可以说没有该地区的照片。");
            return sb.toString();
        } catch (Exception e) {
            // 诊断失败不能影响主流程，顶多退回原来的「未找到」
            log.warn("[AI][AgentTools] 空结果诊断失败（忽略）: {}", e.getMessage());
            return "";
        }
    }

    /** 取字符串值，为空/空白时返回默认值 */
    private String blankTo(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? defaultValue : s;
    }

    /**
     * 相册列表查询（读）。
     */
    @Tool("列出当前用户的所有相册（ID与名称）")
    public String listAlbums() {
        AgentToolContext ctx = AgentToolContext.current();
        return albumTools.listAlbums(ctx.getUserId());
    }

    /**
     * 相册问数子 Agent（读）：自然语言 → 查询 DSL → 参数化 SQL。
     *
     * <p>与 {@link #listAlbums()} 的区别：listAlbums 只做「列出全部」这一件事，
     * 本工具支持任意维度筛选与聚合统计（数量、照片数、创建时间区间、名称模糊匹配），
     * 且结果为空时会回传诊断信息（是哪个条件把结果筛没了），让主 Agent 能给出
     * 「你有 12 个相册，但最多的只有 43 张」这类有价值的回复，而不是干巴巴的"没找到"。
     *
     * <p>注意职责边界：问的是<b>相册自身</b>信息用本工具；问「某个相册里有哪些照片」
     * 是照片维度，用 {@link #searchPhotosByAlbum(String)}。
     */
    @Tool("查询当前用户的相册信息（纯读）。支持按相册名称、创建时间、照片数量等任意维度筛选，也可只统计数量。"
            + "用于「我有多少个相册」「2026年1月创建的相册」「照片数超过10张的相册」「名字带旅行的相册」这类问题。"
            + "注意：查某个相册里的具体照片请用 searchPhotosByAlbum")
    public String queryAlbums(@P("相册查询需求的自然语言描述，例如：2026年1月创建且照片数超过10张的相册") String question) {
        AgentToolContext ctx = AgentToolContext.current();
        if (question == null || question.isBlank()) {
            return "查询条件为空，请说明你想查相册的什么信息。";
        }
        AskDataService.AskResult result = askDataService.askAlbums(question, ctx.getUserId());
        if (!result.ok()) {
            // 意图无法理解 / 执行失败：把原因回传模型，由其决定换说法重试或向用户说明
            return result.summary();
        }
        if (result.countIntent()) {
            // 标量指标：推 metric 事件（前端渲染为 KPI 数字）
            Map<String, Object> metric = new LinkedHashMap<>();
            metric.put("label", "相册数量");
            metric.put("value", result.total());
            ctx.pushEvent("metric", metric);
            return "查询结果：" + result.summary() + "。请在最终答复中直接告诉用户这个数字。";
        }
        List<Map<String, Object>> rows = result.rows();
        if (rows.isEmpty()) {
            return "未找到符合条件的相册。"
                    + (result.diagnostic().isEmpty() ? "" : "诊断信息：" + result.diagnostic());
        }
        // 明细表格：列定义由后端下发（且只含本次实际返回的列），前端按 type 渲染
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("columns", AskDataService.albumColumns(rows));
        table.put("rows", rows);
        ctx.pushEvent("table", table);
        return "查询到 " + rows.size() + " 个相册，明细如下：" + briefAlbums(rows)
                + "\n表格已展示给用户，请在最终答复中用自然语言概括结果。";
    }

    /** 相册列表的文本摘要（供模型总结；限制条数避免长列表撑爆上下文） */
    private String briefAlbums(List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder();
        int n = Math.min(rows.size(), 20);
        for (int i = 0; i < n; i++) {
            Map<String, Object> r = rows.get(i);
            sb.append("\n- ").append(r.get("albumName"))
              .append("（").append(r.get("photoCount")).append(" 张");
            Object created = r.get("createdAt");
            if (created != null && !String.valueOf(created).isBlank()) {
                sb.append("，创建于 ").append(created);
            }
            sb.append("）");
        }
        if (rows.size() > n) {
            sb.append("\n...（共 ").append(rows.size()).append(" 个，已省略其余）");
        }
        return sb.toString();
    }

    /**
     * 按相册查询照片（读）：走关联表 album_picture 的固定参数化 SQL，
     * 不依赖 text2sql（相册表不在 SQL 安全引擎白名单内，刻意不暴露）。
     *
     * <p>用于回答「某相册里有哪些照片」「XX 相册里的照片」类问题。
     * 注意：普通 {@link #searchPhotos(String)} 走 text2sql，无法感知相册维度，
     * 涉及具体相册时必须用本工具。
     */
    @Tool("查询某个相册里的所有照片。传入相册名称或相册ID。用于回答「某相册里有哪些照片」「XX相册里的照片」类问题。注意：不要把它当成关键词搜索，它只返回指定相册内的照片")
    public String searchPhotosByAlbum(@P("目标相册名称或相册ID") String albumNameOrId) {
        AgentToolContext ctx = AgentToolContext.current();
        if (albumNameOrId == null || albumNameOrId.isBlank()) {
            return "未指定相册，请先调用 listAlbums 查看相册列表。";
        }
        Album album = resolveAlbum(albumNameOrId, ctx.getUserId());
        if (album == null) {
            return "未找到相册「" + albumNameOrId + "」，可先调用 listAlbums 查看用户的相册列表。";
        }
        List<Map<String, Object>> photos = queryTools.searchPhotosByAlbumId(album.getAlbumId(), ctx.getUserId());
        // 前端网格展示（photos 事件，与既有 SSE 契约一致）
        ctx.pushEvent("photos", photos);
        if (photos.isEmpty()) {
            return "相册「" + album.getAlbumName() + "」中没有照片。";
        }
        List<String> fileIds = photos.stream()
                .map(p -> String.valueOf(p.get("fileId")))
                .toList();
        String ref = ctx.registerPhotos(fileIds,
                photos.size() + "张·相册「" + album.getAlbumName() + "」");
        return "在相册「" + album.getAlbumName() + "」中找到 " + photos.size() + " 张照片，已记为 " + ref
                + "。后续工具要用这批照片时，fileIdsJson 传 \"" + ref + "\"；传 [] 表示用它。";
    }

    /**
     * EXIF 拍摄信息查询（读）：按 fileId 走参数化 SQL，返回拍摄参数详情。
     *
     * <p>不走 text2sql：用户指「这两张/选中的照片」时应按 fileId 精确直查，
     * text2sql 面向自然语言条件筛选，不适合精确 ID 维度的详情查询。
     * 数据来源：file（时间/设备/尺寸）+ image_meta_data（ISO/光圈/快门/焦距等）+ location（地点）。
     */
    @Tool("查询指定照片的EXIF拍摄信息（拍摄时间、地点、设备、尺寸、ISO、光圈、快门、焦距、白平衡、测光模式等详情）。"
            + "用于「这两张照片的EXIF」「照片用什么设备拍的」「查看拍摄参数」类需求；"
            + "照片来自用户选中的照片或最近查询结果时 fileIdsJson 传[]即可自动使用")
    public String getPhotosExif(
            @P("照片fileId列表，JSON数组字符串。若用用户选中的照片或最近查询到的照片，传[]即可自动使用") String fileIdsJson) {
        AgentToolContext ctx = AgentToolContext.current();
        List<String> fileIds = resolvePhotoIds(ctx, fileIdsJson);
        if (fileIds.isEmpty()) {
            return "未指定照片且上下文中没有可用照片。请先调用 searchPhotos 查询照片，"
                    + "或让用户在界面选中照片后再查询 EXIF。";
        }
        validatePhotoOwnership(fileIds);

        String placeholders = String.join(",", java.util.Collections.nCopies(fileIds.size(), "?"));
        List<Object> args = new ArrayList<>(fileIds);
        args.add(ctx.getUserId());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT f.file_id, f.origin_file_name, f.make, f.model, f.date_time_original, f.width, f.height, "
                        + "m.iso, m.f_number, m.exposure_time, m.shutter_speed, m.focal_length, m.focal_length35, "
                        + "m.white_balance, m.metering_mode, m.exposure_program, m.software, m.altitude, "
                        + "l.province, l.city, l.district "
                        + "FROM file f "
                        + "LEFT JOIN image_meta_data m ON m.file_id = f.file_id "
                        + "LEFT JOIN location l ON l.file_id = f.file_id "
                        + "JOIN user_file uf ON uf.file_id = f.file_id "
                        + "WHERE f.file_id IN (" + placeholders + ") AND uf.user_id = ? AND uf.is_deleted = 0",
                args.toArray());
        if (rows.isEmpty()) {
            return "未找到这些照片的EXIF信息。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("共 ").append(rows.size()).append(" 张照片的EXIF信息：");
        for (Map<String, Object> r : rows) {
            sb.append("\n- ").append(r.get("origin_file_name"));
            Object time = r.get("date_time_original");
            sb.append("\n  拍摄时间：").append(time == null ? "未知" : time);
            String loc = joinLocation(r);
            if (!loc.isEmpty()) {
                sb.append("\n  地点：").append(loc);
            }
            StringBuilder device = new StringBuilder();
            appendIfPresent(device, r, "make", "", " ");
            appendIfPresent(device, r, "model", "", "");
            if (device.length() > 0) {
                sb.append("\n  设备：").append(device);
            }
            Object w = r.get("width");
            Object h = r.get("height");
            if (w != null && h != null) {
                sb.append("\n  尺寸：").append(w).append("x").append(h);
            }
            StringBuilder param = new StringBuilder();
            appendIfPresent(param, r, "iso", "ISO ", "");
            appendIfPresent(param, r, "f_number", "，光圈：F", "");
            appendIfPresent(param, r, "exposure_time", "，曝光时间：", "s");
            appendIfPresent(param, r, "focal_length", "，焦距：", "mm");
            appendIfPresent(param, r, "focal_length35", "（等效", "mm）");
            appendIfPresent(param, r, "white_balance", "，白平衡：", "");
            appendIfPresent(param, r, "metering_mode", "，测光：", "");
            appendIfPresent(param, r, "exposure_program", "，曝光程序：", "");
            appendIfPresent(param, r, "software", "，处理软件：", "");
            appendIfPresent(param, r, "altitude", "，海拔：", "m");
            if (param.length() > 0) {
                sb.append("\n  拍摄参数：").append(param);
            }
        }
        return sb.toString();
    }

    // ==================== 写操作（草稿 + 人工确认） ====================

    /**
     * 创建相册（草稿）：生成确认卡片，用户确认后才真正创建并放入照片。
     */
    @Tool("创建新相册并可放入指定照片。此操作先生成待确认卡片，用户在界面确认后才会真正执行")
    public String createAlbum(@P("相册名称") String albumName,
                              @P("相册描述，用户未指定时传空字符串") String description,
                              @P("要放入的照片fileId列表，JSON数组字符串（如[\"id1\",\"id2\"]）。若直接用用户选中的照片或最近查询到的照片，传[]即可自动使用；确实只要空相册才传[]且上下文无照片") String photoFileIdsJson) {
        AgentToolContext ctx = AgentToolContext.current();
        if (albumName == null || albumName.isBlank() || albumName.length() > 30) {
            return "相册名称不合法（不能为空且不超过30字），请根据用户输入提取。";
        }
        List<String> fileIds = resolvePhotoIds(ctx, photoFileIdsJson);
        validatePhotoOwnership(fileIds);

        String desc = description == null ? "" : description.trim();
        PendingActionVO vo = new PendingActionVO();
        vo.setActionType("CREATE_ALBUM");
        vo.setTitle("创建相册");
        vo.setDescription("即将创建相册「" + albumName + "」"
                + (desc.isEmpty() ? "" : "，描述：" + desc)
                + (fileIds.isEmpty() ? "" : "，并放入 " + fileIds.size() + " 张照片"));

        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("albumName", albumName);
        preview.put("description", desc);
        preview.put("count", fileIds.size());
        preview.put("thumbnails", loadThumbnails(fileIds));
        vo.setPreview(preview);

        String execParams = toJsonQuietly(buildExecParams("CREATE_ALBUM", ctx, albumName, desc, fileIds, null));
        String actionId = pendingActionService.createPendingAction(
                ctx.getUserId(), ctx.getSessionId(), "CREATE_ALBUM", execParams, vo);
        vo.setActionId(actionId);
        ctx.pushEvent("card", vo);
        log.info("[AI][AgentTools] 生成创建相册草稿 actionId={}, name={}, photos={}", actionId, albumName, fileIds.size());
        return "已生成相册「" + albumName + "」的创建请求（含 " + fileIds.size() + " 张照片），"
                + "等待用户在界面确认后才会执行。请在最终答复中提醒用户确认。";
    }

    /**
     * 向已有相册添加照片（草稿）：支持按相册名称或ID定位。
     */
    @Tool("向已存在的相册添加指定照片。此操作先生成待确认卡片，用户在界面确认后才会真正执行")
    public String addPicturesToAlbum(@P("目标相册名称或相册ID") String albumNameOrId,
                                     @P("照片fileId列表，JSON数组字符串。若用用户选中的照片或最近查询到的照片，传[]即可自动使用") String photoFileIdsJson) {
        AgentToolContext ctx = AgentToolContext.current();
        if (albumNameOrId == null || albumNameOrId.isBlank()) {
            return "未指定目标相册，可先调用 listAlbums 查看相册列表。";
        }
        Album album = resolveAlbum(albumNameOrId, ctx.getUserId());
        if (album == null) {
            return "未找到相册「" + albumNameOrId + "」，可先调用 listAlbums 查看用户的相册列表。";
        }
        List<String> fileIds = resolvePhotoIds(ctx, photoFileIdsJson);
        if (fileIds.isEmpty()) {
            return "照片列表为空，未生成操作。请先调用 searchPhotos 查询照片。";
        }
        validatePhotoOwnership(fileIds);

        PendingActionVO vo = new PendingActionVO();
        vo.setActionType("ADD_PICTURES");
        vo.setTitle("添加照片到相册");
        vo.setDescription("即将向相册「" + album.getAlbumName() + "」添加 " + fileIds.size() + " 张照片");
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("albumId", album.getAlbumId());
        preview.put("albumName", album.getAlbumName());
        preview.put("count", fileIds.size());
        preview.put("thumbnails", loadThumbnails(fileIds));
        vo.setPreview(preview);

        String execParams = toJsonQuietly(buildExecParams("ADD_PICTURES", ctx, null, null, fileIds, album.getAlbumId()));
        String actionId = pendingActionService.createPendingAction(
                ctx.getUserId(), ctx.getSessionId(), "ADD_PICTURES", execParams, vo);
        vo.setActionId(actionId);
        ctx.pushEvent("card", vo);
        log.info("[AI][AgentTools] 生成添加照片草稿 actionId={}, albumId={}, photos={}",
                actionId, album.getAlbumId(), fileIds.size());
        return "已生成向相册「" + album.getAlbumName() + "」添加 " + fileIds.size()
                + " 张照片的请求，等待用户在界面确认后执行。请在最终答复中提醒用户确认。";
    }

    /**
     * 更新相册信息（草稿）：改名/改描述，留空表示保持原值。
     */
    @Tool("更新已有相册的名称或描述，参数留空字符串表示保持原值不变。此操作先生成待确认卡片，用户在界面确认后才会真正执行")
    public String updateAlbumInfo(@P("目标相册名称或相册ID") String albumNameOrId,
                                  @P("新相册名称，不修改时传空字符串") String newAlbumName,
                                  @P("新相册描述，不修改时传空字符串") String newDescription) {
        AgentToolContext ctx = AgentToolContext.current();
        Album album = resolveAlbum(albumNameOrId, ctx.getUserId());
        if (album == null) {
            return "未找到相册「" + albumNameOrId + "」，可先调用 listAlbums 查看用户的相册列表。";
        }
        String name = (newAlbumName == null || newAlbumName.isBlank())
                ? album.getAlbumName() : newAlbumName.trim();
        String desc = (newDescription == null || newDescription.isBlank())
                ? (album.getDescription() == null ? "" : album.getDescription())
                : newDescription.trim();
        if (name.length() > 30) {
            return "新相册名称超过30字，请缩短后重试。";
        }
        if (name.equals(album.getAlbumName()) && desc.equals(album.getDescription() == null ? "" : album.getDescription())) {
            return "新名称和新描述与当前值相同，无需修改。";
        }

        PendingActionVO vo = new PendingActionVO();
        vo.setActionType("UPDATE_ALBUM");
        vo.setTitle("修改相册信息");
        vo.setDescription("即将修改相册「" + album.getAlbumName() + "」："
                + (name.equals(album.getAlbumName()) ? "名称不变" : "名称 →「" + name + "」")
                + (desc.equals(album.getDescription() == null ? "" : album.getDescription())
                        ? "，描述不变" : "，描述 →「" + desc + "」"));
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("albumId", album.getAlbumId());
        preview.put("albumName", album.getAlbumName());
        preview.put("oldName", album.getAlbumName());
        preview.put("newName", name);
        preview.put("oldDescription", album.getDescription());
        preview.put("newDescription", desc);
        vo.setPreview(preview);

        String execParams = toJsonQuietly(buildExecParams("UPDATE_ALBUM", ctx, name, desc, List.of(), album.getAlbumId()));
        String actionId = pendingActionService.createPendingAction(
                ctx.getUserId(), ctx.getSessionId(), "UPDATE_ALBUM", execParams, vo);
        vo.setActionId(actionId);
        ctx.pushEvent("card", vo);
        log.info("[AI][AgentTools] 生成修改相册草稿 actionId={}, albumId={}", actionId, album.getAlbumId());
        return "已生成修改相册「" + album.getAlbumName() + "」信息的请求，等待用户在界面确认后执行。"
                + "请在最终答复中提醒用户确认。";
    }

    /**
     * 删除相册（草稿）：危险操作，预览中明示是否连带删除照片。
     */
    @Tool("删除相册。此操作先生成待确认卡片，用户在界面确认后才会真正执行。deletePicturesToo 为 true 时相册内照片会一并移入回收站，需向用户明确提示后果")
    public String deleteAlbum(@P("目标相册名称或相册ID") String albumNameOrId,
                              @P("是否同时删除相册内照片：true/false") boolean deletePicturesToo) {
        AgentToolContext ctx = AgentToolContext.current();
        Album album = resolveAlbum(albumNameOrId, ctx.getUserId());
        if (album == null) {
            return "未找到相册「" + albumNameOrId + "」，可先调用 listAlbums 查看用户的相册列表。";
        }

        // 危险操作守卫：把影响范围（多少张照片）算出来明示。
        // 删除的危险性此前只靠提示词约束（软约束，模型抽风就没了），
        // 改为在工具入口强制计算并写进确认卡片，用户看到的一定是真实数字。
        int photoCount = countAlbumPictures(album.getAlbumId(), ctx.getUserId());

        PendingActionVO vo = new PendingActionVO();
        vo.setActionType("DELETE_ALBUM");
        vo.setTitle("删除相册");
        vo.setDescription("即将删除相册「" + album.getAlbumName() + "」"
                + (deletePicturesToo
                    ? "，连同相册内 " + photoCount + " 张照片一并移入回收站"
                    : "，相册内 " + photoCount + " 张照片不会被删除（仅删除相册本身）"));
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("albumId", album.getAlbumId());
        preview.put("albumName", album.getAlbumName());
        preview.put("photoCount", photoCount);
        preview.put("deletePicturesToo", deletePicturesToo);
        preview.put("deleteWarning", deletePicturesToo
                ? (photoCount > 0
                    ? "危险：该相册内 " + photoCount + " 张照片将一并移入回收站"
                    : "相册内没有照片，仅删除空相册")
                : "仅删除相册，" + photoCount + " 张照片不会被删除");
        vo.setPreview(preview);

        Map<String, Object> params = buildExecParams("DELETE_ALBUM", ctx, album.getAlbumName(), null,
                List.of(), album.getAlbumId());
        params.put("isDeletePicture", deletePicturesToo);
        String execParams = toJsonQuietly(params);
        String actionId = pendingActionService.createPendingAction(
                ctx.getUserId(), ctx.getSessionId(), "DELETE_ALBUM", execParams, vo);
        vo.setActionId(actionId);
        ctx.pushEvent("card", vo);
        log.info("[AI][AgentTools] 生成删除相册草稿 actionId={}, albumId={}, deletePictures={}",
                actionId, album.getAlbumId(), deletePicturesToo);
        return "已生成删除相册「" + album.getAlbumName() + "」的请求"
                + (deletePicturesToo ? "（含删除照片）" : "") + "，等待用户在界面确认后执行。"
                + "这是危险操作，请在最终答复中重点提醒用户确认。";
    }

    /**
     * 从相册移除照片（草稿）：仅解除关联，照片本体不删除。
     */
    @Tool("从指定相册移除照片，仅解除照片与相册的关联，照片本身不会被删除。此操作先生成待确认卡片，用户在界面确认后才会真正执行")
    public String removePicturesFromAlbum(@P("目标相册名称或相册ID") String albumNameOrId,
                                          @P("要移除的照片fileId列表，JSON数组字符串") String photoFileIdsJson) {
        AgentToolContext ctx = AgentToolContext.current();
        Album album = resolveAlbum(albumNameOrId, ctx.getUserId());
        if (album == null) {
            return "未找到相册「" + albumNameOrId + "」，可先调用 listAlbums 查看用户的相册列表。";
        }
        List<String> fileIds = parseFileIds(photoFileIdsJson);
        if (fileIds.isEmpty()) {
            return "照片列表为空，未生成操作。";
        }
        validatePhotoOwnership(fileIds);

        PendingActionVO vo = new PendingActionVO();
        vo.setActionType("REMOVE_PICTURES");
        vo.setTitle("从相册移除照片");
        vo.setDescription("即将从相册「" + album.getAlbumName() + "」移除 " + fileIds.size()
                + " 张照片（照片本身不会被删除）");
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("albumId", album.getAlbumId());
        preview.put("albumName", album.getAlbumName());
        preview.put("count", fileIds.size());
        preview.put("thumbnails", loadThumbnails(fileIds));
        vo.setPreview(preview);

        String execParams = toJsonQuietly(buildExecParams("REMOVE_PICTURES", ctx, null, null, fileIds, album.getAlbumId()));
        String actionId = pendingActionService.createPendingAction(
                ctx.getUserId(), ctx.getSessionId(), "REMOVE_PICTURES", execParams, vo);
        vo.setActionId(actionId);
        ctx.pushEvent("card", vo);
        log.info("[AI][AgentTools] 生成移除照片草稿 actionId={}, albumId={}, photos={}",
                actionId, album.getAlbumId(), fileIds.size());
        return "已生成从相册「" + album.getAlbumName() + "」移除 " + fileIds.size()
                + " 张照片的请求，等待用户在界面确认后执行。请在最终答复中提醒用户确认。";
    }

    // ==================== 网页生成子 Agent ====================

    /**
     * 网页生成子 Agent：基于用户选中照片 + 创意需求生成 HTML。
     */
    @Tool("根据照片与创意需求生成一个HTML展示网页并展示给用户")
    public String generateHtmlPage(
            @P("网页的创意需求描述（如风格、标题、布局）") String requirement,
            @P("照片fileId列表，JSON数组字符串（如[\"id1\",\"id2\"]）。优先传系统提示中给出的「用户选中的照片」或「最近查询到的照片」ID；若用户就是要用他选中的照片，传[]即可自动使用") String fileIdsJson) {
        AgentToolContext ctx = AgentToolContext.current();
        // 统一走解析链：显式 ID → 句柄 → 焦点工件 → 勾选 → 并集 → 会话历史
        List<String> fileIds = resolvePhotoIds(ctx, fileIdsJson);

        // 容错：模型常把 fileId 列表写进需求文本而非独立参数（如"照片fileId列表为：[\"id1\",...]"），
        // 此时从文本中提取，避免走到"无照片"分支导致网页里没有真实图片
        if (fileIds.isEmpty() && requirement != null && !requirement.isBlank()) {
            fileIds = extractFileIdsFromText(requirement);
            if (!fileIds.isEmpty()) {
                log.info("[AI][AgentTools] 从需求文本中回提取出 {} 个 fileId", fileIds.size());
            }
        }

        if (!fileIds.isEmpty()) {
            validatePhotoOwnership(fileIds);
        }
        log.info("[AI][AgentTools] 生成网页：fileIdsJson={}, 需求文本提取={}, 最终照片数={}",
                fileIdsJson == null ? "null" : fileIdsJson.substring(0, Math.min(60, fileIdsJson.length())),
                requirement == null ? "" : requirement.substring(0, Math.min(60, requirement.length())),
                fileIds.size());

        String html = webpageTools.generateHtml(fileIds, requirement, ctx.getUserId());
        ctx.pushEvent("html", html);
        return "网页已生成并展示给用户（含 " + fileIds.size() + " 张照片）。"
                + (fileIds.isEmpty() ? "注意：未指定任何照片，网页中不含真实照片，只有占位排版。" : "");
    }

    /**
     * 解析"照片来源"：优先用显式参数，否则按上下文优先级回退。
     *
     * <p>回退顺序：显式 ID → 工件句柄 → <b>焦点工件</b>（最近一次查询）→ 界面勾选
     * → 本轮并集（兜底）→ 会话历史。
     *
     * <p><b>为什么焦点优先于并集</b>：并集会把「查上海照片」与「查家人相册」的结果
     * 混在一起，用户说「用这些照片」时拿到的是混合集合，语义错误且没有任何一方能察觉。
     * 焦点工件只指向最近一次查询，符合语言直觉；连焦点都没有时才退到并集兜底。
     */
    private List<String> resolvePhotoIds(AgentToolContext ctx, String fileIdsJson) {
        List<String> ids = parseFileIds(fileIdsJson);
        if (!ids.isEmpty()) {
            return ids;
        }
        String raw = fileIdsJson == null ? "" : fileIdsJson.trim();
        // 句柄引用（photos#1）：模型显式指定某一批
        if (raw.contains("#")) {
            Artifact hit = ctx.resolveArtifact(raw);
            if (hit == null) {
                // 引用了不存在的工件：回传可用清单，让模型自我修正后重试
                throw new BusinessException(StatusCode.PARAMS_ERROR,
                        "引用的照片集 " + raw + " 不存在，当前可用：" + ctx.describeArtifacts());
            }
            log.info("[AI][AgentTools] 引用工件 {}（{} 张）", hit.getRef(), hit.size());
            return hit.getFileIds();
        }
        Artifact focus = ctx.getFocusArtifact();
        if (focus != null) {
            log.info("[AI][AgentTools] 使用焦点工件 {}（{} 张）", focus.getRef(), focus.size());
            return focus.getFileIds();
        }
        if (ctx.getContextFileIds() != null && !ctx.getContextFileIds().isEmpty()) {
            return ctx.getContextFileIds();
        }
        if (!ctx.getQueriedPhotoIds().isEmpty()) {
            return ctx.getQueriedPhotoIds();
        }
        if (ctx.getRecentPhotoIds() != null && !ctx.getRecentPhotoIds().isEmpty()) {
            log.info("[AI][AgentTools] 复用会话历史中的 {} 张照片", ctx.getRecentPhotoIds().size());
            return ctx.getRecentPhotoIds();
        }
        return List.of();
    }

    /** 摘要截断（工件摘要只给模型看，过长会浪费 token） */
    private String abbreviate(String s, int max) {
        if (s == null || s.isBlank()) {
            return "全部照片";
        }
        String t = s.trim().replaceAll("\\s+", " ");
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    /**
     * 从任意文本中提取 fileId（32 位十六进制，带或不带连字符均可）。
     * 用于容错模型把 ID 列表写在描述文本里而非独立参数的情况。
     */
    private List<String> extractFileIdsFromText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}")
                .matcher(text);
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        while (m.find() && ids.size() < 100) {
            ids.add(m.group().toLowerCase());
        }
        return new ArrayList<>(ids);
    }

    // ==================== 内部工具方法 ====================

    /** 解析模型传入的照片ID列表（JSON数组字符串，容错单个裸ID） */
    private List<String> parseFileIds(String photoFileIdsJson) {
        if (photoFileIdsJson == null || photoFileIdsJson.isBlank()) {
            return List.of();
        }
        String t = photoFileIdsJson.trim();
        try {
            if (t.startsWith("[")) {
                List<Object> raw = objectMapper.readValue(t, new TypeReference<>() {});
                return raw.stream()
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
            // 容错：模型直接传单个ID
            return List.of(t.replaceAll("^\"|\"$", ""));
        } catch (Exception e) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "照片ID列表格式不正确，应为JSON数组字符串");
        }
    }

    /** 校验照片归属当前用户（防模型幻觉出不存在的 fileId 直接写库） */
    private void validatePhotoOwnership(List<String> fileIds) {
        if (fileIds.isEmpty()) {
            return;
        }
        for (String id : fileIds) {
            if (id == null || !id.matches("[0-9a-zA-Z-]{1,64}")) {
                throw new BusinessException(StatusCode.PARAMS_ERROR, "照片ID不合法: " + id);
            }
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(fileIds.size(), "?"));
        List<Object> args = new ArrayList<>();
        args.add(AgentToolContext.current().getUserId());
        args.addAll(fileIds);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_file WHERE user_id = ? AND is_deleted = 0 AND file_id IN ("
                        + placeholders + ")",
                Integer.class, args.toArray());
        if (count == null || count != fileIds.size()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR,
                    "部分照片不存在或不属于当前用户，请先通过查询工具获取真实的照片ID，不要编造");
        }
    }

    /** 解析相册：纯数字按ID定位，否则按名称模糊匹配；强制 user_id 归属条件 */
    private Album resolveAlbum(String albumNameOrId, Long userId) {
        QueryWrapper<Album> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        if (albumNameOrId.matches("\\d+")) {
            qw.eq("album_id", Long.valueOf(albumNameOrId));
        } else {
            qw.like("album_name", albumNameOrId.trim());
        }
        qw.last("LIMIT 1");
        return albumMapper.selectOne(qw);
    }

    /** 统计相册内照片数（危险操作前明示影响范围；查询失败按 0 处理，不阻断流程） */
    private int countAlbumPictures(Long albumId, Long userId) {
        try {
            Integer n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM album_picture WHERE album_id = ? AND user_id = ?",
                    Integer.class, albumId, userId);
            return n == null ? 0 : n;
        } catch (Exception e) {
            log.warn("[AI][AgentTools] 统计相册照片数失败 albumId={}: {}", albumId, e.getMessage());
            return 0;
        }
    }

    /** 批量加载照片缩略图（最多6张，供确认卡片展示） */
    private List<String> loadThumbnails(List<String> fileIds) {
        if (fileIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(fileIds.size(), "?"));
        List<String> urls = jdbcTemplate.queryForList(
                "SELECT thumbnail_url FROM file WHERE file_id IN (" + placeholders + ")",
                String.class, fileIds.toArray());
        return urls.stream()
                .filter(u -> u != null && !u.isBlank())
                .limit(6)
                .toList();
    }

    /**
     * 照片下载（读）：按 fileId 生成下载清单，推送 download 事件，前端渲染下载按钮。
     *
     * <p>URL 新鲜度：MinIO 预签名 URL 会过期，优先用 file_object_name 重新签名，
     * 失败时回退 DB 中已存的 file_url（与 WebpageTools 的 resolveImageUrl 同策略）。
     */
    @Tool("为指定照片生成下载卡片展示给用户，用户点击按钮即可把照片保存到本地。"
            + "用于「下载这些照片」「把照片保存到本地」「导出照片」类需求；"
            + "照片来自用户选中的照片或最近查询结果时 fileIdsJson 传[]即可自动使用")
    public String downloadPhotos(
            @P("照片fileId列表，JSON数组字符串。若用用户选中的照片或最近查询到的照片，传[]即可自动使用") String fileIdsJson) {
        AgentToolContext ctx = AgentToolContext.current();
        List<String> fileIds = resolvePhotoIds(ctx, fileIdsJson);
        if (fileIds.isEmpty()) {
            return "未指定照片且上下文中没有可用照片。请先调用 searchPhotos 查询照片，"
                    + "或让用户在界面选中照片后再下载。";
        }
        validatePhotoOwnership(fileIds);

        String placeholders = String.join(",", java.util.Collections.nCopies(fileIds.size(), "?"));
        List<Object> args = new ArrayList<>(fileIds);
        args.add(ctx.getUserId());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT f.file_id, f.origin_file_name "
                        + "FROM file f "
                        + "JOIN user_file uf ON uf.file_id = f.file_id "
                        + "WHERE f.file_id IN (" + placeholders + ") AND uf.user_id = ? AND uf.is_deleted = 0",
                args.toArray());
        if (rows.isEmpty()) {
            return "未找到可下载的照片。";
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fileName", String.valueOf(row.getOrDefault("origin_file_name", "photo")));
            item.put("fileId", String.valueOf(row.get("file_id")));
            items.add(item);
        }
        if (items.isEmpty()) {
            return "这些照片没有可用信息。";
        }
        ctx.pushEvent("download", items);
        return "已生成 " + items.size() + " 张照片的下载清单并展示给用户（含打包下载按钮，点击将请求服务端打包 ZIP）,"
                + "请在最终答复中提醒用户点击下载按钮保存照片。";
    }

    /** 拼接省市区为可读地点（字段存在才拼） */
    private String joinLocation(Map<String, Object> row) {
        StringBuilder loc = new StringBuilder();
        appendIfPresent(loc, row, "province", "", "");
        appendIfPresent(loc, row, "city", " ", "");
        appendIfPresent(loc, row, "district", " ", "");
        return loc.toString().trim();
    }

    /** 拼接非空字段：值存在且非空时追加「label + 值 + sep」，否则跳过 */
    private void appendIfPresent(StringBuilder sb, Map<String, Object> row, String key, String label, String sep) {
        Object v = row.get(key);
        if (v == null || String.valueOf(v).isBlank()) {
            return;
        }
        sb.append(label).append(v).append(sep);
    }

    /** 生成确认后真正执行的参数 JSON（AiServiceImpl.execute 消费） */
    private Map<String, Object> buildExecParams(String actionType, AgentToolContext ctx,
                                                String albumName, String description,
                                                List<String> fileIds, Long albumId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("actionType", actionType);
        params.put("userId", ctx.getUserId());
        if (albumName != null) {
            params.put("albumName", albumName);
        }
        if (description != null) {
            params.put("description", description);
        }
        params.put("fileIds", fileIds);
        if (albumId != null) {
            params.put("albumId", albumId);
        }
        return params;
    }

    private String toJsonQuietly(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "[]".equals(String.valueOf(o)) ? "[]" : "{}";
        }
    }
}
