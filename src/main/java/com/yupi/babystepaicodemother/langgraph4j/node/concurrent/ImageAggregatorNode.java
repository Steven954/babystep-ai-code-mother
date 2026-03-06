package com.yupi.babystepaicodemother.langgraph4j.node.concurrent;

import com.yupi.babystepaicodemother.langgraph4j.model.ImageResource;
import com.yupi.babystepaicodemother.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 鍥剧墖姹囨€昏妭鐐?
 */
@Slf4j
public class ImageAggregatorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> allImages = new ArrayList<>();
            log.info("寮€濮嬭仛鍚堝苟鍙戞敹闆嗙殑鍥剧墖");
            // 浠庡悇涓腑闂村瓧娈佃仛鍚堝浘鐗?
            if (context.getContentImages() != null) {
                allImages.addAll(context.getContentImages());
            }
            if (context.getIllustrations() != null) {
                allImages.addAll(context.getIllustrations());
            }
            if (context.getDiagrams() != null) {
                allImages.addAll(context.getDiagrams());
            }
            if (context.getLogos() != null) {
                allImages.addAll(context.getLogos());
            }
            log.info("鍥剧墖鑱氬悎瀹屾垚锛屾€诲叡 {} 寮犲浘鐗?", allImages.size());
            // 鏇存柊鏈€缁堢殑鍥剧墖鍒楄〃
            context.setImageList(allImages);
            context.setCurrentStep("鍥剧墖鑱氬悎");
            return WorkflowContext.saveContext(context);
        });
    }
} 
