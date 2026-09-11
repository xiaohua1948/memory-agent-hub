package com.memory.xiaohua.ai.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.memory.xiaohua.ai.config.AiProperties;
import com.memory.xiaohua.common.cache.MultiLevelCacheManager;
import com.memory.xiaohua.mapper.AiMessageMapper;
import com.memory.xiaohua.model.entity.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 对话记忆装配工厂
 *
 * <p>对齐《AI智能助手-优化计划.md》§4.2：按 memoryId 装配
 * {@link MessageWindowChatMemory}，落在 {@link ChatMemoryStore}（Redis），
 * 记忆为空（首次 / Redis 过期）时从 DB {@code ai_message} 回灌最近 N 条历史。
 *
 * <p>记忆分层：
 * <ul>
 *   <li>Redis（运行时记忆，LangChain4j 自动读写，TTL 可配）；</li>
 *   <li>DB {@code ai_message}（持久真相源，跨 Redis 过期 / 重启可恢复）。</li>
 * </ul>
 *
 * <p><b>隔离键</b>：{@code userId:sessionId} 复合键。仅用 sessionId 做隔离依赖
 * 「sessionId 全局唯一」这一约定；一旦出现会话 ID 复用或跨用户传入他人 sessionId，
 * 记忆就会串号。加上 userId 前缀后，即使 sessionId 撞车，不同用户的记忆也物理隔离，
 * 与 {@code checkSessionOwner} 的授权校验形成「授权 + 隔离」双保险。
 *
 * <p><b>回灌缓存</b>：DB 回灌查询在 Redis 记忆过期后会被反复触发（尤其是
 * 未启用 Redis 记忆的降级模式，每轮对话都会回灌），因此接入两级缓存；
 * 写路径（{@code AiServiceImpl#saveMessage}）负责失效，保证不会读到过期历史。
 *
 * @author xiaohua
 */
@Slf4j
@Component
public class AiChatMemoryFactory {

    /** 回灌数据源的类型信息：L2 以 JSON 存储，反序列化需显式泛型 */
    private static final TypeReference<List<AiMessage>> RECENT_MESSAGES_TYPE = new TypeReference<>() {
    };

    private final AiMessageMapper messageMapper;
    private final AiProperties properties;
    private final MultiLevelCacheManager cacheManager;

    /** Redis 记忆存储（ai.enabled=true 时才注入，未启用时为 null → 降级内存记忆） */
    @Autowired(required = false)
    private ChatMemoryStore chatMemoryStore;

    public AiChatMemoryFactory(AiMessageMapper messageMapper,
                              AiProperties properties,
                              MultiLevelCacheManager cacheManager) {
        this.messageMapper = messageMapper;
        this.properties = properties;
        this.cacheManager = cacheManager;
    }

    /**
     * 装配会话记忆（含 DB 回灌）。
     *
     * @param userId    当前用户（参与隔离键构造）
     * @param sessionId 会话业务ID
     * @return 窗口记忆
     */
    public MessageWindowChatMemory buildMemory(Long userId, String sessionId) {
        MessageWindowChatMemory.Builder builder = MessageWindowChatMemory.builder()
                .id(memoryId(userId, sessionId))
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
     * 清除会话的 Redis 运行时记忆（删除会话时调用，避免残留脏记忆被误回灌）。
     * DB 侧记忆由调用方软删 ai_message。
     */
    public void clearMemory(Long userId, String sessionId) {
        if (chatMemoryStore == null) {
            return;
        }
        String memoryId = memoryId(userId, sessionId);
        try {
            chatMemoryStore.deleteMessages(memoryId);
            log.debug("[AI] 已清除 Redis 记忆 memoryId={}", memoryId);
        } catch (Exception e) {
            // 清理失败不影响删除主流程（TTL 最终会过期）
            log.warn("[AI] 清除 Redis 记忆失败 memoryId={}", memoryId, e);
        }
    }

    /**
     * 记忆隔离键：userId + sessionId 复合。
     *
     * <p>注意这会改变 Redis 中记忆 key 的形态，升级后旧记忆（仅 sessionId 作为 id）
     * 不会被命中，等效于「从 DB 回灌一次」—— 数据不丢（DB 才是真相源），
     * 仅损失一次 Redis 读取。
     */
    private String memoryId(Long userId, String sessionId) {
        return (userId == null ? "0" : userId) + ":" + sessionId;
    }

    /**
     * DB → 记忆回灌：取最近 maxCount 条，按时间正序，
     * role=user → UserMessage，role=assistant → AiMessage，逐条 add。
     * 仅回灌纯文本（content），photos / html / pending_action 不进记忆。
     */
    private void loadHistoryToMemory(String sessionId, MessageWindowChatMemory memory, int maxCount) {
        List<AiMessage> recent = loadRecentMessages(sessionId, maxCount);
        if (recent.isEmpty()) {
            return;
        }
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

    /**
     * 读取回灌数据源（带两级缓存）。
     *
     * <p>缓存里存的是**已按时间正序**整理好的列表，避免调用方再反转；
     * 返回前复制成不可变列表，防止调用方改动污染 L1 中的共享实例。
     */
    private List<AiMessage> loadRecentMessages(String sessionId, int maxCount) {
        List<AiMessage> recent = cacheManager.memoryCache().get(
                sessionId,
                RECENT_MESSAGES_TYPE,
                () -> {
                    List<AiMessage> rows = messageMapper.selectRecentBySessionId(sessionId, maxCount);
                    if (rows == null || rows.isEmpty()) {
                        return List.of();
                    }
                    // 查询按 id 倒序（取最近 N 条），回灌需时间正序
                    List<AiMessage> ascending = new ArrayList<>(rows);
                    Collections.reverse(ascending);
                    return ascending;
                });
        return recent == null || recent.isEmpty() ? List.of() : List.copyOf(recent);
    }
}
