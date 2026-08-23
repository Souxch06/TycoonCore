package fr.valoriatycoon.gui;

import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Secure identity holder for one farm's rank-gated zone selector. */
public final class FarmZoneMenuHolder implements InventoryHolder {
    private final String farmId;
    private final Map<Integer, Integer> zonesBySlot;
    private Inventory inventory;

    public FarmZoneMenuHolder(String farmId, Map<Integer, Integer> zonesBySlot) {
        this.farmId = farmId;
        this.zonesBySlot = Map.copyOf(zonesBySlot);
    }

    public String farmId() {
        return farmId;
    }

    public Map<Integer, Integer> zonesBySlot() {
        return zonesBySlot;
    }

    public void bind(Inventory inventory) {
        if (this.inventory != null) {
            throw new IllegalStateException("Farm zone menu holder is already bound");
        }
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Farm zone menu holder is not bound yet");
        }
        return inventory;
    }
}
