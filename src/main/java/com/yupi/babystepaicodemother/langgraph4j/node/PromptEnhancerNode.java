package com.yupi.babystepaicodemother.langgraph4j.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.babystepaicodemother.langgraph4j.model.ImageResource;
import com.yupi.babystepaicodemother.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 鎻愮ず璇嶅寮哄伐浣滆妭鐐?
 */
@Slf4j
public class PromptEnhancerNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("鎵ц鑺傜偣: 鎻愮ず璇嶅寮?");
            // 鑾峰彇鍘熷鎻愮ず璇嶅拰鍥剧墖鍒楄〃
            String originalPrompt = context.getOriginalPrompt();
            String imageListStr = context.getImageListStr();
            List<ImageResource> imageList = context.getImageList();
            // 鏋勫缓澧炲己鍚庣殑鎻愮ず璇?
            StringBuilder enhancedPromptBuilder = new StringBuilder();
            enhancedPromptBuilder.append(originalPrompt);
            // 濡傛灉鏈夊浘鐗囪祫婧愶紝鍒欐坊鍔犲浘鐗囦俊鎭?
            if (CollUtil.isNotEmpty(imageList) || StrUtil.isNotBlank(imageListStr)) {
                enhancedPromptBuilder.append("\n\n## 鍙敤绱犳潗璧勬簮\n");
                enhancedPromptBuilder.append("璇峰湪鐢熸垚缃戠珯浣跨敤浠ヤ笅鍥剧墖璧勬簮锛屽皢杩欎簺鍥剧墖鍚堢悊鍦板祵鍏ュ埌缃戠珯鐨勭浉搴斾綅缃腑銆俓n");
                if (CollUtil.isNotEmpty(imageList)) {
                    for (ImageResource image : imageList) {
                        enhancedPromptBuilder.append("- ")
                                .append(image.getCategory().getText())
                                .append("锛?")
                                .append(image.getDescription())
                                .append("锛?")
                                .append(image.getUrl())
                                .append("锛塡n");
                    }
                } else {
                    enhancedPromptBuilder.append(imageListStr);
                }
            }
            String enhancedPrompt = enhancedPromptBuilder.toString();
            // 鏇存柊鐘舵€?
            context.setCurrentStep("鎻愮ず璇嶅寮?");
            context.setEnhancedPrompt(enhancedPrompt);
            log.info("鎻愮ず璇嶅寮哄畬鎴愶紝澧炲己鍚庨暱搴? {} 瀛楃", enhancedPrompt.length());
            return WorkflowContext.saveContext(context);
        });
    }
}
