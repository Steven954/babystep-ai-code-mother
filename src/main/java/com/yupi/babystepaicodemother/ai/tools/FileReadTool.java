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
 * 鏂囦欢璇诲彇宸ュ叿
 * 鏀寔 AI 閫氳繃宸ュ叿璋冪敤鐨勬柟寮忚鍙栨枃浠跺唴瀹?
 */
@Slf4j
@Component
public class FileReadTool extends BaseTool {

    @Tool("璇诲彇鎸囧畾璺緞鐨勬枃浠跺唴瀹?")
    public String readFile(
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
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return "閿欒锛氭枃浠朵笉瀛樺湪鎴栦笉鏄枃浠?- " + relativeFilePath;
            }
            return Files.readString(path);
        } catch (IOException e) {
            String errorMessage = "璇诲彇鏂囦欢澶辫触: " + relativeFilePath + ", 閿欒: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "readFile";
    }

    @Override
    public String getDisplayName() {
        return "璇诲彇鏂囦欢";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        return String.format("[宸ュ叿璋冪敤] %s %s", getDisplayName(), relativeFilePath);
    }
} 
