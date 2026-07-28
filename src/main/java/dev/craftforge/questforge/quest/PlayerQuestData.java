package dev.craftforge.questforge.quest;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerQuestData {

    private final Map<String, QuestProgress> activeProgress = new ConcurrentHashMap<>();
    private final Set<String> completedQuestIds = ConcurrentHashMap.newKeySet();
    private volatile boolean dirty = false;

    public Map<String, QuestProgress> getActiveProgress() {
        return Collections.unmodifiableMap(activeProgress);
    }

    public QuestProgress getProgress(final String questId) {
        return activeProgress.get(questId);
    }

    public void putProgress(final String questId, final QuestProgress progress) {
        activeProgress.put(questId, progress);
        dirty = true;
    }

    public void removeProgress(final String questId) {
        activeProgress.remove(questId);
        dirty = true;
    }

    public Set<String> getCompletedQuestIds() {
        return Collections.unmodifiableSet(completedQuestIds);
    }

    public void addCompleted(final String questId) {
        completedQuestIds.add(questId);
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        dirty = false;
    }

    public int activeCount() {
        return activeProgress.size();
    }
}
