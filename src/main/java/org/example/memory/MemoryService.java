package org.example.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class MemoryService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryService.class);

    private final Map<String, Map<String, Object>> sessionMemory = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> taskMemory = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> domainMemory = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> userMemory = new ConcurrentHashMap<>();

    private static final long SESSION_TTL_MS = TimeUnit.HOURS.toMillis(2);
    private static final long TASK_TTL_MS = TimeUnit.HOURS.toMillis(24);
    private static final long USER_TTL_MS = TimeUnit.DAYS.toMillis(30);

    private final Map<String, Long> sessionTimestamps = new ConcurrentHashMap<>();
    private final Map<String, Long> taskTimestamps = new ConcurrentHashMap<>();

    public void saveToSession(String sessionId, String key, Object value) {
        sessionMemory.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>()).put(key, value);
        sessionTimestamps.put(sessionId, System.currentTimeMillis());
    }

    public Object getFromSession(String sessionId, String key) {
        Map<String, Object> mem = sessionMemory.get(sessionId);
        return mem != null ? mem.get(key) : null;
    }

    public Map<String, Object> getSessionMemory(String sessionId) {
        return sessionMemory.getOrDefault(sessionId, Map.of());
    }

    public void clearSession(String sessionId) {
        sessionMemory.remove(sessionId);
        sessionTimestamps.remove(sessionId);
    }

    public void saveToTask(String taskId, String key, Object value) {
        taskMemory.computeIfAbsent(taskId, k -> new ConcurrentHashMap<>()).put(key, value);
        taskTimestamps.put(taskId, System.currentTimeMillis());
    }

    public Object getFromTask(String taskId, String key) {
        Map<String, Object> mem = taskMemory.get(taskId);
        return mem != null ? mem.get(key) : null;
    }

    public Map<String, Object> getTaskMemory(String taskId) {
        return taskMemory.getOrDefault(taskId, Map.of());
    }

    public void clearTask(String taskId) {
        taskMemory.remove(taskId);
        taskTimestamps.remove(taskId);
    }

    public void saveToDomain(String domain, String key, Object value) {
        domainMemory.computeIfAbsent(domain, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    public Object getFromDomain(String domain, String key) {
        Map<String, Object> mem = domainMemory.get(domain);
        return mem != null ? mem.get(key) : null;
    }

    public Map<String, Object> getDomainMemory(String domain) {
        return domainMemory.getOrDefault(domain, Map.of());
    }

    public void saveToUser(String userId, String key, Object value) {
        userMemory.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    public Object getFromUser(String userId, String key) {
        Map<String, Object> mem = userMemory.get(userId);
        return mem != null ? mem.get(key) : null;
    }

    public Map<String, Object> getUserMemory(String userId) {
        return userMemory.getOrDefault(userId, Map.of());
    }

    public void clearUser(String userId) {
        userMemory.remove(userId);
    }

    public String buildContextForAgent(String sessionId, String taskId, String userId) {
        StringBuilder context = new StringBuilder();

        Map<String, Object> session = getSessionMemory(sessionId);
        if (!session.isEmpty()) {
            context.append("【会话上下文】\n");
            session.forEach((k, v) -> context.append("- ").append(k).append(": ").append(v).append("\n"));
        }

        Map<String, Object> task = getTaskMemory(taskId);
        if (!task.isEmpty()) {
            context.append("【任务上下文】\n");
            task.forEach((k, v) -> context.append("- ").append(k).append(": ").append(v).append("\n"));
        }

        Map<String, Object> domainMem = getDomainMemory("power_ops");
        if (!domainMem.isEmpty()) {
            context.append("【领域知识】\n");
            domainMem.forEach((k, v) -> context.append("- ").append(k).append(": ").append(v).append("\n"));
        }

        if (userId != null && !userId.isEmpty()) {
            Map<String, Object> userMem = getUserMemory(userId);
            if (!userMem.isEmpty()) {
                context.append("【用户偏好】\n");
                userMem.forEach((k, v) -> context.append("- ").append(k).append(": ").append(v).append("\n"));
            }
        }

        return context.toString();
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpired() {
        long now = System.currentTimeMillis();

        sessionTimestamps.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > SESSION_TTL_MS) {
                sessionMemory.remove(entry.getKey());
                return true;
            }
            return false;
        });

        taskTimestamps.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > TASK_TTL_MS) {
                taskMemory.remove(entry.getKey());
                return true;
            }
            return false;
        });

        logger.debug("过期记忆清理完成, session={}, task={}, domain={}, user={}",
                sessionMemory.size(), taskMemory.size(), domainMemory.size(), userMemory.size());
    }
}
