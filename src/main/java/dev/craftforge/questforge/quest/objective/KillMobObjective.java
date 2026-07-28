package dev.craftforge.questforge.quest.objective;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class KillMobObjective extends Objective {

    public KillMobObjective(final String target, final int requiredAmount) {
        super(ObjectiveType.KILL_MOB, target, requiredAmount);
    }

    @Override
    public Component description() {
        final String label = getTarget().equalsIgnoreCase("any") ? "any mob" : getTarget().replace("_", " ").toLowerCase();
        return MiniMessage.miniMessage().deserialize(
                "<gray>Kill <yellow>" + getRequiredAmount() + "</yellow> <white>" + label + "</white></gray>"
        );
    }

    @Override
    public boolean matches(final ObjectiveEvent event) {
        return event.type() == ObjectiveType.KILL_MOB && targetMatches(event.targetKey());
    }
}
