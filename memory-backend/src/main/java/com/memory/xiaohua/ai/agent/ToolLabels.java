package com.memory.xiaohua.ai.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Map;

/**
 * 工具调用 → 人话步骤标题
 *
 * <p>用户不该看到 {@code searchPhotos} 这种英文方法名。这里把工具名 + 调用参数
 * 渲染成「查询照片：上海且大于2MB」「创建相册「南京之旅」」这类可直接展示的文案。
 *
 * <p>标题由<b>后端</b>生成而非前端映射：工具集扩展时只需改这一处，
 * 前端保持"给什么显示什么"的哑渲染即可。
 *
 * @author xiaohua
 */
public final class ToolLabels {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 写操作工具集：这些工具只生成待确认草稿，
     * 对应步骤在界面上应显示为「待确认」而非「已完成」。
     */
    public static final java.util.Set<String> WRITE_TOOLS = java.util.Set.of(
            "createAlbum", "addPicturesToAlbum", "updateAlbumInfo",
            "deleteAlbum", "removePicturesFromAlbum");

    private ToolLabels() {
    }

    /**
     * 生成步骤标题。
     *
     * @param tool      工具名
     * @param arguments 模型传入的参数 JSON
     */
    public static String label(String tool, String arguments) {
        Map<String, Object> args = parseArgs(arguments);
        String t = tool == null ? "" : tool;
        return switch (t) {
            case "searchPhotos" -> "查询照片：" + abbreviate(str(args.get("question")), 24);
            case "searchPhotosByAlbum" -> "查看相册「" + str(args.get("albumNameOrId")) + "」的照片";
            case "queryAlbums" -> "查询相册：" + abbreviate(str(args.get("question")), 24);
            case "getPhotosExif" -> "读取照片拍摄参数";
            case "listAlbums" -> "查看相册列表";
            case "createAlbum" -> "创建相册「" + str(args.get("albumName")) + "」";
            case "addPicturesToAlbum" -> "向相册「" + str(args.get("albumNameOrId")) + "」添加照片";
            case "updateAlbumInfo" -> "修改相册「" + str(args.get("albumNameOrId")) + "」";
            case "deleteAlbum" -> "删除相册「" + str(args.get("albumNameOrId")) + "」";
            case "removePicturesFromAlbum" -> "从相册「" + str(args.get("albumNameOrId")) + "」移除照片";
            case "generateHtmlPage" -> "生成网页：" + abbreviate(str(args.get("requirement")), 20);
            case "downloadPhotos" -> "生成下载清单";
            default -> "执行 " + t;
        };
    }

    /** 是否为写操作工具（决定步骤终态是「待确认」还是「已完成」） */
    public static boolean isWriteTool(String tool) {
        return tool != null && WRITE_TOOLS.contains(tool);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseArgs(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(arguments, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static String abbreviate(String s, int max) {
        String t = s.trim().replaceAll("\\s+", " ");
        if (t.isEmpty()) {
            return "全部照片";
        }
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    /** 取工具返回摘要的首行（步骤的 detail 只用一句话，避免撑爆步骤条） */
    public static String firstLine(String text, int max) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String t = text.trim().replaceAll("\\s+", " ");
        int cut = t.length() <= max ? t.length() : max;
        String head = t.substring(0, cut);
        // 尽量在句号处截断，避免半句话
        int dot = head.lastIndexOf('。');
        if (t.length() > max && dot > max / 2) {
            head = head.substring(0, dot + 1);
        } else if (t.length() > max) {
            head = head + "…";
        }
        return head.toLowerCase(Locale.ROOT).startsWith("工具执行失败") ? head : head;
    }
}
