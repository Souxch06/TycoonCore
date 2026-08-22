package fr.valoriatycoon.quests;

import java.util.Map;

public record QuestProfile(
        Map<String, QuestProgress> progress,
        Map<QuestRarity, Long> availableCompletions
) {
    public QuestProfile {
        progress = Map.copyOf(progress);
        availableCompletions = Map.copyOf(availableCompletions);
    }
    public long available(QuestRarity rarity) { return availableCompletions.getOrDefault(rarity, 0L); }
}
