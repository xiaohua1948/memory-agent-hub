package com.memory.xiaohua.ai.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memory.xiaohua.ai.action.PendingActionService;
import com.memory.xiaohua.ai.agent.AgentContext;
import com.memory.xiaohua.ai.agent.MainAgent;
import com.memory.xiaohua.ai.config.AiProperties;
import com.memory.xiaohua.ai.memory.AiChatMemoryFactory;
import com.memory.xiaohua.ai.service.AiService;
import com.memory.xiaohua.ai.tool.AlbumTools;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.mapper.AiMessageMapper;
import com.memory.xiaohua.mapper.AiSessionMapper;
import com.memory.xiaohua.mapper.FileMapper;
import com.memory.xiaohua.model.dto.ai.AiChatRequest;
import com.memory.xiaohua.model.dto.ai.AiCreateSessionRequest;
import com.memory.xiaohua.model.entity.AiMessage;
import com.memory.xiaohua.model.entity.AiSession;
import com.memory.xiaohua.model.entity.FileEntity;
import com.memory.xiaohua.model.vo.ai.AiMessageVO;
import com.memory.xiaohua.model.vo.ai.AiSessionVO;
import com.memory.xiaohua.utils.file.MinioOSSUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.ZipOutputStream;

/**
 * AI 智能助手服务实现
 *
 * <p>记忆分层（对齐《AI智能助手-优化计划.md》§4）：
 * Redis（运行时记忆，MessageWindowChatMemory 自动读写）+ DB（持久真相源，跨 Redis 过期可回灌）。
 *
 * @author xiaohua
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final AiSessionMapper sessionMapper;
    private final AiMessageMapper messageMapper;
    private final FileMapper fileMapper;
    private final MainAgent mainAgent;
    private final PendingActionService pendingActionService;
    private final AlbumTools albumTools;
    private final AiChatMemoryFactory memoryFactory;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    /** createSession 写入的默认标题（命中则触发标题生成） */
    private static final String DEFAULT_SESSION_TITLE = "新对话";

    /** 单次打包下载的照片数量上限（防滥用） */
    private static final int MAX_DOWNLOAD_FILES = 200;

    /** 对象存储工具（打包下载用；未装配时报错提示） */
    @Autowired(required = false)
    private MinioOSSUtil minioOSSUtil;

    /** 轻量 router 模型：用于会话标题生成（未配置时兜底截取前12字） */
    @Autowired(required = false)
    @Qualifier("routerModel")
    private ChatModel routerModel;

    public AiServiceImpl(AiSessionMapper sessionMapper,
                         AiMessageMapper messageMapper,
                         FileMapper fileMapper,
                         MainAgent mainAgent,
                         PendingActionService pendingActionService,
                         AlbumTools albumTools,
                         AiChatMemoryFactory memoryFactory,
                         AiProperties properties,
                         ObjectMapper objectMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.fileMapper = fileMapper;
        this.mainAgent = mainAgent;
        this.pendingActionService = pendingActionService;
        this.albumTools = albumTools;
        this.memoryFactory = memoryFactory;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSessionVO createSession(Long userId, AiCreateSessionRequest request) {
        // 任务路由已统一由主 Agent（MainAgent）基于工具判别，agent_type 固定 mixed，
        // 不再区分 data/code 会话类型（字段保留仅为 DB schema 兼容）
        AiSession session = new AiSession();
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(userId);
        session.setAgentType("mixed");
        session.setTitle("新对话");
        session.setStatus(0);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);
        return toSessionVO(session);
    }

    @Override
    public List<AiSessionVO> listSessions(Long userId) {
        List<AiSession> sessions = sessionMapper.selectByUserId(userId);
        List<AiSessionVO> vos = new ArrayList<>();
        for (AiSession s : sessions) {
            vos.add(toSessionVO(s));
        }
        return vos;
    }

    @Override
    public List<AiMessageVO> listMessages(Long userId, String sessionId) {
        checkSessionOwner(sessionId, userId);
        List<AiMessage> messages = messageMapper.selectBySessionId(sessionId);
        List<AiMessageVO> vos = new ArrayList<>();
        for (AiMessage m : messages) {
            vos.add(toMessageVO(m));
        }
        return vos;
    }

    /**
     * 删除会话：软删会话 + 软删消息 + 清理 Redis 运行时记忆。
     *
     * <p>软删（deleted=1）保留审计痕迹；Redis 记忆必须同步清除，
     * 否则同一 memoryId 复用时会残留已删会话的历史。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long userId, String sessionId) {
        checkSessionOwner(sessionId, userId);
        if (sessionMapper.markDeleted(sessionId, userId) == 0) {
            throw new BusinessException(StatusCode.NOT_FOUND_ERROR, "会话不存在或已删除");
        }
        messageMapper.markDeletedBySessionId(sessionId);
        memoryFactory.clearMemory(sessionId);
        log.info("[AI] 会话已删除 sessionId={}, userId={}", sessionId, userId);
    }

    @Override
    public String chat(Long userId, AiChatRequest request, Consumer<Map<String, Object>> sink) {
        if (!properties.isEnabled()) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", "text");
            msg.put("data", "AI 服务未启用，请在 application.yml 配置 ai.api-key 后重试。");
            sink.accept(msg);
            return request.getSessionId();
        }

        String sessionId = resolveSession(userId, request);
        String userInput = request.getContent();
        if (userInput == null || userInput.isBlank()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "消息不能为空");
        }

        // 1. 装配记忆（空则从 DB 回灌最近 N 条）
        MessageWindowChatMemory memory = memoryFactory.buildMemory(sessionId);

        // 2. 提取历史上下文（此时不含本轮输入，避免与 userInput 重复）
        String history = buildHistoryText(memory);

        // 3. 持久化用户消息（content 纯文本，供记忆回灌）
        saveMessage(sessionId, userId, "user", "text", userInput, null);

        // 4. 记忆追加本轮用户消息（LangChain4j 自动写入 Redis store）
        memory.add(UserMessage.from(userInput));

        // 5. 编排执行（流式回调）
        AgentContext context = new AgentContext();
        context.setUserId(userId);
        context.setSessionId(sessionId);
        context.setUserInput(userInput);
        context.setHistory(history);
        context.setContextFileIds(request.getFileIds());
        // 多轮上下文：取本会话最近一次查询出的照片，供后续工具直接复用（无需重查）
        context.setRecentPhotoIds(loadRecentPhotoIds(messageMapper, sessionId));

        final List<Map<String, Object>> produced = new ArrayList<>();
        mainAgent.execute(context, event -> {
            produced.add(event);            // 原始事件（对象形式）：供下方持久化
            sink.accept(toSseEvent(event)); // 字符串化事件：SSE 传输确定性 JSON
        });

        // 6. 持久化助手产出：content 纯文本 + extra 结构化（HTML/卡片不进记忆）
        List<Map<String, Object>> stepEvents = new ArrayList<>();
        for (Map<String, Object> event : produced) {
            String type = String.valueOf(event.getOrDefault("type", "text"));
            if ("step".equals(type)) {
                // 步骤事件汇总为一条「执行步骤」消息，不逐条落库
                stepEvents.add(event);
                continue;
            }
            if ("tool".equals(type)) {
                // 工具调用进度事件：仅前端展示，不落库、不进记忆
                continue;
            }
            String contentType;
            String content;
            String extra = null;
            switch (type) {
                case "card" -> {
                    contentType = "pending_action";
                    content = "待确认操作";
                    extra = toJsonQuietly(event.get("data"));
                }
                case "html" -> {
                    contentType = "html";
                    content = "已生成网页";
                    // extra 列为 MySQL JSON 类型：HTML 原文不是合法 JSON，必须包装成 JSON 对象存储
                    extra = toJsonQuietly(Map.of("html", String.valueOf(event.getOrDefault("data", ""))));
                }
                case "photos" -> {
                    contentType = "photos";
                    Object data = event.get("data");
                    content = (data instanceof List<?> l)
                            ? ("查询到 " + l.size() + " 张照片")
                            : "查询结果";
                    extra = toJsonQuietly(data);
                }
                case "download" -> {
                    contentType = "download";
                    Object data = event.get("data");
                    content = (data instanceof List<?> l)
                            ? ("下载 " + l.size() + " 张照片")
                            : "下载清单";
                    extra = toJsonQuietly(data);
                }
                case "table" -> {
                    contentType = "table";
                    Object data = event.get("data");
                    content = describeTablePayload(data);
                    extra = toJsonQuietly(data);
                }
                case "metric" -> {
                    contentType = "metric";
                    Object data = event.get("data");
                    content = describeMetricPayload(data);
                    extra = toJsonQuietly(data);
                }
                default -> {
                    contentType = "text";
                    content = String.valueOf(event.getOrDefault("data", ""));
                }
            }
            saveMessage(sessionId, userId, "assistant", contentType, content, extra);
            // 记忆只追加纯文本，避免 HTML/卡片污染上下文与 token 浪费
            memory.add(dev.langchain4j.data.message.AiMessage.from(content));
        }

        // 6.1 执行步骤汇总落一条：供历史回看与前端渲染，不进对话记忆（过程信息会污染上下文）
        if (!stepEvents.isEmpty()) {
            saveMessage(sessionId, userId, "assistant", "steps",
                    "已完成 " + countFinishedSteps(stepEvents) + " 个步骤", toJsonQuietly(stepEvents));
        }

        // 7. 刷新会话更新时间（不覆盖标题）
        sessionMapper.touch(sessionId, userId);

        // 8. 首轮对话后生成会话标题（仅在默认标题时触发；同步执行保证流关闭前送达）
        maybeGenerateTitle(sessionId, userId, userInput, sink);

        return sessionId;
    }

    @Override
    public Map<String, Object> execute(Long userId, String actionId) {
        // 1. 原子确认（Redis Lua 幂等 + 状态迁移）
        String execParamsJson = pendingActionService.confirm(actionId, userId);

        // 2. 解析执行参数并真正执行写操作
        try {
            Map<String, Object> params = objectMapper.readValue(execParamsJson, Map.class);
            String actionType = String.valueOf(params.getOrDefault("actionType", ""));
            Map<String, Object> result = new LinkedHashMap<>();

            switch (actionType) {
                case "CREATE_ALBUM" -> {
                    String albumName = asString(params.get("albumName"));
                    if (albumName == null) {
                        // 兼容旧版草稿（仅含自然语言 input）
                        albumName = extractAlbumName(String.valueOf(params.getOrDefault("input", "")));
                    }
                    String description = asString(params.get("description"));
                    Long albumId = albumTools.createAlbum(albumName, description, userId);
                    result.put("albumId", albumId);
                    result.put("albumName", albumName);
                    List<String> fileIds = asStringList(params.get("fileIds"));
                    if (!fileIds.isEmpty()) {
                        albumTools.addPicturesToAlbum(fileIds, albumId, userId);
                    }
                    result.put("message", "相册「" + albumName + "」创建成功"
                            + (fileIds.isEmpty() ? "" : "，已放入 " + fileIds.size() + " 张照片"));
                }
                case "ADD_PICTURES" -> {
                    Object albumIdObj = params.get("albumId");
                    List<String> fileIds = asStringList(params.get("fileIds"));
                    if (albumIdObj == null || fileIds.isEmpty()) {
                        result.put("message", "操作已确认，但缺少目标相册或照片参数");
                    } else {
                        Long albumId = ((Number) albumIdObj).longValue();
                        albumTools.addPicturesToAlbum(fileIds, albumId, userId);
                        result.put("albumId", albumId);
                        result.put("message", "已将 " + fileIds.size() + " 张照片添加到相册");
                    }
                }
                case "UPDATE_ALBUM" -> {
                    Object albumIdObj = params.get("albumId");
                    if (albumIdObj == null) {
                        result.put("message", "操作已确认，但缺少目标相册参数");
                    } else {
                        Long albumId = ((Number) albumIdObj).longValue();
                        String newName = asString(params.get("albumName"));
                        String newDesc = asString(params.get("description"));
                        albumTools.updateAlbumInfo(albumId, newName, newDesc, userId);
                        result.put("albumId", albumId);
                        result.put("message", "相册信息已更新"
                                + (newName != null ? "，名称：「" + newName + "」" : ""));
                    }
                }
                case "DELETE_ALBUM" -> {
                    Object albumIdObj = params.get("albumId");
                    if (albumIdObj == null) {
                        result.put("message", "操作已确认，但缺少目标相册参数");
                    } else {
                        Long albumId = ((Number) albumIdObj).longValue();
                        boolean isDeletePicture = Boolean.TRUE.equals(params.get("isDeletePicture"));
                        albumTools.deleteAlbum(albumId, isDeletePicture, userId);
                        result.put("albumId", albumId);
                        result.put("message", "相册已删除"
                                + (isDeletePicture ? "，相册内照片已移入回收站" : "，相册内照片未受影响"));
                    }
                }
                case "REMOVE_PICTURES" -> {
                    Object albumIdObj = params.get("albumId");
                    List<String> fileIds = asStringList(params.get("fileIds"));
                    if (albumIdObj == null || fileIds.isEmpty()) {
                        result.put("message", "操作已确认，但缺少目标相册或照片参数");
                    } else {
                        Long albumId = ((Number) albumIdObj).longValue();
                        albumTools.removePicturesFromAlbum(fileIds, albumId, userId);
                        result.put("albumId", albumId);
                        result.put("message", "已从相册移除 " + fileIds.size() + " 张照片");
                    }
                }
                default -> {
                    result.put("message", "操作已执行: " + actionType);
                }
            }
            result.put("actionId", actionId);
            // 回写对话：此前执行结果只返回给前端，对话历史永远停在「请在界面确认」，
            // 导致用户下一轮说「把刚才那个相册改个名」时，模型无从知道相册是否真的建成了。
            writeBackConfirmation(actionId, userId, result);
            return result;
        } catch (Exception e) {
            log.error("[AI] 执行确认操作失败 actionId={}", actionId, e);
            throw new BusinessException(StatusCode.OPERATION_ERROR, "执行失败: " + e.getMessage());
        }
    }

    /**
     * 把写操作的真实执行结果写回会话（DB 消息 + 运行时记忆）。
     *
     * <p>这是 HITL 闭环的最后一环：确认发生在独立 HTTP 请求里，
     * 若不回写，主 Agent 在此后的每一轮都看不到这次操作的结果。
     */
    private void writeBackConfirmation(String actionId, Long userId, Map<String, Object> result) {
        try {
            String sessionId = pendingActionService.getSessionId(actionId);
            if (sessionId == null) {
                return;
            }
            String text = String.valueOf(result.getOrDefault("message", "操作已完成"));
            saveMessage(sessionId, userId, "assistant", "text", text, null);
            memoryFactory.buildMemory(sessionId)
                    .add(dev.langchain4j.data.message.AiMessage.from(text));
            log.info("[AI] 写操作结果已回写会话 sessionId={}, actionId={}", sessionId, actionId);
        } catch (Exception e) {
            // 回写失败不能影响已成功执行的写操作，仅记录日志
            log.warn("[AI] 写操作结果回写失败 actionId={}: {}", actionId, e.getMessage());
        }
    }

    // ==================== 内部工具方法 ====================

    private String resolveSession(Long userId, AiChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            AiCreateSessionRequest req = new AiCreateSessionRequest();
            req.setAgentType(request.getAgentType());
            return createSession(userId, req).getSessionId();
        }
        checkSessionOwner(sessionId, userId);
        return sessionId;
    }

    private void checkSessionOwner(String sessionId, Long userId) {
        AiSession session = sessionMapper.selectBySessionId(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(StatusCode.NOT_FOUND_ERROR, "会话不存在或无权限");
        }
    }

    private void saveMessage(String sessionId, Long userId, String role, String contentType,
                             String content, String extra) {
        AiMessage m = new AiMessage();
        m.setSessionId(sessionId);
        m.setUserId(userId);
        m.setRole(role);
        m.setContentType(contentType);
        m.setContent(content);
        m.setExtra(extra);
        m.setCreateTime(LocalDateTime.now());
        messageMapper.insert(m);
    }

    /** 记忆消息 → 历史对话文本（仅 user / assistant 纯文本） */
    private String buildHistoryText(MessageWindowChatMemory memory) {
        List<ChatMessage> messages = memory.messages();
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : messages) {
            if (m instanceof UserMessage um) {
                sb.append("用户: ").append(um.singleText()).append("\n");
            } else if (m instanceof dev.langchain4j.data.message.AiMessage am) {
                sb.append("助手: ").append(am.text()).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 从本会话历史消息中提取最近一次查询出的照片 fileId（photos 类型消息的 extra）。
     *
     * <p>目的：用户说「用这些照片生成网页 / 把它们加入相册」时，模型可直接复用上下文结果，
     * 无需再次调用 searchPhotos 重新查询。
     */
    private List<String> loadRecentPhotoIds(AiMessageMapper mapper, String sessionId) {
        try {
            List<AiMessage> messages = mapper.selectBySessionId(sessionId);
            if (messages == null || messages.isEmpty()) {
                return List.of();
            }
            // 倒序找最近一条 photos 消息
            for (int i = messages.size() - 1; i >= 0; i--) {
                AiMessage m = messages.get(i);
                if (m == null || !"photos".equals(m.getContentType()) || m.getExtra() == null) {
                    continue;
                }
                List<Map<String, Object>> photos =
                        objectMapper.readValue(m.getExtra(), new TypeReference<>() {});
                List<String> ids = photos.stream()
                        .map(p -> p.get("fileId"))
                        .filter(java.util.Objects::nonNull)
                        .map(String::valueOf)
                        .toList();
                if (!ids.isEmpty()) {
                    log.info("[AI] 复用会话历史照片 {} 张（来自消息 id={}）", ids.size(), m.getId());
                    return ids;
                }
            }
        } catch (Exception e) {
            log.warn("[AI] 解析历史照片上下文失败（忽略，转为重新查询）: {}", e.getMessage());
        }
        return List.of();
    }

    private String extractAlbumName(String input) {
        String name = input.replaceAll("(创建|新建|一个|名为|叫做|相册)", "").trim();
        return name.isBlank() ? "新相册" : name;
    }

    /** 安全取字符串：null / "null" / 空白 一律返回 null */
    private String asString(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return (s.isEmpty() || "null".equalsIgnoreCase(s)) ? null : s;
    }

    /** 安全取字符串列表（JSON 数组字段） */
    private List<String> asStringList(Object o) {
        if (!(o instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            String s = asString(item);
            if (s != null) {
                result.add(s);
            }
        }
        return result;
    }

    private String toJsonQuietly(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 统计终态步骤数（同一 id 会先 running 再 done/failed，按 id 去重）。
     *
     * <p>waiting_confirm 视为终态：它代表本轮编排已结束，只是等待用户确认。
     */
    @SuppressWarnings("unchecked")
    private int countFinishedSteps(List<Map<String, Object>> stepEvents) {
        java.util.Set<Object> finished = new java.util.HashSet<>();
        for (Map<String, Object> event : stepEvents) {
            Object data = event.get("data");
            if (data instanceof Map<?, ?> d) {
                String status = String.valueOf(d.get("status"));
                if (!"running".equals(status)) {
                    finished.add(d.get("id"));
                }
            }
        }
        return finished.size();
    }

    /**
     * 表格事件 → 进记忆的纯文本摘要。
     *
     * <p>表格原文（含封面 URL）不进对话记忆：一是浪费 token，二是历史回灌时
     * 过期的图片地址会污染上下文。记忆里只需「查到几条」这类结论。
     */
    @SuppressWarnings("unchecked")
    private String describeTablePayload(Object data) {
        int rows = 0;
        String title = "查询结果";
        if (data instanceof Map<?, ?> map) {
            Object r = map.get("rows");
            if (r instanceof List<?> list) {
                rows = list.size();
            }
            Object t = map.get("title");
            if (t != null && !String.valueOf(t).isBlank()) {
                title = String.valueOf(t);
            }
        }
        return rows > 0 ? (title + "：" + rows + " 条记录") : (title + "：无匹配记录");
    }

    /** 指标事件 → 进记忆的纯文本摘要（如「相册数量：12」） */
    @SuppressWarnings("unchecked")
    private String describeMetricPayload(Object data) {
        if (data instanceof Map<?, ?> map) {
            Object label = map.get("label");
            Object value = map.get("value");
            if (label != null && value != null) {
                return label + "：" + value;
            }
        }
        return "查询指标";
    }

    /**
     * 构造 SSE 事件：非字符串 data 统一序列化为 JSON 字符串。
     *
     * <p>动机：SseEmitter 对非 String 的 data 依赖 Spring 运行时选择 converter 序列化，
     * 行为受 converter 注册顺序影响，历史上曾导致前端 JSON.parse 失败、确认卡片静默不渲染。
     * String 类型的 data 由 StringHttpMessageConverter 直写，行为完全确定。
     */
    private Map<String, Object> toSseEvent(Map<String, Object> event) {
        Object data = event.get("data");
        if (data instanceof String || data instanceof byte[]) {
            return event;
        }
        Map<String, Object> sse = new LinkedHashMap<>(event);
        sse.put("data", toJsonQuietly(data));
        return sse;
    }

    /**
     * 照片打包下载：归属校验后从对象存储拉流打包 ZIP，流式写入响应（不落盘）。
     *
     * <p>复用 {@link MinioOSSUtil#downloadFileToZip}（含 zip 内重名自动编号），
     * 响应头/流式刷新与相册打包下载（FileServiceImpl）保持一致。
     */
    @Override
    public void downloadPhotosZip(Long userId, List<String> fileIds, HttpServletResponse response) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "未指定要下载的照片");
        }
        if (fileIds.size() > MAX_DOWNLOAD_FILES) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "单次最多下载 " + MAX_DOWNLOAD_FILES + " 张照片");
        }
        if (minioOSSUtil == null) {
            throw new BusinessException(StatusCode.OPERATION_ERROR, "对象存储未配置，无法打包下载");
        }
        // selectFileByIds 按当前用户过滤，天然防越权；查不到即全部 fileId 非法
        List<FileEntity> files = fileMapper.selectFileByIds(fileIds, userId);
        if (files == null || files.isEmpty()) {
            throw new BusinessException(StatusCode.NOT_FOUND_ERROR, "没有可下载的照片");
        }
        try {
            response.setContentType("application/zip");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Transfer-Encoding", "binary");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            String encodedName = URLEncoder.encode("照片下载.zip", StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);

            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                Map<String, Integer> nameCounter = new HashMap<>();
                for (FileEntity fileEntity : files) {
                    minioOSSUtil.downloadFileToZip(zos, fileEntity, nameCounter);
                    zos.flush();
                    response.flushBuffer();
                }
            }
            log.info("[AI] 照片打包下载完成 userId={} count={}", userId, files.size());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 客户端中途取消（连接断开）也会走到这里，记录告警即可
            log.warn("[AI] 照片打包下载失败 userId={}", userId, e);
            throw new BusinessException(StatusCode.OPERATION_ERROR, "打包下载失败，请稍后重试");
        }
    }

    /**
     * 会话标题生成：会话仍为默认标题时，用轻量 router 模型从本轮用户输入提炼（≤12字）。
     *
     * <p>同步执行（位于 SSE 流关闭之前）：标题落库后推送 {@code title} 事件，
     * 前端即时更新会话列表，无需整表刷新。任何失败均不影响对话主流程。
     */
    private void maybeGenerateTitle(String sessionId, Long userId, String userInput,
                                    Consumer<Map<String, Object>> sink) {
        try {
            AiSession session = sessionMapper.selectBySessionId(sessionId);
            if (session == null) {
                return;
            }
            String current = session.getTitle();
            boolean needTitle = current == null || current.isBlank() || DEFAULT_SESSION_TITLE.equals(current);
            if (!needTitle) {
                return;
            }
            String title = generateTitle(userInput);
            sessionMapper.updateTitle(sessionId, userId, title);
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("type", "title");
            ev.put("data", title);
            sink.accept(ev);
            log.info("[AI] 会话标题已生成 sessionId={} title={}", sessionId, title);
        } catch (Exception e) {
            // 标题生成失败不影响对话主流程
            log.warn("[AI] 会话标题生成失败 sessionId={}", sessionId, e);
        }
    }

    /** 用 router 模型提炼标题；模型未配置或调用失败时兜底截取用户输入前12字 */
    private String generateTitle(String userInput) {
        String fallback = userInput.length() > 12 ? userInput.substring(0, 12) : userInput;
        if (routerModel == null) {
            return fallback;
        }
        try {
            String prompt = """
                    请为下面的对话生成一个简洁的会话标题，要求：
                    1. 不超过12个字，简体中文
                    2. 概括用户的核心意图（如「查询南京照片」「创建西藏相册」）
                    3. 不要任何标点符号、引号、序号
                    4. 只输出标题本身，不要解释

                    用户输入：%s
                    """.formatted(userInput.length() > 200 ? userInput.substring(0, 200) : userInput);
            String title = routerModel.chat(prompt);
            if (title == null) {
                return fallback;
            }
            title = title.trim()
                    .replaceAll("^[\"'“”「『]+|[\"'“”」』]+$", "")
                    .replaceAll("\\s+", "");
            if (title.isEmpty()) {
                return fallback;
            }
            return title.length() > 12 ? title.substring(0, 12) : title;
        } catch (Exception e) {
            log.warn("[AI] 标题模型调用失败，使用兜底标题: {}", fallback);
            return fallback;
        }
    }

    private AiSessionVO toSessionVO(AiSession s) {
        AiSessionVO vo = new AiSessionVO();
        vo.setSessionId(s.getSessionId());
        vo.setAgentType(s.getAgentType());
        vo.setTitle(s.getTitle());
        vo.setUpdateTime(s.getUpdateTime());
        return vo;
    }

    private AiMessageVO toMessageVO(AiMessage m) {
        AiMessageVO vo = new AiMessageVO();
        vo.setId(m.getId());
        vo.setSessionId(m.getSessionId());
        vo.setRole(m.getRole());
        vo.setContentType(m.getContentType());
        vo.setContent(m.getContent());
        vo.setExtra(m.getExtra());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }
}
