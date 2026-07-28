package dev.craftforge.questforge.util;

import org.bukkit.plugin.java.JavaPlugin;

public final class Messages {

    private Messages() {}

    public static String prefix(final JavaPlugin plugin) {
        return plugin.getConfig().getString("messages.prefix", "<dark_gray>[<gold>QuestForge</gold>]</dark_gray> ");
    }

    public static String get(final JavaPlugin plugin, final String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    public static String get(final JavaPlugin plugin, final String key, final String fallback) {
        return plugin.getConfig().getString("messages." + key, fallback);
    }
}
