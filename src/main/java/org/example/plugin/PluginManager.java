package org.example.plugin;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PluginManager {

    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);
    private final Map<String, PluginInfo> plugins = new ConcurrentHashMap<>();
    private final Map<String, PluginLifecycle> lifecycleHandlers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        registerBuiltinPlugins();
        logger.info("Plugin Manager 初始化完成，已注册 {} 个插件", plugins.size());
    }

    public PluginInfo install(PluginInfo plugin) {
        plugin.setInstalledAt(LocalDateTime.now());
        plugins.put(plugin.getPluginId(), plugin);

        PluginLifecycle handler = lifecycleHandlers.get(plugin.getPluginId());
        if (handler != null) {
            handler.onInstall();
        }

        logger.info("安装插件: pluginId={}, name={}", plugin.getPluginId(), plugin.getName());
        return plugin;
    }

    public void uninstall(String pluginId) {
        PluginLifecycle handler = lifecycleHandlers.get(pluginId);
        if (handler != null) {
            handler.onUninstall();
        }

        plugins.remove(pluginId);
        logger.info("卸载插件: pluginId={}", pluginId);
    }

    public void enable(String pluginId) {
        PluginInfo plugin = plugins.get(pluginId);
        if (plugin != null) {
            plugin.setEnabled(true);

            PluginLifecycle handler = lifecycleHandlers.get(pluginId);
            if (handler != null) {
                handler.onEnable();
            }
        }
    }

    public void disable(String pluginId) {
        PluginInfo plugin = plugins.get(pluginId);
        if (plugin != null) {
            plugin.setEnabled(false);

            PluginLifecycle handler = lifecycleHandlers.get(pluginId);
            if (handler != null) {
                handler.onDisable();
            }
        }
    }

    public String executePlugin(String pluginId, String input) {
        PluginInfo plugin = plugins.get(pluginId);
        if (plugin == null || !plugin.isEnabled()) {
            return "插件不可用: " + pluginId;
        }

        PluginLifecycle handler = lifecycleHandlers.get(pluginId);
        if (handler != null) {
            return handler.execute(input);
        }

        return "插件 " + plugin.getName() + " 暂无执行逻辑";
    }

    public void registerLifecycle(String pluginId, PluginLifecycle lifecycle) {
        lifecycleHandlers.put(pluginId, lifecycle);
        logger.info("注册插件生命周期处理器: pluginId={}", pluginId);
    }

    public List<PluginInfo> listPlugins() {
        return new ArrayList<>(plugins.values());
    }

    public List<PluginInfo> getEnabledPlugins() {
        return plugins.values().stream().filter(PluginInfo::isEnabled).toList();
    }

    public PluginInfo getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }

    private void registerBuiltinPlugins() {
        install(PluginInfo.builder()
                .pluginId("power-monitoring").name("电力监控插件").version("1.0")
                .description("提供设备状态监控、告警历史查询等能力").category("monitoring")
                .entryClass("org.example.plugin.builtin.PowerMonitoringPlugin")
                .enabled(true).build());

        install(PluginInfo.builder()
                .pluginId("power-safety").name("安规查询插件").version("1.0")
                .description("提供电力安规查询、安全措施提示等能力").category("safety")
                .entryClass("org.example.plugin.builtin.PowerSafetyPlugin")
                .enabled(true).build());

        install(PluginInfo.builder()
                .pluginId("power-diagnosis").name("故障诊断插件").version("1.0")
                .description("提供多Agent协作故障诊断能力").category("diagnosis")
                .entryClass("org.example.plugin.builtin.PowerDiagnosisPlugin")
                .enabled(true).build());

        install(PluginInfo.builder()
                .pluginId("power-knowledge").name("知识库插件").version("1.0")
                .description("提供RAG知识库检索、文档上传等能力").category("knowledge")
                .entryClass("org.example.plugin.builtin.PowerKnowledgePlugin")
                .enabled(true).build());
    }
}
