package fr.valoriatycoon.crates;

import org.bukkit.entity.Player;

/** Main-thread visual hook fired only after a generic key/reward transaction commits. */
@FunctionalInterface
public interface CrateOpeningEffect {
    CrateOpeningEffect NONE = (player, type) -> {
    };

    void play(Player player, CrateType type);
}
