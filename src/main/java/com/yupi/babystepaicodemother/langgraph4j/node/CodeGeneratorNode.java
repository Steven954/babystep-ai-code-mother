package com.yupi.babystepaicodemother.langgraph4j.node;

import com.yupi.babystepaicodemother.constant.AppConstant;
import com.yupi.babystepaicodemother.core.AiCodeGeneratorFacade;
import com.yupi.babystepaicodemother.langgraph4j.model.QualityResult;
import com.yupi.babystepaicodemother.langgraph4j.state.WorkflowContext;
import com.yupi.babystepaicodemother.model.enums.CodeGenTypeEnum;
import com.yupi.babystepaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 缃戠珯浠ｇ爜鐢熸垚鑺傜偣
 */
@Slf4j
public class CodeGeneratorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("鎵ц鑺傜偣: 浠ｇ爜鐢熸垚");
            // 鏋勯€犵敤鎴锋秷鎭紙鍖呭惈鍘熷鎻愮ず璇嶅拰鍙兘鐨勯敊璇慨澶嶄俊鎭級
            String userMessage = buildUserMessage(context);
            CodeGenTypeEnum generationType = context.getGenerationType();
            // 鑾峰彇 AI 浠ｇ爜鐢熸垚澶栬鏈嶅姟
            AiCodeGeneratorFacade codeGeneratorFacade = SpringContextUtil.getBean(AiCodeGeneratorFacade.class);
            log.info("寮€濮嬬敓鎴愪唬鐮侊紝绫诲瀷: {} ({})", generationType.getValue(), generationType.getText());
            // 鍏堜娇鐢ㄥ浐瀹氱殑 appId (鍚庣画鍐嶆暣鍚堝埌涓氬姟涓?
            Long appId = 0L;
            // 璋冪敤娴佸紡浠ｇ爜鐢熸垚
            Flux<String> codeStream = codeGeneratorFacade.generateAndSaveCodeStream(userMessage, generationType, appId);
            // 鍚屾绛夊緟娴佸紡杈撳嚭瀹屾垚
            codeStream.blockLast(Duration.ofMinutes(10)); // 鏈€澶氱瓑寰?10 鍒嗛挓
            // 鏍规嵁绫诲瀷璁剧疆鐢熸垚鐩綍
            String generatedCodeDir = String.format("%s/%s_%s", AppConstant.CODE_OUTPUT_ROOT_DIR, generationType.getValue(), appId);
            log.info("AI 浠ｇ爜鐢熸垚瀹屾垚锛岀敓鎴愮洰褰? {}", generatedCodeDir);

            // 鏇存柊鐘舵€?
            context.setCurrentStep("浠ｇ爜鐢熸垚");
            context.setGeneratedCodeDir(generatedCodeDir);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 鏋勯€犵敤鎴锋秷鎭紝濡傛灉瀛樺湪璐ㄦ澶辫触缁撴灉鍒欐坊鍔犻敊璇慨澶嶄俊鎭?
     */
    private static String buildUserMessage(WorkflowContext context) {
        String userMessage = context.getEnhancedPrompt();
        // 妫€鏌ユ槸鍚﹀瓨鍦ㄨ川妫€澶辫触缁撴灉
        QualityResult qualityResult = context.getQualityResult();
        if (isQualityCheckFailed(qualityResult)) {
            // 鐩存帴灏嗛敊璇慨澶嶄俊鎭綔涓烘柊鐨勬彁绀鸿瘝锛堣捣鍒颁簡淇敼鐨勪綔鐢級
            userMessage = buildErrorFixPrompt(qualityResult);
        }
        return userMessage;
    }

    /**
     * 鍒ゆ柇璐ㄦ鏄惁澶辫触
     */
    private static boolean isQualityCheckFailed(QualityResult qualityResult) {
        return qualityResult != null &&
                !qualityResult.getIsValid() &&
                qualityResult.getErrors() != null &&
                !qualityResult.getErrors().isEmpty();
    }

    /**
     * 鏋勯€犻敊璇慨澶嶆彁绀鸿瘝
     */
    private static String buildErrorFixPrompt(QualityResult qualityResult) {
        StringBuilder errorInfo = new StringBuilder();
        errorInfo.append("\n\n## 涓婃鐢熸垚鐨勪唬鐮佸瓨鍦ㄤ互涓嬮棶棰橈紝璇蜂慨澶嶏細\n");
        // 娣诲姞閿欒鍒楄〃
        qualityResult.getErrors().forEach(error ->
                errorInfo.append("- ").append(error).append("\n"));
        // 娣诲姞淇寤鸿锛堝鏋滄湁锛?
        if (qualityResult.getSuggestions() != null && !qualityResult.getSuggestions().isEmpty()) {
            errorInfo.append("\n## 淇寤鸿锛歕n");
            qualityResult.getSuggestions().forEach(suggestion ->
                    errorInfo.append("- ").append(suggestion).append("\n"));
        }
        errorInfo.append("\n璇锋牴鎹笂杩伴棶棰樺拰寤鸿閲嶆柊鐢熸垚浠ｇ爜锛岀‘淇濅慨澶嶆墍鏈夋彁鍒扮殑闂銆?");
        return errorInfo.toString();
    }
}
