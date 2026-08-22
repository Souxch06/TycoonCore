package fr.valoriatycoon.progression;

import java.math.BigDecimal;

/** Immutable contract consumed by deterministic level-experience calculations. */
public interface ExperienceCurve {

    /** Maximum reachable level. */
    int maximumLevel();

    /** XP required to leave level one before additional multipliers. */
    long baseExperience();

    /** Compounding growth applied once for every current level above one. */
    BigDecimal experienceMultiplier();

    /** Additional non-compounding difficulty active at one current level. */
    BigDecimal difficultyMultiplier(int level);
}
