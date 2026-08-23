package fr.valoriatycoon.farm.autosell;

/** Cached player state. Level zero is always locked and disabled. */
public record AutoSellProfile(boolean enabled, int level) {
    public AutoSellProfile {
        if (level < 0 || level > 5) {
            throw new IllegalArgumentException("Auto-sell level must be between 0 and 5");
        }
        if (level == 0) {
            enabled = false;
        }
    }

    public boolean unlocked() {
        return level > 0;
    }
}
