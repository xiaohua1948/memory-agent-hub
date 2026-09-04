package com.memory.xiaohua.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.mapper.AlbumMapper;
import com.memory.xiaohua.model.entity.Album;
import com.memory.xiaohua.service.AlbumService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 相册管理 Agent 工具集
 *
 * <p>⚠️ 这些工具**不直接暴露给大模型自主执行**，而是由编排层在用户确认后调用
 * （human-in-the-loop），这里提供的是「执行实现」。方法被标记为 @Tool 是为了
 * 工具规格生成与文档统一，实际执行由 {@code AlbumAgentExecutor} 在确认后触发。
 *
 * @author xiaohua
 */
@Component
public class AlbumTools {

    private final AlbumService albumService;
    private final AlbumMapper albumMapper;

    public AlbumTools(AlbumService albumService, AlbumMapper albumMapper) {
        this.albumService = albumService;
        this.albumMapper = albumMapper;
    }

    /**
     * 创建相册（用户确认后执行）
     */
    @Tool("创建一个新相册，返回相册ID")
    public Long createAlbum(@P("相册名称") String albumName,
                            @P("相册描述，可为空") String description,
                            @P("当前用户ID") Long userId) {
        if (albumName == null || albumName.isBlank() || albumName.length() > 30) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "相册名称不合法");
        }
        Album album = new Album();
        album.setUserId(userId);
        album.setAlbumName(albumName);
        if (description != null && !description.isBlank() && !"null".equalsIgnoreCase(description)) {
            album.setDescription(description.trim());
        }
        album.setStatus(0);
        albumMapper.insert(album);
        return album.getAlbumId();
    }

    /**
     * 添加照片到相册（用户确认后执行）
     */
    @Tool("将指定照片批量加入相册")
    public void addPicturesToAlbum(@P("照片ID列表") List<String> fileIds,
                                   @P("相册ID") Long albumId,
                                   @P("当前用户ID") Long userId) {
        if (fileIds == null || fileIds.isEmpty() || albumId == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误");
        }
        albumService.addPictureToAlbum(fileIds, albumId, userId);
    }

    /**
     * 更新相册名称/描述（用户确认后执行）
     */
    public void updateAlbumInfo(Long albumId, String albumName, String description, Long userId) {
        if (albumId == null || albumName == null || albumName.isBlank()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "相册更新参数不完整");
        }
        Album album = new Album();
        album.setAlbumId(albumId);
        album.setAlbumName(albumName);
        album.setDescription(description == null ? "" : description);
        albumService.updateAlbumInfo(album, userId);
    }

    /**
     * 删除相册（用户确认后执行），isDeletePicture=true 时相册内照片一并逻辑删除
     */
    public void deleteAlbum(Long albumId, boolean isDeletePicture, Long userId) {
        if (albumId == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "相册ID不能为空");
        }
        albumService.deleteAlbum(List.of(albumId), isDeletePicture, userId);
    }

    /**
     * 从相册移除照片（用户确认后执行）
     */
    @Tool("将指定照片从相册移除")
    public void removePicturesFromAlbum(@P("照片ID列表") List<String> fileIds,
                                        @P("相册ID") Long albumId,
                                        @P("当前用户ID") Long userId) {
        if (fileIds == null || fileIds.isEmpty() || albumId == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误");
        }
        albumService.removePictureFromAlbum(fileIds, albumId, userId);
    }

    /**
     * 查询用户所有相册（纯读，可被 Router 直接调用）
     */
    @Tool("列出当前用户的所有相册")
    public String listAlbums(@P("当前用户ID") Long userId) {
        List<Album> albums = albumMapper.selectList(
                new QueryWrapper<Album>().eq("user_id", userId).orderByDesc("create_time"));
        if (albums.isEmpty()) {
            return "暂无相册";
        }
        StringBuilder sb = new StringBuilder("相册列表：\n");
        for (Album a : albums) {
            sb.append("- ").append(a.getAlbumId()).append(" ").append(a.getAlbumName()).append("\n");
        }
        return sb.toString();
    }
}
