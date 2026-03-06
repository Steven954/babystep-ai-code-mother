package com.yupi.babystepaicodemother.langgraph4j;

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

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

@Slf4j
public class CodeGenSubgraphWorkflow {

    /**
     * 鍒涘缓鍐呭鍥剧墖鏀堕泦瀛愬浘
     */
    private StateGraph<MessagesState<String>> createContentImageSubgraph() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("content_collect", ContentImageCollectorNode.create())
                    .addEdge(START, "content_collect")
                    .addEdge("content_collect", END);
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "鍐呭鍥剧墖瀛愬浘鍒涘缓澶辫触");
        }
    }

    /**
     * 鍒涘缓鎻掔敾鏀堕泦瀛愬浘
     */
    private StateGraph<MessagesState<String>> createIllustrationSubgraph() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("illustration_collect", IllustrationCollectorNode.create())
                    .addEdge(START, "illustration_collect")
                    .addEdge("illustration_collect", END);
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "鎻掔敾瀛愬浘鍒涘缓澶辫触");
        }
    }

    /**
     * 鍒涘缓鏋舵瀯鍥剧敓鎴愬瓙鍥?
     */
    private StateGraph<MessagesState<String>> createDiagramSubgraph() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("diagram_generate", DiagramCollectorNode.create())
                    .addEdge(START, "diagram_generate")
                    .addEdge("diagram_generate", END);
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "鏋舵瀯鍥惧瓙鍥惧垱寤哄け璐?");
        }
    }

    /**
     * 鍒涘缓Logo鐢熸垚瀛愬浘
     */
    private StateGraph<MessagesState<String>> createLogoSubgraph() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("logo_generate", LogoCollectorNode.create())
                    .addEdge(START, "logo_generate")
                    .addEdge("logo_generate", END);
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Logo瀛愬浘鍒涘缓澶辫触");
        }
    }

    /**
     * 鍒涘缓瀛愬浘宸ヤ綔娴?
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            // 鑾峰彇鍚勪釜鏈紪璇戠殑瀛愬浘锛堣窡鐖跺浘瀹屽叏鍏变韩鐘舵€侊級
            StateGraph<MessagesState<String>> contentImageSubgraph = createContentImageSubgraph();
            StateGraph<MessagesState<String>> illustrationSubgraph = createIllustrationSubgraph();
            StateGraph<MessagesState<String>> diagramSubgraph = createDiagramSubgraph();
            StateGraph<MessagesState<String>> logoSubgraph = createLogoSubgraph();

            return new MessagesStateGraph<String>()
                    // 娣诲姞甯歌鑺傜偣
                    .addNode("image_plan", ImagePlanNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())
                    
                    // 娣诲姞缂栬瘧鍚庣殑瀛愬浘浣滀负鑺傜偣
                    .addNode("content_image_subgraph", contentImageSubgraph)
                    .addNode("illustration_subgraph", illustrationSubgraph)
                    .addNode("diagram_subgraph", diagramSubgraph)
                    .addNode("logo_subgraph", logoSubgraph)
                    
                    // 娣诲姞鍥剧墖鑱氬悎鑺傜偣
                    .addNode("image_aggregator", ImageAggregatorNode.create())

                    // 娣诲姞杈?- 涓茶閮ㄥ垎
                    .addEdge(START, "image_plan")
                    
                    // 骞跺彂瀛愬浘鍒嗘敮锛氫粠璁″垝鑺傜偣鍒嗗彂鍒板悇涓瓙鍥?
                    .addEdge("image_plan", "content_image_subgraph")
                    .addEdge("image_plan", "illustration_subgraph")
                    .addEdge("image_plan", "diagram_subgraph")
                    .addEdge("image_plan", "logo_subgraph")
                    
                    // 姹囪仛锛氭墍鏈夊瓙鍥鹃兘姹囪仛鍒拌仛鍚堝櫒
                    .addEdge("content_image_subgraph", "image_aggregator")
                    .addEdge("illustration_subgraph", "image_aggregator")
                    .addEdge("diagram_subgraph", "image_aggregator")
                    .addEdge("logo_subgraph", "image_aggregator")
                    
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
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "瀛愬浘宸ヤ綔娴佸垱寤哄け璐?");
        }
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

    /**
     * 鎵ц瀛愬浘宸ヤ綔娴?
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();

        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("鍒濆鍖?")
                .build();

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("瀛愬浘宸ヤ綔娴佸浘:\n{}", graph.content());
        log.info("寮€濮嬫墽琛屽瓙鍥句唬鐮佺敓鎴愬伐浣滄祦");

        WorkflowContext finalContext = null;
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- 绗?{} 姝ュ畬鎴?---", stepCounter);
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("褰撳墠姝ラ涓婁笅鏂? {}", currentContext);
            }
            stepCounter++;
        }
        log.info("瀛愬浘浠ｇ爜鐢熸垚宸ヤ綔娴佹墽琛屽畬鎴愶紒");
        return finalContext;
    }
} 
