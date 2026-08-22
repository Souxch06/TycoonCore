package fr.valoriatycoon.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Identity-bearing holder for one player's parcel upgrades. */
public final class PlotUpgradePanelHolder implements InventoryHolder {
    private Inventory inventory;

    public void bind(Inventory inventory) {
        if (this.inventory != null) {
            throw new IllegalStateException("Plot upgrade panel is already bound");
        }
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Plot upgrade panel is not bound");
        }
        return inventory;
    }
}
