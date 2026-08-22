package fr.valoriatycoon.ranks;

import fr.valoriatycoon.compaction.CompactedResource;
import fr.valoriatycoon.professions.ProfessionType;
import fr.valoriatycoon.quests.QuestRarity;
import fr.valoriatycoon.tools.ToolType;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;

/** Requirements consumed or checked when earning one medieval rank. */
public record RankRequirement(
        int level,
        String name,
        long requiredMoneyCents,
        long requiredPlaytimeSeconds,
        int requiredVanillaExperienceLevels,
        Map<QuestRarity, Integer> quests,
        Map<ToolType, Integer> toolLevels,
        Map<ProfessionType, Integer> professionLevels,
        Map<Material, Integer> items,
        Map<CompactedResource, Integer> compactedItems,
        double permanentRevenueBonus,
        double toolExperienceBonus,
        double professionExperienceBonus,
        double toolCoinBonus,
        double generatorProductionBonus,
        int generatorSlotBonus
) {
    public RankRequirement {
        if (level < 1) {
            throw new IllegalArgumentException("Rank level must be positive");
        }
        name = Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Rank name must not be blank");
        }
        if (requiredMoneyCents < 0) {
            throw new IllegalArgumentException("Rank price must not be negative");
        }
        if (requiredPlaytimeSeconds < 0) {
            throw new IllegalArgumentException("Required playtime must not be negative");
        }
        if (requiredVanillaExperienceLevels < 0) {
            throw new IllegalArgumentException("Required vanilla experience must not be negative");
        }
        validateBonus(permanentRevenueBonus, "revenue");
        validateBonus(toolExperienceBonus, "tool experience");
        validateBonus(professionExperienceBonus, "profession experience");
        validateBonus(toolCoinBonus, "tool coin");
        validateBonus(generatorProductionBonus, "generator production");
        if (generatorSlotBonus < 0 || generatorSlotBonus > 10_000) {
            throw new IllegalArgumentException("Generator slot bonus must be between 0 and 10000");
        }
        quests = Map.copyOf(quests);
        toolLevels = Map.copyOf(toolLevels);
        professionLevels = Map.copyOf(professionLevels);
        items = Map.copyOf(items);
        compactedItems = Map.copyOf(compactedItems);
    }

    /** Creates a rank without compacted-item requirements. */
    public RankRequirement(
            int level,
            String name,
            long requiredMoneyCents,
            long requiredPlaytimeSeconds,
            int requiredVanillaExperienceLevels,
            Map<QuestRarity, Integer> quests,
            Map<ToolType, Integer> toolLevels,
            Map<ProfessionType, Integer> professionLevels,
            Map<Material, Integer> items,
            double permanentRevenueBonus
    ) {
        this(
                level,
                name,
                requiredMoneyCents,
                requiredPlaytimeSeconds,
                requiredVanillaExperienceLevels,
                quests,
                toolLevels,
                professionLevels,
                items,
                Map.of(),
                permanentRevenueBonus,
                0.0,
                0.0,
                0.0,
                0.0,
                0
        );
    }

    /** Creates a rank without playtime, vanilla-XP or profession requirements. */
    public RankRequirement(
            int level,
            String name,
            long requiredMoneyCents,
            Map<QuestRarity, Integer> quests,
            Map<ToolType, Integer> toolLevels,
            Map<Material, Integer> items,
            double permanentRevenueBonus
    ) {
        this(
                level,
                name,
                requiredMoneyCents,
                0L,
                0,
                quests,
                toolLevels,
                Map.of(),
                items,
                Map.of(),
                permanentRevenueBonus,
                0.0,
                0.0,
                0.0,
                0.0,
                0
        );
    }

    /** Creates a rank with playtime but without vanilla-XP or profession requirements. */
    public RankRequirement(
            int level,
            String name,
            long requiredMoneyCents,
            long requiredPlaytimeSeconds,
            Map<QuestRarity, Integer> quests,
            Map<ToolType, Integer> toolLevels,
            Map<Material, Integer> items,
            double permanentRevenueBonus
    ) {
        this(
                level,
                name,
                requiredMoneyCents,
                requiredPlaytimeSeconds,
                0,
                quests,
                toolLevels,
                Map.of(),
                items,
                Map.of(),
                permanentRevenueBonus,
                0.0,
                0.0,
                0.0,
                0.0,
                0
        );
    }

    private static void validateBonus(double bonus, String name) {
        if (!Double.isFinite(bonus) || bonus < 0.0 || bonus > 10.0) {
            throw new IllegalArgumentException(
                    "Permanent " + name + " bonus must be finite and between 0 and 10"
            );
        }
    }
}
