package com.yupi.babystepaicodemother.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.babystepaicodemother.exception.BusinessException;
import com.yupi.babystepaicodemother.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.UUID;

/**
 * 鎴浘宸ュ叿绫?
 */
@Slf4j
public class WebScreenshotUtils {

    private static final WebDriver webDriver;

    // 鍏ㄥ眬闈欐€佸垵濮嬪寲锛岄伩鍏嶉噸澶嶅垵濮嬪寲椹卞姩绋嬪簭锛?
    static {
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT = 900;
        webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * 閫€鍑烘椂閿€姣?
     */
    @PreDestroy
    public void destroy() {
        webDriver.quit();
    }

    /**
     * 鐢熸垚缃戦〉鎴浘
     *
     * @param webUrl 瑕佹埅鍥剧殑缃戝潃
     * @return 鍘嬬缉鍚庣殑鎴浘鏂囦欢璺緞锛屽け璐ヨ繑鍥?null
     */
    public static String saveWebPageScreenshot(String webUrl) {
        // 闈炵┖鏍￠獙
        if (StrUtil.isBlank(webUrl)) {
            log.error("缃戦〉鎴浘澶辫触锛寀rl涓虹┖");
            return null;
        }
        // 鍒涘缓涓存椂鐩綍
        try {
            String rootPath = System.getProperty("user.dir") + "/tmp/screenshots/" + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);
            // 鍥剧墖鍚庣紑
            final String IMAGE_SUFFIX = ".png";
            // 鍘熷鍥剧墖淇濆瓨璺緞
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + IMAGE_SUFFIX;
            // 璁块棶缃戦〉
            webDriver.get(webUrl);
            // 绛夊緟缃戦〉鍔犺浇
            waitForPageLoad(webDriver);
            // 鎴浘
            byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            // 淇濆瓨鍘熷鍥剧墖
            saveImage(screenshotBytes, imageSavePath);
            log.info("鍘熷鎴浘淇濆瓨鎴愬姛锛歿}", imageSavePath);
            // 鍘嬬缉鍥剧墖
            final String COMPRESS_SUFFIX = "_compressed.jpg";
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + COMPRESS_SUFFIX;
            compressImage(imageSavePath, compressedImagePath);
            log.info("鍘嬬缉鍥剧墖淇濆瓨鎴愬姛锛歿}", compressedImagePath);
            // 鍒犻櫎鍘熷鍥剧墖
            FileUtil.del(imageSavePath);
            return compressedImagePath;
        } catch (Exception e) {
            log.error("缃戦〉鎴浘澶辫触锛歿}", webUrl, e);
            return null;
        }
    }

    /**
     * 鍒濆鍖?Chrome 娴忚鍣ㄩ┍鍔?
     */
    private static WebDriver initChromeDriver(int width, int height) {
        try {
            // 鑷姩绠＄悊 ChromeDriver
            WebDriverManager.chromedriver().setup();
            // 閰嶇疆 Chrome 閫夐」
            ChromeOptions options = new ChromeOptions();
            // 鏃犲ご妯″紡
            options.addArguments("--headless");
            // 绂佺敤GPU锛堝湪鏌愪簺鐜涓嬮伩鍏嶉棶棰橈級
            options.addArguments("--disable-gpu");
            // 绂佺敤娌欑洅妯″紡锛圖ocker鐜闇€瑕侊級
            options.addArguments("--no-sandbox");
            // 绂佺敤寮€鍙戣€卻hm浣跨敤
            options.addArguments("--disable-dev-shm-usage");
            // 璁剧疆绐楀彛澶у皬
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // 绂佺敤鎵╁睍
            options.addArguments("--disable-extensions");
            // 璁剧疆鐢ㄦ埛浠ｇ悊
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // 鍒涘缓椹卞姩
            WebDriver driver = new ChromeDriver(options);
            // 璁剧疆椤甸潰鍔犺浇瓒呮椂
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 璁剧疆闅愬紡绛夊緟
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("鍒濆鍖?Chrome 娴忚鍣ㄥけ璐?", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "鍒濆鍖?Chrome 娴忚鍣ㄥけ璐?");
        }
    }

    /**
     * 淇濆瓨鍥剧墖鍒版枃浠?
     *
     * @param imageBytes
     * @param imagePath
     */
    private static void saveImage(byte[] imageBytes, String imagePath) {
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("淇濆瓨鍥剧墖澶辫触锛歿}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "淇濆瓨鍥剧墖澶辫触");
        }
    }

    /**
     * 鍘嬬缉鍥剧墖
     *
     * @param originImagePath
     * @param compressedImagePath
     */
    private static void compressImage(String originImagePath, String compressedImagePath) {
        // 鍘嬬缉鍥剧墖璐ㄩ噺锛?.1 = 10% 璐ㄩ噺锛?
        final float COMPRESSION_QUALITY = 0.3f;
        try {
            ImgUtil.compress(
                    FileUtil.file(originImagePath),
                    FileUtil.file(compressedImagePath),
                    COMPRESSION_QUALITY
            );
        } catch (Exception e) {
            log.error("鍘嬬缉鍥剧墖澶辫触锛歿} -> {}", originImagePath, compressedImagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "鍘嬬缉鍥剧墖澶辫触");
        }
    }

    /**
     * 绛夊緟椤甸潰鍔犺浇瀹屾垚
     *
     * @param webDriver
     */
    private static void waitForPageLoad(WebDriver webDriver) {
        try {
            // 鍒涘缓绛夊緟椤甸潰鍔犺浇瀵硅薄
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
            // 绛夊緟 document.readyState 涓?complete
            wait.until(driver -> ((JavascriptExecutor) driver)
                    .executeScript("return document.readyState").
                    equals("complete")
            );
            // 棰濆绛夊緟涓€娈垫椂闂达紝纭繚鍔ㄦ€佸唴瀹瑰姞杞藉畬鎴?
            Thread.sleep(2000);
            log.info("椤甸潰鍔犺浇瀹屾垚");
        } catch (Exception e) {
            log.error("绛夊緟椤甸潰鍔犺浇鏃跺嚭鐜板紓甯革紝缁х画鎵ц鎴浘", e);
        }
    }
}

