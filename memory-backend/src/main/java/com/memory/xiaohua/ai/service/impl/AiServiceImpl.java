package com.memory.xiaohua.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memory.xiaohua.ai.action.PendingActionService;
import com.memory.xiaohua.ai.agent.AgentContext;
import com.memory.xiaohua.ai.agent.AgentOrchestrator;
import com.memory.xiaohua.ai.config.AiProperties;
import com.memory.xiaohua.ai.memory.AiChatMemoryFactory;
import com.memory.xiaohua.ai.service.AiService;
import com.memory.xiaohua.ai.tool.AlbumTools;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.mapper.AiMessageMapper;
import com.memory.xiaohua.mapper.AiSessionMapper;
import com.memory.xiaohua.model.dto.ai.AiChatRequest;
import com.memory.xiaohua.model.dto.ai.AiCreateSessionRequest;
import com.memory.xiaohua.model.entity.AiMessage;
import com.memory.xiaohua.model.entity.AiSession;
import com.memory.xiaohua.model.vo.ai.AiMessageVO;
import com.memory.xiaohua.model.vo.ai.AiSessionVO;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

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
    private final AgentOrchestrator orchestrator;
    private final PendingActionService pendingActionService;
    private final AlbumTools albumTools;
    private final AiChatMemoryFactory memoryFactory;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public AiServiceImpl(AiSessionMapper sessionMapper,
                         AiMessageMapper messageMapper,
                         AgentOrchestrator orchestrator,
                         PendingActionService pendingActionService,
                         AlbumTools albumTools,
                         AiChatMemoryFactory memoryFactory,
                         AiProperties properties,
                         ObjectMapper objectMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.orchestrator = orchestrator;
        this.pendingActionService = pendingActionService;
        this.albumTools = albumTools;
        this.memoryFactory = memoryFactory;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSessionVO createSession(Long userId, AiCreateSessionRequest request) {
        String agentType = request == null || request.getAgentType() == null
                ? "mixed" : request.getAgentType().toLowerCase();
        AiSession session = new AiSession();
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(userId);
        session.setAgentType(agentType);
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

        final List<Map<String, Object>> produced = new ArrayList<>();
        orchestrator.execute(context, event -> {
            produced.add(event);
            sink.accept(event);
        });

        // 6. 持久化助手产出：content 纯文本 + extra 结构化（HTML/卡片不进记忆）
        for (Map<String, Object> event : produced) {
            String type = String.valueOf(event.getOrDefault("type", "text"));
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
                    extra = String.valueOf(event.getOrDefault("data", ""));
                }
                case "photos" -> {
                    contentType = "photos";
                    Object data = event.get("data");
                    content = (data instanceof List<?> l)
                            ? ("查询到 " + l.size() + " 张照片")
                            : "查询结果";
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

        // 7. 刷新会话更新时间（不覆盖标题）
        sessionMapper.touch(sessionId, userId);

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
            String input = String.valueOf(params.getOrDefault("input", ""));
            Map<String, Object> result = new LinkedHashMap<>();

            switch (actionType) {
                case "CREATE_ALBUM" -> {
                    String albumName = extractAlbumName(input);
                    Long albumId = albumTools.createAlbum(albumName, userId);
                    result.put("albumId", albumId);
                    result.put("message", "相册「" + albumName + "」创建成功");
                }
                case "ADD_PICTURES" -> {
                    result.put("message", "照片添加需在确认卡片中指定照片，已收到确认");
                }
                default -> {
                    result.put("message", "操作已执行: " + actionType);
                }
            }
            result.put("actionId", actionId);
            return result;
        } catch (Exception e) {
            log.error("[AI] 执行确认操作失败 actionId={}", actionId, e);
            throw new BusinessException(StatusCode.OPERATION_ERROR, "执行失败: " + e.getMessage());
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

    private String extractAlbumName(String input) {
        String name = input.replaceAll("(创建|新建|一个|名为|叫做|相册)", "").trim();
        return name.isBlank() ? "新相册" : name;
    }

    private String toJsonQuietly(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
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
