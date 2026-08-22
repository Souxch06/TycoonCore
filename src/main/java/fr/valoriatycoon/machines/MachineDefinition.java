package fr.valoriatycoon.machines;

import fr.valoriatycoon.tools.ToolType;
import java.time.Duration;
import java.util.List;
import org.bukkit.Material;

/** Validated resource generator behavior, presentation and shop price. */
public record MachineDefinition(
        String id,
        Material blockMaterial,
        Material icon,
        int shopSlot,
        String displayName,
        List<String> lore,
        long moneyCostCents,
        ToolType coinType,
        long coinCost,
        Duration productionInterval,
        Material outputMaterial,
        int outputAmount,
        long outputSellPriceCents,
        long storageCapacity
) {
    public MachineDefinition {
        lore = List.copyOf(lore);
    }
}
