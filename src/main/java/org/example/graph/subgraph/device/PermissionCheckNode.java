package org.example.graph.subgraph.device;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.example.security.RbacService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PermissionCheckNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(PermissionCheckNode.class);
    private final RbacService rbacService;

    public PermissionCheckNode(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String userId = state.value("user_id").map(Object::toString).orElse("default");
        logger.info("PermissionCheckNode: 权限检查, userId={}", userId);

        boolean hasPermission = rbacService.hasPermission(userId, "DEVICE_STATUS");
        if (!hasPermission) {
            return Map.of("final_response", "您没有设备查询权限，请联系管理员。");
        }

        return Map.of();
    }
}
