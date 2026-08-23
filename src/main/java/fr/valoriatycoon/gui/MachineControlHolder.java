package fr.valoriatycoon.gui;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class MachineControlHolder implements InventoryHolder {
    private final UUID machineId;
    private Inventory inventory;
    public MachineControlHolder(UUID machineId) { this.machineId = machineId; }
    public UUID machineId() { return machineId; }
    public void bind(Inventory inventory) { this.inventory = inventory; }
    @Override public @NotNull Inventory getInventory() {
        if (inventory == null) throw new IllegalStateException("Machine control is not bound");
        return inventory;
    }
}
