package fr.valoriatycoon.machines;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable machines.yml snapshot. */
public record MachineSettings(
        Shop shop,
        Control control,
        Upgrades upgrades,
        int maximumMachinesPerIsland,
        int maximumCyclesPerTick,
        Map<String, MachineDefinition> machines
) {
    public MachineSettings {
        machines = Collections.unmodifiableMap(new LinkedHashMap<>(machines));
    }

    public MachineDefinition machine(String id) {
        MachineDefinition definition = machines.get(id);
        if (definition == null) throw new IllegalArgumentException("Unknown machine: " + id);
        return definition;
    }

    public record Shop(int size, String title) {
    }

    public record Control(
            int size,
            String title,
            int statusSlot,
            int outputSlot,
            int collectSlot,
            int autoSellSlot,
            int speedUpgradeSlot,
            int sellUpgradeSlot
    ) {
    }

    public record Upgrades(Speed speed, SellPrice sellPrice) {
    }

    public record Speed(
            int maximumLevel,
            BigDecimal reductionPerLevel,
            BigDecimal minimumIntervalMultiplier,
            long baseCostCents,
            long costPerLevelCents
    ) {
    }

    public record SellPrice(
            int maximumLevel,
            BigDecimal bonusPerLevel,
            long baseCostCents,
            long costPerLevelCents
    ) {
    }
}
