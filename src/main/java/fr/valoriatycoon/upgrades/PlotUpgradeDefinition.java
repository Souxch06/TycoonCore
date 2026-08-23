package fr.valoriatycoon.upgrades;

import java.util.List;
import java.util.Optional;
import org.bukkit.Material;

/** Configured value/cost curve and GUI presentation for one plot upgrade. */
public record PlotUpgradeDefinition(
        PlotUpgradeType type,
        int slot,
        Material icon,
        String name,
        List<String> lore,
        List<Level> levels
) {
    public PlotUpgradeDefinition {
        lore = List.copyOf(lore);
        levels = List.copyOf(levels);
    }

    public int maximumLevel() {
        return levels.size();
    }

    public Optional<Level> level(int level) {
        if (level < 1 || level > levels.size()) {
            return Optional.empty();
        }
        return Optional.of(levels.get(level - 1));
    }

    public record Level(int level, int value, long costCents) {
    }
}
