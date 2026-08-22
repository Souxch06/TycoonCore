package fr.valoriatycoon.tools;

import java.util.Locale;

/** Built-in common and tool-specific farming capabilities. */
public enum ToolCapability {
    EFFICIENCY,
    LEVEL_BOOST,
    MONEY_BOOST,
    COIN_BOOST,
    SPEED_BURST,
    FARM_KEY_FINDER,
    CRATE_KEY_FINDER,

    AREA_MINING,
    ORE_FORTUNE,
    AUTO_SMELT,
    GEM_FINDER,
    MINE_COIN_FINDER,

    AREA_HARVEST,
    HARVEST_FORTUNE,
    AUTO_REPLANT,
    SEED_FINDER,
    FARM_COIN_FINDER,
    UFO_HARVEST,

    TIMBER,
    WOOD_FORTUNE,
    APPLE_FINDER,
    WOOD_COIN_FINDER,

    DOUBLE_CATCH,
    TREASURE_LUCK,
    RARE_CATCH,
    FISH_COIN_FINDER;

    public String storageKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
