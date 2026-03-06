package com.yupi.babystepaicodemother.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.yupi.babystepaicodemother.exception.BusinessException;
import com.yupi.babystepaicodemother.exception.ErrorCode;
import com.yupi.babystepaicodemother.langgraph4j.model.ImageResource;
import com.yupi.babystepaicodemother.langgraph4j.model.enums.ImageCategoryEnum;
import com.yupi.babystepaicodemother.manager.CosManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mermaid 鏋舵瀯鍥剧敓鎴愬伐鍏?
 */
@Slf4j
@Component
public class MermaidDiagramTool {

    @Resource
    private CosManager cosManager;
    
    @Tool("灏?Mermaid 浠ｇ爜杞崲涓烘灦鏋勫浘鍥剧墖锛岀敤浜庡睍绀虹郴缁熺粨鏋勫拰鎶€鏈叧绯?")
    public List<ImageResource> generateMermaidDiagram(@P("Mermaid 鍥捐〃浠ｇ爜") String mermaidCode,
                                                      @P("鏋舵瀯鍥炬弿杩?") String description) {
        if (StrUtil.isBlank(mermaidCode)) {
            return new ArrayList<>();
        }
        try {
            // 杞崲涓篠VG鍥剧墖
            File diagramFile = convertMermaidToSvg(mermaidCode);
            // 涓婁紶鍒癈OS
            String keyName = String.format("/mermaid/%s/%s",
                    RandomUtil.randomString(5), diagramFile.getName());
            String cosUrl = cosManager.uploadFile(keyName, diagramFile);
            // 娓呯悊涓存椂鏂囦欢
            FileUtil.del(diagramFile);
            if (StrUtil.isNotBlank(cosUrl)) {
                return Collections.singletonList(ImageResource.builder()
                        .category(ImageCategoryEnum.ARCHITECTURE)
                        .description(description)
                        .url(cosUrl)
                        .build());
            }
        } catch (Exception e) {
            log.error("鐢熸垚鏋舵瀯鍥惧け璐? {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * 灏哅ermaid浠ｇ爜杞崲涓篠VG鍥剧墖
     */
    private File convertMermaidToSvg(String mermaidCode) {
        // 鍒涘缓涓存椂杈撳叆鏂囦欢
        File tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
        FileUtil.writeUtf8String(mermaidCode, tempInputFile);
        // 鍒涘缓涓存椂杈撳嚭鏂囦欢
        File tempOutputFile = FileUtil.createTempFile("mermaid_output_", ".svg", true);
        // 鏍规嵁鎿嶄綔绯荤粺閫夋嫨鍛戒护
        String command = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : "mmdc";
        // 鏋勫缓鍛戒护
        String cmdLine = String.format("%s -i %s -o %s -b transparent",
                command,
                tempInputFile.getAbsolutePath(),
                tempOutputFile.getAbsolutePath()
        );
        // 鎵ц鍛戒护
        RuntimeUtil.execForStr(cmdLine);
        // 妫€鏌ヨ緭鍑烘枃浠?
        if (!tempOutputFile.exists() || tempOutputFile.length() == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI 鎵ц澶辫触");
        }
        // 娓呯悊杈撳叆鏂囦欢锛屼繚鐣欒緭鍑烘枃浠朵緵涓婁紶浣跨敤
        FileUtil.del(tempInputFile);
        return tempOutputFile;
    }
} 
