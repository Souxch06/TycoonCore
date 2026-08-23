package fr.valoriatycoon.quests;

import fr.valoriatycoon.tools.ToolType;

/** Repeatable, action-based quest used by the medieval rank prerequisite system. */
public record QuestDefinition(
        String id,
        QuestRarity rarity,
        ToolType toolType,
        long targetActions,
        long rewardMoneyCents
) {
}
