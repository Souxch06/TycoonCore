package fr.valoriatycoon.upgrades;

/** Result of an atomic plot-upgrade purchase. */
public enum PlotUpgradeStatus {
    SUCCESS,
    INSUFFICIENT_FUNDS,
    MAXIMUM_LEVEL,
    PROFILE_STALE,
    NO_ACTIVE_TYCOON
}
