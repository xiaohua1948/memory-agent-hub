package com.memory.xiaohua.ai.sql.query;

import lombok.Data;

import java.util.List;

/**
 * 结构化查询意图 —— NL → DSL → SQL 的中间层
 *
 * <p>存在意义：端到端 text2sql 让 LLM 直接写 SQL，在聚合（COUNT/GROUP BY）、
 * 派生字段（相册照片数、封面）、时间区间等场景下失败率高，且错误 SQL 可能返回
 * 错误数据而不报错（问数场景用户不会验证）。引入 DSL 后：
 * <ul>
 *   <li>LLM 只负责「理解意图」，输出受限的结构化 JSON；</li>
 *   <li>SQL 由 {@link AlbumDslCompiler} 确定性生成，字段/算子全部走白名单；</li>
 *   <li>用户隔离谓词由编译器强制注入，LLM 无从绕过；</li>
 *   <li>结果为空时可逐条摘除 filter 重查，定位「元凶条件」。</li>
 * </ul>
 *
 * <p>示例（用户问「2026年1月创建且照片数超过10张的相册」）：
 * <pre>
 * {
 *   "entity": "album",
 *   "intent": "list",
 *   "select": ["name", "photoCount", "cover", "createdAt"],
 *   "filters": [
 *     {"field": "createdAt", "op": "between", "value": ["2026-01-01", "2026-01-31"]},
 *     {"field": "photoCount", "op": "gt", "value": 10}
 *   ],
 *   "order": {"field": "photoCount", "dir": "desc"},
 *   "limit": 20
 * }
 * </pre>
 *
 * @author xiaohua
 */
@Data
public class QueryDsl {

    /** 查询实体：album（相册）。照片维度暂由 text2sql 承担，后续可扩展 */
    private String entity = "album";

    /**
     * 查询意图：
     * <ul>
     *   <li>{@code list} —— 返回明细列表（表格）；</li>
     *   <li>{@code count} —— 只返回数量（标量指标）。</li>
     * </ul>
     */
    private String intent = "list";

    /** 需要返回的字段（list 意图生效）；为空则返回全部默认字段 */
    private List<String> select = List.of();

    /** 筛选条件（AND 连接） */
    private List<Filter> filters = List.of();

    /** 排序（可为 null，表示不排序） */
    private Order order;

    /** 返回条数上限（为空则用系统默认） */
    private Integer limit;

    /** 筛选条件 */
    @Data
    public static class Filter {

        /** 字段名（见编译器白名单，如 name / createdAt / photoCount） */
        private String field;

        /**
         * 算子：eq / like / gt / gte / lt / lte / between
         */
        private String op;

        /**
         * 条件值：单值（字符串/数字）或二元组（between 时为 [begin, end]）
         */
        private Object value;
    }

    /** 排序 */
    @Data
    public static class Order {

        /** 排序字段 */
        private String field;

        /** asc / desc，默认 desc */
        private String dir = "desc";
    }
}
