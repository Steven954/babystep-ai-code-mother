package com.yupi.babystepaicodemother.langgraph4j.node.concurrent;

import com.yupi.babystepaicodemother.langgraph4j.model.ImageCollectionPlan;
import com.yupi.babystepaicodemother.langgraph4j.model.ImageResource;
import com.yupi.babystepaicodemother.langgraph4j.state.WorkflowContext;
import com.yupi.babystepaicodemother.langgraph4j.tools.ImageSearchTool;
import com.yupi.babystepaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ContentImageCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> contentImages = new ArrayList<>();
            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getContentImageTasks() != null) {
                    ImageSearchTool imageSearchTool = SpringContextUtil.getBean(ImageSearchTool.class);
                    log.info("寮€濮嬪苟鍙戞敹闆嗗唴瀹瑰浘鐗囷紝浠诲姟鏁? {}", plan.getContentImageTasks().size());
                    for (ImageCollectionPlan.ImageSearchTask task : plan.getContentImageTasks()) {
                        List<ImageResource> images = imageSearchTool.searchContentImages(task.query());
                        if (images != null) {
                            contentImages.addAll(images);
                        }
                    }
                    log.info("鍐呭鍥剧墖鏀堕泦瀹屾垚锛屽叡鏀堕泦鍒?{} 寮犲浘鐗?", contentImages.size());
                }
            } catch (Exception e) {
                log.error("鍐呭鍥剧墖鏀堕泦澶辫触: {}", e.getMessage(), e);
            }
            // 灏嗘敹闆嗗埌鐨勫浘鐗囧瓨鍌ㄥ埌涓婁笅鏂囩殑涓棿瀛楁涓?
            context.setContentImages(contentImages);
            context.setCurrentStep("鍐呭鍥剧墖鏀堕泦");
            return WorkflowContext.saveContext(context);
        });
    }
} 
