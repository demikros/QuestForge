package dev.craftforge.questforge.quest.objective;

import net.kyori.adventure.text.Component;

public abstract class Objective {

    private final ObjectiveType type;
    private final String target;
    private final int requiredAmount;

    protected Objective(final ObjectiveType type, final String target, final int requiredAmount) {
        this.type = type;
        this.target = target;
        this.requiredAmount = requiredAmount;
    }

    public ObjectiveType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }

    public abstract Component description();

    public abstract boolean matches(ObjectiveEvent event);

    protected boolean targetMatches(final String eventTarget) {
        return target.equalsIgnoreCase("any") || target.equalsIgnoreCase(eventTarget);
    }
}
