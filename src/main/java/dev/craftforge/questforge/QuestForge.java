package dev.craftforge.questforge;

import dev.craftforge.questforge.command.QuestCommand;
import dev.craftforge.questforge.gui.QuestMenu;
import dev.craftforge.questforge.listener.ConnectionListener;
import dev.craftforge.questforge.listener.ObjectiveListener;
import dev.craftforge.questforge.quest.QuestRegistry;
import dev.craftforge.questforge.quest.QuestService;
import dev.craftforge.questforge.storage.ProgressStorage;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class QuestForge extends JavaPlugin {

    private QuestRegistry questRegistry;
    private QuestService questService;
    private ProgressStorage progressStorage;
    private ObjectiveListener objectiveListener;
    private BukkitTask flushTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        questRegistry = new QuestRegistry(this);
        questRegistry.load();

        progressStorage = new ProgressStorage(this, questRegistry);
        questService = new QuestService(this, questRegistry);

        objectiveListener = new ObjectiveListener(this, questService);
        final ConnectionListener connectionListener = new ConnectionListener(this, questService, progressStorage);

        final QuestMenu questMenu = new QuestMenu(this, questRegistry, questService);
        final QuestCommand questCommand = new QuestCommand(this, questRegistry, questService, progressStorage, questMenu);

        getServer().getPluginManager().registerEvents(objectiveListener, this);
        getServer().getPluginManager().registerEvents(connectionListener, this);
        getServer().getPluginManager().registerEvents(questMenu, this);

        final var command = getCommand("quest");
        if (command != null) {
            command.setExecutor(questCommand);
            command.setTabCompleter(questCommand);
        }

        final long flushIntervalTicks = getConfig().getLong("flush-interval-seconds", 60L) * 20L;
        flushTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () ->
                progressStorage.saveAll(questService.getAllPlayerData()), flushIntervalTicks, flushIntervalTicks
        );

        getLogger().info("QuestForge enabled. Loaded " + questRegistry.all().size() + " quest(s).");
    }

    @Override
    public void onDisable() {
        if (flushTask != null) {
            flushTask.cancel();
        }
        progressStorage.saveAll(questService.getAllPlayerData());
        getLogger().info("QuestForge disabled. All progress saved.");
    }
}
