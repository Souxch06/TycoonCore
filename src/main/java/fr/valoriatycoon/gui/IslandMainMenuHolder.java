package fr.valoriatycoon.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class IslandMainMenuHolder implements InventoryHolder {
    private Inventory inventory;
    public void bind(Inventory inventory) { this.inventory = inventory; }
    @Override public @NotNull Inventory getInventory() {
        if (inventory == null) throw new IllegalStateException("Island menu is not bound");
        return inventory;
    }
}
