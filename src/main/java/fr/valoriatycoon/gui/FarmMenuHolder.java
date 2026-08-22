package fr.valoriatycoon.gui;

import java.util.Map;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Identity-bearing holder; no title matching is used for GUI security. */
public final class FarmMenuHolder implements InventoryHolder {
    private final Map<Integer, String> destinations;
    private Inventory inventory;

    public FarmMenuHolder(Map<Integer, String> destinations) {
        this.destinations = Map.copyOf(destinations);
    }

    public Map<Integer, String> destinations() {
        return destinations;
    }

    public void bind(Inventory inventory) {
        if (this.inventory != null) {
            throw new IllegalStateException("Farm menu holder is already bound");
        }
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Farm menu holder is not bound yet");
        }
        return inventory;
    }
}
