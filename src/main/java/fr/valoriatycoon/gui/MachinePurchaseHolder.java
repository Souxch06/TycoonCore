package fr.valoriatycoon.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class MachinePurchaseHolder implements InventoryHolder {
    private final String machineType;
    private Inventory inventory;
    public MachinePurchaseHolder(String machineType) { this.machineType = machineType; }
    public String machineType() { return machineType; }
    public void bind(Inventory inventory) { this.inventory = inventory; }
    @Override public @NotNull Inventory getInventory() {
        if (inventory == null) throw new IllegalStateException("Machine purchase menu is not bound");
        return inventory;
    }
}
