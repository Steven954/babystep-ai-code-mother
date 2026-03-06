package com.yupi.babystepaicodemother.langgraph4j;

import com.yupi.babystepaicodemother.langgraph4j.node.*;
import com.yupi.babystepaicodemother.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

/**
 * 宸ヤ綔娴佸簲鐢紙妯℃嫙鐘舵€佹祦杞級
 */
@Slf4j
public class WorkflowApp {

    public static void main(String[] args) throws GraphStateException {
        // 鍒涘缓宸ヤ綔娴佸浘
        CompiledGraph<MessagesState<String>> workflow = new MessagesStateGraph<String>()
                // 娣诲姞鑺傜偣 - 浣跨敤鐪熷疄鐨勫伐浣滆妭鐐?
                .addNode("image_collector", ImageCollectorNode.create())
                .addNode("prompt_enhancer", PromptEnhancerNode.create())
                .addNode("router", RouterNode.create())
                .addNode("code_generator", CodeGeneratorNode.create())
                .addNode("project_builder", ProjectBuilderNode.create())
                // 娣诲姞杈?
                .addEdge(START, "image_collector")
                .addEdge("image_collector", "prompt_enhancer")
                .addEdge("prompt_enhancer", "router")
                .addEdge("router", "code_generator")
                .addEdge("code_generator", "project_builder")
                .addEdge("project_builder", END)
                // 缂栬瘧宸ヤ綔娴?
                .compile();

        // 鍒濆鍖?WorkflowContext - 鍙缃熀鏈俊鎭?
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt("鍒涘缓涓€涓奔鐨殑涓汉鍗氬缃戠珯")
                .currentStep("鍒濆鍖?")
                .build();
        log.info("鍒濆杈撳叆: {}", initialContext.getOriginalPrompt());
        log.info("寮€濮嬫墽琛屽伐浣滄祦");

        // 鏄剧ず宸ヤ綔娴佸浘
        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("宸ヤ綔娴佸浘:\n{}", graph.content());

        // 鎵ц宸ヤ綔娴?
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- 绗?{} 姝ュ畬鎴?---", stepCounter);
            // 鏄剧ず褰撳墠鐘舵€?
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                log.info("褰撳墠姝ラ涓婁笅鏂? {}", currentContext);
            }
            stepCounter++;
        }
        log.info("宸ヤ綔娴佹墽琛屽畬鎴愶紒");
    }
}
