package com.memory.xiaohua.ai.sql.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 自然语言 → 查询 DSL 规划器
 *
 * <p>职责边界：<b>只做意图理解，不做 SQL 生成</b>。LLM 输出受限的结构化 JSON，
 * 由 {@link AlbumDslCompiler} 编译为 SQL。这样模型的自由度被限制在「字段 + 算子 + 值」
 * 的笛卡尔积内，即使理解有偏差，最坏结果是查不到或查错维度，而不会产生非法 SQL
 * 或越权数据访问。
 *
 * <p>解析失败（模型不输出 JSON / 输出非法字段）时返回 null，由上层降级为
 * 「换个说法」的友好提示，绝不尝试猜测执行。
 *
 * @author xiaohua
 */
@Slf4j
@Component
public class QueryPlanner {

    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    @Qualifier("mainModel")
    private ChatModel mainModel;

    public QueryPlanner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 模型未配置或解析失败时返回 null */
    public QueryDsl plan(String question) {
        if (mainModel == null || question == null || question.isBlank()) {
            return null;
        }
        String raw = mainModel.chat(buildPrompt(question));
        log.info("[AI][Planner] 问题={} → DSL原文: {}", question, raw);
        try {
            return objectMapper.readValue(extractJson(raw), QueryDsl.class);
        } catch (Exception e) {
            log.warn("[AI][Planner] DSL 解析失败，原文={}, 原因={}", raw, e.getMessage());
            return null;
        }
    }

    private String buildPrompt(String question) {
        return """
                你是智能相册的「查询意图解析器」。把用户问题翻译成结构化查询 JSON，不要生成 SQL，不要解释。

                ## 输出格式（只输出一个 JSON 对象）
                {
                  "entity": "album",
                  "intent": "list | count",
                  "select": ["name", "photoCount", "cover", "createdAt", "description"],
                  "filters": [{"field": "...", "op": "...", "value": ...}],
                  "order": {"field": "...", "dir": "asc | desc"},
                  "limit": 20
                }

                ## 字段白名单（只能用这些）
                - name          相册名称        算子：eq / like
                - description   相册描述        算子：like
                - createdAt     创建时间        算子：between / gte / lte / gt / lt
                - updatedAt     更新时间        算子：同上
                - photoCount    相册内照片数    算子：eq / gt / gte / lt / lte
                - status        状态(0正常1公开2私密)  算子：eq
                （cover 封面只能在 select 里出现，不能作为筛选条件）

                ## 取值规则
                1. intent=count 表示用户只要一个数字（如「我有多少个相册」），此时不需要 select/order/limit。
                2. intent=list 表示用户要看明细；未指明要哪些列时 select 留空数组 []。
                3. 时间必须规范化：日期用 "2026-01-01"，日期时间用 "2026-01-01 12:00:00"。
                   「2026年1月创建的」→ createdAt between ["2026-01-01", "2026-01-31 23:59:59"]。
                   「最近一周创建的」→ createdAt gte "<今天减7天的日期>"，今天是 %s。
                4. 数量比较一律用数字，不要带单位：「照片超过10张」→ {field:"photoCount", op:"gt", value:10}。
                5. 用户说「空相册」→ {field:"photoCount", op:"eq", value:0}。
                6. 排序：用户说「最多的」→ order={field:"photoCount",dir:"desc"}；「最新的」→ createdAt desc。
                7. 无法确定维度时 filters 留空数组 []，不要编造条件。

                ## 示例
                问：我有多少个相册
                答：{"entity":"album","intent":"count"}

                问：查询2026年1月创建的相册，显示名称和照片数
                答：{"entity":"album","intent":"list","select":["name","photoCount"],
                    "filters":[{"field":"createdAt","op":"between","value":["2026-01-01","2026-01-31 23:59:59"]}]}

                问：照片数超过10张的相册有哪些
                答：{"entity":"album","intent":"list","select":["name","photoCount","cover"],
                    "filters":[{"field":"photoCount","op":"gt","value":10}],"order":{"field":"photoCount","dir":"desc"}}

                问：名字里带「旅行」的相册
                答：{"entity":"album","intent":"list","filters":[{"field":"name","op":"like","value":"旅行"}]}

                ## 用户问题
                %s
                """.formatted(java.time.LocalDate.now(), question);
    }

    /** 提取 JSON 本体：容忍 ```json 包裹与前后解释文字 */
    private String extractJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("```(json)?", "").trim();
        }
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return "{}";
    }
}
