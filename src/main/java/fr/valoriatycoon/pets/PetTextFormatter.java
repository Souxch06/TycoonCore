package fr.valoriatycoon.pets;

import java.math.BigDecimal;
import java.util.Objects;

/** Shared French labels and exact percentage formatting for pet interfaces. */
public final class PetTextFormatter {
    private PetTextFormatter() {
    }

    /** Returns the player-facing French label of one pet effect. */
    public static String effectName(PetEffect effect) {
        return switch (Objects.requireNonNull(effect, "effect")) {
            case MONEY -> "revenus";
            case TOOL_EXPERIENCE -> "XP outils";
            case PROFESSION_EXPERIENCE -> "XP métiers";
            case TOOL_COINS -> "coins outils";
            case GENERATOR_PRODUCTION -> "production générateurs";
            case DOUBLE_TOOL_REWARD_CHANCE -> "chance de double récompense outil";
            case DOUBLE_GENERATOR_OUTPUT_CHANCE -> "chance de double production";
        };
    }

    /** Converts a decimal ratio such as 0.01 into the exact percentage text 1. */
    public static String percentage(BigDecimal value) {
        return Objects.requireNonNull(value, "value")
                .movePointRight(2)
                .stripTrailingZeros()
                .toPlainString();
    }
}
