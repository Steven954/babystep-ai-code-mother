package com.yupi.babystepaicodemother.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.yupi.babystepaicodemother.config.CosClientConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * COS 瀵硅薄瀛樺偍绠＄悊鍣?
 */
@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 涓婁紶瀵硅薄
     *
     * @param key  鍞竴閿?
     * @param file 鏂囦欢
     * @return 涓婁紶缁撴灉
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 涓婁紶鏂囦欢鍒?COS 骞惰繑鍥炶闂?URL
     *
     * @param key  COS瀵硅薄閿紙瀹屾暣璺緞锛?
     * @param file 瑕佷笂浼犵殑鏂囦欢
     * @return 鏂囦欢鐨勮闂甎RL锛屽け璐ヨ繑鍥瀗ull
     */
    public String uploadFile(String key, File file) {
        PutObjectResult result = putObject(key, file);
        if (result != null) {
            String url = String.format("%s%s", cosClientConfig.getHost(), key);
            log.info("鏂囦欢涓婁紶鍒?COS 鎴愬姛锛歿} -> {}", file.getName(), url);
            return url;
        } else {
            log.error("鏂囦欢涓婁紶鍒?COS 澶辫触锛歿}锛岃繑鍥炵粨鏋滀负绌?", file.getName());
            return null;
        }
    }
}

