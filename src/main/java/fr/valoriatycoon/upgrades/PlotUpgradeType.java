package fr.valoriatycoon.upgrades;

import java.util.Locale;

/** Initial plot-scoped upgrades; values are shared by the Tycoon, not individual members. */
public enum PlotUpgradeType {
    PLOT_SIZE,
    HOPPER_LIMIT,
    MEMBER_LIMIT;

    public String configKey() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
