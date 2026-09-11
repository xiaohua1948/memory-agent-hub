package com.memory.xiaohua.ai.resilience;

import java.util.List;

/**
 * 重试判定器 —— 区分「可重试的瞬时故障」与「重试也没用的确定性错误」。
 *
 * <p>只有瞬时故障才重试：
 * <ul>
 *   <li>网络类：连接超时 / 连接被拒 / 连接重置 / IO 异常；</li>
 *   <li>服务端类：429 限流、5xx（500/502/503/504）。</li>
 * </ul>
 *
 * <p>业务类异常（参数非法、模型返回内容不合法、SQL 被安全引擎拒绝等）**绝不重试** ——
 * 重试只是白白消耗 token 与延迟，正确的处理是把原因回传给模型让它自我修正
 * （该机制已在 {@code MainAgent} 中实现）。
 *
 * <p>实现上刻意使用「类名前缀 + 异常链 + 消息关键词」三重判断，而非直接
 * {@code catch (dev.langchain4j.exception.TimeoutException e)}：
 * LangChain4j 各版本把不同厂商异常映射为不同子类，按类名匹配可同时覆盖
 * 当前版本与后续版本新增的异常类型，且不会因类缺失导致编译失败。
 *
 * @author xiaohua
 */
public final class RetryClassifier {

    /** 可重试异常的类名前缀（含 LangChain4j 统一异常与 JDK 网络异常） */
    private static final List<String> RETRYABLE_CLASS_PREFIXES = List.of(
            "dev.langchain4j.exception.Timeout",
            "dev.langchain4j.exception.RateLimit",
            "dev.langchain4j.exception.InternalServer",
            "dev.langchain4j.exception.ServiceUnavailable",
            "dev.langchain4j.exception.HttpException",
            "java.net.SocketTimeoutException",
            "java.net.ConnectException",
            "java.net.NoRouteToHostException",
            "java.net.UnknownHostException",
            "org.apache.hc.core5.http.NoHttpResponseException"
    );

    /** 可重试的异常消息关键词（用于兜底未被映射为特定类型的 HTTP 错误） */
    private static final List<String> RETRYABLE_MESSAGE_HINTS = List.of(
            "429",
            "Too Many Requests",
            "500",
            "502",
            "503",
            "504",
            "Internal Server Error",
            "Bad Gateway",
            "Service Unavailable",
            "Gateway Timeout",
            "timeout",
            "timed out",
            "Read timed out",
            "connect timed out",
            "Connection reset",
            "Connection refused",
            "connection is closed",
            "temporarily unavailable"
    );

    private RetryClassifier() {
    }

    /** 是否为可重试的瞬时故障（沿异常链逐层判断） */
    public static boolean isRetryable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String className = current.getClass().getName();
            for (String prefix : RETRYABLE_CLASS_PREFIXES) {
                if (className.startsWith(prefix)) {
                    return true;
                }
            }
            // IO 异常（含子类）统一视为网络层瞬时故障
            if (current instanceof java.io.IOException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                for (String hint : RETRYABLE_MESSAGE_HINTS) {
                    if (message.contains(hint)) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
    }

    /** 取异常链最底层的消息（LLM 调用失败时真正有用的信息通常在根因上） */
    public static String rootMessage(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        Throwable root = error;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
    }
}
