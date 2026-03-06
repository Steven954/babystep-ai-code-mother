package com.yupi.babystepaicodemother.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.babystepaicodemother.exception.ErrorCode;
import com.yupi.babystepaicodemother.exception.ThrowUtils;
import com.yupi.babystepaicodemother.manager.CosManager;
import com.yupi.babystepaicodemother.service.ScreenshotService;
import com.yupi.babystepaicodemother.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    @Resource
    private CosManager cosManager;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        // 鍙傛暟鏍￠獙
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAMS_ERROR, "鎴浘鐨勭綉鍧€涓嶈兘涓虹┖");
        log.info("寮€濮嬬敓鎴愮綉椤垫埅鍥撅紝URL锛歿}", webUrl);
        // 鏈湴鎴浘
        String localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        ThrowUtils.throwIf(StrUtil.isBlank(localScreenshotPath), ErrorCode.OPERATION_ERROR, "鐢熸垚缃戦〉鎴浘澶辫触");
        // 涓婁紶鍥剧墖鍒?COS
        try {
            String cosUrl = uploadScreenshotToCos(localScreenshotPath);
            ThrowUtils.throwIf(StrUtil.isBlank(cosUrl), ErrorCode.OPERATION_ERROR, "涓婁紶鎴浘鍒板璞″瓨鍌ㄥけ璐?");
            log.info("鎴浘涓婁紶鎴愬姛锛孶RL锛歿}", cosUrl);
            return cosUrl;
        } finally {
            // 娓呯悊鏈湴鏂囦欢
            cleanupLocalFile(localScreenshotPath);
        }
    }

    /**
     * 涓婁紶鎴浘鍒板璞″瓨鍌?
     *
     * @param localScreenshotPath 鏈湴鎴浘璺緞
     * @return 瀵硅薄瀛樺偍璁块棶URL锛屽け璐ヨ繑鍥瀗ull
     */
    private String uploadScreenshotToCos(String localScreenshotPath) {
        if (StrUtil.isBlank(localScreenshotPath)) {
            return null;
        }
        File screenshotFile = new File(localScreenshotPath);
        if (!screenshotFile.exists()) {
            log.error("鎴浘鏂囦欢涓嶅瓨鍦? {}", localScreenshotPath);
            return null;
        }
        // 鐢熸垚 COS 瀵硅薄閿?
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compressed.jpg";
        String cosKey = generateScreenshotKey(fileName);
        return cosManager.uploadFile(cosKey, screenshotFile);
    }

    /**
     * 鐢熸垚鎴浘鐨勫璞″瓨鍌ㄩ敭
     * 鏍煎紡锛?screenshots/2025/07/31/filename.jpg
     */
    private String generateScreenshotKey(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("/screenshots/%s/%s", datePath, fileName);
    }

    /**
     * 娓呯悊鏈湴鏂囦欢
     *
     * @param localFilePath 鏈湴鏂囦欢璺緞
     */
    private void cleanupLocalFile(String localFilePath) {
        File localFile = new File(localFilePath);
        if (localFile.exists()) {
            FileUtil.del(localFile);
            log.info("娓呯悊鏈湴鏂囦欢鎴愬姛: {}", localFilePath);
        }
    }
}

