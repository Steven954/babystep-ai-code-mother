package com.yupi.babystepaicodemother.langgraph4j;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 绠€鍖栫増缃戠珯鐢熸垚宸ヤ綔娴佸簲鐢?- 浣跨敤 MessagesState
 */
@Slf4j
public class SimpleWorkflowApp {

    /**
     * 鍒涘缓宸ヤ綔鑺傜偣鐨勯€氱敤鏂规硶
     */
    static AsyncNodeAction<MessagesState<String>> makeNode(String message) {
        return node_async(state -> {
            log.info("鎵ц鑺傜偣: {}", message);
            return Map.of("messages", message);
        });
    }

    public static void main(String[] args) throws GraphStateException {
        // 鍒涘缓宸ヤ綔娴佸浘
        CompiledGraph<MessagesState<String>> workflow = new MessagesStateGraph<String>()
                // 娣诲姞鑺傜偣
                .addNode("image_collector", makeNode("鑾峰彇鍥剧墖绱犳潗"))
                .addNode("prompt_enhancer", makeNode("澧炲己鎻愮ず璇?"))
                .addNode("router", makeNode("鏅鸿兘璺敱閫夋嫨"))
                .addNode("code_generator", makeNode("缃戠珯浠ｇ爜鐢熸垚"))
                .addNode("project_builder", makeNode("椤圭洰鏋勫缓"))

                // 娣诲姞杈?
                .addEdge(START, "image_collector")                // 寮€濮?-> 鍥剧墖鏀堕泦
                .addEdge("image_collector", "prompt_enhancer")    // 鍥剧墖鏀堕泦 -> 鎻愮ず璇嶅寮?
                .addEdge("prompt_enhancer", "router")             // 鎻愮ず璇嶅寮?-> 鏅鸿兘璺敱
                .addEdge("router", "code_generator")              // 鏅鸿兘璺敱 -> 浠ｇ爜鐢熸垚
                .addEdge("code_generator", "project_builder")     // 浠ｇ爜鐢熸垚 -> 椤圭洰鏋勫缓
                .addEdge("project_builder", END)                  // 椤圭洰鏋勫缓 -> 缁撴潫

                // 缂栬瘧宸ヤ綔娴?
                .compile();

        log.info("寮€濮嬫墽琛屽伐浣滄祦");

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("宸ヤ綔娴佸浘: \n{}", graph.content());

        // 鎵ц宸ヤ綔娴?
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(Map.of())) {
            log.info("--- 绗?{} 姝ュ畬鎴?---", stepCounter);
            log.info("姝ラ杈撳嚭: {}", step);
            stepCounter++;
        }

        log.info("宸ヤ綔娴佹墽琛屽畬鎴愶紒");
    }
} 
