package com.memory.xiaohua.utils.file;

import com.memory.xiaohua.config.MinIOConfig;
import com.memory.xiaohua.model.entity.FileEntity;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static cn.dev33.satoken.SaManager.log;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/10,21:31
 */
@Component
public class MinioOSSUtil {

    @Resource
    private MinioClient minioClient;
    @Resource
    private MinIOConfig configuration;

    /**
     * 判断bucket是否存在，不存在则创建
     */
    public boolean existBucket(String bucketName) {
        boolean exists;
        try {
            exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                exists = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            exists = false;
        }
        return exists;
    }


    /**
     * 删除bucket
     */
    public Boolean removeBucket(String bucketName) {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void uploadToOSS(String objectName, InputStream inputstream, long objectSize,
                               String contentType)  {
        // 2.8 修复：用 try-with-resources 确保异常时流也关闭
        try (inputstream) {
            // 检查存储桶是否存在
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(configuration.getBucketName())
                    .build());

            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(configuration.getBucketName())
                        .build());
            }

            // 上传文件对象
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(configuration.getBucketName())
                    .object(objectName)
                    .stream(inputstream, objectSize, -1)
                    .contentType(contentType)
                    .build());
        }
        catch (Exception e) {
            throw new RuntimeException("上传文件错误: "+objectName + e.getMessage(), e);
        }


    }




    /**
     * 构建用于生成预签名 URL 的客户端。
     *
     * 两个关键点：
     * 1) endpoint 必须是浏览器可达地址(public-url)，因为签名与 Host 绑定，用内网地址(minio:9000)
     *    生成再字符串替换 host 会破坏签名(SignatureDoesNotMatch)；
     * 2) 必须显式指定 region，否则 SDK 会先发 GetBucketLocation 探测 region，
     *    而该 endpoint 是浏览器地址，容器内不可达 → Connection refused（上传接口 500 的根因）。
     *    指定 region 后签名纯本地计算，不会真的发起连接。
     */
    private MinioClient buildSignClient() {
        String publicUrl = configuration.getPublicUrl();
        MinioClient.Builder builder = MinioClient.builder()
                .region(configuration.getRegion())
                .credentials(configuration.getAccessKey(), configuration.getSecretKey());
        if (publicUrl != null && !publicUrl.isBlank()) {
            builder.endpoint(publicUrl);
        } else {
            builder.endpoint(configuration.getUrl());
        }
        return builder.build();
    }

    /**
     * 获取文件访问地址（有过期时间）
     */
    public String getExpireFileUrl(String objectName, int time, TimeUnit timeUnit) {
        try {
            MinioClient signClient = buildSignClient();
            return signClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(configuration.getBucketName())
                    .object(objectName)
                    .expiry(time, timeUnit).build());
        }  catch (Exception e) {
            throw new RuntimeException("获取文件Url错误: "+objectName + e.getMessage(), e);
        }
    }

    /**
     * 获取文件访问地址
     *
     * 说明：预签名 URL 的签名与生成时使用的 endpoint(host) 绑定，浏览器访问的 host 必须与之一致，
     * 否则 MinIO 返回 SignatureDoesNotMatch。因此生成签名时应直接使用"浏览器可达地址"(publicUrl)
     * 作为 endpoint 构建专用 client，而不是先按内部 endpoint(minio:9000) 生成再字符串替换 host
     * （字符串替换会破坏签名）。签名 client 需显式指定 region 才会纯本地计算签名（见 buildSignClient）。
     */
    public String getFileUrl(String objectName) {
        try {
            MinioClient signClient = buildSignClient();
            return signClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(configuration.getBucketName())
                    .object(objectName)
                    .build()
            );
        }  catch (Exception e) {
            throw new RuntimeException("获取文件Url错误: "+objectName + e.getMessage(), e);
        }
    }

    /**
     * 获取对象输入流（调用方负责关闭）。
     * 大文件场景应优先使用该方法流式处理，避免整文件读入内存。
     */
    public InputStream getObjectStream(String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(configuration.getBucketName())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("获取文件流错误: " + objectName + e.getMessage(), e);
        }
    }

    /**
     * 读取对象全部字节（仅适用于小文件）
     */
    public byte[] getFileBytes(String objectName){
        // 2.8 修复：用 try-with-resources 确保流关闭
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(configuration.getBucketName())
                        .object(objectName)
                        .build())) {
            return inputStream.readAllBytes();
        }
        catch (IOException | ErrorResponseException | InsufficientDataException | InternalException |
               InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException | ServerException |
               XmlParserException e) {
            throw new RuntimeException("获取文件流错误: "+objectName + e.getMessage(), e);
        }
    }

    /**
     * 下载文件
     */
    public void download(HttpServletResponse response, FileEntity fileEntity) {
        // 2.8 修复：用 try-with-resources 确保异常时流也关闭
        try (InputStream in = minioClient.getObject(GetObjectArgs.builder().bucket(configuration.getBucketName()).object(fileEntity.getFileObjectName()).build())) {
            String originFileName = fileEntity.getOriginFileName();
            // 获取对象信息
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder().bucket(configuration.getBucketName()).object(fileEntity.getFileObjectName()).build());
            response.setContentType(stat.contentType());
            response.setContentLengthLong(fileEntity.getSize());
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(originFileName, StandardCharsets.UTF_8));
            // 文件下载
            IOUtils.copy(in, response.getOutputStream());
        } catch (Exception e) {
            throw new RuntimeException("文件下载错误: " + e.getMessage(), e);
        }
    }

    /**
     * 下载压缩文件
     */
    public void downloadFileToZip(ZipOutputStream zipOut, FileEntity fileEntity,Map<String, Integer> fileNameCounter) {

        InputStream inputStream = null;
        try {
            // 获取文件流
            inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(configuration.getBucketName())
                            .object(fileEntity.getFileObjectName())
                            .build());

            // 创建压缩条目（使用原始文件名）
            String uniqueName = generateUniqueName(fileEntity.getOriginFileName(), fileNameCounter);
            // 计算CRC32校验和
            CRC32 crc = new CRC32();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                crc.update(buffer, 0, len);
            }

            // 重置输入流
            inputStream.close();
            InputStream dataStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(configuration.getBucketName())
                            .object(fileEntity.getFileObjectName())
                            .build());

            // 创建ZipEntry并设置STORED参数
            ZipEntry zipEntry = new ZipEntry((uniqueName));
            zipEntry.setMethod(ZipEntry.STORED); // 关键：使用STORED模式
            zipEntry.setSize(fileEntity.getSize()); // 文件原始大小
            zipEntry.setCompressedSize(fileEntity.getSize()); // 压缩后大小（同原始）
            zipEntry.setCrc(crc.getValue());
            zipOut.putNextEntry(zipEntry);

            // 写入文件内容
            buffer = new byte[4096];
            while ((len = dataStream.read(buffer)) > 0) {
                zipOut.write(buffer, 0, len);
            }
            zipOut.closeEntry();
        } catch (Exception e) {
            throw new RuntimeException("下载压缩文件失败: " + fileEntity.getOriginFileName(), e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                log.error("关闭文件流失败", e);
            }
        }
    }

    // 文件名生成器（关键逻辑）
    public String generateUniqueName(String originName, Map<String, Integer> counter) {
        // 分割文件名和扩展名（兼容无扩展名文件）
        int dotIndex = originName.lastIndexOf('.');
        String baseName = (dotIndex > 0) ? originName.substring(0, dotIndex) : originName;
        String extension = (dotIndex > 0) ? originName.substring(dotIndex) : "";

        // 计算出现次数并生成新文件名
        int count = counter.getOrDefault(originName, 0);
        counter.put(originName, count + 1);

        return (count == 0) ? originName
                : String.format("%s(%d)%s", baseName, count, extension);  // 格式：test(1).jpg[5,8](@ref)
    }
    public long calculateZipTotalSize(List<FileEntity> files) throws Exception {
        long totalSize = 0;
        Map<String, Integer> nameCounter = new HashMap<>();
        // ZIP目录结尾固定22字节
        final long END_SIZE = 22;
        for (FileEntity file : files) {
            String uniqueName = generateUniqueName(file.getOriginFileName(), nameCounter);

            byte[] nameBytes = uniqueName.getBytes(StandardCharsets.UTF_8);
            // 本地文件头大小 = 30 + 文件名长度 + 额外字段（此处为0）
            long localHeaderSize = 30 + nameBytes.length;
            // 中央目录条目大小 = 46 + 文件名长度 + 额外字段（此处为0）
            long centralDirEntrySize = 46 + nameBytes.length;

            totalSize += localHeaderSize + file.getSize() + centralDirEntrySize;
        }
        return totalSize + END_SIZE; // 总大小 = 所有条目 + 目录结尾
    }

    /**
     * 删除文件
     */
    public void delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(configuration.getBucketName()).object(objectName).build());

        } catch (Exception e) {
            throw new RuntimeException("删除文件错误: " + e.getMessage(), e);
        }
    }

    /**
     * 判断 OSS 对象是否存在。
     * 用于秒传兜底校验：file 主表残留但 OSS 已被彻底删除时，不应再秒传。
     */
    public boolean exists(String objectName) {
        if (objectName == null || objectName.isEmpty()) {
            return false;
        }
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(configuration.getBucketName())
                    .object(objectName)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            // 404 表示对象不存在
            if (e.errorResponse().code() != null
                    && (e.errorResponse().code().equals("NoSuchKey")
                        || e.errorResponse().code().equals("NoSuchObject"))) {
                return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
