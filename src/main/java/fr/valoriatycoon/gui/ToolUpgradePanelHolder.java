package fr.valoriatycoon.gui;

import fr.valoriatycoon.tools.ToolType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Identity and selected tool type for a shift-right-click upgrade panel. */
public final class ToolUpgradePanelHolder implements InventoryHolder {
    private final ToolType toolType;
    private Inventory inventory;

    public ToolUpgradePanelHolder(ToolType toolType) {
        this.toolType = toolType;
    }

    public ToolType toolType() {
        return toolType;
    }

    public void bind(Inventory inventory) {
        if (this.inventory != null) {
            throw new IllegalStateException("Tool panel holder is already bound");
        }
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Tool panel holder is not bound");
        }
        return inventory;
    }
}
