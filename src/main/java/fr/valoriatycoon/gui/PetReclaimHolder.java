package fr.valoriatycoon.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Identifies the pet-to-egg reclaim inventory opened by the spawn NPC. */
public final class PetReclaimHolder implements InventoryHolder {
    private Inventory inventory;

    public void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Pet reclaim inventory has not been bound");
        }
        return inventory;
    }
}
