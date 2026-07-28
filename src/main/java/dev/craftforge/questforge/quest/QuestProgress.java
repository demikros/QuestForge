package dev.craftforge.questforge.quest;

import java.util.concurrent.atomic.AtomicIntegerArray;

public final class QuestProgress {

    private final String questId;
    private final AtomicIntegerArray objectiveCounters;
    private final long startedTimestamp;
    private volatile boolean completed;

    public QuestProgress(final String questId, final int objectiveCount) {
        this.questId = questId;
        this.objectiveCounters = new AtomicIntegerArray(objectiveCount);
        this.startedTimestamp = System.currentTimeMillis();
        this.completed = false;
    }

    public QuestProgress(final String questId, final int[] savedCounters, final long startedTimestamp, final boolean completed) {
        this.questId = questId;
        this.objectiveCounters = new AtomicIntegerArray(savedCounters);
        this.startedTimestamp = startedTimestamp;
        this.completed = completed;
    }

    public String getQuestId() {
        return questId;
    }

    public long getStartedTimestamp() {
        return startedTimestamp;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(final boolean completed) {
        this.completed = completed;
    }

    public int getCounter(final int index) {
        return objectiveCounters.get(index);
    }

    public int incrementCounter(final int index, final int delta) {
        return objectiveCounters.addAndGet(index, delta);
    }

    public void setCounter(final int index, final int value) {
        objectiveCounters.set(index, value);
    }

    public int[] snapshotCounters() {
        final int[] snapshot = new int[objectiveCounters.length()];
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i] = objectiveCounters.get(i);
        }
        return snapshot;
    }

    public boolean isObjectiveComplete(final int index, final int required) {
        return objectiveCounters.get(index) >= required;
    }

    public boolean isComplete(final Quest quest) {
        final var objectives = quest.getObjectives();
        for (int i = 0; i < objectives.size(); i++) {
            if (!isObjectiveComplete(i, objectives.get(i).getRequiredAmount())) {
                return false;
            }
        }
        return true;
    }
}
