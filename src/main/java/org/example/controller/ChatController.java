package org.example.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.entity.ChatMessage;
import org.example.entity.ChatSession;
import org.example.mapper.ChatMessageMapper;
import org.example.mapper.ChatSessionMapper;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Value("${powerops.graph.enabled:false}")
    private boolean graphEnabled;

    private final Map<String, List<Map<String, String>>> sessionHistoryMap = new ConcurrentHashMap<>();
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    private void saveMessageToDb(String sessionId, String userId, String role, String content, String intent, String agentName) {
        ChatSession session = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getSessionId, sessionId)
        );
        if (session == null) {
            session = ChatSession.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .build();
            chatSessionMapper.insert(session);
        } else {
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }

        ChatMessage message = ChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .intent(intent)
                .agentName(agentName)
                .build();
        chatMessageMapper.insert(message);
    }

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
                answer = orchestrationService.handleChat(question, history, sessionId);
            }

            history.add(Map.of("role", "user", "content", question));
            history.add(Map.of("role", "assistant", "content", answer));

            saveMessageToDb(sessionId, userId, "user", question, null, null);
            saveMessageToDb(sessionId, userId, "assistant", answer, null, null);

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
            return orchestrationService.handleChat(question, history, sessionId);
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

                            saveMessageToDb(sessionId, userId, "user", question, null, null);
                            saveMessageToDb(sessionId, userId, "assistant", fullContent, null, null);

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
            chatMessageMapper.delete(
                    new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, sessionId)
            );
            chatSessionMapper.delete(
                    new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getSessionId, sessionId)
            );
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "会话已清空");
        return response;
    }

    @GetMapping("/history")
    public Map<String, Object> getHistory(@RequestParam String sessionId) {
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreatedAt)
        );

        List<Map<String, String>> historyList = messages.stream()
                .map(msg -> Map.of(
                        "role", msg.getRole(),
                        "content", msg.getContent() != null ? msg.getContent() : ""
                ))
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionId", sessionId);
        response.put("messages", historyList);
        return response;
    }

    @GetMapping("/sessions")
    public Map<String, Object> getSessions(@RequestParam(defaultValue = "default") String userId) {
        List<ChatSession> sessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdatedAt)
                        .last("LIMIT 50")
        );

        List<Map<String, Object>> sessionList = sessions.stream()
                .map(s -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("sessionId", s.getSessionId());
                    map.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
                    map.put("updatedAt", s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : "");
                    ChatMessage lastMsg = chatMessageMapper.selectOne(
                            new LambdaQueryWrapper<ChatMessage>()
                                    .eq(ChatMessage::getSessionId, s.getSessionId())
                                    .eq(ChatMessage::getRole, "user")
                                    .orderByDesc(ChatMessage::getCreatedAt)
                                    .last("LIMIT 1")
                    );
                    map.put("lastMessage", lastMsg != null ? lastMsg.getContent() : "");
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessions", sessionList);
        return response;
    }
}
