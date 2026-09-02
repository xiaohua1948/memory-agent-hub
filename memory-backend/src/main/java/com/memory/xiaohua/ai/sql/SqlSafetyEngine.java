package com.memory.xiaohua.ai.sql;

import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text2SQL 安全校验引擎
 *
 * <p>构建「白名单 + 黑名单 + 资源限制」多重防线，将权限控制与 SQL 生成解耦：
 * <ul>
 *   <li>白名单：仅允许 SELECT 单语句；</li>
 *   <li>黑名单：拦截危险函数调用（load_file / sleep / benchmark / user 等）与
 *       {@code INTO OUTFILE} 等危险语法；</li>
 *   <li>行权限改写：基于 JSqlParser 遍历 AST，在用户照片查询最外层注入
 *       {@code user_file.user_id = ?} 隔离谓词，采用「异常即阻断」策略；</li>
 *   <li>地点条件放宽：{@code l.city = '南京'} → {@code l.city LIKE '南京%'}（见
 *       {@link #LOCATION_COLUMNS}）；</li>
 *   <li>资源限制：强制注入 LIMIT，防止全表拉取。</li>
 * </ul>
 *
 * <p>说明：照片查询统一约定「主表 file + 关联表 user_file（别名 uf）」，本引擎校验该关联
 * 必须存在，否则直接拒绝——这是比自由改写更严格的兜底，从根本上消除越权窗口。
 *
 * <p>⚠️ 坑（已修复，勿回退）：危险函数<b>绝不能</b>用 {@code lower.contains(fn)} 子串匹配。
 * 黑名单含 MySQL 函数 {@code user}（即 {@code USER()}），而合法表名 {@code user_file}、
 * 合法字段 {@code uf.user_id} 都包含 {@code user} 子串，子串匹配会把正常 SQL 误判为
 * 「SQL 包含危险函数: user」。
 * 现改为<b>函数调用形态</b>匹配：{@code fn} 前不能是标识符字符、后必须紧跟左括号（见
 * {@link #FUNCTION_CALL_PATTERNS}），从而区分 {@code user()} 与 {@code user_file}。
 *
 * <p>另注：LLM 可能在 {@code JOIN ... ON} 里写出 {@code uf.user_id = ?}，而执行侧
 * {@code JdbcTemplate.queryForList(String)} 不传参数，会抛
 * 「No value supplied for the SQL parameter」。故注入隔离谓词前，先摘除 LLM 自己写的
 * user_id 条件（WHERE 与 JOIN ON 都要处理），再由本引擎统一注入权威谓词。
 *
 * @author xiaohua
 */
@Component
public class SqlSafetyEngine {

    /**
     * 危险函数黑名单（小写）。
     *
     * <p>⚠️ <b>必须按「函数调用」形态匹配</b>（见 {@link #FUNCTION_CALL_PATTERNS}），
     * 不能直接对整条 SQL 做子串 contains —— 否则 {@code user} 会命中
     * {@code user_file}/{@code user_id}，{@code database} 会命中 {@code memory_space} 之类。
     */
    private static final Set<String> DANGEROUS_FUNCTIONS = Set.of(
            "load_file", "sleep", "benchmark", "get_lock", "release_lock",
            "sys_eval", "sys_exec", "updatexml", "extractvalue",
            "user", "database", "version", "schema"
    );

    /** 危险 SQL 片段（非函数调用形式，如 {@code SELECT ... INTO OUTFILE '/etc/passwd'}） */
    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
            Pattern.compile("\\binto\\s+outfile\\b"),
            Pattern.compile("\\binto\\s+dumpfile\\b")
    );

    /** 函数调用形态匹配：fn 前不能是标识符字符，fn 后必须紧跟左括号 */
    private static final Map<String, Pattern> FUNCTION_CALL_PATTERNS = buildFunctionCallPatterns();

    /**
     * 允许访问的表白名单（防 LLM 越权访问 user 表等敏感表）。
     *
     * <p>text2sql 职责域限定为「只查询照片」：仅放行照片主表、用户关联表及照片维度表，
     * 相册相关表（album / album_picture）属相册管理 Agent 职责，不放行。
     */
    private static final Set<String> ALLOWED_TABLES = Set.of(
            "file", "user_file", "location", "picture_tag",
            "image_meta_data", "video_meta_data"
    );

    /**
     * 需要放宽匹配的地点字段（小写列名）。
     *
     * <p>背景：{@code location} 表存的是行政全称 —— {@code province='四川省'}、
     * {@code city='西安市'}、{@code district='雁塔区'}，带「省/市/区」后缀。
     * 而 LLM 生成 SQL 时会按语言习惯把后缀去掉（用户说「西安」，LLM 写
     * {@code l.city = '西安'}），等值匹配必然 0 行，表现为「明明有照片却查不到」。
     *
     * <p>处理：把这些字段上的等值匹配改写为<b>前缀模糊匹配</b>
     * （{@code l.city = '南京'} → {@code l.city LIKE '南京%'}）。
     * 这样无论 LLM 是否带后缀都能命中：'南京%' 命中 '南京市'，'南京市%' 同样命中。
     *
     * <p>按列名匹配是安全的：{@link #ALLOWED_TABLES} 白名单内只有 {@code location}
     * 表含这三个字段，不存在同名歧义。
     */
    private static final Set<String> LOCATION_COLUMNS = Set.of("province", "city", "district");

    /**
     * 校验并改写 LLM 生成的 SQL，返回带 userId 隔离谓词 + LIMIT 的安全只读 SQL。
     *
     * @param rawSql  LLM 生成的原始 SQL
     * @param userId  当前登录用户 ID
     * @param maxRows 结果上限
     * @return 改写后的安全 SQL（不含参数占位符，可直接交给 JdbcTemplate 执行）
     */
    public String validateAndRewrite(String rawSql, Long userId, int maxRows) {
        if (rawSql == null || rawSql.isBlank()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "SQL 为空");
        }
        String sql = rawSql.trim();
        String lower = sql.toLowerCase(Locale.ROOT);

        // 1. 黑名单：多语句（首个分号后仍有内容）
        int firstSemicolon = sql.indexOf(';');
        if (firstSemicolon >= 0 && firstSemicolon != sql.length() - 1) {
            throw new BusinessException(StatusCode.OPERATION_ERROR, "SQL 包含多语句，已拒绝");
        }
        // 2. 白名单：仅 SELECT
        if (!lower.startsWith("select")) {
            throw new BusinessException(StatusCode.OPERATION_ERROR, "仅允许 SELECT 查询");
        }
        // 3. 黑名单：危险函数 / 危险语法
        checkDangerous(lower);

        try {
            Statement statement = CCJSqlParserUtil.parse(sanitizeTrailingSemicolon(sql));
            if (!(statement instanceof Select select)) {
                throw new BusinessException(StatusCode.OPERATION_ERROR, "仅允许 SELECT 查询");
            }

            // 4. 表白名单校验 + 5. 行权限改写 + 6. 强制 LIMIT
            // JSqlParser 5.0 起 Select 已是 PlainSelect/SetOperationList/ParenthesedSelect 的
            // 抽象基类，parse 结果即具体子类，就地改写即可，无需 get/setSelectBody。
            validateAndRewriteBody(select, userId, maxRows);

            String safeSql = select.toString();

            // 7. 兜底：不允许残留未绑定的 ? 占位符（queryForList 不传参，执行必炸）
            if (countPlaceholders(safeSql) > 0) {
                throw new BusinessException(StatusCode.OPERATION_ERROR, "SQL 不允许参数占位符，条件值请写成字面量");
            }
            return safeSql;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(StatusCode.OPERATION_ERROR, "SQL 解析失败: " + e.getMessage());
        }
    }

    /**
     * 递归校验 + 改写 Select（PlainSelect / SetOperationList / ParenthesedSelect，其余阻断）。
     *
     * <p>JSqlParser 5.0 起 {@code SelectBody} 接口被移除，{@link PlainSelect} /
     * {@link SetOperationList} / {@link ParenthesedSelect} 均直接继承抽象基类 {@link Select}，
     * 本方法就地（in-place）改写 AST，无需回写返回值。
     */
    private void validateAndRewriteBody(Select body, Long userId, int maxRows) {
        if (body instanceof PlainSelect ps) {
            // 表白名单校验
            validatePlainSelectTables(ps, userId, maxRows);
            // 地点条件放宽为前缀模糊匹配（= '南京' → LIKE '南京%'）
            ps.setWhere(relaxLocationPredicates(ps.getWhere()));
            relaxLocationPredicatesInJoins(ps);
            // 先摘除 LLM 自行编写的 user_id 条件（避免残留未绑定的 ? 占位符），
            // 再由本引擎注入权威的 user_id 隔离谓词（含 user_file 关联强校验）
            stripUserIdFromJoins(ps);
            ps.setWhere(appendUserIdCondition(stripUserIdConditions(ps.getWhere()), userId, ps));
            // 强制 LIMIT
            ps.setLimit(buildLimit(ps.getLimit(), maxRows));
        } else if (body instanceof SetOperationList setOp) {
            // UNION / INTERSECT / EXCEPT：逐个分支就地递归改写
            for (Select branch : setOp.getSelects()) {
                validateAndRewriteBody(branch, userId, maxRows);
            }
        } else if (body instanceof ParenthesedSelect paren) {
            Select inner = paren.getSelect();
            if (inner == null) {
                throw new BusinessException(StatusCode.OPERATION_ERROR, "不支持的表结构");
            }
            validateAndRewriteBody(inner, userId, maxRows);
        } else {
            // 其他结构（Values 等）无法安全改写 → 异常即阻断
            throw new BusinessException(StatusCode.OPERATION_ERROR, "无法安全注入权限条件，已拒绝");
        }
    }

    private void validatePlainSelectTables(PlainSelect ps, Long userId, int maxRows) {
        FromItem from = ps.getFromItem();
        if (from != null) {
            validateFromItem(from, userId, maxRows);
        }
        if (ps.getJoins() != null) {
            for (Join join : ps.getJoins()) {
                validateFromItem(join.getRightItem(), userId, maxRows);
            }
        }
    }

    private void validateFromItem(FromItem item, Long userId, int maxRows) {
        if (item instanceof Table table) {
            String name = table.getName().toLowerCase(Locale.ROOT);
            if (!ALLOWED_TABLES.contains(name)) {
                throw new BusinessException(StatusCode.OPERATION_ERROR, "禁止访问表: " + name);
            }
        } else if (item instanceof ParenthesedSelect paren) {
            // 子查询：递归校验其内部 Select（就地改写，透传 userId / maxRows）
            Select inner = paren.getSelect();
            if (inner == null) {
                throw new BusinessException(StatusCode.OPERATION_ERROR, "不支持的表结构");
            }
            validateAndRewriteBody(inner, userId, maxRows);
        } else {
            // LateralSubSelect / 函数表等复杂结构 → 阻断
            throw new BusinessException(StatusCode.OPERATION_ERROR, "不支持的表结构");
        }
    }

    // ------------------------------------------------------------------
    // 地点条件放宽（= '南京' → LIKE '南京%'）
    // ------------------------------------------------------------------

    /**
     * 递归放宽表达式树中的地点等值条件（WHERE 部分）。
     *
     * @param exp 原表达式（可为 null）
     * @return 改写后的表达式
     */
    private Expression relaxLocationPredicates(Expression exp) {
        if (exp == null) {
            return null;
        }
        Expression relaxed = relaxLocationPredicate(exp);
        if (relaxed != exp) {
            // 当前节点已被整体替换，无需下钻
            return relaxed;
        }
        if (exp instanceof AndExpression and) {
            and.setLeftExpression(relaxLocationPredicates(and.getLeftExpression()));
            and.setRightExpression(relaxLocationPredicates(and.getRightExpression()));
        } else if (exp instanceof OrExpression or) {
            or.setLeftExpression(relaxLocationPredicates(or.getLeftExpression()));
            or.setRightExpression(relaxLocationPredicates(or.getRightExpression()));
        } else if (exp instanceof ParenthesedExpressionList pel) {
            // JSqlParser 5.0 将布尔表达式括号 (...) 解析为 ParenthesedExpressionList（size=1），
            // 需递归进内部表达式；Parenthesis 已废弃（for-removal），不再使用。
            @SuppressWarnings("unchecked")
            List<Expression> exprs = pel.getExpressions();
            if (exprs != null && !exprs.isEmpty()) {
                exprs.set(0, relaxLocationPredicates(exprs.get(0)));
            }
        }
        return exp;
    }

    /** 同 {@link #relaxLocationPredicates(Expression)}，作用于 {@code JOIN ... ON} 子句 */
    private void relaxLocationPredicatesInJoins(PlainSelect ps) {
        if (ps.getJoins() == null) {
            return;
        }
        for (Join join : ps.getJoins()) {
            Collection<Expression> onExpressions = join.getOnExpressions();
            if (onExpressions == null || onExpressions.isEmpty()) {
                continue;
            }
            List<Expression> relaxed = new ArrayList<>(onExpressions.size());
            for (Expression on : onExpressions) {
                relaxed.add(relaxLocationPredicates(on));
            }
            join.setOnExpressions(relaxed);
        }
    }

    /**
     * 单节点放宽：地点字段的等值匹配 → 前缀模糊匹配。
     *
     * <p>仅处理「列 = 字符串字面量」形态；LLM 已写 {@code LIKE} 的（含 {@code %} / {@code _}
     * 通配符的）保持原样，避免二次追加通配符。
     */
    private Expression relaxLocationPredicate(Expression exp) {
        if (!(exp instanceof EqualsTo eq)
                || !(eq.getLeftExpression() instanceof Column column)
                || !(eq.getRightExpression() instanceof StringValue value)) {
            return exp;
        }
        if (!LOCATION_COLUMNS.contains(column.getColumnName().toLowerCase(Locale.ROOT))) {
            return exp;
        }
        String literal = value.getValue();
        if (literal == null || literal.isBlank() || literal.indexOf('%') >= 0 || literal.indexOf('_') >= 0) {
            return exp;
        }
        LikeExpression like = new LikeExpression();
        like.setLeftExpression(eq.getLeftExpression());
        like.setRightExpression(new StringValue(literal + "%"));
        return like;
    }

    // ------------------------------------------------------------------
    // 危险函数 / 语法校验
    // ------------------------------------------------------------------

    private void checkDangerous(String lowerSql) {
        for (Map.Entry<String, Pattern> entry : FUNCTION_CALL_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(lowerSql).find()) {
                throw new BusinessException(StatusCode.OPERATION_ERROR, "SQL 包含危险函数: " + entry.getKey());
            }
        }
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            Matcher matcher = pattern.matcher(lowerSql);
            if (matcher.find()) {
                throw new BusinessException(StatusCode.OPERATION_ERROR, "SQL 包含危险语法: " + matcher.group());
            }
        }
    }

    private static Map<String, Pattern> buildFunctionCallPatterns() {
        Map<String, Pattern> patterns = new LinkedHashMap<>();
        for (String fn : DANGEROUS_FUNCTIONS) {
            // (?<![\w.$]) —— 前置不是 字母/数字/下划线/点/$ ：排除 user_file、uf.user_id、my_database 等
            // \s*\(      —— 后置必须是左括号：确保匹配的是「函数调用」而非同名标识符
            patterns.put(fn, Pattern.compile("(?<![\\w.$])" + fn + "\\s*\\("));
        }
        return patterns;
    }

    // ------------------------------------------------------------------
    // 行权限改写
    // ------------------------------------------------------------------

    /**
     * 构造 userId 隔离条件，并强校验 user_file 关联存在。
     *
     * <p>约定：照片查询必须显式 JOIN user_file（别名 uf）。若未关联，直接拒绝，
     * 从根本上防止「查全表 file」绕过用户隔离。
     *
     * <p>⚠️ 运算符优先级（已修复，勿回退）：SQL 中 {@code AND} 优先级高于 {@code OR}。
     * 若原 WHERE 顶层是 OR，直接 {@code new AndExpression(where, condition)} 会得到
     * {@code a OR b AND uf.user_id = ?} —— 实际等价于 {@code a OR (b AND uf.user_id = ?)}，
     * 隔离谓词<b>只作用于 OR 的右分支</b>，左分支完全不受限 → 跨用户越权读取数据。
     * 故顶层为 OR 时必须先加括号：{@code (a OR b) AND uf.user_id = ?}。
     */
    private Expression appendUserIdCondition(Expression where, Long userId, PlainSelect ps) {
        String userFileAlias = findUserFileAlias(ps);
        if (userFileAlias == null) {
            throw new BusinessException(StatusCode.OPERATION_ERROR, "SQL 必须关联 user_file 表以完成用户隔离");
        }
        Column col = new Column(userFileAlias + ".user_id");
        Expression condition = new EqualsTo(col, new LongValue(userId));
        if (where == null) {
            return condition;
        }
        Expression base = where;
        if (where instanceof OrExpression) {
            // 用 ParenthesedExpressionList 而非已废弃的 Parenthesis：后者 setExpression()
            // 内部是 List.set(0, e)，对新建的空实例会抛 IndexOutOfBoundsException。
            base = new ParenthesedExpressionList<Expression>(where);
        }
        return new AndExpression(base, condition);
    }

    private String findUserFileAlias(PlainSelect ps) {
        FromItem from = ps.getFromItem();
        if (from instanceof Table table && "user_file".equalsIgnoreCase(table.getName())) {
            return table.getAlias() != null ? table.getAlias().getName() : table.getName();
        }
        if (ps.getJoins() != null) {
            for (Join join : ps.getJoins()) {
                FromItem right = join.getRightItem();
                if (right instanceof Table table && "user_file".equalsIgnoreCase(table.getName())) {
                    return table.getAlias() != null ? table.getAlias().getName() : table.getName();
                }
            }
        }
        return null;
    }

    /**
     * 摘除 LLM 自行编写的 user_id 条件（WHERE 部分）。
     *
     * <p>本引擎随后会注入权威的 {@code uf.user_id = userId}，此处先清理是为了避免
     * LLM 写出的 {@code uf.user_id = ?} 留下未绑定的占位符，或与注入谓词产生冲突。
     *
     * @param exp 原表达式（可为 null）
     * @return 处理后的表达式；条件被整体摘空时返回 {@code null}，等价于无 WHERE
     */
    private Expression stripUserIdConditions(Expression exp) {
        if (exp == null) {
            return null;
        }
        if (exp instanceof AndExpression and) {
            Expression left = stripUserIdConditions(and.getLeftExpression());
            Expression right = stripUserIdConditions(and.getRightExpression());
            if (left == null) {
                return right;
            }
            if (right == null) {
                return left;
            }
            and.setLeftExpression(left);
            and.setRightExpression(right);
            return and;
        }
        if (exp instanceof OrExpression or) {
            Expression left = stripUserIdConditions(or.getLeftExpression());
            Expression right = stripUserIdConditions(or.getRightExpression());
            if (left == null) {
                return right;
            }
            if (right == null) {
                return left;
            }
            or.setLeftExpression(left);
            or.setRightExpression(right);
            return or;
        }
        if (exp instanceof ParenthesedExpressionList pel) {
            // JSqlParser 5.0 将布尔表达式括号 (...) 解析为 ParenthesedExpressionList（size=1）。
            @SuppressWarnings("unchecked")
            List<Expression> exprs = pel.getExpressions();
            if (exprs == null || exprs.isEmpty()) {
                return exp;
            }
            Expression inner = stripUserIdConditions(exprs.get(0));
            if (inner == null) {
                return null;
            }
            exprs.set(0, inner);
            return exp;
        }
        if (exp instanceof NotExpression) {
            // NOT 内部摘空会导致语义反转，保守原样保留（若含 ? 会被占位符兜底校验拦下）
            return exp;
        }
        if (isUserIdPredicate(exp)) {
            return null;
        }
        return exp;
    }

    /**
     * 摘除 {@code JOIN ... ON} 子句中的 user_id 条件。
     *
     * <p>注意：{@link Join#setOnExpression(Expression)} 是「在下标 0 处插入」而非替换
     * （内部 {@code onExpressions.add(0, expr)}），必须改用
     * {@link Join#setOnExpressions(Collection)} 才能正确覆盖。
     */
    private void stripUserIdFromJoins(PlainSelect ps) {
        if (ps.getJoins() == null) {
            return;
        }
        for (Join join : ps.getJoins()) {
            Collection<Expression> onExpressions = join.getOnExpressions();
            if (onExpressions == null || onExpressions.isEmpty()) {
                continue;
            }
            List<Expression> kept = new ArrayList<>();
            for (Expression on : onExpressions) {
                Expression stripped = stripUserIdConditions(on);
                if (stripped != null) {
                    kept.add(stripped);
                }
            }
            if (kept.isEmpty()) {
                // JOIN 条件被摘空 → INNER JOIN 无 ON 会生成非法 SQL，直接拒绝
                throw new BusinessException(StatusCode.OPERATION_ERROR, "JOIN 条件仅含 user_id，无法安全改写，已拒绝");
            }
            join.setOnExpressions(kept);
        }
    }

    /** 判定是否为「以 user_id 列为主体的比较/包含条件」，如 {@code uf.user_id = ?}、{@code user_id IN (...)} */
    private boolean isUserIdPredicate(Expression exp) {
        if (exp instanceof BinaryExpression binary) {
            return binary.getLeftExpression() instanceof Column column
                    && "user_id".equalsIgnoreCase(column.getColumnName());
        }
        return false;
    }

    // ------------------------------------------------------------------
    // 工具方法
    // ------------------------------------------------------------------

    private Limit buildLimit(Limit existing, int maxRows) {
        Limit limit = new Limit();
        if (existing != null && existing.getOffset() != null) {
            limit.setOffset(existing.getOffset());
        }
        limit.setRowCount(new LongValue(maxRows));
        return limit;
    }

    /** 统计字符串字面量之外的 {@code ?} 数量（避免 'who?.jpg' 之类误判） */
    private int countPlaceholders(String sql) {
        int count = 0;
        boolean inStringLiteral = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inStringLiteral) {
                if (c == '\\') {
                    i++;
                } else if (c == '\'') {
                    inStringLiteral = false;
                }
            } else if (c == '\'') {
                inStringLiteral = true;
            } else if (c == '?') {
                count++;
            }
        }
        return count;
    }

    private String sanitizeTrailingSemicolon(String sql) {
        String s = sql.trim();
        while (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        return s;
    }
}
