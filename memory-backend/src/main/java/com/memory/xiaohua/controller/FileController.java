package com.memory.xiaohua.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.common.BaseResponse;
import com.memory.xiaohua.common.ResultUtil;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.model.dto.DownLoadInfoDTO;
import com.memory.xiaohua.model.entity.PictureTag;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import com.memory.xiaohua.model.vo.entity.FileMetaDataVO;
import com.memory.xiaohua.model.vo.entity.ShareFileVO;
import com.memory.xiaohua.model.vo.picture.TagResult;
import com.memory.xiaohua.service.AlbumService;
import com.memory.xiaohua.service.FileService;
import com.memory.xiaohua.service.RecordService;
import com.memory.xiaohua.utils.file.FileUtil;
import com.memory.xiaohua.utils.RedisKeyConstants;
import com.memory.xiaohua.utils.auth.RedisUtil;
import com.memory.xiaohua.utils.file.MinioOSSUtil;
import com.memory.xiaohua.utils.picture.ImageTagCacheUtil;
import com.memory.xiaohua.utils.picture.ImageTagUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;


@RestController
@RequestMapping("/file")
@Tag(name = "文件接口", description = "文件接口")
public class FileController {

    @Resource
    private FileUtil fileUtil;
    @Resource
    private FileService fileService;

    @Resource
    private RecordService recordService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private ImageTagUtil imageTagUtil;

    @Resource
    private ImageTagCacheUtil imageTagCacheUtil;

    @Resource
    private MinioOSSUtil minioOSSUtil;


    @Resource
    private AlbumService albumService;

    // 上传接口已迁移至 UploadController：
    // 由"整文件单次上传"改为分片上传三阶段（pre-check 预检/秒传 → chunk 传分片 → complete 合并），
    // 支持断点续传、秒传与精确的失败重试。

    @PostMapping("/selectAllFileInfo")
    @Operation(summary = "获取文件信息", description = "分页按照排序字段、类型、图片类型获取文件信息")
    public BaseResponse<Page<FileInfoListVO>> SelectFileList(String current, String size, String orderType, String orderKeyword, String imageTypeText, String locationLevel, String locationValue, Long albumId) {

        // 分组时间
        if ("upload_time".equals(orderKeyword)) {
            orderKeyword = "upload_time";
        } else {
            orderKeyword = "date_time_original";
        }
        // 排序方式
        if ("desc".equals(orderType)) {
            orderType = "desc";
        } else {
            orderType = "asc";
        }
        int c;
        int s;
        try {
            c = Integer.parseInt(current);
            s = Integer.parseInt(size);
        } catch (NumberFormatException e) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "分页错误!");
        }

        //图片类型
        Set<String> imageTypeTextSet = new HashSet<>(Arrays.asList("picture", "gif", "video"));
        imageTypeText = imageTypeTextSet.contains(imageTypeText) ? imageTypeText : "all";

        //地理位置
        Set<String> locationLevelSet = new HashSet<>(Arrays.asList("country", "province", "city", "district"));

        if (!locationLevelSet.contains(locationLevel)) {
            locationLevel = null;
        } else {
            if ((!(locationLevel.equals("country")) && (locationValue.contains(",")))) {
                String[] split = locationValue.split(",");
                locationValue = split[1];
            }
        }

        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        if (albumId == -1) albumId = null;
        Page<FileInfoListVO> page = fileService.getFileInfoList(c, s, orderType, orderKeyword, imageTypeText, locationLevel, locationValue, userId, albumId, false);
        return ResultUtil.success(page, "获取文件信息成功!");
    }

    @PostMapping("/deleteFileByIds")
    public BaseResponse<?> deleteFileByIds(HttpServletRequest request, @RequestParam List<String> fileIds) {

        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        boolean res = fileService.setIsDeleted(fileIds, true, userId);
        //日志记录HttpServletRequest request
        if (res)
            recordService.createRecordLog("删除照片", fileIds.size(), userId, request);
        return res ? ResultUtil.success("删除成功!") : ResultUtil.error(StatusCode.PARAMS_ERROR, "删除失败!");
    }

    @PostMapping("/getDownloadToken")
    public BaseResponse<?> getDownloadToken(@RequestParam List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        String downloadToken = fileService.getDownloadToken(fileIds, userId);
        return ResultUtil.success(downloadToken, "获取下载token成功");
    }


    @PostMapping("/getDownloadSharedFileToken")
    public BaseResponse<?> downloadSharedFile(String shareToken, @RequestParam List<String> fileIds) {
        String shareKey = RedisKeyConstants.SHARE_LINK + shareToken;
        Map<String, String> shareMap = redisUtil.hgetAllAsString(shareKey);
        if (shareMap == null || shareMap.isEmpty()) {
            throw new BusinessException(StatusCode.NOT_FOUND_ERROR, "分享链接已过期或不存在");
        }
        Long sharerId = Long.parseLong(shareMap.get("userId"));
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        String downloadToken = fileService.getDownloadToken(fileIds, sharerId);
        return ResultUtil.success(downloadToken, "获取下载token成功");
    }


    @GetMapping("/downloadFileByToken")
    public void downloadFileByToken(HttpServletRequest request, HttpServletResponse response, String downloadToken) {

        if (downloadToken == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "令牌不能为空!");
        }
        DownLoadInfoDTO downLoadInfoDTO = (DownLoadInfoDTO) redisUtil.get(RedisKeyConstants.TEMP_DOWNLOAD + downloadToken);
        if (downLoadInfoDTO == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "令牌已过期!");
        }
        List<String> fileIds = downLoadInfoDTO.getFileIds();
        Long userId = downLoadInfoDTO.getUserId();
        // 2. 动态生成文件名
        String downloadZipName = "Memory_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH时mm分")) +
                "_批量下载" + fileIds.size() + "项.zip";
        if (fileIds.size() > 1) {
            fileService.downloadZipFileByIds(response, fileIds, userId, downloadZipName);
        } else {
            fileService.downloadFileByIds(response, fileIds, userId);
        }
        redisUtil.delete(RedisKeyConstants.TEMP_DOWNLOAD + downloadToken);
        //日志记录
        recordService.createRecordLog("下载照片", fileIds.size(), userId, request);
    }


    @PostMapping("/selectMetaDataByFileId")
    @Operation(summary = "获取文件媒体信息", description = "获取文件媒体信息")
    public BaseResponse<?> selectMetaDataByFileId(String fileId) {
        // 越权修复：校验文件归属当前用户（元数据含 GPS 定位等隐私）
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        FileMetaDataVO fileMetaDataVO = fileService.selectFileMetaDataById(fileId, userId);
        return ResultUtil.success(fileMetaDataVO, "获取数据成功");
    }


    @PostMapping("/createShareUrl")
    public BaseResponse<?> createShareUrl(HttpServletRequest request, Integer shareDay, @RequestParam List<String> fileIds) {

        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        if (shareDay == null || shareDay <= 0) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "分享天数必须大于0");
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());

        String shareKey = fileService.createShareUrl(fileIds, userId, shareDay);

        //日志记录
        recordService.createRecordLog("分享照片:" + shareDay + "天", fileIds.size(), userId, request);
        // 返回分享链接（这里返回短标识，前端自己拼链接）
        return ResultUtil.success(shareKey, "分享成功！");
    }


    @PostMapping("/saveSharePicture")
    public BaseResponse<?> saveSharePicture(HttpServletRequest request, @RequestParam List<String> fileIds, Long albumId, String shareToken) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        if (albumId == null || albumId == (long) (-1)) {
            albumId = (long) -1;
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());

        fileService.saveSharePicture(userId, albumId, fileIds, shareToken);
        //日志记录HttpServletRequest request
        recordService.createRecordLog("保存分享照片", fileIds.size(), userId, request);
        return ResultUtil.success("保存成功！");
    }


    @PostMapping("/getShareFileInfo")
    public BaseResponse<ShareFileVO> getShareFileInfo(String shareToken) {
        ShareFileVO shareInfo = fileService.getShareInfo(shareToken);
        return ResultUtil.success(shareInfo, "获取照片成功！");
    }

    @PostMapping("/addSomePictureTag")
    public BaseResponse<?> addSomePictureTag(HttpServletRequest request, @RequestBody ArrayList<PictureTag> pictureTags) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        for (PictureTag pictureTag : pictureTags) {
            String fileId = pictureTag.getFileId();
            List<String> fileIds = new ArrayList<>();
            fileIds.add(fileId);
           String imageType =  pictureTag.getImageType();
            String tagName = pictureTag.getTagName();
            //日志记录HttpServletRequest request
            if(imageType == null){
                imageType = "其他";
            }
          fileService.addTag(fileIds, imageType,tagName, userId);
            recordService.createRecordLog("添加照片标签:" + imageType+"-"+tagName, 1, userId, request);
        }

        return ResultUtil.success("添加成功！");
    }
    @PostMapping("/addPictureTag")
    public BaseResponse<?> addPictureTag(HttpServletRequest request, @RequestParam("fileIds") List<String> fileIds,String imageType, String tagName) {
        // 2.2 修复：tagName 校验必须在 addTag 调用之前（原顺序反了）
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "文件ID不能为空!");
        }
        if (tagName == null || tagName.isBlank()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "标签名不能为空!");
        }
        if (imageType == null || imageType.isBlank()) {
            imageType = "其他";
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        boolean res = fileService.addTag(fileIds, imageType, tagName, userId);
        recordService.createRecordLog("添加照片标签:" + imageType + "-" + tagName, 1, userId, request);
        return res ? ResultUtil.success("添加成功！") : ResultUtil.error(StatusCode.PARAMS_ERROR, "添加失败");
    }

    @PostMapping("/removePictureTag")
    public BaseResponse<?> removePictureTag(HttpServletRequest request, @RequestParam("fileIds") List<String> fileIds, String tag) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        boolean res = fileService.removeTag(fileIds, tag, userId);
        //日志记录HttpServletRequest request
        recordService.createRecordLog("删除照片标签:" + tag, 1, userId, request);
        return res ? ResultUtil.success("删除成功！") : ResultUtil.error(StatusCode.PARAMS_ERROR, "删除失败");
    }


    @PostMapping("/selectTagByFileId")
    public BaseResponse<List<String>> selectTagByFileId(String fileId) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        List<String> strings = fileService.selectTagByFileId(fileId, userId);
        return ResultUtil.success(strings, "查询成功！");
    }


    @PostMapping("/createSimilarFileList")
    public BaseResponse<?> createSimilarFileList(Double similarity, Integer size) {
        if (similarity == null) similarity = 0.80;
        if (size == null) size = 30;
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        fileService.createSimilarFileList(similarity,size,userId);
        return ResultUtil.success( "相似图片分组成功！");
    }

    @PostMapping("/getSimilarFileList")
    public BaseResponse<?> getSimilarFileList(String imageTypeText) {

        Set<String> imageTypeTextSet = new HashSet<>(Arrays.asList("picture", "gif", "video"));
        imageTypeText = imageTypeTextSet.contains(imageTypeText) ? imageTypeText : "all";
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        List<FileInfoListVO> similarFileList = fileService.getSimilarFileList(imageTypeText, userId);
        return ResultUtil.success(similarFileList, "获取相似图片数据");
    }


    @PostMapping("/getPictureTag")
    public BaseResponse<?> getPictureTag(String fileId,String thumbnailObjectName,Boolean autoAddTag) throws InterruptedException {
        // 优先读取上传后异步预热的识别结果；未命中（异步未完成/已过期）再实时识别并回填缓存
        List<TagResult> tagResult = imageTagCacheUtil.get(fileId);
        if (tagResult == null) {
            byte[] fileBytes = minioOSSUtil.getFileBytes(thumbnailObjectName);
            String base64String = Base64.getEncoder().encodeToString(fileBytes);
            tagResult = imageTagUtil.classifyImage("base64", base64String);
            imageTagCacheUtil.save(fileId, tagResult);
        }
        // 模拟基础数据准备
//        List<TagResult> tagResult = new ArrayList<>();
//
//        // 生成随机测试数据
//        String[] mockTypes = {"自然", "人物", "建筑", "动物", "交通工具"};
//        String[] mockTags = {
//                "山脉", "海滩日落", "城市天际线",
//                "肖像特写", "野生动物", "现代建筑",
//                "宠物猫", "古典轿车", "森林景观"
//        };
//
//        // 随机生成3-5个标签结果
//        int resultCount = new Random().nextInt(30) + 3;
//        for(int i=0; i<resultCount; i++){
//            TagResult result = new TagResult();
//            result.setImageType(mockTypes[new Random().nextInt(mockTypes.length)]);
//            result.setTagName(mockTags[new Random().nextInt(mockTags.length)]);
//            result.setConfidence((double) Math.round(80 + new Random().nextDouble()*20)); // 80%~99%置信度
//            tagResult.add(result);
//        }
//        tagResult.sort((a,b)->b.getConfidence().compareTo(a.getConfidence()));
       List<String> fileIds = new ArrayList<>();
       fileIds.add(fileId);
        if(autoAddTag&& !tagResult.isEmpty()){
            Long userId = Long.parseLong(StpUtil.getLoginId().toString());
            fileService.addTag(fileIds,tagResult.get(0).getImageType(),tagResult.get(0).getTagName(),userId);
        }

        return ResultUtil.success(tagResult,"图像识别成功");
    }
}

