package com.memory.xiaohua.ai.tool;

import com.memory.xiaohua.ai.config.AiProperties;
import com.memory.xiaohua.ai.sql.Text2SqlService;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 照片查询 Agent 工具集（text2sql，纯读）
 *
 * <p>所有查询通过 {@link Text2SqlService} 生成 + 校验 + 只读执行，
 * 强制注入 userId 隔离谓词，无跨用户数据访问路径。
 *
 * @author xiaohua
 */
@Component
public class QueryTools {

    private final Text2SqlService text2SqlService;
    private final JdbcTemplate jdbcTemplate;
    private final int maxRows;

    public QueryTools(Text2SqlService text2SqlService, JdbcTemplate jdbcTemplate, AiProperties properties) {
        this.text2SqlService = text2SqlService;
        this.jdbcTemplate = jdbcTemplate;
        this.maxRows = properties.getQueryMaxRows();
    }

    /**
     * 根据自然语言查询用户照片。
     *
     * @param question 查询需求（如"威海旅游的照片"）
     * @param userId   当前用户（由编排层注入）
     * @return 照片列表 JSON
     */
    @Tool("根据自然语言查询当前用户的照片，支持按地点、时间、标签、设备等条件筛选")
    public String searchPhotos(@P("查询需求描述") String question,
                               @P("当前用户ID") Long userId) {
        if (question == null || question.isBlank()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "查询条件为空");
        }
        List<Map<String, Object>> rows = text2SqlService.queryPhotos(question, userId);
        if (rows.isEmpty()) {
            return "未找到符合条件的照片";
        }
        // 提取 fileId 列表 + 简要信息，便于下游复用
        StringBuilder sb = new StringBuilder();
        sb.append("找到 ").append(rows.size()).append(" 张照片：\n");
        for (Map<String, Object> row : rows) {
            sb.append("- ").append(row.getOrDefault("file_id", ""))
              .append(" ").append(row.getOrDefault("thumbnail_url", "")).append("\n");
        }
        return sb.toString();
    }

    /**
     * 结构化查询照片（供前端网格展示）。
     *
     * @param question 查询需求
     * @param userId   当前用户
     * @return 照片列表，字段为驼峰：fileId / thumbnailUrl / fileUrl / originFileName
     */
    public List<Map<String, Object>> searchPhotosStructured(String question, Long userId) {
        if (question == null || question.isBlank()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "查询条件为空");
        }
        List<Map<String, Object>> rows = text2SqlService.queryPhotos(question, userId);
        return toStructured(rows);
    }

    /**
     * 按相册ID查询照片（纯读，参数化 SQL，强制 user_id 隔离）。
     *
     * <p>不经过 text2sql：相册表不在 SQL 安全引擎白名单内（职责隔离设计），
     * 此处用固定参数化 SQL 直接查询，兼顾安全与能力。
     *
     * @param albumId 相册ID（调用前须校验归属）
     * @param userId  当前用户
     * @return 照片列表（字段驼峰，与 searchPhotosStructured 一致）
     */
    public List<Map<String, Object>> searchPhotosByAlbumId(Long albumId, Long userId) {
        if (albumId == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "相册ID不能为空");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT f.file_id, f.thumbnail_url, f.file_url, f.origin_file_name " +
                        "FROM album_picture ap " +
                        "JOIN file f ON f.file_id = ap.file_id " +
                        "JOIN user_file uf ON uf.file_id = f.file_id " +
                        "WHERE ap.album_id = ? AND ap.user_id = ? AND uf.user_id = ? AND uf.is_deleted = 0 " +
                        "ORDER BY ap.id DESC LIMIT ?",
                albumId, userId, userId, maxRows);
        return toStructured(rows);
    }

    private List<Map<String, Object>> toStructured(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fileId", row.get("file_id"));
            item.put("thumbnailUrl", row.get("thumbnail_url"));
            item.put("fileUrl", row.get("file_url"));
            item.put("originFileName", row.get("origin_file_name"));
            result.add(item);
        }
        return result;
    }
}
