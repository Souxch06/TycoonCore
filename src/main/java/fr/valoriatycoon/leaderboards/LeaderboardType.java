package fr.valoriatycoon.leaderboards;

/** Server-authoritative cached leaderboard categories. */
public enum LeaderboardType {
    MONEY("money", "Fortunes"),
    ISLAND_LEVEL("island-level", "Niveaux de Skyblock"),
    RANK("rank", "Rangs médiévaux"),
    PRODUCTION("production", "Production totale"),
    PLAYTIME("playtime", "Temps de jeu");

    private final String configKey;
    private final String displayName;

    LeaderboardType(String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }

    public String configKey() {
        return configKey;
    }

    public String displayName() {
        return displayName;
    }
}
