package dev.craftforge.questforge.quest;

import dev.craftforge.questforge.quest.objective.Objective;
import net.kyori.adventure.text.Component;

import java.util.List;

public final class Quest {

    private final String id;
    private final Component displayName;
    private final List<Component> descriptionLines;
    private final List<Objective> objectives;
    private final List<Reward> rewards;
    private final String requiredQuestId;
    private final boolean repeatable;

    public Quest(
            final String id,
            final Component displayName,
            final List<Component> descriptionLines,
            final List<Objective> objectives,
            final List<Reward> rewards,
            final String requiredQuestId,
            final boolean repeatable
    ) {
        this.id = id;
        this.displayName = displayName;
        this.descriptionLines = List.copyOf(descriptionLines);
        this.objectives = List.copyOf(objectives);
        this.rewards = List.copyOf(rewards);
        this.requiredQuestId = requiredQuestId;
        this.repeatable = repeatable;
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public List<Component> getDescriptionLines() {
        return descriptionLines;
    }

    public List<Objective> getObjectives() {
        return objectives;
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    public String getRequiredQuestId() {
        return requiredQuestId;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public boolean isUnlockedFor(final PlayerQuestData playerData) {
        if (requiredQuestId == null) {
            return true;
        }
        return playerData.getCompletedQuestIds().contains(requiredQuestId);
    }
}
