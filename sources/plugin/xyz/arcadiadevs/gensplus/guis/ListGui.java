/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package xyz.arcadiadevs.gensplus.guis;

import com.awaitquality.api.spigot.chat.ChatUtil;
import com.cryptomorin.xseries.XMaterial;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import xyz.arcadiadevs.gensplus.GensPlus;
import xyz.arcadiadevs.gensplus.models.LocationsData;
import xyz.arcadiadevs.gensplus.models.PlayerData;
import xyz.arcadiadevs.gensplus.utils.LimitUtil;
import xyz.arcadiadevs.gensplus.utils.config.Config;
import xyz.arcadiadevs.guilib.Gui;
import xyz.arcadiadevs.guilib.GuiItem;
import xyz.arcadiadevs.guilib.GuiItemType;
import xyz.arcadiadevs.guilib.ItemBuilder;

public class ListGui {
    public static void open(Player player) {
        GensPlus gensPlus = GensPlus.getInstance();
        Gui gui = new Gui(ChatUtil.translate("Liste des joueurs"), 6, (Plugin)gensPlus);
        Material material = XMaterial.matchXMaterial(Config.GUIS_GENERATORS_GUI_NEXT_PAGE_MATERIAL.getString()).orElse(XMaterial.ARROW).parseMaterial();
        Material material2 = XMaterial.matchXMaterial(Config.GUIS_GENERATORS_GUI_PREVIOUS_PAGE_MATERIAL.getString()).orElse(XMaterial.ARROW).parseMaterial();
        Material material3 = XMaterial.matchXMaterial(Config.GUIS_GENERATORS_GUI_CLOSE_BUTTON_MATERIAL.getString()).orElse(XMaterial.BARRIER).parseMaterial();
        ItemStack itemStack = new ItemBuilder(material).name(ChatUtil.translate("&aPage suivante")).build();
        ItemStack itemStack2 = new ItemBuilder(material2).name(ChatUtil.translate("&aPage précédente")).build();
        ItemStack itemStack3 = new ItemBuilder(material3).name(ChatUtil.translate("&cFermer")).build();
        gui.setItem(48, new GuiItem(GuiItemType.PREVIOUS, itemStack2, null));
        gui.setItem(49, new GuiItem(GuiItemType.CLOSE, itemStack3, null));
        gui.setItem(50, new GuiItem(GuiItemType.NEXT, itemStack, null));
        PlayerData playerData = gensPlus.getPlayerData();
        for (OfflinePlayer offlinePlayer : gensPlus.getServer().getOnlinePlayers()) {
            int n = LimitUtil.calculateCombinedLimit(offlinePlayer, playerData);
            ArrayList<String> arrayList = new ArrayList<String>();
            arrayList.add(ChatUtil.translate("&7Placés : &e" + String.valueOf(gensPlus.getLocationsData().getGeneratorsCountByPlayer(offlinePlayer.getPlayer()))));
            arrayList.add(ChatUtil.translate("&7Limite : &e" + n));
            arrayList.add(ChatUtil.translate("&7Cliquez pour voir les générateurs"));
            ItemStack itemStack4 = new ItemBuilder(XMaterial.PLAYER_HEAD.parseMaterial()).name(ChatUtil.translate(offlinePlayer.getName())).lore(arrayList).skullOwner(offlinePlayer.getName()).build();
            gui.addItem(new GuiItem(GuiItemType.ITEM, itemStack4, () -> ListGui.generatorListForPlayer(player, (OfflinePlayer)offlinePlayer.getPlayer())));
        }
        player.getPlayer().openInventory(gui.getInventory());
    }

    private static void generatorListForPlayer(Player player, OfflinePlayer offlinePlayer) {
        GensPlus gensPlus = GensPlus.getInstance();
        Gui gui = new Gui(ChatUtil.translate("Liste des générateurs de " + offlinePlayer.getName()), 6, (Plugin)gensPlus);
        Material material = XMaterial.matchXMaterial(Config.GUIS_GENERATORS_GUI_NEXT_PAGE_MATERIAL.getString()).orElse(XMaterial.ARROW).parseMaterial();
        Material material2 = XMaterial.matchXMaterial(Config.GUIS_GENERATORS_GUI_PREVIOUS_PAGE_MATERIAL.getString()).orElse(XMaterial.ARROW).parseMaterial();
        Material material3 = XMaterial.matchXMaterial(Config.GUIS_GENERATORS_GUI_CLOSE_BUTTON_MATERIAL.getString()).orElse(XMaterial.BARRIER).parseMaterial();
        ItemStack itemStack = new ItemBuilder(material).name(ChatUtil.translate("&aPage suivante")).build();
        ItemStack itemStack2 = new ItemBuilder(material2).name(ChatUtil.translate("&aPage précédente")).build();
        ItemStack itemStack3 = new ItemBuilder(material3).name(ChatUtil.translate("&cFermer")).build();
        gui.setItem(48, new GuiItem(GuiItemType.PREVIOUS, itemStack2, null));
        gui.setItem(49, new GuiItem(GuiItemType.CLOSE, itemStack3, null));
        gui.setItem(50, new GuiItem(GuiItemType.NEXT, itemStack, null));
        gensPlus.getLocationsData().locations().stream().filter(generatorLocation -> generatorLocation.getPlacedBy().equals(offlinePlayer)).forEach(generatorLocation -> {
            ItemStack itemStack = new ItemStack(generatorLocation.getGeneratorObject().blockType());
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName(ChatUtil.translate(generatorLocation.getCenter().getBlockX() + ", " + generatorLocation.getCenter().getBlockY() + ", " + generatorLocation.getCenter().getBlockZ()));
            itemStack.setItemMeta(itemMeta);
            gui.addItem(new GuiItem(GuiItemType.ITEM, itemStack, () -> ListGui.openInside((OfflinePlayer)player.getPlayer(), generatorLocation)));
        });
        player.getPlayer().openInventory(gui.getInventory());
    }

    private static void openInside(OfflinePlayer offlinePlayer, LocationsData.GeneratorLocation generatorLocation) {
        GensPlus gensPlus = GensPlus.getInstance();
        Gui gui = new Gui(ChatUtil.translate("Liste des générateurs de " + offlinePlayer.getName()), 6, (Plugin)gensPlus);
        generatorLocation.getBlockLocations().forEach(block -> {
            ItemStack itemStack = new ItemStack(generatorLocation.getGeneratorObject().blockType());
            Location location = block.getLocation();
            itemStack.setItemMeta(null);
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName(ChatUtil.translate("&7Cliquez pour choisir une option"));
            itemMeta.setLore(List.of(ChatUtil.translate("&7Nom : " + generatorLocation.getGeneratorObject().name()), ChatUtil.translate("&7Palier : " + generatorLocation.getGeneratorObject().tier()), ChatUtil.translate("&7X: " + location.getBlockX()), ChatUtil.translate("&7Y: " + location.getBlockY()), ChatUtil.translate("&7Z: " + location.getBlockZ())));
            itemStack.setItemMeta(itemMeta);
            gui.addItem(new GuiItem(GuiItemType.ITEM, itemStack, () -> ListGui.chooseOption((OfflinePlayer)offlinePlayer.getPlayer(), block.getLocation())));
        });
        offlinePlayer.getPlayer().openInventory(gui.getInventory());
    }

    private static void chooseOption(OfflinePlayer offlinePlayer, Location location) {
        GensPlus gensPlus = GensPlus.getInstance();
        Gui gui = new Gui(ChatUtil.translate("Choisir une option"), 3, (Plugin)gensPlus);
        ItemStack itemStack = new ItemStack(XMaterial.ENDER_PEARL.parseItem());
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatUtil.translate("Se téléporter au générateur"));
        itemStack.setItemMeta(itemMeta);
        gui.addItem(new GuiItem(GuiItemType.ITEM, itemStack, () -> offlinePlayer.getPlayer().teleport(location.add(0.5, 1.0, 0.5))));
        ItemStack itemStack2 = new ItemStack(XMaterial.TNT.parseItem());
        itemMeta = itemStack2.getItemMeta();
        itemMeta.setDisplayName(ChatUtil.translate("Supprimer le générateur"));
        itemStack2.setItemMeta(itemMeta);
        gui.addItem(new GuiItem(GuiItemType.ITEM, itemStack2, () -> {
            Block block2 = location.getBlock();
            LocationsData.GeneratorLocation generatorLocation = gensPlus.getLocationsData().getGeneratorLocation(block2);
            if (generatorLocation == null) {
                return;
            }
            int n = generatorLocation.getGenerator();
            ArrayList<Block> arrayList = generatorLocation.getBlockLocations();
            arrayList.remove(block2);
            gensPlus.getLocationsData().removeLocation(generatorLocation);
            arrayList.forEach(block -> {
                LocationsData.GeneratorLocation generatorLocation = gensPlus.getLocationsData().getGeneratorLocation((Block)block);
                if (generatorLocation != null) {
                    return;
                }
                gensPlus.getLocationsData().createLocation(offlinePlayer, n, (Block)block);
            });
            location.getBlock().setType(XMaterial.AIR.parseMaterial());
            offlinePlayer.getPlayer().sendMessage(ChatUtil.translate("&aGénérateur supprimé !"));
            offlinePlayer.getPlayer().closeInventory();
        }));
        offlinePlayer.getPlayer().openInventory(gui.getInventory());
    }
}

