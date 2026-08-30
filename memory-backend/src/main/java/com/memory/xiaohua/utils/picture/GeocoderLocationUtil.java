package com.memory.xiaohua.utils.picture;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/3/1,16:47
 */
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

/**
 * 高德地图逆地理编码工具类
 * 文档参考：https://lbs.amap.com/api/webservice/guide/api/georegeo
 */
@Component
public class GeocoderLocationUtil {

    @Value("${amp.api.key}")
    private String apiKey;

    @Value("${amp.geo.url}")
    private String apiUrl;

    @Resource
    private final RestTemplate restTemplate;




    public GeocoderLocationUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }





    /**
     * 根据经纬度获取完整地址信息
     * @param longitude 经度
     * @param latitude 纬度
     * @return 结构化地址信息
     */
    public  Regeocode reverseGeocode(double longitude, double latitude) {
        String location = String.format("%.6f,%.6f", longitude, latitude);
        String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("key", apiKey)
                .queryParam("location", location)
                .queryParam("radius", 1000) // 搜索半径
                .queryParam("extensions", "all") // 返回基本地址信息
                .queryParam("roadlevel", 0) // 不返回道路信息
                .toUriString();


            ResponseEntity<GeoResponse> response = restTemplate.getForEntity(url, GeoResponse.class);
            try {    if (response.getStatusCode().is2xxSuccessful()) {
                response.getBody();
                GeoResponse body = response.getBody();
                if ("1".equals(body.getStatus())) {
                    return body.getRegeocode();
                }
                throw new GeocodingException("API请求失败: " + body.getInfo());
            }
            throw new GeocodingException("HTTP请求失败: " + response.getStatusCode());
        } catch (Exception e) {
            throw new GeocodingException("地理编码异常: " + e.getMessage(), e);
        }
    }




    public static class StringOrEmptyArrayDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);


            if (node.isArray()) {
                // 处理数组（空数组或非空数组）
                return node.isEmpty() ? null : node.get(0).asText();
            } else if (node.isTextual()) {
                // 直接返回字符串
                return node.asText();
            }

            return null; // 其他类型（如对象、数字）返回null
        }
    }
    @Data
    private static class GeoResponse {
        private String status;
        private String info;
        private Regeocode regeocode;
    }

    @Data
    public static class Regeocode {

        @JsonProperty("addressComponent")
        private AddressComponent addressComponent;
        @JsonDeserialize(using = StringOrEmptyArrayDeserializer.class)
        @JsonProperty("formatted_address")
        private String formattedAddress; // 完整地址
    }

    @Data
    public static class AddressComponent {
        // 结构化地址信息
        @JsonDeserialize(using = StringOrEmptyArrayDeserializer.class)
        @JsonProperty("country")
        private String country;  // 国家

        @JsonDeserialize(using = StringOrEmptyArrayDeserializer.class)
        @JsonProperty("province")
        private String province;  // 省

        @JsonDeserialize(using = StringOrEmptyArrayDeserializer.class)
        @JsonProperty("city")
        private String city;      // 市


        @JsonDeserialize(using = StringOrEmptyArrayDeserializer.class)
        @JsonProperty("district")
        private String district;      // 区


        @JsonDeserialize(using = StringOrEmptyArrayDeserializer.class)
        @JsonProperty("township")
        private String township;  // 乡镇


        @JsonDeserialize(using = StringOrEmptyArrayDeserializer.class)
        @JsonProperty("streetNumber")
        private StreetNumber streetNumber;
    }
    @Data
    public static class StreetNumber{

        @JsonDeserialize(using = StringOrEmptyArrayDeserializer.class)
        @JsonProperty("street")
        private String street;    // 街道

        @JsonDeserialize(using = StringOrEmptyArrayDeserializer.class)
        @JsonProperty("number")
        private String number; // 门牌号
    }



    public static class GeocodingException extends RuntimeException {
        public GeocodingException(String message) {
            super(message);
        }

        public GeocodingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}