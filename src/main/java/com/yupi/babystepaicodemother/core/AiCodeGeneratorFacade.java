package com.yupi.babystepaicodemother.core;

import cn.hutool.json.JSONUtil;
import com.yupi.babystepaicodemother.ai.AiCodeGeneratorService;
import com.yupi.babystepaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.yupi.babystepaicodemother.ai.model.HtmlCodeResult;
import com.yupi.babystepaicodemother.ai.model.MultiFileCodeResult;
import com.yupi.babystepaicodemother.ai.model.message.AiResponseMessage;
import com.yupi.babystepaicodemother.ai.model.message.ToolExecutedMessage;
import com.yupi.babystepaicodemother.ai.model.message.ToolRequestMessage;
import com.yupi.babystepaicodemother.constant.AppConstant;
import com.yupi.babystepaicodemother.core.builder.VueProjectBuilder;
import com.yupi.babystepaicodemother.core.parser.CodeParserExecutor;
import com.yupi.babystepaicodemother.core.saver.CodeFileSaverExecutor;
import com.yupi.babystepaicodemother.exception.BusinessException;
import com.yupi.babystepaicodemother.exception.ErrorCode;
import com.yupi.babystepaicodemother.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 浠ｇ爜鐢熸垚闂ㄩ潰绫伙紝缁勫悎浠ｇ爜鐢熸垚鍜屼繚瀛樺姛鑳?
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 缁熶竴鍏ュ彛锛氭牴鎹被鍨嬬敓鎴愬苟淇濆瓨浠ｇ爜
     *
     * @param userMessage     鐢ㄦ埛鎻愮ず璇?
     * @param codeGenTypeEnum 鐢熸垚绫诲瀷
     * @param appId           搴旂敤 ID
     * @return 淇濆瓨鐨勭洰褰?
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "鐢熸垚绫诲瀷涓嶈兘涓虹┖");
        }
        // 鏍规嵁 appId 鑾峰彇鐩稿簲鐨?AI 鏈嶅姟瀹炰緥
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "涓嶆敮鎸佺殑鐢熸垚绫诲瀷锛?" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 缁熶竴鍏ュ彛锛氭牴鎹被鍨嬬敓鎴愬苟淇濆瓨浠ｇ爜锛堟祦寮忥級
     *
     * @param userMessage     鐢ㄦ埛鎻愮ず璇?
     * @param codeGenTypeEnum 鐢熸垚绫诲瀷
     * @param appId           搴旂敤 ID
     * @return 淇濆瓨鐨勭洰褰?
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "鐢熸垚绫诲瀷涓嶈兘涓虹┖");
        }
        // 鏍规嵁 appId 鑾峰彇鐩稿簲鐨?AI 鏈嶅姟瀹炰緥
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream, appId);
            }
            default -> {
                String errorMessage = "涓嶆敮鎸佺殑鐢熸垚绫诲瀷锛?" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 灏?TokenStream 杞崲涓?Flux<String>锛屽苟浼犻€掑伐鍏疯皟鐢ㄤ俊鎭?
     *
     * @param tokenStream TokenStream 瀵硅薄
     * @param appId       搴旂敤 ID
     * @return Flux<String> 娴佸紡鍝嶅簲
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        // 鎵ц Vue 椤圭洰鏋勫缓锛堝悓姝ユ墽琛岋紝纭繚棰勮鏃堕」鐩凡灏辩华锛?
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                        vueProjectBuilder.buildProject(projectPath);
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }

    /**
     * 閫氱敤娴佸紡浠ｇ爜澶勭悊鏂规硶
     *
     * @param codeStream  浠ｇ爜娴?
     * @param codeGenType 浠ｇ爜鐢熸垚绫诲瀷
     * @param appId       搴旂敤 ID
     * @return 娴佸紡鍝嶅簲
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        // 瀛楃涓叉嫾鎺ュ櫒锛岀敤浜庡綋娴佸紡杩斿洖鎵€鏈夌殑浠ｇ爜涔嬪悗锛屽啀淇濆瓨浠ｇ爜
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            // 瀹炴椂鏀堕泦浠ｇ爜鐗囨
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            // 娴佸紡杩斿洖瀹屾垚鍚庯紝淇濆瓨浠ｇ爜
            try {
                String completeCode = codeBuilder.toString();
                // 浣跨敤鎵ц鍣ㄨВ鏋愪唬鐮?
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                // 浣跨敤鎵ц鍣ㄤ繚瀛樹唬鐮?
                File saveDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("淇濆瓨鎴愬姛锛岀洰褰曚负锛歿}", saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("淇濆瓨澶辫触: {}", e.getMessage());
            }
        });
    }
}

