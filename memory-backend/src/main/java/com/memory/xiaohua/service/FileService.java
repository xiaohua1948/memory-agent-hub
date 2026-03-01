package com.memory.xiaohua.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import com.memory.xiaohua.model.vo.entity.FileMetaDataVO;
import com.memory.xiaohua.model.vo.entity.ShareFileVO;
import com.memory.xiaohua.model.vo.visual.FileTagVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;


@Service
public interface FileService {
    Boolean uploadImageFile(Long userId, MultipartFile multipartFile, String fileSuffix, LocalDateTime lastModifiedTime,Long albumId);

    Boolean uploadVideoFile(Long userId, MultipartFile multipartFile, String fileSuffix, LocalDateTime lastModifiedTime,Long albumId);

    Boolean checkIsUpload(Long userId, Long albumId,String fileMd5);

    void saveSharePicture(Long userId, Long albumId,List<String> fileIds);



    Page<FileInfoListVO> getFileInfoList(Integer current,Integer size,String orderType,String orderKeyword,String imageTypeText,String locationLevel,String locationValue, Long userId, Long albumId,Boolean isDeleted);

    Page<FileInfoListVO> getTagFileInfo(Integer current, Integer size,Long userId, String orderType, String orderKeyword, String imageTypeText, String tag);

    List<FileInfoListVO> getSimilarFileList(String imageTypeText,Long userId);

    void createSimilarFileList(Double similarity,Integer size,Long userId);
    Boolean setIsDeleted(List<String> fileIds,boolean isDeleted,Long useId);

    void downloadFileByIds(HttpServletResponse response, List<String> fileIds, Long useId);
    String getDownloadAlbumToken(Long albumId, Long userId);
    String getDownloadToken(List<String> fileIds,Long userId);

    void downloadAlbumByToken(HttpServletRequest request, HttpServletResponse response, Long albumId, Long userId);
    void downloadZipFileByIds(HttpServletResponse response, List<String> fileIds, Long useId,String downloadZipName);

    FileMetaDataVO selectFileMetaDataById(String fileId, Long userId);

    String createShareUrl(List<String> fileIds,Long userId,Integer shareDay);

    String createAlbumShareUrl(List<Long> albumIds, Long userId, Integer shareDay);

    ShareFileVO getShareInfo(String shareKey);

    boolean addTag(List<String> fileIds,String imageType,String tagName,Long userId);

    boolean removeTag(List<String> fileIds,String tag,Long userId);

    List<String> selectTagByFileId(String fileId,Long userId);
    List<String> selectAllModels(Long userId);

    void cronUpdateFileUrl();


}
