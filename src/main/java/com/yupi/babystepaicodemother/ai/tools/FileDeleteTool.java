package com.yupi.babystepaicodemother.ai.tools;

import cn.hutool.json.JSONObject;
import com.yupi.babystepaicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 鏂囦欢鍒犻櫎宸ュ叿
 * 鏀寔 AI 閫氳繃宸ュ叿璋冪敤鐨勬柟寮忓垹闄ゆ枃浠?
 */
@Slf4j
@Component
public class FileDeleteTool extends BaseTool {

    @Tool("鍒犻櫎鎸囧畾璺緞鐨勬枃浠?")
    public String deleteFile(
            @P("鏂囦欢鐨勭浉瀵硅矾寰?")
            String relativeFilePath,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }
            if (!Files.exists(path)) {
                return "璀﹀憡锛氭枃浠朵笉瀛樺湪锛屾棤闇€鍒犻櫎 - " + relativeFilePath;
            }
            if (!Files.isRegularFile(path)) {
                return "閿欒锛氭寚瀹氳矾寰勪笉鏄枃浠讹紝鏃犳硶鍒犻櫎 - " + relativeFilePath;
            }
            // 瀹夊叏妫€鏌ワ細閬垮厤鍒犻櫎閲嶈鏂囦欢
            String fileName = path.getFileName().toString();
            if (isImportantFile(fileName)) {
                return "閿欒锛氫笉鍏佽鍒犻櫎閲嶈鏂囦欢 - " + fileName;
            }
            Files.delete(path);
            log.info("鎴愬姛鍒犻櫎鏂囦欢: {}", path.toAbsolutePath());
            return "鏂囦欢鍒犻櫎鎴愬姛: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "鍒犻櫎鏂囦欢澶辫触: " + relativeFilePath + ", 閿欒: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 鍒ゆ柇鏄惁鏄噸瑕佹枃浠讹紝涓嶅厑璁稿垹闄?
     */
    private boolean isImportantFile(String fileName) {
        String[] importantFiles = {
                "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
                "vite.config.js", "vite.config.ts", "vue.config.js",
                "tsconfig.json", "tsconfig.app.json", "tsconfig.node.json",
                "index.html", "main.js", "main.ts", "App.vue", ".gitignore", "README.md"
        };
        for (String important : importantFiles) {
            if (important.equalsIgnoreCase(fileName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getToolName() {
        return "deleteFile";
    }

    @Override
    public String getDisplayName() {
        return "鍒犻櫎鏂囦欢";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        return String.format(" [宸ュ叿璋冪敤] %s %s", getDisplayName(), relativeFilePath);
    }
}

