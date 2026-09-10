package com.memory.xiaohua.ai.sql.query;

import com.memory.xiaohua.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 问数编排服务（AskAgent 核心）
 *
 * <p>链路：NL → {@link QueryPlanner} 生成 DSL → {@link AlbumDslCompiler} 编译 SQL
 * → 只读执行 → <b>空结果诊断</b> → 结构化结果。
 *
 * <p><b>空结果诊断的意义</b>：直接回「未找到」是最差的答复。用户问「照片数超过100张的
 * 相册」而实际一个都没有时，他真正想知道的是「是有相册但都不够100张，还是条件理解错了」。
 * 诊断会逐条摘除筛选条件重查数量，定位是哪个条件把结果杀空的，把原因回传给主 Agent，
 * 让它能给出「你有 12 个相册，但最多的一个只有 43 张」这种有价值的回复。
 *
 * @author xiaohua
 */
@Slf4j
@Service
public class AskDataService {

    /** 诊断时最多尝试摘除的条件数（避免条件过多时放大查询次数） */
    private static final int MAX_DIAGNOSTIC_TRIES = 3;

    private final QueryPlanner planner;
    private final AlbumDslCompiler compiler;
    private final JdbcTemplate jdbcTemplate;
    private final AiProperties properties;

    public AskDataService(QueryPlanner planner, AlbumDslCompiler compiler,
                          JdbcTemplate jdbcTemplate, AiProperties properties) {
        this.planner = planner;
        this.compiler = compiler;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    /**
     * 问数结果。
     *
     * @param ok         是否成功执行（false 表示意图无法理解，需提示用户换个说法）
     * @param rows       明细行（count 意图时为空）
     * @param total      标量结果（count 意图时有值）
     * @param summary    给主 Agent 的一行摘要
     * @param diagnostic 空结果时的诊断说明（无则为空字符串）
     */
    public record AskResult(boolean ok, List<Map<String, Object>> rows, Long total,
                            String summary, String diagnostic, boolean countIntent) {

        public static AskResult unavailable(String reason) {
            return new AskResult(false, List.of(), null, reason, "", false);
        }
    }

    /** 相册问数入口 */
    public AskResult askAlbums(String question, Long userId) {
        if (question == null || question.isBlank()) {
            return AskResult.unavailable("查询条件为空，请说明你想查相册的什么信息。");
        }
        QueryDsl dsl = planner.plan(question);
        if (dsl == null) {
            return AskResult.unavailable("没能理解这个相册查询需求，请换个说法（例如：我有多少个相册、照片数超过10张的相册）。");
        }
        boolean countIntent = "count".equalsIgnoreCase(dsl.getIntent());
        try {
            AlbumDslCompiler.CompiledSql compiled = compiler.compile(dsl, userId, properties.getQueryMaxRows());
            log.info("[AI][Ask] SQL: {} | args: {}", compiled.sql(), compiled.args());
            List<Map<String, Object>> rows =
                    jdbcTemplate.queryForList(compiled.sql(), compiled.args().toArray());

            if (countIntent) {
                long total = rows.isEmpty() ? 0L : toLong(rows.get(0).get("total"));
                return new AskResult(true, List.of(), total, "共 " + total + " 个相册", "", true);
            }
            if (rows.isEmpty()) {
                String diagnostic = diagnose(dsl, userId);
                return new AskResult(true, List.of(), 0L, "未找到符合条件的相册", diagnostic, false);
            }
            return new AskResult(true, rows, (long) rows.size(),
                    "找到 " + rows.size() + " 个相册", "", false);
        } catch (Exception e) {
            log.warn("[AI][Ask] 相册查询失败: {}", e.getMessage());
            return AskResult.unavailable("相册查询失败：" + e.getMessage());
        }
    }

    /**
     * 空结果诊断：逐条摘除筛选条件重查总数，找到第一个「去掉它就有结果」的条件。
     *
     * <p>这意味着它是把结果杀空的主因。全部摘完仍为 0，说明用户根本没有相册。
     */
    private String diagnose(QueryDsl dsl, Long userId) {
        List<QueryDsl.Filter> filters = dsl.getFilters() == null ? List.of() : dsl.getFilters();
        if (filters.isEmpty()) {
            return "你的相册库中还没有任何相册。";
        }
        int tries = Math.min(filters.size(), MAX_DIAGNOSTIC_TRIES);
        for (int i = 0; i < tries; i++) {
            List<QueryDsl.Filter> reduced = new ArrayList<>(filters);
            QueryDsl.Filter removed = reduced.remove(i);
            long n = countAlbums(withFilters(dsl, reduced), userId);
            if (n > 0) {
                return "没有相册同时满足全部条件；去掉「" + describe(removed) + "」后能查到 " + n + " 个相册。";
            }
        }
        // 单个条件都不是主因：看看完全不限条件时有多少
        long all = countAlbums(withFilters(dsl, List.of()), userId);
        if (all == 0) {
            return "你的相册库中还没有任何相册。";
        }
        return "你的 " + all + " 个相册都不满足这些条件，可以适当放宽后重试。";
    }

    /** 统计某 DSL 变体下的相册总数（失败返回 -1，视为无法诊断） */
    private long countAlbums(QueryDsl dsl, Long userId) {
        try {
            AlbumDslCompiler.CompiledSql compiled =
                    compiler.compile(dsl, userId, properties.getQueryMaxRows());
            List<Map<String, Object>> rows =
                    jdbcTemplate.queryForList(compiled.sql(), compiled.args().toArray());
            return rows.isEmpty() ? 0L : toLong(rows.get(0).get("total"));
        } catch (Exception e) {
            log.warn("[AI][Ask] 诊断查询失败: {}", e.getMessage());
            return -1L;
        }
    }

    /** 复制 DSL 并替换筛选条件（用于诊断变体） */
    private QueryDsl withFilters(QueryDsl src, List<QueryDsl.Filter> filters) {
        QueryDsl copy = new QueryDsl();
        copy.setEntity(src.getEntity());
        copy.setIntent("count");
        copy.setSelect(List.of());
        copy.setFilters(filters);
        copy.setOrder(null);
        copy.setLimit(null);
        return copy;
    }

    /** 筛选条件的人话描述（供诊断文案与摘要使用） */
    private String describe(QueryDsl.Filter filter) {
        if (filter == null) {
            return "未知条件";
        }
        Map<String, String> fieldLabels = Map.of(
                "name", "名称", "description", "描述", "createdAt", "创建时间",
                "updatedAt", "更新时间", "photoCount", "照片数", "status", "状态");
        Map<String, String> opLabels = Map.of(
                "eq", "等于", "like", "包含", "gt", "大于", "gte", "不小于",
                "lt", "小于", "lte", "不大于", "between", "介于");
        String field = fieldLabels.getOrDefault(String.valueOf(filter.getField()), String.valueOf(filter.getField()));
        String op = opLabels.getOrDefault(String.valueOf(filter.getOp()).toLowerCase(), String.valueOf(filter.getOp()));
        return field + op + renderValue(filter.getValue());
    }

    private String renderValue(Object value) {
        if (value instanceof List<?> list) {
            return list.isEmpty() ? "" : "「" + list.get(0) + " ~ " + list.get(list.size() - 1) + "」";
        }
        return "「" + value + "」";
    }

    private long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        return v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v));
    }

    /** 列定义全集（顺序即表格列顺序） */
    private static final List<Map<String, String>> ALBUM_COLUMNS = List.of(
            col("coverUrl", "封面", "image"),
            col("albumName", "相册名称", "text"),
            col("photoCount", "照片数", "number"),
            col("createdAt", "创建时间", "text"),
            col("description", "描述", "text"));

    /**
     * 构造前端表格的列定义（label 用于表头，type 决定渲染方式）。
     *
     * <p>只保留本次查询<b>实际返回</b>的列：LLM 的 select 可能只要 ["name"]，
     * 若仍下发全量列定义，前端会渲染出几个全是「-」的空列。
     */
    public static List<Map<String, String>> albumColumns(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return ALBUM_COLUMNS;
        }
        Set<String> present = rows.get(0).keySet();
        List<Map<String, String>> cols = new ArrayList<>();
        for (Map<String, String> c : ALBUM_COLUMNS) {
            if (present.contains(c.get("key"))) {
                cols.add(c);
            }
        }
        // 兜底：列定义与实际返回完全不匹配时（理论不会发生）仍返回全集，保证表格有内容
        return cols.isEmpty() ? ALBUM_COLUMNS : cols;
    }

    private static Map<String, String> col(String key, String label, String type) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        m.put("type", type);
        return m;
    }
}
