package fr.valoriatycoon.crates;

import java.math.BigDecimal;
import java.util.UUID;

/** Receives one valid multi-tool action and independently rolls Farm and rarity keys. */
@FunctionalInterface
public interface ToolCrateRewardSink {
    void roll(UUID playerId, BigDecimal farmChance, BigDecimal rarityChance);
}
