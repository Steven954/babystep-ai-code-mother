package com.yupi.babystepaicodemother.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.yupi.babystepaicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * 鏂囦欢鐩綍璇诲彇宸ュ叿
 * 浣跨敤 Hutool 绠€鍖栨枃浠舵搷浣?
 */
@Slf4j
@Component
public class FileDirReadTool extends BaseTool {

    /**
     * 闇€瑕佸拷鐣ョ殑鏂囦欢鍜岀洰褰?
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store",
            ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );

    /**
     * 闇€瑕佸拷鐣ョ殑鏂囦欢鎵╁睍鍚?
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log", ".tmp", ".cache", ".lock"
    );

    @Tool("璇诲彇鐩綍缁撴瀯锛岃幏鍙栨寚瀹氱洰褰曚笅鐨勬墍鏈夋枃浠跺拰瀛愮洰褰曚俊鎭?")
    public String readDir(
            @P("鐩綍鐨勭浉瀵硅矾寰勶紝涓虹┖鍒欒鍙栨暣涓」鐩粨鏋?")
            String relativeDirPath,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeDirPath == null ? "" : relativeDirPath);
            if (!path.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeDirPath == null ? "" : relativeDirPath);
            }
            File targetDir = path.toFile();
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                return "閿欒锛氱洰褰曚笉瀛樺湪鎴栦笉鏄洰褰?- " + relativeDirPath;
            }
            StringBuilder structure = new StringBuilder();
            structure.append("椤圭洰鐩綍缁撴瀯:\n");
            // 浣跨敤 Hutool 閫掑綊鑾峰彇鎵€鏈夋枃浠?
            List<File> allFiles = FileUtil.loopFiles(targetDir, file -> !shouldIgnore(file.getName()));
            // 鎸夎矾寰勬繁搴﹀拰鍚嶇О鎺掑簭鏄剧ず
            allFiles.stream()
                    .sorted((f1, f2) -> {
                        int depth1 = getRelativeDepth(targetDir, f1);
                        int depth2 = getRelativeDepth(targetDir, f2);
                        if (depth1 != depth2) {
                            return Integer.compare(depth1, depth2);
                        }
                        return f1.getPath().compareTo(f2.getPath());
                    })
                    .forEach(file -> {
                        int depth = getRelativeDepth(targetDir, file);
                        String indent = "  ".repeat(depth);
                        structure.append(indent).append(file.getName());
                    });
            return structure.toString();
        } catch (Exception e) {
            String errorMessage = "璇诲彇鐩綍缁撴瀯澶辫触: " + relativeDirPath + ", 閿欒: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 璁＄畻鏂囦欢鐩稿浜庢牴鐩綍鐨勬繁搴?
     */
    private int getRelativeDepth(File root, File file) {
        Path rootPath = root.toPath();
        Path filePath = file.toPath();
        return rootPath.relativize(filePath).getNameCount() - 1;
    }

    /**
     * 鍒ゆ柇鏄惁搴旇蹇界暐璇ユ枃浠舵垨鐩綍
     */
    private boolean shouldIgnore(String fileName) {
        // 妫€鏌ユ槸鍚﹀湪蹇界暐鍚嶇О鍒楄〃涓?
        if (IGNORED_NAMES.contains(fileName)) {
            return true;
        }

        // 妫€鏌ユ枃浠舵墿灞曞悕
        return IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    @Override
    public String getToolName() {
        return "readDir";
    }

    @Override
    public String getDisplayName() {
        return "璇诲彇鐩綍";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeDirPath = arguments.getStr("relativeDirPath");
        if (StrUtil.isEmpty(relativeDirPath)) {
            relativeDirPath = "鏍圭洰褰?";
        }
        return String.format("[宸ュ叿璋冪敤] %s %s", getDisplayName(), relativeDirPath);
    }
} 
