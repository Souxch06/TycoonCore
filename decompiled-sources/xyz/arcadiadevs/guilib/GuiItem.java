/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.ItemStack
 */
package xyz.arcadiadevs.guilib;

import org.bukkit.inventory.ItemStack;
import xyz.arcadiadevs.guilib.GuiItemType;

public class GuiItem {
    private final GuiItemType type;
    private final ItemStack item;
    private final Runnable action;

    public GuiItem(GuiItemType guiItemType, ItemStack itemStack, Runnable runnable) {
        this.type = guiItemType;
        this.item = itemStack;
        this.action = runnable;
    }

    public GuiItemType getType() {
        return this.type;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public Runnable getAction() {
        return this.action;
    }
}

