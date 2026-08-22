package fr.valoriatycoon.pets;

/** Passive multipliers and special proc chances available to an active pet. */
public enum PetEffect {
    MONEY,
    TOOL_EXPERIENCE,
    PROFESSION_EXPERIENCE,
    TOOL_COINS,
    GENERATOR_PRODUCTION,
    DOUBLE_TOOL_REWARD_CHANCE,
    DOUBLE_GENERATOR_OUTPUT_CHANCE;

    /** Returns whether the configured value is a probability instead of a multiplier bonus. */
    public boolean chance() {
        return this == DOUBLE_TOOL_REWARD_CHANCE
                || this == DOUBLE_GENERATOR_OUTPUT_CHANCE;
    }
}
