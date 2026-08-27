/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  net.milkbowl.vault.economy.Economy
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package xyz.arcadiadevs.gensplus.guis;

import com.awaitquality.api.spigot.chat.ChatUtil;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import xyz.arcadiadevs.gensplus.GensPlus;
import xyz.arcadiadevs.gensplus.models.GeneratorsData;
import xyz.arcadiadevs.gensplus.utils.GuiUtil;
import xyz.arcadiadevs.gensplus.utils.config.Config;
import xyz.arcadiadevs.gensplus.utils.config.message.Messages;
import xyz.arcadiadevs.guilib.Gui;
import xyz.arcadiadevs.guilib.GuiItem;
import xyz.arcadiadevs.guilib.GuiItemType;
import xyz.arcadiadevs.guilib.ItemBuilder;

public class GeneratorsGui {
    public static void open(Player player) {
        GensPlus gensPlus = GensPlus.getInstance();
        FileConfiguration fileConfiguration = gensPlus.getConfig();
        Economy economy = gensPlus.getEcon();
        if (!fileConfiguration.getBoolean(Config.GUIS_GENERATORS_GUI_ENABLED.getPath())) {
            return;
        }
        int n = fileConfiguration.getInt(Config.GUIS_GENERATORS_GUI_ROWS.getPath());
        Gui gui = new Gui(ChatUtil.translate(fileConfiguration.getString(Config.GUIS_GENERATORS_GUI_TITLE.getPath())), n, (Plugin)gensPlus);
        GeneratorsData generatorsData = gensPlus.getGeneratorsData();
        List list = fileConfiguration.getMapList(Config.GENERATORS.getPath());
        for (GeneratorsData.Generator generator : generatorsData.getGenerators()) {
            ItemStack itemStack = new ItemStack(generator.blockType());
            Map map2 = list.stream().filter(map -> map.get("name").equals(generator.name())).findFirst().orElse(null);
            if (map2 == null) continue;
            List list2 = ((List)map2.get("lore")).isEmpty() ? fileConfiguration.getStringList("default-lore") : (List)map2.get("lore");
            list2 = list2.stream().map(string -> string.replace("%tier%", String.valueOf(generator.tier()))).map(string -> string.replace("%speed%", String.valueOf(generator.speed()))).map(string -> string.replace("%price%", economy.format(generator.price()))).map(string -> string.replace("%sellPrice%", economy.format(generator.sellPrice()))).map(string -> string.replace("%spawnItem%", generator.spawnItem().getType().name())).map(string -> string.replace("%blockType%", generator.blockType().getType().name())).map(ChatUtil::translate).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            ItemStack itemStack2 = new ItemBuilder(itemStack).name(ChatUtil.translate(generator.name())).lore(list2).build();
            if (Config.GUIS_GENERATORS_GUI_BORDER_ENABLED.getBoolean()) {
                GuiUtil.addBorder(gui, Config.GUIS_GENERATORS_GUI_BORDER_MATERIAL.getString());
            }
            Material material = XMaterial.matchXMaterial(Config.GUIS_GENERATORS_GUI_NEXT_PAGE_MATERIAL.getString()).orElse(XMaterial.ARROW).parseMaterial();
            Material material2 = XMaterial.matchXMaterial(Config.GUIS_GENERATORS_GUI_PREVIOUS_PAGE_MATERIAL.getString()).orElse(XMaterial.ARROW).parseMaterial();
            Material material3 = XMaterial.matchXMaterial(Config.GUIS_GENERATORS_GUI_CLOSE_BUTTON_MATERIAL.getString()).orElse(XMaterial.BARRIER).parseMaterial();
            ItemStack itemStack3 = new ItemBuilder(material).name(ChatUtil.translate("&aPage suivante")).build();
            ItemStack itemStack4 = new ItemBuilder(material2).name(ChatUtil.translate("&aPage précédente")).build();
            ItemStack itemStack5 = new ItemBuilder(material3).name(ChatUtil.translate("&cFermer")).build();
            gui.setItem((n - 1) * 9 + 3, new GuiItem(GuiItemType.PREVIOUS, itemStack4, null));
            gui.setItem((n - 1) * 9 + 4, new GuiItem(GuiItemType.CLOSE, itemStack5, null));
            gui.setItem((n - 1) * 9 + 5, new GuiItem(GuiItemType.NEXT, itemStack3, null));
            String string2 = economy.format(economy.getBalance((OfflinePlayer)player));
            gui.addItem(new GuiItem(GuiItemType.ITEM, itemStack2, () -> {
                if (generator.price() > economy.getBalance((OfflinePlayer)player)) {
                    Messages.NOT_ENOUGH_MONEY.format("currentBalance", string2, "price", economy.currencyNameSingular() + generator.price()).send((CommandSender)player);
                    XSound.ENTITY_VILLAGER_NO.play((Entity)player);
                    return;
                }
                generator.giveItem(player);
                economy.withdrawPlayer((OfflinePlayer)player, generator.price());
                Messages.SUCCESSFULLY_BOUGHT.format("generator", generator.name(), "tier", generator.tier(), "price", generator.price()).send((CommandSender)player);
                XSound.ENTITY_PLAYER_LEVELUP.play((Entity)player);
            }));
        }
        player.openInventory(gui.getInventory());
    }
}

