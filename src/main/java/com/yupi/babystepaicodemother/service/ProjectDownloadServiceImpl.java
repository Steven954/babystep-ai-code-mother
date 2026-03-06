package com.yupi.babystepaicodemother.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.yupi.babystepaicodemother.exception.BusinessException;
import com.yupi.babystepaicodemother.exception.ErrorCode;
import com.yupi.babystepaicodemother.exception.ThrowUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

@Service
@Slf4j
public class ProjectDownloadServiceImpl implements ProjectDownloadService {

    /**
     * 闇€瑕佽繃婊ょ殑鏂囦欢鍜岀洰褰曞悕绉?
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            ".DS_Store",
            ".env",
            "target",
            ".mvn",
            ".idea",
            ".vscode"
    );

    /**
     * 闇€瑕佽繃婊ょ殑鏂囦欢鎵╁睍鍚?
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log",
            ".tmp",
            ".cache"
    );


    @Override
    public void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response) {
        // 鍩虹鏍￠獙
        ThrowUtils.throwIf(StrUtil.isBlank(projectPath), ErrorCode.PARAMS_ERROR, "椤圭洰璺緞涓嶈兘涓虹┖");
        ThrowUtils.throwIf(StrUtil.isBlank(downloadFileName), ErrorCode.PARAMS_ERROR, "涓嬭浇鏂囦欢鍚嶄笉鑳戒负绌?");
        File projectDir = new File(projectPath);
        ThrowUtils.throwIf(!projectDir.exists(), ErrorCode.PARAMS_ERROR, "椤圭洰璺緞涓嶅瓨鍦?");
        ThrowUtils.throwIf(!projectDir.isDirectory(), ErrorCode.PARAMS_ERROR, "椤圭洰璺緞涓嶆槸涓€涓洰褰?");
        log.info("寮€濮嬫墦鍖呬笅杞介」鐩? {} -> {}.zip", projectPath, downloadFileName);
        // 璁剧疆 HTTP 鍝嶅簲澶?
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.addHeader("Content-Disposition",
                String.format("attachment; filename=\"%s.zip\"", downloadFileName));
        // 瀹氫箟鏂囦欢杩囨护鍣?
        FileFilter filter = file -> isPathAllowed(projectDir.toPath(), file.toPath());
        // 鍘嬬缉
        try {
            // 浣跨敤 Hutool 鐨?ZipUtil 鐩存帴灏嗚繃婊ゅ悗鐨勭洰褰曞帇缂╁埌鍝嶅簲杈撳嚭娴?
            ZipUtil.zip(response.getOutputStream(), StandardCharsets.UTF_8, false, filter, projectDir);
            log.info("鎵撳寘涓嬭浇椤圭洰鎴愬姛: {} -> {}.zip", projectPath, downloadFileName);
        } catch (IOException e) {
            log.error("鎵撳寘涓嬭浇椤圭洰澶辫触", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "鎵撳寘涓嬭浇椤圭洰澶辫触");
        }
    }

    /**
     * 鏍￠獙璺緞鏄惁鍏佽鍖呭惈鍦ㄥ帇缂╁寘涓?
     *
     * @param projectRoot 椤圭洰鏍圭洰褰?
     * @param fullPath    瀹屾暣璺緞
     * @return 鏄惁鍏佽
     */
    private boolean isPathAllowed(Path projectRoot, Path fullPath) {
        // 鑾峰彇鐩稿璺緞
        Path relativePath = projectRoot.relativize(fullPath);
        // 妫€鏌ヨ矾寰勪腑鐨勬瘡涓€閮ㄥ垎鏄惁绗﹀悎瑕佹眰
        for (Path part : relativePath) {
            String partName = part.toString();
            // 妫€鏌ユ槸鍚﹀湪蹇界暐鍚嶇О鍒楄〃涓?
            if (IGNORED_NAMES.contains(partName)) {
                return false;
            }
            // 妫€鏌ユ槸鍚︿互蹇界暐鎵╁睍鍚嶇粨灏?
            if (IGNORED_EXTENSIONS.stream().anyMatch(ext -> partName.toLowerCase().endsWith(ext))) {
                return false;
            }
        }
        return true;
    }
}

