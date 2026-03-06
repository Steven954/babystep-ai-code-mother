package com.yupi.babystepaicodemother.langgraph4j.node.concurrent;

import com.yupi.babystepaicodemother.langgraph4j.model.ImageCollectionPlan;
import com.yupi.babystepaicodemother.langgraph4j.model.ImageResource;
import com.yupi.babystepaicodemother.langgraph4j.state.WorkflowContext;
import com.yupi.babystepaicodemother.langgraph4j.tools.MermaidDiagramTool;
import com.yupi.babystepaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class DiagramCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> diagrams = new ArrayList<>();
            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getDiagramTasks() != null) {
                    MermaidDiagramTool diagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);
                    log.info("寮€濮嬪苟鍙戠敓鎴愭灦鏋勫浘锛屼换鍔℃暟: {}", plan.getDiagramTasks().size());
                    for (ImageCollectionPlan.DiagramTask task : plan.getDiagramTasks()) {
                        List<ImageResource> images = diagramTool.generateMermaidDiagram(
                                task.mermaidCode(), task.description());
                        if (images != null) {
                            diagrams.addAll(images);
                        }
                    }
                    log.info("鏋舵瀯鍥剧敓鎴愬畬鎴愶紝鍏辩敓鎴?{} 寮犲浘鐗?", diagrams.size());
                }
            } catch (Exception e) {
                log.error("鏋舵瀯鍥剧敓鎴愬け璐? {}", e.getMessage(), e);
            }
            context.setDiagrams(diagrams);
            context.setCurrentStep("鏋舵瀯鍥剧敓鎴?");
            return WorkflowContext.saveContext(context);
        });
    }
} 
