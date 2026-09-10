package com.memory.xiaohua.ai.sql.query;

import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 相册查询 DSL → 参数化 SQL 编译器
 *
 * <p><b>安全模型</b>：SQL 结构完全由本类生成，用户/LLM 提供的内容只能作为
 * {@code ?} 绑定参数出现，字段名与算子名必须命中白名单，否则直接拒绝。
 * 相比「LLM 写 SQL + 正则/AST 兜底」，这里从源头上没有注入面。
 *
 * <p><b>派生字段处理</b>：照片数（photoCount）与封面（cover）都不是物理列，
 * 通过 LEFT JOIN 子查询提供。这样带来一个重要好处——聚合条件可以走普通 WHERE，
 * <b>不需要 HAVING + GROUP BY</b>，SQL 结构大幅简化：
 * <pre>
 *   stats.photoCount >= ?     -- 而非 HAVING COUNT(*) >= ?
 * </pre>
 *
 * <p>表结构依据（与 AlbumMapper.xml 中既有查询保持一致）：
 * <ul>
 *   <li>album(album_id, user_id, album_name, description, create_time, update_time, status)</li>
 *   <li>album_picture(id, album_id, file_id, user_id, is_cover)</li>
 *   <li>封面规则：{@code ORDER BY is_cover DESC, id} 取首条，再 JOIN file 取缩略图</li>
 * </ul>
 *
 * @author xiaohua
 */
@Component
public class AlbumDslCompiler {

    /** 编译产物：SQL + 绑定参数（顺序与 SQL 中 ? 出现顺序一致） */
    public record CompiledSql(String sql, List<Object> args) {}

    /** 字段语义类别 */
    private enum Kind { TEXT, DATETIME, NUMBER }

    /**
     * 字段白名单：DSL 字段名 → 物理列表达式。
     *
     * <p><b>不在表中的字段一律拒绝</b>，LLM 无法凭空造字段。
     */
    private enum Field {
        NAME("name", "a.album_name", Kind.TEXT),
        DESCRIPTION("description", "a.description", Kind.TEXT),
        CREATED_AT("createdAt", "a.create_time", Kind.DATETIME),
        UPDATED_AT("updatedAt", "a.update_time", Kind.DATETIME),
        STATUS("status", "a.status", Kind.NUMBER),
        /** 派生：相册内照片数（LEFT JOIN 子查询列） */
        PHOTO_COUNT("photoCount", "stats.photoCount", Kind.NUMBER),
        /** 派生：封面缩略图 URL（仅可查询，不可作为筛选条件） */
        COVER("cover", "cover.thumbnailUrl", Kind.TEXT);

        final String dslName;
        final String column;
        final Kind kind;

        Field(String dslName, String column, Kind kind) {
            this.dslName = dslName;
            this.column = column;
            this.kind = kind;
        }

        static Field of(String dslName) {
            if (dslName == null) {
                return null;
            }
            String key = dslName.trim();
            for (Field f : values()) {
                if (f.dslName.equalsIgnoreCase(key)) {
                    return f;
                }
            }
            return null;
        }
    }

    /** 算子白名单 → SQL 运算符 */
    private static final Map<String, String> OPERATORS = Map.of(
            "eq", "=", "like", "LIKE", "gt", ">", "gte", ">=",
            "lt", "<", "lte", "<=", "between", "BETWEEN"
    );

    /**
     * 编译 DSL 为可执行 SQL。
     *
     * @param dsl     结构化查询意图
     * @param userId  当前用户（强制注入隔离谓词）
     * @param maxRows 结果上限（防止全表拉取）
     */
    public CompiledSql compile(QueryDsl dsl, Long userId, int maxRows) {
        if (dsl == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "查询意图为空");
        }
        if (userId == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "用户身份缺失，无法执行隔离查询");
        }
        if (!"album".equalsIgnoreCase(dsl.getEntity())) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "暂不支持的查询实体: " + dsl.getEntity());
        }
        boolean count = "count".equalsIgnoreCase(dsl.getIntent());
        // Jackson 遇到 "select": null 会覆盖默认值，此处必须防空
        List<String> select = nullSafe(dsl.getSelect());

        /**
         * 参数收集顺序必须与 SQL 中 {@code ?} 的出现顺序<b>严格一致</b>：
         * JOIN 子查询 userId → WHERE 的 a.user_id → 筛选条件 → LIMIT。
         * 三者恰好都是 userId 时错位不会暴露问题，但一旦值不同就是静默的数据错误。
         */
        List<Object> args = new ArrayList<>();

        boolean needStats = count ? usesField(dsl, Field.PHOTO_COUNT) : true;
        boolean needCover = !count
                && (select.isEmpty() || select.stream().anyMatch(s -> "cover".equalsIgnoreCase(s)));

        StringBuilder sb = new StringBuilder();

        if (count) {
            sb.append("SELECT COUNT(*) AS total");
        } else {
            sb.append("SELECT a.album_id AS albumId");
            for (String sel : resolveSelect(select)) {
                switch (sel) {
                    case "name" -> sb.append(", a.album_name AS albumName");
                    case "description" -> sb.append(", a.description AS description");
                    case "createdAt" -> sb.append(", DATE_FORMAT(a.create_time, '%Y-%m-%d %H:%i') AS createdAt");
                    case "photoCount" -> sb.append(", COALESCE(stats.photoCount, 0) AS photoCount");
                    case "cover" -> sb.append(", cover.thumbnailUrl AS coverUrl");
                    default -> { /* 未知字段在 resolveSelect 已过滤 */ }
                }
            }
        }

        sb.append("\nFROM album a");

        if (needStats) {
            sb.append("""
                    
                    LEFT JOIN (SELECT ap.album_id, COUNT(*) AS photoCount
                               FROM album_picture ap WHERE ap.user_id = ?
                               GROUP BY ap.album_id) stats ON stats.album_id = a.album_id""");
            args.add(userId);
        }
        if (needCover) {
            // 封面：is_cover 优先、其次最早加入的照片，取首条；窗口函数需在内层过滤后外层取 rn=1
            sb.append("""
                    
                    LEFT JOIN (SELECT t.album_id, t.thumbnailUrl FROM (
                               SELECT ap.album_id, f.thumbnail_url AS thumbnailUrl,
                                      ROW_NUMBER() OVER (PARTITION BY ap.album_id
                                                         ORDER BY ap.is_cover DESC, ap.id) AS rn
                               FROM album_picture ap JOIN file f ON ap.file_id = f.file_id
                               WHERE ap.user_id = ?) t
                               WHERE t.rn = 1) cover ON cover.album_id = a.album_id""");
            args.add(userId);
        }

        // 用户隔离谓词：JOIN 参数已全部入列后才能追加，保证与 SQL 中 ? 顺序一致。
        // 隔离由编译器强制注入，不依赖 LLM 是否写了 user_id 条件。
        List<String> where = new ArrayList<>();
        where.add("a.user_id = ?");
        args.add(userId);

        // 筛选条件（AND 连接）
        for (QueryDsl.Filter filter : nullSafe(dsl.getFilters())) {
            String cond = buildCondition(filter, args);
            if (cond != null) {
                where.add(cond);
            }
        }

        sb.append("\nWHERE ").append(String.join("\n  AND ", where));

        if (!count) {
            QueryDsl.Order order = dsl.getOrder();
            if (order != null) {
                Field f = Field.of(order.getField());
                if (f != null) {
                    String dir = "asc".equalsIgnoreCase(order.getDir()) ? "ASC" : "DESC";
                    // 派生列排序同样要 COALESCE，避免 NULL 排在最前
                    String expr = f == Field.PHOTO_COUNT ? "COALESCE(stats.photoCount, 0)" : f.column;
                    sb.append("\nORDER BY ").append(expr).append(' ').append(dir);
                }
            }
            int limit = dsl.getLimit() == null ? maxRows : Math.min(Math.max(dsl.getLimit(), 1), maxRows);
            sb.append("\nLIMIT ?");
            args.add(limit);
        }

        return new CompiledSql(sb.toString(), args);
    }

    /**
     * 构造单个筛选条件，并将绑定参数追加到 args。
     *
     * @return SQL 片段；字段或算子非法时返回 null（跳过该条件，不阻断整个查询）
     */
    private String buildCondition(QueryDsl.Filter filter, List<Object> args) {
        if (filter == null || filter.getField() == null) {
            return null;
        }
        Field field = Field.of(filter.getField());
        if (field == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "不支持的查询字段: " + filter.getField());
        }
        if (field == Field.COVER) {
            // 封面是派生展示字段，不作为筛选维度（按封面内容检索没有业务意义）
            return null;
        }
        String op = OPERATORS.get(String.valueOf(filter.getOp()).toLowerCase(Locale.ROOT));
        if (op == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "不支持的查询算子: " + filter.getOp());
        }
        Object value = filter.getValue();
        if (value == null) {
            return null;
        }

        String expr = field == Field.PHOTO_COUNT ? "COALESCE(stats.photoCount, 0)" : field.column;

        if ("BETWEEN".equals(op)) {
            List<?> range = value instanceof List<?> list ? list : List.of(value);
            if (range.size() < 2) {
                throw new BusinessException(StatusCode.PARAMS_ERROR, "区间查询需要两个边界值");
            }
            args.add(normalize(range.get(0), field.kind));
            args.add(normalize(range.get(1), field.kind));
            return expr + " BETWEEN ? AND ?";
        }
        if ("LIKE".equals(op)) {
            String v = String.valueOf(value).trim();
            if (v.isEmpty()) {
                return null;
            }
            args.add("%" + v + "%");
            return expr + " LIKE ?";
        }
        args.add(normalize(value, field.kind));
        return expr + " " + op + " ?";
    }

    /** 按字段语义归一化绑定值（数值字段传字符串会导致 MySQL 隐式转换，索引失效） */
    private Object normalize(Object value, Kind kind) {
        if (kind == Kind.NUMBER) {
            if (value instanceof Number n) {
                return n;
            }
            try {
                return Long.parseLong(String.valueOf(value).trim());
            } catch (NumberFormatException e) {
                throw new BusinessException(StatusCode.PARAMS_ERROR, "数值字段收到了非数字值: " + value);
            }
        }
        return String.valueOf(value).trim();
    }

    /** 解析 select：为空返回默认集；去重且剔除非法字段 */
    private Set<String> resolveSelect(List<String> select) {
        if (select == null || select.isEmpty()) {
            // 默认集里含 albumId（恒返回），此处只取真正需要拼接的列
            return new LinkedHashSet<>(List.of("name", "description", "createdAt", "photoCount", "cover"));
        }
        Set<String> result = new LinkedHashSet<>();
        for (String s : select) {
            Field f = Field.of(s);
            if (f != null) {
                result.add(f.dslName);
            }
        }
        if (result.isEmpty()) {
            return new LinkedHashSet<>(List.of("name", "photoCount"));
        }
        return result;
    }

    private boolean usesField(QueryDsl dsl, Field target) {
        if (dsl.getFilters() != null) {
            for (QueryDsl.Filter f : dsl.getFilters()) {
                if (f != null && target.dslName.equalsIgnoreCase(f.getField())) {
                    return true;
                }
            }
        }
        return dsl.getOrder() != null && target.dslName.equalsIgnoreCase(dsl.getOrder().getField());
    }

    private <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
