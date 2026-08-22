package fr.valoriatycoon.tutorial;

import java.util.Locale;

/** Ordered onboarding stages; enum order is persisted in SQLite. */
public enum TutorialStep {
    MINE_COAL,
    HARVEST_WHEAT,
    CHOP_OAK,
    CATCH_COD,
    REACH_VANILLA_LEVEL,
    COMPLETE_COMMON_QUEST,
    READY_FOR_RANK;

    public String configKey() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public TutorialStep next() {
        int next = ordinal() + 1;
        return next >= values().length ? READY_FOR_RANK : values()[next];
    }

    public boolean actionable() {
        return this != READY_FOR_RANK;
    }
}
