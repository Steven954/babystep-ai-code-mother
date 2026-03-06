package com.yupi.babystepaicodemother.langgraph4j.node.concurrent;

import com.yupi.babystepaicodemother.langgraph4j.ai.ImageCollectionPlanService;
import com.yupi.babystepaicodemother.langgraph4j.model.ImageCollectionPlan;
import com.yupi.babystepaicodemother.langgraph4j.state.WorkflowContext;
import com.yupi.babystepaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 鍥剧墖瑙勫垝鑺傜偣
 */
@Slf4j
public class ImagePlanNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            try {
                // 鑾峰彇鍥剧墖鏀堕泦璁″垝鏈嶅姟
                ImageCollectionPlanService planService = SpringContextUtil.getBean(ImageCollectionPlanService.class);
                ImageCollectionPlan plan = planService.planImageCollection(originalPrompt);
                log.info("鐢熸垚鍥剧墖鏀堕泦璁″垝锛屽噯澶囧惎鍔ㄥ苟鍙戝垎鏀?");
                // 灏嗚鍒掑瓨鍌ㄥ埌涓婁笅鏂囦腑
                context.setImageCollectionPlan(plan);
                context.setCurrentStep("鍥剧墖璁″垝");
            } catch (Exception e) {
                log.error("鍥剧墖璁″垝鐢熸垚澶辫触: {}", e.getMessage(), e);
            }
            return WorkflowContext.saveContext(context);
        });
    }
} 
