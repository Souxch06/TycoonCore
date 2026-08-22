package fr.valoriatycoon.tools;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable per-player, per-tool progression and dedicated coin snapshot. */
public record ToolProfile(
        ToolType toolType,
        int toolLevel,
        long toolExperience,
        long specialCoins,
        Map<ToolCapability, Integer> capabilityLevels
) {
    public ToolProfile {
        if (specialCoins < 0) {
            throw new IllegalArgumentException("Tool coins cannot be negative");
        }
        EnumMap<ToolCapability, Integer> copy = new EnumMap<>(ToolCapability.class);
        copy.putAll(capabilityLevels);
        capabilityLevels = Collections.unmodifiableMap(copy);
    }

    public int capabilityLevel(ToolCapability capability) {
        return capabilityLevels.getOrDefault(capability, 0);
    }

    public ToolProfile withCapability(ToolCapability capability, int level, long resultingCoins) {
        EnumMap<ToolCapability, Integer> updated = new EnumMap<>(ToolCapability.class);
        updated.putAll(capabilityLevels);
        updated.put(capability, level);
        return new ToolProfile(toolType, toolLevel, toolExperience, resultingCoins, updated);
    }

    public ToolProfile withProgress(int level, long experience, long coins) {
        return new ToolProfile(toolType, level, experience, coins, capabilityLevels);
    }
}
