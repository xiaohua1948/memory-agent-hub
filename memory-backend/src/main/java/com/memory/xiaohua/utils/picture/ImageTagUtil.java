package com.memory.xiaohua.utils.picture;

import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.image.v2.ImageClient;
import com.huaweicloud.sdk.image.v2.model.*;
import com.huaweicloud.sdk.image.v2.region.ImageRegion;
import com.memory.xiaohua.model.vo.picture.TagResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025-04-07,16:10
 */

@Component
public class ImageTagUtil {

    @Value("${huawei.cloud.image.ak}")
    private String AccessKey;

    @Value("${huawei.cloud.image.sk}")
    private String SecretAccessKey;
    @Value("${huawei.cloud.image.region}")
    private String region;
    @Value("${huawei.cloud.image.endpoint}")
    private String endpoint;
    @Value("${huawei.cloud.image.threshold}")
    private Float threshold;
    @Value("${huawei.cloud.image.language}")
    private String language;

    public List<TagResult> classifyImage(String requestImageType, String image) {
        ICredential auth = new BasicCredentials()
                .withAk(AccessKey)
                .withSk(SecretAccessKey);

        ImageClient client = ImageClient.newBuilder()
                .withCredential(auth)
                .withRegion(ImageRegion.valueOf(region))
                .build();

        ImageTaggingReq body = new ImageTaggingReq()
                .withThreshold(threshold)
                .withLanguage(language);  // 使用Base64图像数据
        if (requestImageType.equals("url")) {
            body.withUrl(image);
        } else {
            body.withImage(image);
        }
        RunImageTaggingRequest   request = new RunImageTaggingRequest().withBody(body);

        try {
            RunImageTaggingResponse  response = client.runImageTagging(request);
            return parseResponse(response.getResult());
        } catch (ServiceResponseException e) {
            throw new RuntimeException("华为API调用失败: " + e.getErrorMsg());
        }
    }

    private List<TagResult> parseResponse(ImageTaggingResponseResult  result) {
        return result.getTags().stream()
                .map(tag -> new TagResult(tag.getType(), tag.getTag(), Double.parseDouble(tag.getConfidence())))
                .collect(Collectors.toList());
    }



}