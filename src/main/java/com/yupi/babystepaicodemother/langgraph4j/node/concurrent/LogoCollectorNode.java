package com.yupi.babystepaicodemother.langgraph4j.node.concurrent;

import com.yupi.babystepaicodemother.langgraph4j.model.ImageCollectionPlan;
import com.yupi.babystepaicodemother.langgraph4j.model.ImageResource;
import com.yupi.babystepaicodemother.langgraph4j.state.WorkflowContext;
import com.yupi.babystepaicodemother.langgraph4j.tools.LogoGeneratorTool;
import com.yupi.babystepaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class LogoCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> logos = new ArrayList<>();
            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getLogoTasks() != null) {
                    LogoGeneratorTool logoTool = SpringContextUtil.getBean(LogoGeneratorTool.class);
                    log.info("寮€濮嬪苟鍙戠敓鎴怢ogo锛屼换鍔℃暟: {}", plan.getLogoTasks().size());
                    for (ImageCollectionPlan.LogoTask task : plan.getLogoTasks()) {
                        List<ImageResource> images = logoTool.generateLogos(task.description());
                        if (images != null) {
                            logos.addAll(images);
                        }
                    }
                    log.info("Logo鐢熸垚瀹屾垚锛屽叡鐢熸垚 {} 寮犲浘鐗?", logos.size());
                }
            } catch (Exception e) {
                log.error("Logo鐢熸垚澶辫触: {}", e.getMessage(), e);
            }
            context.setLogos(logos);
            context.setCurrentStep("Logo鐢熸垚");
            return WorkflowContext.saveContext(context);
        });
    }
} 
