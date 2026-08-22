package fr.valoriatycoon.crates;

import java.util.Locale;

/** Physical crate models available in the market station, including the specialized Pet crate. */
public enum CrateStationType {
    VOTE(CrateType.VOTE, "Caisse Vote"),
    QUEST(CrateType.QUEST, "Caisse Quêtes"),
    FARM(CrateType.FARM, "Caisse Farm"),
    COMMON(CrateType.COMMON, "Caisse Commune"),
    RARE(CrateType.RARE, "Caisse Rare"),
    EPIC(CrateType.EPIC, "Caisse Épique"),
    LEGENDARY(CrateType.LEGENDARY, "Caisse Légendaire"),
    VALORIA(CrateType.VALORIA, "Caisse Valoria"),
    PETS(null, "Caisse Pets");

    private final CrateType crateType;
    private final String displayName;

    CrateStationType(CrateType crateType, String displayName) {
        this.crateType = crateType;
        this.displayName = displayName;
    }

    public CrateType crateType() {
        return crateType;
    }

    public boolean pets() {
        return this == PETS;
    }

    public String displayName() {
        return displayName;
    }

    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
