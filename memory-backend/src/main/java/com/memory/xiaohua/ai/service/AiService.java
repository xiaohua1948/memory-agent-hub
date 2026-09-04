package com.memory.xiaohua.ai.service;

import com.memory.xiaohua.model.dto.ai.AiChatRequest;
import com.memory.xiaohua.model.dto.ai.AiCreateSessionRequest;
import com.memory.xiaohua.model.vo.ai.AiMessageVO;
import com.memory.xiaohua.model.vo.ai.AiSessionVO;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AI 智能助手服务接口
 *
 * @author xiaohua
 */
public interface AiService {

    /** 创建会话 */
    AiSessionVO createSession(Long userId, AiCreateSessionRequest request);

    /** 会话列表 */
    List<AiSessionVO> listSessions(Long userId);

    /** 会话历史消息 */
    List<AiMessageVO> listMessages(Long userId, String sessionId);

    /** 删除会话（软删会话与消息，并清理 Redis 运行时记忆） */
    void deleteSession(Long userId, String sessionId);

    /**
     * 聊天（流式回调）。sink 推送 type: text/card/html 的事件。
     *
     * @return 会话ID
     */
    String chat(Long userId, AiChatRequest request, Consumer<Map<String, Object>> sink);

    /** 执行确认的写操作 */
    Map<String, Object> execute(Long userId, String actionId);
}
