package com.memory.xiaohua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.model.entity.FileEntity;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import com.memory.xiaohua.model.vo.entity.FileInfo;
import com.memory.xiaohua.model.vo.entity.FileMetaDataVO;
import com.memory.xiaohua.model.vo.visual.FileContentType;
import com.memory.xiaohua.model.vo.visual.FileSize;
import com.memory.xiaohua.model.vo.visual.VisualFileVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 文件存储表 Mapper 接口
 * </p>
 *
 * @author xiaohua
 * @since 2025-02-20
 */
public interface FileMapper extends BaseMapper<FileEntity> {


    /**
     * 用户是否拥有该文件
     */
    @Select("select file_id from user_file where file_id = #{fileId} and user_id = #{userId}")
    String isUserHasFile(String fileId, Long userId);

    @Select("select file_id from file where md5 = #{md5}")
    String isFileExist(String md5);


    /**
     * 查询文件信息
     */
    List<FileInfoListVO> getFileInfoList(
            Page<FileInfoListVO> page,
            @Param("orderType") String orderType,
            @Param("orderKeyword") String orderKeyword,
            @Param("imageTypeText") String imageTypeText,
            @Param("locationLevel") String locationLevel,
            @Param("locationValue") String locationValue,
            @Param("userId") Long userId,
            @Param("albumId") Long albumId,
            @Param("isDeleted") Boolean isDeleted
    );

    List<FileInfoListVO> getTagFileInfo(
            Page<FileInfoListVO> page,
            @Param("userId") Long userId,
            @Param("orderType") String orderType,
            @Param("orderKeyword") String orderKeyword,
            @Param("imageTypeText") String imageTypeText,
            @Param("tagName") String tagName,
            @Param("isDeleted") Boolean isDeleted
    );



    @Select("SELECT DISTINCT f.model FROM file f JOIN user_file uf ON f.file_id = uf.file_id WHERE uf.user_id = #{userId}  AND f.model != ' ' ")
    List<String> selectAllModels(  @Param("userId") Long userId);



    List<FileInfoListVO> selectFileInfoListByIds(
            @Param("fileIds") List<String> fileIds
    );

    @Select("select count(*) from user_file where user_id=#{userId} ")
    int selectFileToTalById(@Param("userId") Long userId);


    /**
     * 逻辑删除，删除相册内图片,删除关联表
     *
     * @param albumIds
     */
    void setIsDeletedByAlbumIds(List<Long> albumIds, Long userId);

    /**
     * 逻辑删除，删除相册内图片,删除关联表
     *
     * @param fileIds
     * @param isDeleted(1删除,0恢复)
     */
    int setIsDeletedByFileIds(List<String> fileIds, boolean isDeleted, Long userId);

    @Select("select file.file_id from file join album_picture ap on file.file_id = ap.file_id where album_id=#{albumId} and user_id = #{userId}")
    List<String> selectFileIdByAlbumId(Long albumId, Long userId);

    List<FileEntity> selectFileByIds(@Param("fileIds") List<String> fileIds, @Param("userId") Long userId);

    List<FileInfo> selectAllFileByUserId(@Param("userId") Long userId,@Param("size")Integer size);

    List<FileInfoListVO> selectAllSimilarPicture(@Param("userId") Long userId, @Param("imageTypeText") String imageTypeText);

    FileMetaDataVO selectFileMetaDataById(@Param("fileId") String fileId, @Param("userId") Long userId);

    @Select("SELECT * FROM file WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND location IS NULL")
    List<FileEntity> findUnprocessedGeoData();


    // 文件大小分布统计
    List<FileSize> selectVisualFileSize(@Param("userId") Long userId);

    // 文件类型分布统计
    List<FileContentType> selectVisualContentType(@Param("userId") Long userId);


    VisualFileVO selectFileCount(@Param("userId") Long userId);


}
