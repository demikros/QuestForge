package dev.craftforge.questforge.quest;

import dev.craftforge.questforge.quest.objective.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

public final class QuestRegistry {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LinkedHashMap<String, Quest> quests = new LinkedHashMap<>();

    public QuestRegistry(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void load() {
        quests.clear();
        final ConfigurationSection questsSection = plugin.getConfig().getConfigurationSection("quests");
        if (questsSection == null) {
            logger.warning("[QuestRegistry] No 'quests' section found in config.yml.");
            return;
        }

        for (final String questId : questsSection.getKeys(false)) {
            final ConfigurationSection section = questsSection.getConfigurationSection(questId);
            if (section == null) {
                logger.warning("[QuestRegistry] Quest '" + questId + "' is not a valid section, skipping.");
                continue;
            }

            final Quest quest = parseQuest(questId, section);
            if (quest != null) {
                quests.put(questId, quest);
            }
        }

        logger.info("[QuestRegistry] Loaded " + quests.size() + " quest(s).");
    }

    private Quest parseQuest(final String questId, final ConfigurationSection section) {
        final String rawDisplayName = section.getString("display-name");
        if (rawDisplayName == null || rawDisplayName.isBlank()) {
            logger.warning("[QuestRegistry] Quest '" + questId + "' is missing 'display-name', skipping.");
            return null;
        }

        final Component displayName = miniMessage.deserialize(rawDisplayName);

        final List<Component> descriptionLines = new ArrayList<>();
        for (final String line : section.getStringList("description")) {
            descriptionLines.add(miniMessage.deserialize(line));
        }

        final List<Objective> objectives = parseObjectives(questId, section);
        if (objectives == null) {
            return null;
        }

        final List<Reward> rewards = parseRewards(questId, section);
        if (rewards == null) {
            return null;
        }

        final String requiredQuest = section.getString("required-quest", null);
        final boolean repeatable = section.getBoolean("repeatable", false);

        return new Quest(questId, displayName, descriptionLines, objectives, rewards, requiredQuest, repeatable);
    }

    private List<Objective> parseObjectives(final String questId, final ConfigurationSection section) {
        final List<Map<?, ?>> rawObjectives = section.getMapList("objectives");
        if (rawObjectives.isEmpty()) {
            logger.warning("[QuestRegistry] Quest '" + questId + "' has no objectives, skipping.");
            return null;
        }

        final List<Objective> objectives = new ArrayList<>();
        for (int i = 0; i < rawObjectives.size(); i++) {
            final Map<?, ?> raw = rawObjectives.get(i);
            final String typeStr = String.valueOf(raw.get("type"));
            final String target = String.valueOf(raw.get("target"));
            final int amount = raw.get("amount") instanceof Number n ? n.intValue() : -1;

            if (amount <= 0) {
                logger.warning("[QuestRegistry] Quest '" + questId + "' objective #" + i + " has invalid amount, skipping quest.");
                return null;
            }

            final ObjectiveType objectiveType;
            try {
                objectiveType = ObjectiveType.valueOf(typeStr.toUpperCase());
            } catch (final IllegalArgumentException e) {
                logger.warning("[QuestRegistry] Quest '" + questId + "' objective #" + i + " has unknown type '" + typeStr + "', skipping quest.");
                return null;
            }

            if (!target.equalsIgnoreCase("any") && !validateTarget(objectiveType, target)) {
                logger.warning("[QuestRegistry] Quest '" + questId + "' objective #" + i + " has invalid target '" + target + "' for type " + objectiveType + ", skipping quest.");
                return null;
            }

            objectives.add(buildObjective(objectiveType, target, amount));
        }

        return objectives;
    }

    private boolean validateTarget(final ObjectiveType type, final String target) {
        return switch (type) {
            case KILL_MOB -> {
                try {
                    EntityType.valueOf(target.toUpperCase());
                    yield true;
                } catch (final IllegalArgumentException e) {
                    yield false;
                }
            }
            case BREAK_BLOCK, PLACE_BLOCK, COLLECT_ITEM, CRAFT_ITEM, FISH_ITEM -> {
                try {
                    Material.valueOf(target.toUpperCase());
                    yield true;
                } catch (final IllegalArgumentException e) {
                    yield false;
                }
            }
            case TRAVEL_DISTANCE -> true;
        };
    }

    private Objective buildObjective(final ObjectiveType type, final String target, final int amount) {
        return switch (type) {
            case KILL_MOB -> new KillMobObjective(target, amount);
            case BREAK_BLOCK -> new BreakBlockObjective(target, amount);
            case PLACE_BLOCK -> new PlaceBlockObjective(target, amount);
            case COLLECT_ITEM -> new CollectItemObjective(target, amount);
            case CRAFT_ITEM -> new CraftItemObjective(target, amount);
            case FISH_ITEM -> new FishItemObjective(target, amount);
            case TRAVEL_DISTANCE -> new BreakBlockObjective(target, amount);
        };
    }

    private List<Reward> parseRewards(final String questId, final ConfigurationSection section) {
        final List<Map<?, ?>> rawRewards = section.getMapList("rewards");
        final List<Reward> rewards = new ArrayList<>();

        for (int i = 0; i < rawRewards.size(); i++) {
            final Map<?, ?> raw = rawRewards.get(i);
            final String typeStr = String.valueOf(raw.get("type"));

            final Reward reward = switch (typeStr.toUpperCase()) {
                case "ITEM" -> {
                    final String matStr = String.valueOf(raw.get("material"));
                    final int amount = raw.get("amount") instanceof Number n ? n.intValue() : 1;
                    final Material material;
                    try {
                        material = Material.valueOf(matStr.toUpperCase());
                    } catch (final IllegalArgumentException e) {
                        logger.warning("[QuestRegistry] Quest '" + questId + "' reward #" + i + " has invalid material '" + matStr + "', skipping reward.");
                        yield null;
                    }
                    yield new Reward.ItemReward(material, amount);
                }
                case "COMMAND" -> {
                    final String command = String.valueOf(raw.get("command"));
                    if (command.isBlank()) {
                        logger.warning("[QuestRegistry] Quest '" + questId + "' reward #" + i + " has blank command, skipping reward.");
                        yield null;
                    }
                    yield new Reward.CommandReward(command);
                }
                case "EXPERIENCE" -> {
                    final int amount = raw.get("amount") instanceof Number n ? n.intValue() : -1;
                    if (amount <= 0) {
                        logger.warning("[QuestRegistry] Quest '" + questId + "' reward #" + i + " has invalid experience amount, skipping reward.");
                        yield null;
                    }
                    yield new Reward.ExperienceReward(amount);
                }
                case "MESSAGE" -> {
                    final String text = String.valueOf(raw.getOrDefault("text", ""));
                    yield new Reward.MessageReward(text);
                }
                default -> {
                    logger.warning("[QuestRegistry] Quest '" + questId + "' reward #" + i + " has unknown type '" + typeStr + "', skipping reward.");
                    yield null;
                }
            };

            if (reward != null) {
                rewards.add(reward);
            }
        }

        return rewards;
    }

    public Optional<Quest> byId(final String questId) {
        return Optional.ofNullable(quests.get(questId));
    }

    public Collection<Quest> all() {
        return Collections.unmodifiableCollection(quests.values());
    }

    public void reload() {
        load();
    }
}
