package org.example.security;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RbacService {

    private static final Map<String, Set<String>> ROLE_PERMISSIONS = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userRoles = new ConcurrentHashMap<>();

    static {
        ROLE_PERMISSIONS.put("admin", Set.of("chat", "diagnose", "knowledge:upload", "knowledge:delete",
                "knowledge:search", "alarm:receive", "alarm:diagnose", "skill:manage", "system:config",
                "approval:approve", "observability:view", "QUERY", "TICKET_CREATE", "DEVICE_CONTROL",
                "LOAD_CONTROL", "USER_MANAGE"));
        ROLE_PERMISSIONS.put("operator", Set.of("chat", "diagnose", "knowledge:search",
                "alarm:receive", "alarm:diagnose", "observability:view", "QUERY", "TICKET_CREATE"));
        ROLE_PERMISSIONS.put("viewer", Set.of("chat", "knowledge:search", "observability:view", "QUERY"));
    }

    public RbacService() {
        userRoles.put("admin", Set.of("admin"));
        userRoles.put("operator1", Set.of("operator"));
        userRoles.put("viewer1", Set.of("viewer"));
    }

    public boolean hasPermission(String userId, String permission) {
        Set<String> roles = userRoles.getOrDefault(userId, Set.of("viewer"));
        return roles.stream()
                .anyMatch(role -> ROLE_PERMISSIONS.getOrDefault(role, Set.of()).contains(permission));
    }

    public Set<String> getUserPermissions(String userId) {
        Set<String> roles = userRoles.getOrDefault(userId, Set.of("viewer"));
        Set<String> permissions = new HashSet<>();
        roles.forEach(role -> permissions.addAll(ROLE_PERMISSIONS.getOrDefault(role, Set.of())));
        return permissions;
    }

    public Set<String> getUserRoles(String userId) {
        return userRoles.getOrDefault(userId, Set.of("viewer"));
    }

    public void assignRole(String userId, String role) {
        userRoles.computeIfAbsent(userId, k -> new HashSet<>()).add(role);
    }

    public void removeRole(String userId, String role) {
        if (userRoles.containsKey(userId)) {
            userRoles.get(userId).remove(role);
        }
    }

    public Map<String, Set<String>> getAllRolePermissions() {
        return ROLE_PERMISSIONS;
    }

    public Map<String, Set<String>> getAllUserRoles() {
        return userRoles;
    }
}
