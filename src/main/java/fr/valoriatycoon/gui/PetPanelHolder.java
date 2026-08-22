package fr.valoriatycoon.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Identifies inventories owned by the pet collection panel. */
public final class PetPanelHolder implements InventoryHolder {
    private Inventory inventory;

    public void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Pet inventory has not been bound");
        }
        return inventory;
    }
}
