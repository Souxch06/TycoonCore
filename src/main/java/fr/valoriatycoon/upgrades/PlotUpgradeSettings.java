package fr.valoriatycoon.upgrades;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable plot-upgrade menu and curves. */
public record PlotUpgradeSettings(
        int menuSize,
        String menuTitle,
        Map<PlotUpgradeType, PlotUpgradeDefinition> definitions
) {
    public PlotUpgradeSettings {
        EnumMap<PlotUpgradeType, PlotUpgradeDefinition> copy = new EnumMap<>(PlotUpgradeType.class);
        copy.putAll(definitions);
        definitions = Collections.unmodifiableMap(copy);
    }

    public PlotUpgradeDefinition definition(PlotUpgradeType type) {
        PlotUpgradeDefinition definition = definitions.get(type);
        if (definition == null) {
            throw new IllegalArgumentException("Missing plot upgrade " + type);
        }
        return definition;
    }
}
