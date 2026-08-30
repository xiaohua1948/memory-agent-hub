package com.memory.xiaohua.utils.file;


import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.*;
import com.memory.xiaohua.model.entity.FileEntity;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025-04-09,23:11
 */

@Component
public class ALiYunOSSUtil {
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;


    private OSS createOSSClient() {
        // 使用配置信息创建OSSClient实例
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(
                accessKeyId,
                accessKeySecret);
        return new OSSClientBuilder().build(
                endpoint,
                credentialsProvider,
                null);
    }

    /**
     * 判断存储桶是否存在，不存在则创建
     */
    public boolean existBucket(String bucketName) {
        OSS ossClient = createOSSClient();
        try {
            if (!ossClient.doesBucketExist(bucketName)) {
                CreateBucketRequest request = new CreateBucketRequest(bucketName);
                // 设置存储桶权限为私有
                request.setCannedACL(CannedAccessControlList.Private);
                ossClient.createBucket(request);
            }
            return true;
        } catch (OSSException | ClientException e) {
            e.printStackTrace();
            return false;
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 删除存储桶（需先清空存储桶）
     */
    public Boolean removeBucket(String bucketName) {
        OSS ossClient = createOSSClient();
        try {
            // 删除所有对象
            ObjectListing listing = ossClient.listObjects(bucketName);
            for (OSSObjectSummary object : listing.getObjectSummaries()) {
                ossClient.deleteObject(bucketName, object.getKey());
            }
            // 删除存储桶
            ossClient.deleteBucket(bucketName);
            return true;
        } catch (OSSException | ClientException e) {
            e.printStackTrace();
            return false;
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 上传文件到OSS
     */
    public void uploadToOSS(String objectName, InputStream inputStream, long objectSize, String contentType) {
        OSS ossClient = createOSSClient();
        try {
            // 创建上传对象的元信息
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(objectSize);
            metadata.setContentType(contentType);

            // 创建上传请求
            PutObjectRequest request = new PutObjectRequest(
                    bucketName,
                    objectName,
                    inputStream,
                    metadata);

            ossClient.putObject(request);
        } catch (OSSException | ClientException e) {
            throw new RuntimeException("上传文件失败: " + objectName, e);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 生成带有过期时间的访问URL
     */
    public String getExpireFileUrl(String objectName, int expireTime, TimeUnit timeUnit) {
        OSS ossClient = createOSSClient();
        try {
            // 转换过期时间为秒
            long expirationSeconds = TimeUnit.SECONDS.convert(expireTime, timeUnit);
            Date expiration = new Date(System.currentTimeMillis() + expirationSeconds * 1000);

            // 生成预签名URL
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    bucketName,
                    objectName,
                    HttpMethod.GET);
            request.setExpiration(expiration);

            URL url = ossClient.generatePresignedUrl(request);
            return url.toString();
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 获取文件访问URL（默认7天有效期）
     */
    public String getFileUrl(String objectName) {
        return getExpireFileUrl(objectName, 7, TimeUnit.DAYS);
    }

    /**
     * 获取文件字节数据
     */
    public byte[] getFileBytes(String objectName) {
        OSS ossClient = createOSSClient();
        try (OSSObject ossObject = ossClient.getObject(bucketName, objectName)) {
            return IOUtils.toByteArray(ossObject.getObjectContent());
        } catch (IOException | OSSException | ClientException e) {
            throw new RuntimeException("获取文件字节失败: " + objectName, e);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 文件下载
     */
    public void download(HttpServletResponse response, FileEntity fileEntity) {
        OSS ossClient = createOSSClient();
        try (OSSObject ossObject = ossClient.getObject(bucketName, fileEntity.getFileObjectName())) {
            // 设置响应头
            response.setContentType(ossObject.getObjectMetadata().getContentType());
            response.setContentLengthLong(fileEntity.getSize());
            response.setHeader("Content-Disposition", "attachment;filename=" +
                    URLEncoder.encode(fileEntity.getOriginFileName(), StandardCharsets.UTF_8));

            IOUtils.copy(ossObject.getObjectContent(), response.getOutputStream());
        } catch (IOException | OSSException | ClientException e) {
            throw new RuntimeException("文件下载失败: " + fileEntity.getOriginFileName(), e);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 下载并压缩文件（与MinIO实现逻辑相同）
     */
    public void downloadFileToZip(ZipOutputStream zipOut, FileEntity fileEntity, Map<String, Integer> fileNameCounter) {
        // 实现逻辑与MinIO版本保持一致，仅修改获取输入流的方式
        OSS ossClient = createOSSClient();
        try (OSSObject ossObject = ossClient.getObject(bucketName, fileEntity.getFileObjectName())) {
            InputStream dataStream = ossObject.getObjectContent();

            // 以下代码与原MinIO实现相同...
            String uniqueName = generateUniqueName(fileEntity.getOriginFileName(), fileNameCounter);

            // 计算CRC32
            CRC32 crc = new CRC32();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = dataStream.read(buffer)) > 0) {
                crc.update(buffer, 0, len);
            }

            // 重新获取数据流
            dataStream.close();
            try (InputStream newStream = ossObject.getObjectContent()) {
                ZipEntry zipEntry = new ZipEntry(uniqueName);
                zipEntry.setMethod(ZipEntry.STORED);
                zipEntry.setSize(fileEntity.getSize());
                zipEntry.setCompressedSize(fileEntity.getSize());
                zipEntry.setCrc(crc.getValue());

                zipOut.putNextEntry(zipEntry);
                while ((len = newStream.read(buffer)) > 0) {
                    zipOut.write(buffer, 0, len);
                }
                zipOut.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("压缩文件下载失败: " + fileEntity.getOriginFileName(), e);
        } finally {
            ossClient.shutdown();
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
        OSS ossClient = createOSSClient();
        try {
            ossClient.deleteObject(bucketName, objectName);
        } catch (OSSException | ClientException e) {
            throw new RuntimeException("删除文件失败: " + objectName, e);
        } finally {
            ossClient.shutdown();
        }
    }
}