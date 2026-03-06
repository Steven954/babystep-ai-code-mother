package com.yupi.babystepaicodemother.langgraph4j;

import cn.hutool.json.JSONUtil;
import com.yupi.babystepaicodemother.exception.BusinessException;
import com.yupi.babystepaicodemother.exception.ErrorCode;
import com.yupi.babystepaicodemother.langgraph4j.model.QualityResult;
import com.yupi.babystepaicodemother.langgraph4j.node.CodeGeneratorNode;
import com.yupi.babystepaicodemother.langgraph4j.node.CodeQualityCheckNode;
import com.yupi.babystepaicodemother.langgraph4j.node.ImageCollectorNode;
import com.yupi.babystepaicodemother.langgraph4j.node.ProjectBuilderNode;
import com.yupi.babystepaicodemother.langgraph4j.node.PromptEnhancerNode;
import com.yupi.babystepaicodemother.langgraph4j.node.RouterNode;
import com.yupi.babystepaicodemother.langgraph4j.state.WorkflowContext;
import com.yupi.babystepaicodemother.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * Main code generation workflow.
 */
@Slf4j
public class CodeGenWorkflow {

    /**
     * Build the workflow graph.
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("image_collector", ImageCollectorNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())
                    .addEdge(START, "image_collector")
                    .addEdge("image_collector", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "code_quality_check")
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
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to create workflow");
        }
    }

    /**
     * Execute the workflow synchronously.
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("init")
                .build();

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("Workflow graph:\n{}", graph.content());
        log.info("Starting code generation workflow");

        WorkflowContext finalContext = null;
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- Step {} completed ---", stepCounter);
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("Current workflow context: {}", currentContext);
            }
            stepCounter++;
        }
        log.info("Code generation workflow completed");
        return finalContext;
    }

    /**
     * Execute the workflow and stream events as Flux.
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt) {
        return Flux.create(sink -> Thread.startVirtualThread(() -> {
            try {
                CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                WorkflowContext initialContext = WorkflowContext.builder()
                        .originalPrompt(originalPrompt)
                        .currentStep("init")
                        .build();

                sink.next(formatSseEvent("workflow_start", Map.of(
                        "message", "Starting code generation workflow",
                        "originalPrompt", originalPrompt
                )));

                GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                log.info("Workflow graph:\n{}", graph.content());

                int stepCounter = 1;
                for (NodeOutput<MessagesState<String>> step : workflow.stream(
                        Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                    log.info("--- Step {} completed ---", stepCounter);
                    WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                    if (currentContext != null) {
                        sink.next(formatSseEvent("step_completed", Map.of(
                                "stepNumber", stepCounter,
                                "currentStep", currentContext.getCurrentStep()
                        )));
                        log.info("Current workflow context: {}", currentContext);
                    }
                    stepCounter++;
                }

                sink.next(formatSseEvent("workflow_completed", Map.of(
                        "message", "Code generation workflow completed"
                )));
                log.info("Code generation workflow completed");
                sink.complete();
            } catch (Exception e) {
                log.error("Workflow execution failed: {}", e.getMessage(), e);
                sink.next(formatSseEvent("workflow_error", Map.of(
                        "error", e.getMessage(),
                        "message", "Workflow execution failed"
                )));
                sink.error(e);
            }
        }));
    }

    /**
     * Format a simple SSE event payload.
     */
    private String formatSseEvent(String eventType, Object data) {
        try {
            String jsonData = JSONUtil.toJsonStr(data);
            return "event: " + eventType + "\ndata: " + jsonData + "\n\n";
        } catch (Exception e) {
            log.error("Failed to format SSE event: {}", e.getMessage(), e);
            return "event: error\ndata: {\"error\":\"format failed\"}\n\n";
        }
    }

    /**
     * Execute the workflow and push events through SseEmitter.
     */
    public SseEmitter executeWorkflowWithSse(String originalPrompt) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        Thread.startVirtualThread(() -> {
            try {
                CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                WorkflowContext initialContext = WorkflowContext.builder()
                        .originalPrompt(originalPrompt)
                        .currentStep("init")
                        .build();

                sendSseEvent(emitter, "workflow_start", Map.of(
                        "message", "Starting code generation workflow",
                        "originalPrompt", originalPrompt
                ));

                GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                log.info("Workflow graph:\n{}", graph.content());

                int stepCounter = 1;
                for (NodeOutput<MessagesState<String>> step : workflow.stream(
                        Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                    log.info("--- Step {} completed ---", stepCounter);
                    WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                    if (currentContext != null) {
                        sendSseEvent(emitter, "step_completed", Map.of(
                                "stepNumber", stepCounter,
                                "currentStep", currentContext.getCurrentStep()
                        ));
                        log.info("Current workflow context: {}", currentContext);
                    }
                    stepCounter++;
                }

                sendSseEvent(emitter, "workflow_completed", Map.of(
                        "message", "Code generation workflow completed"
                ));
                log.info("Code generation workflow completed");
                emitter.complete();
            } catch (Exception e) {
                log.error("Workflow execution failed: {}", e.getMessage(), e);
                sendSseEvent(emitter, "workflow_error", Map.of(
                        "error", e.getMessage(),
                        "message", "Workflow execution failed"
                ));
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /**
     * Send an SSE event safely.
     */
    private void sendSseEvent(SseEmitter emitter, String eventType, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(data));
        } catch (IOException e) {
            log.error("Failed to send SSE event: {}", e.getMessage(), e);
        }
    }

    /**
     * Decide the next step after quality check.
     */
    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();
        if (qualityResult == null || !qualityResult.getIsValid()) {
            log.error("Code quality check failed, regenerating code");
            return "fail";
        }
        log.info("Code quality check passed");
        return routeBuildOrSkip(state);
    }

    /**
     * Decide whether the generated project requires a build step.
     */
    private String routeBuildOrSkip(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeGenTypeEnum generationType = context.getGenerationType();
        if (generationType == CodeGenTypeEnum.HTML || generationType == CodeGenTypeEnum.MULTI_FILE) {
            return "skip_build";
        }
        return "build";
    }
}
