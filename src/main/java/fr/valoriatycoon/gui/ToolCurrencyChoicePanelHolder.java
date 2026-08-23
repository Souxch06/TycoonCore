package fr.valoriatycoon.gui;

import fr.valoriatycoon.tools.ToolCapability;
import fr.valoriatycoon.tools.ToolType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Selected tool/capability context for the dual-currency purchase panel. */
public final class ToolCurrencyChoicePanelHolder implements InventoryHolder {
    private final ToolType toolType;
    private final ToolCapability capability;
    private Inventory inventory;

    public ToolCurrencyChoicePanelHolder(ToolType toolType, ToolCapability capability) {
        this.toolType = toolType;
        this.capability = capability;
    }

    public ToolType toolType() {
        return toolType;
    }

    public ToolCapability capability() {
        return capability;
    }

    public void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Currency choice panel is not bound");
        }
        return inventory;
    }
}
