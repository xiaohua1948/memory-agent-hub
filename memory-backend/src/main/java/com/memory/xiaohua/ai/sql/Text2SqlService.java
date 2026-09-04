package com.memory.xiaohua.ai.sql;

import com.memory.xiaohua.ai.config.AiProperties;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Text2SQL 查询服务
 *
 * <p>链路：探表结构（schema 提示）→ LLM 生成 SQL → {@link SqlSafetyEngine} 安全校验
 * → JDBC 只读执行（限时）。只允许 SELECT，结果上限由配置 {@code ai.query-max-rows} 控制。
 *
 * <p>照片查询按 user_id 隔离：{@link SqlSafetyEngine} 强制注入 {@code uf.user_id = userId}
 * 谓词，用户只能检索到自己的照片。
 *
 * @author xiaohua
 */
@Slf4j
@Service
public class Text2SqlService {

    private final SqlSafetyEngine safetyEngine;
    private final AiProperties properties;
    private final JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    @Qualifier("mainModel")
    private ChatModel mainModel;

    public Text2SqlService(SqlSafetyEngine safetyEngine, AiProperties properties, JdbcTemplate jdbcTemplate) {
        this.safetyEngine = safetyEngine;
        this.properties = properties;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 自然语言 → 照片查询结果
     *
     * @param question 用户自然语言查询
     * @param userId   当前用户（用于数据隔离）
     * @return 照片列表（含 fileId / thumbnailUrl / 时间 / 地点等字段）
     */
    public List<Map<String, Object>> queryPhotos(String question, Long userId) {
        String schema = buildSchemaHint();
        String rawSql = generateSql(question, schema);
        log.info("[AI][text2sql] userId={} LLM 生成 SQL: {}", userId, rawSql);

        String safeSql = safetyEngine.validateAndRewrite(rawSql, userId, properties.getQueryMaxRows());
        log.info("[AI][text2sql] 安全改写后 SQL: {}", safeSql);

        return jdbcTemplate.queryForList(safeSql);
    }

    private String generateSql(String question, String schema) {
        if (mainModel == null) {
            throw new IllegalStateException("主任务模型未配置");
        }
        String prompt = """
                你是照片数据库的 SQL 生成助手。请根据用户问题生成一条**只读 SELECT** 语句。

                ## 数据库表结构
                %s

                ## 强制规则
                1. 只能生成一条 SELECT 语句，禁止 INSERT/UPDATE/DELETE/DDL，禁止分号。
                2. 查询用户照片必须 JOIN user_file 表（别名 uf）并加 uf.is_deleted = 0 过滤回收站照片；
                   用户隔离由系统自动注入，你不要写 uf.user_id 条件。
                3. 表名/字段名一律使用下划线命名（如 file_id、thumbnail_url、date_time_original）。
                4. 地点用 location 表的 province/city/district 字段，标签用 picture_tag 表的 tag_name 字段。
                   地点条件必须用前缀模糊匹配 LIKE '关键词%%'，绝不能写 = '关键词'：
                   因为字段值带「省/市/区」后缀（如 city 实际值是 '南京市'），而用户/你习惯省略后缀，
                   等值匹配会查不到任何结果。例：用户说「西安的照片」→ l.city LIKE '西安%%'。
                5. 不要写 LIMIT，系统会自动添加。
                6. 只输出 SQL 本身，不要任何解释或代码块标记。
                7. SELECT 列表必须包含 file 表的 file_id、thumbnail_url、file_url、origin_file_name 四列（供前端网格展示）。
                8. 禁止使用 ? 占位符，所有条件值一律写成字面量（如 city = '南京'）。用户隔离由系统注入，不要写 user_id 条件。
                9. 文件大小条件使用 f.size 字段（BIGINT，单位字节）：1MB=1048576字节、1KB=1024字节。
                   例：用户说「2MB以上」→ f.size > 2097152；「小于500KB」→ f.size < 512000。

                ## 用户问题
                %s

                请直接输出 SQL：
                """.formatted(schema, question);

        String sql = mainModel.chat(prompt);
        return extractSql(sql);
    }

    /** 提取 SQL 本体（去除可能的 markdown 代码块包裹） */
    private String extractSql(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("```(sql)?", "").trim();
        }
        return s;
    }

    /**
     * 构造表结构提示（简化 schema）。
     *
     * <p>⚠️ 仅暴露照片查询相关表，与 {@link SqlSafetyEngine#ALLOWED_TABLES} 白名单严格一致，
     * 不暴露 album / album_picture（相册管理职责域），避免 LLM 生成越域 SQL 后被安全引擎拒绝。
     */
    private String buildSchemaHint() {
        return """
                - file(file_id VARCHAR, origin_file_name VARCHAR, category VARCHAR, content_type VARCHAR,
                       size BIGINT,  -- 文件大小，单位：字节（照片筛选常用）
                       file_url VARCHAR, thumbnail_url VARCHAR, date_time_original DATETIME,
                       width INT, height INT, make VARCHAR, model VARCHAR)
                - user_file(id BIGINT, user_id BIGINT, file_id VARCHAR, is_deleted TINYINT, upload_time DATETIME)
                - location(file_id VARCHAR, country VARCHAR, province VARCHAR, city VARCHAR,
                          district VARCHAR, township VARCHAR, full_address VARCHAR)
                  ⚠️ province/city/district 存的是行政全称，带「省/市/区」后缀，
                     例如 province='四川省'、city='南京市'、city='西安市'、district='雁塔区'。
                - picture_tag(id BIGINT, file_id VARCHAR, image_type VARCHAR, tag_name VARCHAR)
                - image_meta_data(file_id VARCHAR, ...)
                - video_meta_data(file_id VARCHAR, ...)
                """;
    }
}
