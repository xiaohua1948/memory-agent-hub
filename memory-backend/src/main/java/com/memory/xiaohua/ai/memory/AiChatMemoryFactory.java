package com.memory.xiaohua.ai.memory;

import com.memory.xiaohua.ai.config.AiProperties;
import com.memory.xiaohua.mapper.AiMessageMapper;
import com.memory.xiaohua.model.entity.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * AI 对话记忆装配工厂
 *
 * <p>对齐《AI智能助手-优化计划.md》§4.2：按 sessionId（memoryId）装配
 * {@link MessageWindowChatMemory}，落在 {@link ChatMemoryStore}（Redis），
 * 记忆为空（首次 / Redis 过期）时从 DB {@code ai_message} 回灌最近 N 条历史。
 *
 * <p>记忆分层：
 * <ul>
 *   <li>Redis（运行时记忆，LangChain4j 自动读写，TTL 可配）；</li>
 *   <li>DB {@code ai_message}（持久真相源，跨 Redis 过期 / 重启可恢复）。</li>
 * </ul>
 *
 * @author xiaohua
 */
@Slf4j
@Component
public class AiChatMemoryFactory {

    private final AiMessageMapper messageMapper;
    private final AiProperties properties;

    /** Redis 记忆存储（ai.enabled=true 时才注入，未启用时为 null → 降级内存记忆） */
    @Autowired(required = false)
    private ChatMemoryStore chatMemoryStore;

    public AiChatMemoryFactory(AiMessageMapper messageMapper, AiProperties properties) {
        this.messageMapper = messageMapper;
        this.properties = properties;
    }

    /**
     * 装配会话记忆（含 DB 回灌）。
     *
     * @param sessionId 会话业务ID（memoryId）
     * @return 窗口记忆
     */
    public MessageWindowChatMemory buildMemory(String sessionId) {
        MessageWindowChatMemory.Builder builder = MessageWindowChatMemory.builder()
                .id(sessionId)
                .maxMessages(properties.getMemoryMaxMessages());
        if (chatMemoryStore != null) {
            builder.chatMemoryStore(chatMemoryStore);
        }

        MessageWindowChatMemory memory = builder.build();

        // 记忆为空（首次 / Redis 过期）→ 从 DB 回灌最近 N 条
        if (memory.messages().isEmpty()) {
            loadHistoryToMemory(sessionId, memory, properties.getMemoryMaxMessages());
        }
        return memory;
    }

    /**
     * DB → 记忆回灌：查 ai_message 最近 maxCount 条，按时间正序，
     * role=user → UserMessage，role=assistant → AiMessage，逐条 add。
     * 仅回灌纯文本（content），photos / html / pending_action 不进记忆。
     */
    private void loadHistoryToMemory(String sessionId, MessageWindowChatMemory memory, int maxCount) {
        List<AiMessage> recent = messageMapper.selectRecentBySessionId(sessionId, maxCount);
        if (recent == null || recent.isEmpty()) {
            return;
        }
        // selectRecentBySessionId 按 id 倒序返回，需反转为时间正序后回灌
        Collections.reverse(recent);
        for (AiMessage m : recent) {
            String role = m.getRole();
            String content = m.getContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            if ("user".equals(role)) {
                memory.add(UserMessage.from(content));
            } else if ("assistant".equals(role)) {
                memory.add(dev.langchain4j.data.message.AiMessage.from(content));
            }
        }
        log.debug("[AI] 记忆回灌 sessionId={}, count={}", sessionId, recent.size());
    }
}
