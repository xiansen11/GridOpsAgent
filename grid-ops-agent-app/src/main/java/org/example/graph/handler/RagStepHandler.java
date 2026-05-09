package org.example.graph.handler;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.example.graph.model.PlanStep;
import org.example.graph.model.StepResult;
import org.example.service.RagService;

import java.util.List;

public class RagStepHandler implements PlanStepHandler {

    private final RagService ragService;

    public RagStepHandler(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public String agentType() {
        return "rag";
    }

    @Override
    public StepResult execute(PlanStep step, OverAllState state) {
        try {
            String input = state.value("cleaned_input").map(Object::toString).orElse("");
            String[] resultHolder = new String[1];
            ragService.queryStream(input, List.of(), new org.example.service.RagService.StreamCallback() {
                @Override
                public void onSearchResults(java.util.List searchResults) {}
                @Override
                public void onReasoningChunk(String chunk) {}
                @Override
                public void onContentChunk(String chunk) {
                    resultHolder[0] = (resultHolder[0] != null ? resultHolder[0] : "") + chunk;
                }
                @Override
                public void onComplete(String fullContent, String fullReasoning) {
                    resultHolder[0] = fullContent;
                }
                @Override
                public void onError(Exception e) {
                    resultHolder[0] = "RAG检索失败: " + e.getMessage();
                }
            });
            String result = resultHolder[0] != null ? resultHolder[0] : "RAG检索完成";
            return StepResult.builder().success(true).result(result).build();
        } catch (Exception e) {
            return StepResult.builder().success(false).error(e.getMessage()).result("RAG检索失败: " + e.getMessage()).build();
        }
    }
}
