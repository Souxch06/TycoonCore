package fr.valoriatycoon.quests;

public record QuestProgressUpdate(
        QuestProgress progress,
        long newlyCompleted,
        long resultingMoneyCents
) {
}
