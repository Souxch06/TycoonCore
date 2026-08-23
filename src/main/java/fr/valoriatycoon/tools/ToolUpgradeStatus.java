package fr.valoriatycoon.tools;

/** Result of a server-authoritative tool capability purchase. */
public enum ToolUpgradeStatus {
    SUCCESS,
    INSUFFICIENT_FUNDS,
    INSUFFICIENT_TOOL_COINS,
    MAXIMUM_LEVEL,
    PROFILE_STALE
}
