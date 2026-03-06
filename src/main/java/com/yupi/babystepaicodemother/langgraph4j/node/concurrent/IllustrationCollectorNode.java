package com.yupi.babystepaicodemother.langgraph4j.node.concurrent;

import com.yupi.babystepaicodemother.langgraph4j.model.ImageCollectionPlan;
import com.yupi.babystepaicodemother.langgraph4j.model.ImageResource;
import com.yupi.babystepaicodemother.langgraph4j.state.WorkflowContext;
import com.yupi.babystepaicodemother.langgraph4j.tools.UndrawIllustrationTool;
import com.yupi.babystepaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class IllustrationCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> illustrations = new ArrayList<>();
            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getIllustrationTasks() != null) {
                    UndrawIllustrationTool illustrationTool = SpringContextUtil.getBean(UndrawIllustrationTool.class);
                    log.info("寮€濮嬪苟鍙戞敹闆嗘彃鐢诲浘鐗囷紝浠诲姟鏁? {}", plan.getIllustrationTasks().size());
                    for (ImageCollectionPlan.IllustrationTask task : plan.getIllustrationTasks()) {
                        List<ImageResource> images = illustrationTool.searchIllustrations(task.query());
                        if (images != null) {
                            illustrations.addAll(images);
                        }
                    }
                    log.info("鎻掔敾鍥剧墖鏀堕泦瀹屾垚锛屽叡鏀堕泦鍒?{} 寮犲浘鐗?", illustrations.size());
                }
            } catch (Exception e) {
                log.error("鎻掔敾鍥剧墖鏀堕泦澶辫触: {}", e.getMessage(), e);
            }
            context.setIllustrations(illustrations);
            context.setCurrentStep("鎻掔敾鍥剧墖鏀堕泦");
            return WorkflowContext.saveContext(context);
        });
    }
} 
