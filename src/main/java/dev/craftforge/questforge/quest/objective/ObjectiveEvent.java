package dev.craftforge.questforge.quest.objective;

import org.bukkit.entity.Player;

public record ObjectiveEvent(
        ObjectiveType type,
        Player player,
        String targetKey,
        int amount
) {}
