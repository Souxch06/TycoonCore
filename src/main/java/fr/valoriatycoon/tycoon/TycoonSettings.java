package fr.valoriatycoon.tycoon;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable tycoons.yml snapshot. */
public record TycoonSettings(
        int resetConfirmationSeconds,
        int resetBlocksPerTick,
        int inviteExpirationSeconds,
        Flight flight,
        Map<String, TycoonPlotGroup> groups
) {
    public TycoonSettings {
        groups = Collections.unmodifiableMap(new LinkedHashMap<>(groups));
    }

    public TycoonPlotGroup group(String id) {
        TycoonPlotGroup group = groups.get(id);
        if (group == null) {
            throw new IllegalArgumentException("Unknown Tycoon group: " + id);
        }
        return group;
    }

    public TycoonPlotGroup defaultGroup() {
        return groups.values().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No Tycoon plot groups configured"));
    }

    public record Flight(boolean enabled, boolean allowMembers, int voidRescueBelowFloor) {
    }
}
