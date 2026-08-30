package com.memory.xiaohua.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.model.entity.Album;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import com.memory.xiaohua.model.vo.album.AlbumVO;
import com.memory.xiaohua.model.vo.album.LocationAlbumVO;
import com.memory.xiaohua.model.vo.album.ModelAlbumVO;
import org.apache.ibatis.annotations.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 相册信息表 Mapper 接口
 * </p>
 *
 * @author xiaohua
 * @since 2025-02-24
 */
public interface AlbumMapper extends BaseMapper<Album> {
    /**
     * 添加相册照片关联表
     */
    @Insert("insert into album_picture(album_id, file_id,user_id) VALUES (#{albumId},#{fileId},#{userId})")
    void addPictureToAlbum(Long albumId, String fileId,Long userId);

    /**
     * 删除相册照片关联表
     */
    void removePictureFromAlbum(
            @Param("albumIds") List<Long> albumIds,
            @Param("fileIds") List<String> fileIds,
            @Param("userId")Long userId
    );


    /**
     * 查询该图片是否在该相册
     *
     * @param albumId
     * @param fileId
     *
     */
    @Select("select id from album_picture where album_id=#{albumId} and file_id=#{fileId}")
    Long selectPictureIsExit(Long albumId, String fileId);


    /**
     * 查询所有相册
     */
    List<AlbumVO> selectAllAlbum(Page<AlbumVO> page, String orderKeyword, String orderType,@Param("userId")Long userId);
    List<LocationAlbumVO> selectAllLocationAlbum(
            Page<LocationAlbumVO> page,
            @Param("userId") Long userId,
            @Param("locationLevel") String locationLevel
    );

    List<ModelAlbumVO> selectAllModelAlbum(
            Page<ModelAlbumVO> page,
            @Param("userId") Long userId
    );
    List<FileInfoListVO> getModelFileInfo(
            Page<FileInfoListVO> page,
            @Param("userId") Long userId,
            @Param("orderType") String orderType,
            @Param("orderKeyword") String orderKeyword,
            @Param("imageTypeText") String imageTypeText,
            @Param("makeName") String makeName,
            @Param("modelName") String modelName,
            @Param("isDeleted") Boolean isDeleted
    );

    List<FileInfoListVO> getPersonFileInfo(
            Page<FileInfoListVO> page,
            @Param("userId") Long userId,
            @Param("orderType") String orderType,
            @Param("orderKeyword") String orderKeyword,
            @Param("imageTypeText") String imageTypeText,
            @Param("personId") Long personId,
            @Param("isDeleted") Boolean isDeleted
    );
    AlbumVO selectAlbumById(Long albumId,Long userId);
    /**
     * 修改相册封面
     *
     * @param albumId
     * @param fileId
     */
    @Transactional
    void updateAlbumCover(@Param("albumId") Long albumId, @Param("fileId") String fileId,@Param("userId")Long userId);



    void deleteAlbumByIds(@Param("albumIds") List<Long> albumIds,@Param("userId")Long userId);



}
