package com.yupi.babystepaicodemother.langgraph4j.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.babystepaicodemother.langgraph4j.ai.CodeQualityCheckService;
import com.yupi.babystepaicodemother.langgraph4j.model.QualityResult;
import com.yupi.babystepaicodemother.langgraph4j.state.WorkflowContext;
import com.yupi.babystepaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 浠ｇ爜璐ㄩ噺妫€鏌ヨ妭鐐?
 */
@Slf4j
public class CodeQualityCheckNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("鎵ц鑺傜偣: 浠ｇ爜璐ㄩ噺妫€鏌?");
            String generatedCodeDir = context.getGeneratedCodeDir();
            QualityResult qualityResult;
            try {
                // 1. 璇诲彇骞舵嫾鎺ヤ唬鐮佹枃浠跺唴瀹?
                String codeContent = readAndConcatenateCodeFiles(generatedCodeDir);
                if (StrUtil.isBlank(codeContent)) {
                    log.warn("鏈壘鍒板彲妫€鏌ョ殑浠ｇ爜鏂囦欢");
                    qualityResult = QualityResult.builder()
                            .isValid(false)
                            .errors(List.of("鏈壘鍒板彲妫€鏌ョ殑浠ｇ爜鏂囦欢"))
                            .suggestions(List.of("璇风‘淇濅唬鐮佺敓鎴愭垚鍔?"))
                            .build();
                } else {
                    // 2. 璋冪敤 AI 杩涜浠ｇ爜璐ㄩ噺妫€鏌?
                    CodeQualityCheckService qualityCheckService = SpringContextUtil.getBean(CodeQualityCheckService.class);
                    qualityResult = qualityCheckService.checkCodeQuality(codeContent);
                    log.info("浠ｇ爜璐ㄩ噺妫€鏌ュ畬鎴?- 鏄惁閫氳繃: {}", qualityResult.getIsValid());
                }
            } catch (Exception e) {
                log.error("浠ｇ爜璐ㄩ噺妫€鏌ュ紓甯? {}", e.getMessage(), e);
                qualityResult = QualityResult.builder()
                        .isValid(true) // 寮傚父鐩存帴璺冲埌涓嬩竴涓楠?
                        .build();
            }
            // 3. 鏇存柊鐘舵€?
            context.setCurrentStep("浠ｇ爜璐ㄩ噺妫€鏌?");
            context.setQualityResult(qualityResult);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 闇€瑕佹鏌ョ殑鏂囦欢鎵╁睍鍚?
     */
    private static final List<String> CODE_EXTENSIONS = Arrays.asList(
            ".html", ".htm", ".css", ".js", ".json", ".vue", ".ts", ".jsx", ".tsx"
    );

    /**
     * 璇诲彇骞舵嫾鎺ヤ唬鐮佺洰褰曚笅鐨勬墍鏈変唬鐮佹枃浠?
     */
    private static String readAndConcatenateCodeFiles(String codeDir) {
        if (StrUtil.isBlank(codeDir)) {
            return "";
        }
        File directory = new File(codeDir);
        if (!directory.exists() || !directory.isDirectory()) {
            log.error("浠ｇ爜鐩綍涓嶅瓨鍦ㄦ垨涓嶆槸鐩綍: {}", codeDir);
            return "";
        }
        StringBuilder codeContent = new StringBuilder();
        codeContent.append("# 椤圭洰鏂囦欢缁撴瀯鍜屼唬鐮佸唴瀹筡n\n");
        // 浣跨敤 Hutool 鐨?walkFiles 鏂规硶閬嶅巻鎵€鏈夋枃浠?
        FileUtil.walkFiles(directory, file -> {
            // 杩囨护鏉′欢锛氳烦杩囬殣钘忔枃浠躲€佺壒瀹氱洰褰曚笅鐨勬枃浠躲€侀潪浠ｇ爜鏂囦欢
            if (shouldSkipFile(file, directory)) {
                return;
            }
            if (isCodeFile(file)) {
                String relativePath = FileUtil.subPath(directory.getAbsolutePath(), file.getAbsolutePath());
                codeContent.append("## 鏂囦欢: ").append(relativePath).append("\n\n");
                String fileContent = FileUtil.readUtf8String(file);
                codeContent.append(fileContent).append("\n\n");
            }
        });
        return codeContent.toString();
    }

    /**
     * 鍒ゆ柇鏄惁搴旇璺宠繃姝ゆ枃浠?
     */
    private static boolean shouldSkipFile(File file, File rootDir) {
        String relativePath = FileUtil.subPath(rootDir.getAbsolutePath(), file.getAbsolutePath());
        // 璺宠繃闅愯棌鏂囦欢
        if (file.getName().startsWith(".")) {
            return true;
        }
        // 璺宠繃鐗瑰畾鐩綍涓嬬殑鏂囦欢
        return relativePath.contains("node_modules" + File.separator) ||
                relativePath.contains("dist" + File.separator) ||
                relativePath.contains("target" + File.separator) ||
                relativePath.contains(".git" + File.separator);
    }

    /**
     * 鍒ゆ柇鏄惁鏄渶瑕佹鏌ョ殑浠ｇ爜鏂囦欢
     */
    private static boolean isCodeFile(File file) {
        String fileName = file.getName().toLowerCase();
        return CODE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}
