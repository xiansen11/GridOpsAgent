package org.example.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import org.example.service.AgentOrchestrationService;
import org.example.service.ChatService;
import org.example.service.RagService;
import org.example.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatService chatService;

    @Autowired
    private AgentOrchestrationService orchestrationService;

    @Autowired
    private RagService ragService;

    @Autowired(required = false)
    private CompiledGraph compiledGraph;

    @Value("${powerops.graph.enabled:false}")
    private boolean graphEnabled;

    private final Map<String, List<Map<String, String>>> sessionHistoryMap = new ConcurrentHashMap<>();
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    @PostMapping
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", UUID.randomUUID().toString());
        String question = request.get("question");
        String userId = request.getOrDefault("userId", "default");

        logger.info("对话请求: sessionId={}, userId={}, question={}, graphEnabled={}", sessionId, userId, question, graphEnabled);

        try {
            List<Map<String, String>> history = sessionHistoryMap.computeIfAbsent(sessionId, k -> new ArrayList<>());

            String answer;
            if (graphEnabled && compiledGraph != null) {
                answer = invokeGraph(question, sessionId, userId, history);
            } else {
                answer = orchestrationService.handleChat(question, history);
            }

            history.add(Map.of("role", "user", "content", question));
            history.add(Map.of("role", "assistant", "content", answer));

            if (history.size() > 40) {
                history.subList(0, history.size() - 40).clear();
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("sessionId", sessionId);
            response.put("answer", answer);
            response.put("engine", graphEnabled && compiledGraph != null ? "graph" : "legacy");
            return response;

        } catch (Exception e) {
            logger.error("对话执行失败", e);
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("error", "对话执行失败: " + e.getMessage());
            return errorResponse;
        }
    }

    private String invokeGraph(String question, String sessionId, String userId, List<Map<String, String>> history) {
        try {
            Map<String, Object> initialState = new LinkedHashMap<>();
            initialState.put("input", question);
            initialState.put("session_id", sessionId);
            initialState.put("user_id", userId);
            initialState.put("task_id", "TASK-" + UUID.randomUUID().toString().substring(0, 8));

            StringBuilder historyStr = new StringBuilder();
            for (Map<String, String> h : history) {
                historyStr.append(h.get("role")).append(": ").append(h.get("content")).append("\n");
            }
            initialState.put("history", historyStr.toString());

            Optional<OverAllState> resultOpt = compiledGraph.invoke(initialState);
            String answer = resultOpt
                    .map(state -> state.value("final_response").map(Object::toString).orElse("处理失败，请重试"))
                    .orElse("处理失败，请重试");
            logger.info("Graph引擎执行成功, sessionId={}", sessionId);
            return answer;
        } catch (Exception e) {
            logger.warn("Graph引擎执行失败，降级到旧编排: {}", e.getMessage());
            return orchestrationService.handleChat(question, history);
        }
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", UUID.randomUUID().toString());
        String question = request.get("question");
        String userId = request.getOrDefault("userId", "default");

        logger.info("SSE对话请求: sessionId={}, userId={}, question={}", sessionId, userId, question);

        SseEmitter emitter = new SseEmitter(300000L);

        sseExecutor.execute(() -> {
            try {
                List<Map<String, String>> history = sessionHistoryMap.computeIfAbsent(sessionId, k -> new ArrayList<>());

                ragService.queryStream(question, history, new RagService.StreamCallback() {
                    @Override
                    public void onSearchResults(List<VectorSearchService.SearchResult> results) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("search_results")
                                    .data(results.size() + " 条检索结果"));
                        } catch (IOException e) {
                            logger.warn("发送检索结果事件失败", e);
                        }
                    }

                    @Override
                    public void onReasoningChunk(String chunk) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("reasoning")
                                    .data(chunk));
                        } catch (IOException e) {
                            logger.warn("发送推理事件失败", e);
                        }
                    }

                    @Override
                    public void onContentChunk(String chunk) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(chunk));
                        } catch (IOException e) {
                            logger.warn("发送内容块事件失败", e);
                        }
                    }

                    @Override
                    public void onComplete(String fullContent, String fullReasoning) {
                        try {
                            history.add(Map.of("role", "user", "content", question));
                            history.add(Map.of("role", "assistant", "content", fullContent));

                            if (history.size() > 40) {
                                history.subList(0, history.size() - 40).clear();
                            }

                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            emitter.complete();
                        } catch (IOException e) {
                            logger.warn("发送完成事件失败", e);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("处理失败: " + e.getMessage()));
                        } catch (IOException ex) {
                            logger.warn("发送错误事件失败", ex);
                        }
                        emitter.complete();
                    }
                });

            } catch (Exception e) {
                logger.error("SSE处理失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("处理失败: " + e.getMessage()));
                } catch (IOException ex) {
                    logger.error("发送错误事件失败", ex);
                }
                emitter.complete();
            }
        });

        return emitter;
    }

    @PostMapping("/clear")
    public Map<String, Object> clearSession(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        if (sessionId != null) {
            sessionHistoryMap.remove(sessionId);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "会话已清空");
        return response;
    }
}
