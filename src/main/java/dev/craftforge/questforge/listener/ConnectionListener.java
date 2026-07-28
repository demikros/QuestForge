package dev.craftforge.questforge.listener;

import dev.craftforge.questforge.quest.PlayerQuestData;
import dev.craftforge.questforge.quest.QuestService;
import dev.craftforge.questforge.storage.ProgressStorage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class ConnectionListener implements Listener {

    private final JavaPlugin plugin;
    private final QuestService questService;
    private final ProgressStorage progressStorage;

    public ConnectionListener(final JavaPlugin plugin, final QuestService questService, final ProgressStorage progressStorage) {
        this.plugin = plugin;
        this.questService = questService;
        this.progressStorage = progressStorage;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            final PlayerQuestData data = progressStorage.load(uuid);
            plugin.getServer().getScheduler().runTask(plugin, () -> questService.loadPlayerData(uuid, data));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        questService.getData(uuid).ifPresent(data -> {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                progressStorage.save(uuid, data);
                plugin.getServer().getScheduler().runTask(plugin, () -> questService.evictPlayerData(uuid));
            });
        });
    }
}
