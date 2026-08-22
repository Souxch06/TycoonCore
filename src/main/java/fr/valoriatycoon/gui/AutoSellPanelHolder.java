package fr.valoriatycoon.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Identity-bearing holder for the dedicated /autosell panel. */
public final class AutoSellPanelHolder implements InventoryHolder {
    private Inventory inventory;

    public void bind(Inventory inventory) {
        if (this.inventory != null) {
            throw new IllegalStateException("Auto-sell panel holder is already bound");
        }
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Auto-sell panel holder is not bound yet");
        }
        return inventory;
    }
}
