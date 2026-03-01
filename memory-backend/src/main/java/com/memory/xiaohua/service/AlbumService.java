package com.memory.xiaohua.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.model.entity.Album;
import com.baomidou.mybatisplus.extension.service.IService;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import com.memory.xiaohua.model.vo.album.AlbumVO;
import com.memory.xiaohua.model.vo.album.LocationAlbumVO;
import com.memory.xiaohua.model.vo.album.ModelAlbumVO;

import java.util.List;

/**
 * <p>
 * 相册信息表 服务类
 * </p>
 *
 * @author xiaohua
 * @since 2025-02-24
 */
public interface AlbumService extends IService<Album> {
    void addPictureToAlbum(List<String> fileIds, Long albumId,Long userId);

    void removePictureFromAlbum(List<String> fileIds, Long albumId,Long userId);

    Page<AlbumVO> selectAllAlbum(int current, int size,String orderKeyWord,String orderType,Long userId);

    Page<LocationAlbumVO> selectAllLocationAlbum(int current, int size, String locationLevel, Long userId);

    Page<ModelAlbumVO> selectAllModelAlbum(int current, int size, Long userId);

    Page<FileInfoListVO> getModelFileInfo(Integer current, Integer size,Long userId ,String orderType, String orderKeyword, String imageTypeText, String makeName,String modelName);
    AlbumVO selectAlbumById(Long albumId,Long userId);

    Boolean updateAlbumCover(Long albumId,String fileId,Long userId);

    Boolean updateAlbumInfo(Album album,Long userId);

    Boolean deleteAlbum(List<Long> albumIds,Boolean IsDeletePicture,Long userId);


}
