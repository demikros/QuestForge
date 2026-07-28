package dev.craftforge.questforge.storage;

import dev.craftforge.questforge.quest.PlayerQuestData;
import dev.craftforge.questforge.quest.QuestProgress;
import dev.craftforge.questforge.quest.QuestRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class ProgressStorage {

    private final JavaPlugin plugin;
    private final QuestRegistry registry;
    private final File storageFile;
    private YamlConfiguration yaml;

    public ProgressStorage(final JavaPlugin plugin, final QuestRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.storageFile = new File(plugin.getDataFolder(), "progress.yml");
        this.yaml = loadYaml();
    }

    private YamlConfiguration loadYaml() {
        if (!storageFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                storageFile.createNewFile();
            } catch (final IOException e) {
                plugin.getLogger().log(Level.SEVERE, "[ProgressStorage] Could not create progress.yml", e);
            }
        }
        return YamlConfiguration.loadConfiguration(storageFile);
    }

    public PlayerQuestData load(final UUID uuid) {
        final PlayerQuestData data = new PlayerQuestData();
        final ConfigurationSection playerSection = yaml.getConfigurationSection(uuid.toString());
        if (playerSection == null) {
            return data;
        }

        final List<String> completed = playerSection.getStringList("completed");
        for (final String questId : completed) {
            data.addCompleted(questId);
        }

        final ConfigurationSection activeSection = playerSection.getConfigurationSection("active");
        if (activeSection != null) {
            for (final String questId : activeSection.getKeys(false)) {
                final ConfigurationSection progressSection = activeSection.getConfigurationSection(questId);
                if (progressSection == null) {
                    continue;
                }

                final long startedAt = progressSection.getLong("started", System.currentTimeMillis());
                final boolean isCompleted = progressSection.getBoolean("completed", false);
                final List<?> rawCounters = progressSection.getList("counters", new ArrayList<>());

                registry.byId(questId).ifPresent(quest -> {
                    final int objectiveCount = quest.getObjectives().size();
                    final int[] counters = new int[objectiveCount];
                    for (int i = 0; i < Math.min(rawCounters.size(), objectiveCount); i++) {
                        if (rawCounters.get(i) instanceof Number n) {
                            counters[i] = n.intValue();
                        }
                    }
                    data.putProgress(questId, new QuestProgress(questId, counters, startedAt, isCompleted));
                });
            }
        }

        return data;
    }

    public synchronized void save(final UUID uuid, final PlayerQuestData data) {
        if (!data.isDirty()) {
            return;
        }

        final String base = uuid.toString();
        yaml.set(base + ".completed", new ArrayList<>(data.getCompletedQuestIds()));

        yaml.set(base + ".active", null);

        for (final Map.Entry<String, QuestProgress> entry : data.getActiveProgress().entrySet()) {
            final String path = base + ".active." + entry.getKey();
            final QuestProgress progress = entry.getValue();
            yaml.set(path + ".started", progress.getStartedTimestamp());
            yaml.set(path + ".completed", progress.isCompleted());

            final int[] counters = progress.snapshotCounters();
            final List<Integer> counterList = new ArrayList<>(counters.length);
            for (final int c : counters) {
                counterList.add(c);
            }
            yaml.set(path + ".counters", counterList);
        }

        persistYaml();
        data.markClean();
    }

    public synchronized void saveAll(final Map<UUID, PlayerQuestData> allData) {
        for (final Map.Entry<UUID, PlayerQuestData> entry : allData.entrySet()) {
            if (entry.getValue().isDirty()) {
                save(entry.getKey(), entry.getValue());
            }
        }
    }

    private void persistYaml() {
        try {
            yaml.save(storageFile);
        } catch (final IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[ProgressStorage] Failed to save progress.yml", e);
        }
    }

    public void reload() {
        yaml = loadYaml();
    }
}
