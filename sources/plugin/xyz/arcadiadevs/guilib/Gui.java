/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryAction
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.plugin.Plugin
 */
package xyz.arcadiadevs.guilib;

import java.util.ArrayList;
import java.util.Arrays;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import xyz.arcadiadevs.guilib.GuiItem;
import xyz.arcadiadevs.guilib.GuiItemType;
import xyz.arcadiadevs.guilib.GuiPage;
import xyz.arcadiadevs.guilib.GuiPageType;

public class Gui
implements Listener {
    private final String title;
    private final int rows;
    private final ArrayList<GuiPage> pages;

    public Gui(String string, int n, Plugin plugin) {
        this.title = string;
        this.rows = n;
        this.pages = new ArrayList();
        this.addPage();
        plugin.getServer().getPluginManager().registerEvents((Listener)this, plugin);
    }

    public void addItem(GuiItem guiItem) {
        int n;
        for (GuiPage guiPage : this.pages) {
            for (n = 0; n < guiPage.getItems().length; ++n) {
                for (int i = 0; i < guiPage.getItems()[n].length; ++i) {
                    if (guiPage.getItems()[n][i] != null) continue;
                    guiPage.getItems()[n][i] = guiItem;
                    return;
                }
            }
        }
        GuiPage guiPage = this.addPage();
        for (int i = 0; i < guiPage.getItems().length; ++i) {
            for (n = 0; n < guiPage.getItems()[i].length; ++n) {
                if (guiPage.getItems()[i][n] != null) continue;
                guiPage.getItems()[i][n] = guiItem;
                return;
            }
        }
    }

    public void setItem(int n, GuiItem guiItem) {
        int n2 = n / (this.rows * 9);
        int n3 = (n - n2 * this.rows * 9) / 9;
        int n4 = n - n2 * this.rows * 9 - n3 * 9;
        if (guiItem.getType() != GuiItemType.ITEM && n2 != 0) {
            throw new IllegalArgumentException("Seuls les items peuvent être placés sur les pages autres que la première.");
        }
        this.pages.get((int)n2).getItems()[n3][n4] = guiItem;
    }

    public GuiPage addPage() {
        GuiPage guiPage = new GuiPage(this.rows, this.title);
        if (this.pages.size() > 0) {
            GuiPage guiPage2 = this.pages.get(0);
            for (int i = 0; i < guiPage2.getItems().length; ++i) {
                for (int j = 0; j < guiPage2.getItems()[i].length; ++j) {
                    GuiItem guiItem = guiPage2.getItems()[i][j];
                    if (guiItem.getType() == GuiItemType.ITEM) continue;
                    guiPage.getItems()[i][j] = guiItem;
                }
            }
        }
        this.pages.add(guiPage);
        return guiPage;
    }

    public Inventory getInventory() {
        return this.getInventory(0);
    }

    public Inventory getInventory(int n) {
        return this.pages.get(n).getInventory(this.pages.size() == 1 ? GuiPageType.SINGLE : (n == this.pages.size() - 1 ? GuiPageType.LAST : (n == 0 ? GuiPageType.FIRST : GuiPageType.NORMAL)));
    }

    private GuiItem[] getItems() {
        return (GuiItem[])this.pages.stream().flatMap(guiPage -> Arrays.stream(guiPage.getItems())).flatMap(Arrays::stream).toArray(GuiItem[]::new);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent inventoryClickEvent) {
        if (inventoryClickEvent.getClickedInventory() == null) {
            return;
        }
        if (inventoryClickEvent.getCurrentItem() == null) {
            return;
        }
        if (!(inventoryClickEvent.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)inventoryClickEvent.getWhoClicked();
        Inventory inventory = inventoryClickEvent.getClickedInventory();
        GuiPage guiPage2 = this.pages.stream().filter(guiPage -> guiPage.getInventory() != null && guiPage.getInventory().equals(inventory)).findFirst().orElse(null);
        if (guiPage2 == null) {
            return;
        }
        GuiItem guiItem = guiPage2.getItems()[inventoryClickEvent.getSlot() / 9][inventoryClickEvent.getSlot() % 9];
        if (guiItem == null) {
            return;
        }
        inventoryClickEvent.setCancelled(true);
        if (inventoryClickEvent.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return;
        }
        switch (guiItem.getType()) {
            case ITEM: 
            case BORDER: {
                if (guiItem.getAction() == null) {
                    return;
                }
                guiItem.getAction().run();
                break;
            }
            case NEXT: {
                int n = this.pages.indexOf(guiPage2) + 1;
                if (n >= this.pages.size()) {
                    return;
                }
                player.openInventory(this.getInventory(n));
                break;
            }
            case PREVIOUS: {
                int n = this.pages.indexOf(guiPage2) - 1;
                if (n < 0) {
                    return;
                }
                player.openInventory(this.getInventory(n));
                break;
            }
            case CLOSE: {
                player.closeInventory();
                break;
            }
            default: {
                throw new IllegalStateException("Valeur inattendue : " + (Object)((Object)guiItem.getType()));
            }
        }
    }

    public int getRows() {
        return this.rows;
    }
}

