package dev.craftforge.questforge.quest.objective;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class FishItemObjective extends Objective {

    public FishItemObjective(final String target, final int requiredAmount) {
        super(ObjectiveType.FISH_ITEM, target, requiredAmount);
    }

    @Override
    public Component description() {
        final String label = getTarget().equalsIgnoreCase("any") ? "any fish" : getTarget().replace("_", " ").toLowerCase();
        return MiniMessage.miniMessage().deserialize(
                "<gray>Fish <yellow>" + getRequiredAmount() + "</yellow> <white>" + label + "</white></gray>"
        );
    }

    @Override
    public boolean matches(final ObjectiveEvent event) {
        return event.type() == ObjectiveType.FISH_ITEM && targetMatches(event.targetKey());
    }
}
