package dev.craftforge.questforge.quest.objective;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class CraftItemObjective extends Objective {

    public CraftItemObjective(final String target, final int requiredAmount) {
        super(ObjectiveType.CRAFT_ITEM, target, requiredAmount);
    }

    @Override
    public Component description() {
        final String label = getTarget().equalsIgnoreCase("any") ? "anything" : getTarget().replace("_", " ").toLowerCase();
        return MiniMessage.miniMessage().deserialize(
                "<gray>Craft <yellow>" + getRequiredAmount() + "</yellow> <white>" + label + "</white></gray>"
        );
    }

    @Override
    public boolean matches(final ObjectiveEvent event) {
        return event.type() == ObjectiveType.CRAFT_ITEM && targetMatches(event.targetKey());
    }
}
