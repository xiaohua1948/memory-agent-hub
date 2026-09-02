package com.memory.xiaohua.ai.tool;

import com.memory.xiaohua.ai.sql.Text2SqlService;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
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

    public QueryTools(Text2SqlService text2SqlService) {
        this.text2SqlService = text2SqlService;
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
        List<Map<String, Object>> result = new ArrayList<>();
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
