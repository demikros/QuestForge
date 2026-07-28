package dev.craftforge.questforge.quest.objective;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class BreakBlockObjective extends Objective {

    public BreakBlockObjective(final String target, final int requiredAmount) {
        super(ObjectiveType.BREAK_BLOCK, target, requiredAmount);
    }

    @Override
    public Component description() {
        final String label = getTarget().equalsIgnoreCase("any") ? "any block" : getTarget().replace("_", " ").toLowerCase();
        return MiniMessage.miniMessage().deserialize(
                "<gray>Break <yellow>" + getRequiredAmount() + "</yellow> <white>" + label + "</white></gray>"
        );
    }

    @Override
    public boolean matches(final ObjectiveEvent event) {
        return event.type() == ObjectiveType.BREAK_BLOCK && targetMatches(event.targetKey());
    }
}
