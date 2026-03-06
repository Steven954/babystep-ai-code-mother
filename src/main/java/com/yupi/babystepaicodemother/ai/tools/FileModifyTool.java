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
import java.nio.file.StandardOpenOption;

/**
 * 鏂囦欢淇敼宸ュ叿
 * 鏀寔 AI 閫氳繃宸ュ叿璋冪敤鐨勬柟寮忎慨鏀规枃浠跺唴瀹?
 */
@Slf4j
@Component
public class FileModifyTool extends BaseTool {

    @Tool("淇敼鏂囦欢鍐呭锛岀敤鏂板唴瀹规浛鎹㈡寚瀹氱殑鏃у唴瀹?")
    public String modifyFile(
            @P("鏂囦欢鐨勭浉瀵硅矾寰?")
            String relativeFilePath,
            @P("瑕佹浛鎹㈢殑鏃у唴瀹?")
            String oldContent,
            @P("鏇挎崲鍚庣殑鏂板唴瀹?")
            String newContent,
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
            String originalContent = Files.readString(path);
            if (!originalContent.contains(oldContent)) {
                return "璀﹀憡锛氭枃浠朵腑鏈壘鍒拌鏇挎崲鐨勫唴瀹癸紝鏂囦欢鏈慨鏀?- " + relativeFilePath;
            }
            String modifiedContent = originalContent.replace(oldContent, newContent);
            if (originalContent.equals(modifiedContent)) {
                return "淇℃伅锛氭浛鎹㈠悗鏂囦欢鍐呭鏈彂鐢熷彉鍖?- " + relativeFilePath;
            }
            Files.writeString(path, modifiedContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("鎴愬姛淇敼鏂囦欢: {}", path.toAbsolutePath());
            return "鏂囦欢淇敼鎴愬姛: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "淇敼鏂囦欢澶辫触: " + relativeFilePath + ", 閿欒: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "modifyFile";
    }

    @Override
    public String getDisplayName() {
        return "淇敼鏂囦欢";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String oldContent = arguments.getStr("oldContent");
        String newContent = arguments.getStr("newContent");
        // 鏄剧ず瀵规瘮鍐呭
        return String.format("""
                [宸ュ叿璋冪敤] %s %s
                
                鏇挎崲鍓嶏細
                ```
                %s
                ```
                
                鏇挎崲鍚庯細
                ```
                %s
                ```
                """, getDisplayName(), relativeFilePath, oldContent, newContent);
    }
}

