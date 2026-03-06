package com.yupi.babystepaicodemother.langgraph4j;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.yupi.babystepaicodemother.exception.BusinessException;
import com.yupi.babystepaicodemother.exception.ErrorCode;
import com.yupi.babystepaicodemother.langgraph4j.model.QualityResult;
import com.yupi.babystepaicodemother.langgraph4j.node.*;
import com.yupi.babystepaicodemother.langgraph4j.node.concurrent.*;
import com.yupi.babystepaicodemother.langgraph4j.state.WorkflowContext;
import com.yupi.babystepaicodemother.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 骞跺彂鎵ц鐨勪唬鐮佺敓鎴愬伐浣滄祦
 */
@Slf4j
public class CodeGenConcurrentWorkflow {

    /**
     * 鍒涘缓骞跺彂宸ヤ綔娴?
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // 娣诲姞鑺傜偣
                    .addNode("image_plan", ImagePlanNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())

                    // 娣诲姞骞跺彂鍥剧墖鏀堕泦鑺傜偣
                    .addNode("content_image_collector", ContentImageCollectorNode.create())
                    .addNode("illustration_collector", IllustrationCollectorNode.create())
                    .addNode("diagram_collector", DiagramCollectorNode.create())
                    .addNode("logo_collector", LogoCollectorNode.create())
                    .addNode("image_aggregator", ImageAggregatorNode.create())

                    // 娣诲姞杈?
                    .addEdge(START, "image_plan")

                    // 骞跺彂鍒嗘敮锛氫粠璁″垝鑺傜偣鍒嗗彂鍒板悇涓敹闆嗚妭鐐?
                    .addEdge("image_plan", "content_image_collector")
                    .addEdge("image_plan", "illustration_collector")
                    .addEdge("image_plan", "diagram_collector")
                    .addEdge("image_plan", "logo_collector")

                    // 姹囪仛锛氭墍鏈夋敹闆嗚妭鐐归兘姹囪仛鍒拌仛鍚堝櫒
                    .addEdge("content_image_collector", "image_aggregator")
                    .addEdge("illustration_collector", "image_aggregator")
                    .addEdge("diagram_collector", "image_aggregator")
                    .addEdge("logo_collector", "image_aggregator")

                    // 缁х画涓茶娴佺▼
                    .addEdge("image_aggregator", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "code_quality_check")

                    // 璐ㄦ鏉′欢杈?
                    .addConditionalEdges("code_quality_check",
                            edge_async(this::routeAfterQualityCheck),
                            Map.of(
                                    "build", "project_builder",
                                    "skip_build", END,
                                    "fail", "code_generator"
                            ))
                    .addEdge("project_builder", END)
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "骞跺彂宸ヤ綔娴佸垱寤哄け璐?");
        }
    }

    /**
     * 鎵ц骞跺彂宸ヤ綔娴?
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("鍒濆鍖?")
                .build();
        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("骞跺彂宸ヤ綔娴佸浘:\n{}", graph.content());
        log.info("寮€濮嬫墽琛屽苟鍙戜唬鐮佺敓鎴愬伐浣滄祦");
        WorkflowContext finalContext = null;
        int stepCounter = 1;
        // 閰嶇疆骞跺彂鎵ц
        ExecutorService pool = ExecutorBuilder.create()
                .setCorePoolSize(10)
                .setMaxPoolSize(20)
                .setWorkQueue(new LinkedBlockingQueue<>(100))
                .setThreadFactory(ThreadFactoryBuilder.create().setNamePrefix("Parallel-Image-Collect").build())
                .build();
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .addParallelNodeExecutor("image_plan", pool)
                .build();
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext),
                runnableConfig)) {
            log.info("--- 绗?{} 姝ュ畬鎴?---", stepCounter);
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("褰撳墠姝ラ涓婁笅鏂? {}", currentContext);
            }
            stepCounter++;
        }
        log.info("骞跺彂浠ｇ爜鐢熸垚宸ヤ綔娴佹墽琛屽畬鎴愶紒");
        return finalContext;
    }

    /**
     * 璺敱鍑芥暟锛氭牴鎹川妫€缁撴灉鍐冲畾涓嬩竴姝?
     */
    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();

        if (qualityResult == null || !qualityResult.getIsValid()) {
            log.error("浠ｇ爜璐ㄦ澶辫触锛岄渶瑕侀噸鏂扮敓鎴愪唬鐮?");
            return "fail";
        }
        log.info("浠ｇ爜璐ㄦ閫氳繃锛岀户缁悗缁祦绋?");
        CodeGenTypeEnum generationType = context.getGenerationType();
        if (generationType == CodeGenTypeEnum.VUE_PROJECT) {
            return "build";
        } else {
            return "skip_build";
        }
    }
} 
