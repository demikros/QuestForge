package dev.craftforge.questforge.quest.objective;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class PlaceBlockObjective extends Objective {

    public PlaceBlockObjective(final String target, final int requiredAmount) {
        super(ObjectiveType.PLACE_BLOCK, target, requiredAmount);
    }

    @Override
    public Component description() {
        final String label = getTarget().equalsIgnoreCase("any") ? "any block" : getTarget().replace("_", " ").toLowerCase();
        return MiniMessage.miniMessage().deserialize(
                "<gray>Place <yellow>" + getRequiredAmount() + "</yellow> <white>" + label + "</white></gray>"
        );
    }

    @Override
    public boolean matches(final ObjectiveEvent event) {
        return event.type() == ObjectiveType.PLACE_BLOCK && targetMatches(event.targetKey());
    }
}
