package dev.craftforge.questforge.quest;

import dev.craftforge.questforge.QuestForge;
import dev.craftforge.questforge.quest.objective.Objective;
import dev.craftforge.questforge.quest.objective.ObjectiveEvent;
import dev.craftforge.questforge.util.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestService {

    private final QuestForge plugin;
    private final QuestRegistry registry;
    private final Map<UUID, PlayerQuestData> playerDataMap = new ConcurrentHashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public QuestService(final QuestForge plugin, final QuestRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void loadPlayerData(final UUID uuid, final PlayerQuestData data) {
        playerDataMap.put(uuid, data);
    }

    public PlayerQuestData getOrCreateData(final UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, k -> new PlayerQuestData());
    }

    public Optional<PlayerQuestData> getData(final UUID uuid) {
        return Optional.ofNullable(playerDataMap.get(uuid));
    }

    public void evictPlayerData(final UUID uuid) {
        playerDataMap.remove(uuid);
    }

    public Map<UUID, PlayerQuestData> getAllPlayerData() {
        return playerDataMap;
    }

    public StartResult start(final Player player, final Quest quest) {
        final PlayerQuestData data = getOrCreateData(player.getUniqueId());

        if (data.getProgress(quest.getId()) != null) {
            return StartResult.ALREADY_ACTIVE;
        }

        if (!quest.isRepeatable() && data.getCompletedQuestIds().contains(quest.getId())) {
            return StartResult.ALREADY_COMPLETED;
        }

        if (!quest.isUnlockedFor(data)) {
            return StartResult.NOT_UNLOCKED;
        }

        final int activeLimit = plugin.getConfig().getInt("active-quest-limit", 3);
        if (!player.hasPermission("questforge.bypass.limit") && data.activeCount() >= activeLimit) {
            return StartResult.LIMIT_REACHED;
        }

        data.putProgress(quest.getId(), new QuestProgress(quest.getId(), quest.getObjectives().size()));
        return StartResult.SUCCESS;
    }

    public AbandonResult abandon(final Player player, final Quest quest) {
        final PlayerQuestData data = getOrCreateData(player.getUniqueId());
        if (data.getProgress(quest.getId()) == null) {
            return AbandonResult.NOT_ACTIVE;
        }
        data.removeProgress(quest.getId());
        return AbandonResult.SUCCESS;
    }

    public void handleEvent(final ObjectiveEvent event) {
        final UUID uuid = event.player().getUniqueId();
        final PlayerQuestData data = playerDataMap.get(uuid);
        if (data == null) {
            return;
        }

        for (final Map.Entry<String, QuestProgress> entry : data.getActiveProgress().entrySet()) {
            final QuestProgress progress = entry.getValue();
            if (progress.isCompleted()) {
                continue;
            }

            final Optional<Quest> questOpt = registry.byId(entry.getKey());
            if (questOpt.isEmpty()) {
                continue;
            }

            final Quest quest = questOpt.get();
            final var objectives = quest.getObjectives();
            boolean progressed = false;

            for (int i = 0; i < objectives.size(); i++) {
                final Objective objective = objectives.get(i);
                if (progress.isObjectiveComplete(i, objective.getRequiredAmount())) {
                    continue;
                }
                if (objective.matches(event)) {
                    final int newValue = progress.incrementCounter(i, event.amount());
                    progressed = true;
                    sendObjectiveProgress(event.player(), quest, objective, newValue);
                }
            }

            if (progressed && progress.isComplete(quest)) {
                complete(event.player(), quest, data);
            }
        }
    }

    private void sendObjectiveProgress(final Player player, final Quest quest, final Objective objective, final int current) {
        final int required = objective.getRequiredAmount();
        final int capped = Math.min(current, required);
        final String template = plugin.getConfig().getString("messages.objective-progress",
                "<gray>{quest} | <yellow>{objective}</yellow> <white>{current}/{required}</white></gray>");

        final String questName = miniMessage.serialize(quest.getDisplayName());
        final String objName = miniMessage.serialize(objective.description());

        final String message = template
                .replace("{quest}", questName)
                .replace("{objective}", objName)
                .replace("{current}", String.valueOf(capped))
                .replace("{required}", String.valueOf(required));

        player.sendActionBar(miniMessage.deserialize(message));
    }

    private void complete(final Player player, final Quest quest, final PlayerQuestData data) {
        data.removeProgress(quest.getId());
        data.addCompleted(quest.getId());

        final String completedTemplate = plugin.getConfig().getString("messages.quest-completed",
                "<gold><bold>Quest Complete!</bold></gold> <yellow>{quest}</yellow>");
        final String questName = miniMessage.serialize(quest.getDisplayName());
        player.sendMessage(miniMessage.deserialize(
                Messages.prefix(plugin) + completedTemplate.replace("{quest}", questName)
        ));

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (final Reward reward : quest.getRewards()) {
                reward.grant(plugin, player);
            }
        });

        notifyNextQuestInChain(player, quest);
    }

    private void notifyNextQuestInChain(final Player player, final Quest completedQuest) {
        for (final Quest candidate : registry.all()) {
            if (completedQuest.getId().equals(candidate.getRequiredQuestId())) {
                final String hintTemplate = plugin.getConfig().getString("messages.next-quest-hint",
                        "<aqua>New quest unlocked: <yellow>{quest}</yellow>! Use /quest start {id} to begin.</aqua>");
                final String label = miniMessage.serialize(candidate.getDisplayName());
                final String message = hintTemplate
                        .replace("{quest}", label)
                        .replace("{id}", candidate.getId());
                player.sendMessage(miniMessage.deserialize(Messages.prefix(plugin) + message));
            }
        }
    }

    public enum StartResult {
        SUCCESS, ALREADY_ACTIVE, ALREADY_COMPLETED, NOT_UNLOCKED, LIMIT_REACHED
    }

    public enum AbandonResult {
        SUCCESS, NOT_ACTIVE
    }
}
