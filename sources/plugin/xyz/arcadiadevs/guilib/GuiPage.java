/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.inventory.Inventory
 */
package xyz.arcadiadevs.guilib;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import xyz.arcadiadevs.guilib.GuiItem;
import xyz.arcadiadevs.guilib.GuiItemType;
import xyz.arcadiadevs.guilib.GuiPageType;

public class GuiPage {
    private Inventory inventory = null;
    private final int rows;
    private final String title;
    private final GuiItem[][] items;

    public GuiPage(int n, String string) {
        this.rows = n;
        this.items = new GuiItem[n][9];
        this.title = string;
    }

    public Inventory getInventory(GuiPageType guiPageType) {
        if (this.inventory == null) {
            this.inventory = Bukkit.createInventory(null, (int)(this.rows * 9), (String)this.title);
            for (int i = 0; i < this.items.length; ++i) {
                for (int j = 0; j < this.items[i].length; ++j) {
                    GuiItem guiItem = this.items[i][j];
                    if (guiItem == null || (guiPageType == GuiPageType.LAST || guiPageType == GuiPageType.SINGLE) && guiItem.getType() == GuiItemType.NEXT || (guiPageType == GuiPageType.FIRST || guiPageType == GuiPageType.SINGLE) && guiItem.getType() == GuiItemType.PREVIOUS) continue;
                    this.inventory.setItem(i * 9 + j, this.items[i][j].getItem());
                }
            }
        }
        return this.inventory;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public int getRows() {
        return this.rows;
    }

    public GuiItem[][] getItems() {
        return this.items;
    }
}

