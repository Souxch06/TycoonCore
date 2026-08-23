package fr.valoriatycoon.crates;

import java.util.Locale;

/** Generic crate families; the existing Pet Crate keeps its specialized egg transaction. */
public enum CrateType {
    VOTE("Vote", false),
    QUEST("Quêtes", false),
    FARM("Farm", false),
    COMMON("Commune", false),
    RARE("Rare", false),
    EPIC("Épique", false),
    LEGENDARY("Légendaire", false),
    VALORIA("Valoria", true);

    private final String displayName;
    private final boolean paid;

    CrateType(String displayName, boolean paid) {
        this.displayName = displayName;
        this.paid = paid;
    }

    public String displayName() {
        return displayName;
    }

    /** Returns whether this is the store-signature family rather than an F2W rarity tier. */
    public boolean paid() {
        return paid;
    }

    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
