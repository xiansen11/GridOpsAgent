package org.example.plugin;

public interface PluginLifecycle {

    default void onInstall() {}

    default void onUninstall() {}

    default void onEnable() {}

    default void onDisable() {}

    String execute(String input);
}
