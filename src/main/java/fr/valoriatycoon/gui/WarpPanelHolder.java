package fr.valoriatycoon.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Identifies the protected /warp menu. */
public final class WarpPanelHolder implements InventoryHolder {
    private Inventory inventory;

    public void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Warp panel is not bound");
        }
        return inventory;
    }
}
