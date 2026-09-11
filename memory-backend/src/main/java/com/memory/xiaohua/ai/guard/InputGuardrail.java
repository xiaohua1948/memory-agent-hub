package com.memory.xiaohua.ai.guard;

import com.memory.xiaohua.ai.config.AiProperties;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 输入护轨 —— 用户输入进入 Agent 链路前的第一道闸门。
 *
 * <p>职责（按顺序）：
 * <ol>
 *   <li><b>归一化</b>：剥离零宽 / 双向控制字符。注入攻击常用它们把关键词拆散，
 *       从而绕过正则匹配（如「忽略\u200B之前的指令」）；</li>
 *   <li><b>空值与长度</b>：拒绝空输入，并把超长输入挡在 LLM 之前 ——
 *       超长输入会同时放大 token 成本、挤占上下文窗口、拖慢首字延迟；</li>
 *   <li><b>指令注入拦截</b>：识别「忽略之前的指令 / 输出你的系统提示词 / 进入开发者模式」
 *       等越权意图，直接拒绝而不是交给模型自行判断（模型对这类输入并不总是可靠）。</li>
 * </ol>
 *
 * <p>通过校验的输入会被返回（已归一化），调用方应使用返回值而非原始入参。
 *
 * <p>注意：这里只做**输入侧**防护。写操作的「只出草稿不落库」属于
 * human-in-the-loop 领域，由 {@code PendingActionService} 负责；
 * 模型输出的过渡语纠正属于**输出侧**护轨，由 {@code MainAgent} 负责。
 *
 * @author xiaohua
 */
@Slf4j
@Component
public class InputGuardrail {

    /** 日志里回显输入的截断长度（避免把超长/含敏感内容的原文整条写进日志） */
    private static final int LOG_PREVIEW_LENGTH = 80;

    /** 零宽与双向控制字符：注入者用来拆散关键词以绕过匹配 */
    private static final Pattern INVISIBLE_CHARS =
            Pattern.compile("[\\u200B-\\u200F\\u202A-\\u202E\\u2060\\uFEFF]");

    /**
     * 疑似 Prompt 注入 / 越狱模式（中英双语）。
     *
     * <p>刻意要求「指令类名词」共现（如「忽略」+「指令/规则」），
     * 避免误伤正常业务表述（例如「忽略这张模糊的照片」不应被拦截）。
     */
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?(the\\s+)?(previous|above|prior|earlier)\\s+"
                    + "(instructions?|prompts?|rules?|context)"),
            Pattern.compile("(?i)disregard\\s+(all\\s+)?(the\\s+)?(previous|above|prior|earlier)"),
            Pattern.compile("(?i)(reveal|print|repeat|output|show)\\s+(me\\s+)?(your\\s+)?"
                    + "(system\\s+|initial\\s+)?(prompt|instructions?|rules?)"),
            Pattern.compile("(?i)(you\\s+are\\s+now|from\\s+now\\s+on\\s+you\\s+are|pretend\\s+to\\s+be)\\s+"
                    + "(a\\s+|an\\s+)?(dan|jailbreak|unrestricted|developer\\s+mode|god\\s+mode)"),
            Pattern.compile("(?i)(bypass|circumvent|override)\\s+(the\\s+)?(safety|security|content)\\s+"
                    + "(filter|policy|policies|guidelines|restrictions?)"),
            Pattern.compile("忽略(掉)?(以上|之前|先前|上面|前面)(的)?(所有|全部)?(指令|指示|规则|要求|设定|设定内容|提示)"),
            Pattern.compile("(无视|不要遵守|不必遵守|无需遵守)(你)?(的)?(系统)?(指令|规则|限制|设定|提示)"),
            Pattern.compile("(输出|显示|打印|重复|泄露|展示|告诉我)(一下)?(你的)?(系统)?"
                    + "(提示词|提示语|prompt|预设指令|系统指令|初始设定|设定内容|人设)"),
            Pattern.compile("(你现在|从现在开始|接下来)?(你是|扮演|作为|切换成)(一个)?"
                    + "(不受限制|没有限制|无限制|开发者模式|越狱模式|DAN|上帝模式)"),
            Pattern.compile("(进入|开启|启用|切换到)(开发者模式|越狱模式|无限制模式|上帝模式|Debug模式)"),
            Pattern.compile("(绕过|突破|跳过|解除)(安全|权限|内容)?(限制|审查|过滤|策略|机制)"),
            Pattern.compile("(把|将)(上面的|之前的|系统的|你的)(指令|提示词|prompt|设定)"
                    + "(原样|逐字|一字不差)?(输出|打印|重复|发给我)")
    );

    private final AiProperties properties;

    public InputGuardrail(AiProperties properties) {
        this.properties = properties;
    }

    /**
     * 校验并归一化用户输入。
     *
     * @param userId    当前用户（仅用于日志追溯）
     * @param rawInput  原始输入
     * @return 归一化后的安全输入（调用方应使用该返回值）
     * @throws BusinessException 未通过护轨时抛出，消息可直接展示给用户
     */
    public String checkAndNormalize(Long userId, String rawInput) {
        String input = rawInput == null
                ? ""
                : INVISIBLE_CHARS.matcher(rawInput).replaceAll("").trim();

        if (input.isEmpty()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "消息不能为空");
        }

        int maxLength = properties.getGuard().getMaxInputLength();
        if (input.length() > maxLength) {
            log.warn("[AI][Guard] 输入超长已拦截 userId={} length={} maxLength={}",
                    userId, input.length(), maxLength);
            throw new BusinessException(StatusCode.PARAMS_ERROR,
                    "消息过长（" + input.length() + " 字），单次最多 " + maxLength + " 字，请精简后重试");
        }

        if (properties.getGuard().isBlockPromptInjection()) {
            for (Pattern pattern : INJECTION_PATTERNS) {
                if (pattern.matcher(input).find()) {
                    log.warn("[AI][Guard] 疑似指令注入已拦截 userId={} pattern={} input={}",
                            userId, pattern.pattern(), preview(input));
                    throw new BusinessException(StatusCode.AI_INPUT_REJECTED,
                            "检测到疑似指令注入内容，已拦截。请用正常语句描述你的需求。");
                }
            }
        }
        return input;
    }

    private String preview(String input) {
        return input.length() <= LOG_PREVIEW_LENGTH
                ? input
                : input.substring(0, LOG_PREVIEW_LENGTH) + "...";
    }
}
