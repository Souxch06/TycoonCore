package fr.valoriatycoon.tools;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Material;

/** Configured dual-currency curve, applicable tools and GUI presentation for one capability. */
public record ToolCapabilityDefinition(
        ToolCapability capability,
        Set<ToolType> applicableTools,
        int initialLevel,
        int slot,
        Material icon,
        String name,
        List<String> lore,
        List<Level> levels
) {
    public ToolCapabilityDefinition {
        EnumSet<ToolType> tools = EnumSet.noneOf(ToolType.class);
        tools.addAll(applicableTools);
        applicableTools = Collections.unmodifiableSet(tools);
        lore = List.copyOf(lore);
        levels = List.copyOf(levels);
    }

    public boolean appliesTo(ToolType type) {
        return applicableTools.contains(type);
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

    public record Level(int level, long moneyCostCents, long toolCoinCost, BigDecimal value) {
    }
}
