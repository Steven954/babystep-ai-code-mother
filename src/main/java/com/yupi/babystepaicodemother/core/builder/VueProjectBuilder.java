package com.yupi.babystepaicodemother.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 鏋勫缓 Vue 椤圭洰
 */
@Slf4j
@Component
public class VueProjectBuilder {

    /**
     * 寮傛鏋勫缓 Vue 椤圭洰
     *
     * @param projectPath
     */
    public void buildProjectAsync(String projectPath) {
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis())
                .start(() -> {
                    try {
                        buildProject(projectPath);
                    } catch (Exception e) {
                        log.error("寮傛鏋勫缓 Vue 椤圭洰鏃跺彂鐢熷紓甯? {}", e.getMessage(), e);
                    }
                });
    }

    /**
     * 鏋勫缓 Vue 椤圭洰
     *
     * @param projectPath 椤圭洰鏍圭洰褰曡矾寰?
     * @return 鏄惁鏋勫缓鎴愬姛
     */
    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("椤圭洰鐩綍涓嶅瓨鍦細{}", projectPath);
            return false;
        }
        // 妫€鏌ユ槸鍚︽湁 package.json 鏂囦欢
        File packageJsonFile = new File(projectDir, "package.json");
        if (!packageJsonFile.exists()) {
            log.error("椤圭洰鐩綍涓病鏈?package.json 鏂囦欢锛歿}", projectPath);
            return false;
        }
        log.info("寮€濮嬫瀯寤?Vue 椤圭洰锛歿}", projectPath);
        // 鎵ц npm install
        if (!executeNpmInstall(projectDir)) {
            log.error("npm install 鎵ц澶辫触锛歿}", projectPath);
            return false;
        }
        // 鎵ц npm run build
        if (!executeNpmBuild(projectDir)) {
            log.error("npm run build 鎵ц澶辫触锛歿}", projectPath);
            return false;
        }
        // 楠岃瘉 dist 鐩綍鏄惁鐢熸垚
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists() || !distDir.isDirectory()) {
            log.error("鏋勫缓瀹屾垚浣?dist 鐩綍鏈敓鎴愶細{}", projectPath);
            return false;
        }
        log.info("Vue 椤圭洰鏋勫缓鎴愬姛锛宒ist 鐩綍锛歿}", projectPath);
        return true;
    }

    /**
     * 鎵ц npm install 鍛戒护
     */
    private boolean executeNpmInstall(File projectDir) {
        log.info("鎵ц npm install...");
        String command = String.format("%s install", buildCommand("npm"));
        return executeCommand(projectDir, command, 300); // 5鍒嗛挓瓒呮椂
    }

    /**
     * 鎵ц npm run build 鍛戒护
     */
    private boolean executeNpmBuild(File projectDir) {
        log.info("鎵ц npm run build...");
        String command = String.format("%s run build", buildCommand("npm"));
        return executeCommand(projectDir, command, 180); // 3鍒嗛挓瓒呮椂
    }

    /**
     * 鏍规嵁鎿嶄綔绯荤粺鏋勯€犲懡浠?
     *
     * @param baseCommand
     * @return
     */
    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }

    /**
     * 鎿嶄綔绯荤粺妫€娴?
     *
     * @return
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * 鎵ц鍛戒护
     *
     * @param workingDir     宸ヤ綔鐩綍
     * @param command        鍛戒护瀛楃涓?
     * @param timeoutSeconds 瓒呮椂鏃堕棿锛堢锛?
     * @return 鏄惁鎵ц鎴愬姛
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("鍦ㄧ洰褰?{} 涓墽琛屽懡浠? {}", workingDir.getAbsolutePath(), command);
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+") // 鍛戒护鍒嗗壊涓烘暟缁?
            );
            // 绛夊緟杩涚▼瀹屾垚锛岃缃秴鏃?
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("鍛戒护鎵ц瓒呮椂锛坽}绉掞級锛屽己鍒剁粓姝㈣繘绋?", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("鍛戒护鎵ц鎴愬姛: {}", command);
                return true;
            } else {
                log.error("鍛戒护鎵ц澶辫触锛岄€€鍑虹爜: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("鎵ц鍛戒护澶辫触: {}, 閿欒淇℃伅: {}", command, e.getMessage());
            return false;
        }
    }

}

