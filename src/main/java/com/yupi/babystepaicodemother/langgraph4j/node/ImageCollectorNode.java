package com.yupi.babystepaicodemother.langgraph4j.node;

import com.yupi.babystepaicodemother.langgraph4j.ai.ImageCollectionPlanService;
import com.yupi.babystepaicodemother.langgraph4j.model.ImageCollectionPlan;
import com.yupi.babystepaicodemother.langgraph4j.model.ImageResource;
import com.yupi.babystepaicodemother.langgraph4j.state.WorkflowContext;
import com.yupi.babystepaicodemother.langgraph4j.tools.ImageSearchTool;
import com.yupi.babystepaicodemother.langgraph4j.tools.LogoGeneratorTool;
import com.yupi.babystepaicodemother.langgraph4j.tools.MermaidDiagramTool;
import com.yupi.babystepaicodemother.langgraph4j.tools.UndrawIllustrationTool;
import com.yupi.babystepaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 鍥剧墖鏀堕泦鑺傜偣锛堝苟鍙戯級
 */
@Slf4j
public class ImageCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            List<ImageResource> collectedImages = new ArrayList<>();

            try {
                // 绗竴姝ワ細鑾峰彇鍥剧墖鏀堕泦璁″垝
                ImageCollectionPlanService planService = SpringContextUtil.getBean(ImageCollectionPlanService.class);
                ImageCollectionPlan plan = planService.planImageCollection(originalPrompt);
                log.info("鑾峰彇鍒板浘鐗囨敹闆嗚鍒掞紝寮€濮嬪苟鍙戞墽琛?");

                // 绗簩姝ワ細骞跺彂鎵ц鍚勭鍥剧墖鏀堕泦浠诲姟
                List<CompletableFuture<List<ImageResource>>> futures = new ArrayList<>();
                // 骞跺彂鎵ц鍐呭鍥剧墖鎼滅储
                if (plan.getContentImageTasks() != null) {
                    ImageSearchTool imageSearchTool = SpringContextUtil.getBean(ImageSearchTool.class);
                    for (ImageCollectionPlan.ImageSearchTask task : plan.getContentImageTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                imageSearchTool.searchContentImages(task.query())));
                    }
                }
                // 骞跺彂鎵ц鎻掔敾鍥剧墖鎼滅储
                if (plan.getIllustrationTasks() != null) {
                    UndrawIllustrationTool illustrationTool = SpringContextUtil.getBean(UndrawIllustrationTool.class);
                    for (ImageCollectionPlan.IllustrationTask task : plan.getIllustrationTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                illustrationTool.searchIllustrations(task.query())));
                    }
                }
                // 骞跺彂鎵ц鏋舵瀯鍥剧敓鎴?
                if (plan.getDiagramTasks() != null) {
                    MermaidDiagramTool diagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);
                    for (ImageCollectionPlan.DiagramTask task : plan.getDiagramTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                diagramTool.generateMermaidDiagram(task.mermaidCode(), task.description())));
                    }
                }
                // 骞跺彂鎵цLogo鐢熸垚
                if (plan.getLogoTasks() != null) {
                    LogoGeneratorTool logoTool = SpringContextUtil.getBean(LogoGeneratorTool.class);
                    for (ImageCollectionPlan.LogoTask task : plan.getLogoTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() ->
                                logoTool.generateLogos(task.description())));
                    }
                }

                // 绛夊緟鎵€鏈変换鍔″畬鎴愬苟鏀堕泦缁撴灉
                CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0]));
                allTasks.join();
                // 鏀堕泦鎵€鏈夌粨鏋?
                for (CompletableFuture<List<ImageResource>> future : futures) {
                    List<ImageResource> images = future.get();
                    if (images != null) {
                        collectedImages.addAll(images);
                    }
                }
                log.info("骞跺彂鍥剧墖鏀堕泦瀹屾垚锛屽叡鏀堕泦鍒?{} 寮犲浘鐗?", collectedImages.size());
            } catch (Exception e) {
                log.error("鍥剧墖鏀堕泦澶辫触: {}", e.getMessage(), e);
            }
            // 鏇存柊鐘舵€?
            context.setCurrentStep("鍥剧墖鏀堕泦");
            context.setImageList(collectedImages);
            return WorkflowContext.saveContext(context);
        });
    }
}

