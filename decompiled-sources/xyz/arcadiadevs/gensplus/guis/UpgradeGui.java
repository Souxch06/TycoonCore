/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.milkbowl.vault.economy.Economy
 *  net.milkbowl.vault.economy.EconomyResponse
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Particle
 *  org.bukkit.block.Block
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package xyz.arcadiadevs.gensplus.guis;

import com.awaitquality.api.spigot.chat.ChatUtil;
import com.cryptomorin.xseries.XSound;
import java.util.ArrayList;
import java.util.List;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import xyz.arcadiadevs.gensplus.GensPlus;
import xyz.arcadiadevs.gensplus.models.GeneratorsData;
import xyz.arcadiadevs.gensplus.models.LocationsData;
import xyz.arcadiadevs.gensplus.utils.GuiUtil;
import xyz.arcadiadevs.gensplus.utils.ServerVersion;
import xyz.arcadiadevs.gensplus.utils.config.Config;
import xyz.arcadiadevs.gensplus.utils.config.Permissions;
import xyz.arcadiadevs.gensplus.utils.config.message.Messages;
import xyz.arcadiadevs.guilib.Gui;
import xyz.arcadiadevs.guilib.GuiItem;
import xyz.arcadiadevs.guilib.GuiItemType;

public class UpgradeGui {
    private static final GensPlus instance = GensPlus.getInstance();
    private static final Economy economy = instance.getEcon();
    private static final FileConfiguration config = instance.getConfig();

    public static void open(Player player, LocationsData.GeneratorLocation generatorLocation, Block block) {
        if (generatorLocation.getPlacedBy() != player && !player.hasPermission(Permissions.ADMIN.getPermission(new String[0])) && !player.isOp()) {
            Messages.NOT_YOUR_GENERATOR_UPGRADE.format(new Object[0]).send((CommandSender)player);
            return;
        }
        int n = Config.GUIS_UPGRADE_GUI_ROWS.getInt();
        Gui gui = new Gui(ChatUtil.translate(config.getString(Config.GUIS_UPGRADE_GUI_TITLE.getPath())), n, (Plugin)instance);
        GeneratorsData.Generator generator = generatorLocation.getGeneratorObject();
        GeneratorsData.Generator generator2 = instance.getGeneratorsData().getGenerator(generator.tier() + 1);
        if (generator2 == null) {
            Messages.REACHED_MAX_TIER.format(new Object[0]).send((CommandSender)player);
            return;
        }
        ItemStack itemStack = new ItemStack(generator2.blockType());
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatUtil.translate(config.getString(Config.GUIS_UPGRADE_GUI_UPGRADE_ONE_FIRST_LINE.getPath())));
        double d = instance.getGeneratorsData().getUpgradePrice(generator, generator2.tier());
        List list = config.getStringList(Config.GUIS_UPGRADE_GUI_UPGRADE_ONE_LORE.getPath());
        list = list.stream().map(string -> string.replace("%tier%", String.valueOf(generator.tier()))).map(string -> string.replace("%speed%", String.valueOf(generator.speed()))).map(string -> string.replace("%price%", economy.format(generator.price()))).map(string -> string.replace("%sellPrice%", economy.format(generator.sellPrice()))).map(string -> string.replace("%spawnItem%", generator.spawnItem().getType().name())).map(string -> string.replace("%blockType%", generator.blockType().getType().name())).map(string -> string.replace("%nextTier%", String.valueOf(generator2.tier()))).map(string -> string.replace("%nextSpeed%", String.valueOf(generator2.speed()))).map(string -> string.replace("%nextPrice%", economy.format(generator2.price()))).map(string -> string.replace("%nextSellPrice%", economy.format(generator2.sellPrice()))).map(string -> string.replace("%money%", economy.format(economy.getBalance((OfflinePlayer)player)))).map(ChatUtil::translate).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        itemMeta.setLore(list);
        itemStack.setItemMeta(itemMeta);
        ItemStack itemStack2 = new ItemStack(generator2.blockType());
        ItemMeta itemMeta2 = itemStack2.getItemMeta();
        itemMeta2.setDisplayName(ChatUtil.translate(config.getString(Config.GUIS_UPGRADE_GUI_UPGRADE_ALL_FIRST_LINE.getPath())));
        List list2 = config.getStringList(Config.GUIS_UPGRADE_GUI_UPGRADE_ALL_LORE.getPath());
        double d2 = d * (double)generatorLocation.getBlockLocations().size();
        list2 = list2.stream().map(string -> string.replace("%tier%", String.valueOf(generator.tier()))).map(string -> string.replace("%speed%", String.valueOf(generator.speed()))).map(string -> string.replace("%price%", economy.format(generator.price()))).map(string -> string.replace("%sellPrice%", economy.format(generator.sellPrice()))).map(string -> string.replace("%spawnItem%", generator.spawnItem().getType().name())).map(string -> string.replace("%blockType%", generator.blockType().getType().name())).map(string -> string.replace("%nextTier%", String.valueOf(generator2.tier()))).map(string -> string.replace("%nextSpeed%", String.valueOf(generator2.speed()))).map(string -> string.replace("%nextPrice%", economy.format(generator2.price()))).map(string -> string.replace("%nextSellPrice%", economy.format(generator2.sellPrice()))).map(string -> string.replace("%money%", economy.format(economy.getBalance((OfflinePlayer)player)))).map(string -> string.replace("%upgradePrice%", economy.format(d2))).map(ChatUtil::translate).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        itemMeta2.setLore(list2);
        itemStack2.setItemMeta(itemMeta2);
        List<String> list3 = economy.has((OfflinePlayer)player, d) ? List.of("ORANGE_STAINED_GLASS_PANE", "LIME_STAINED_GLASS_PANE") : List.of("ORANGE_STAINED_GLASS_PANE", "RED_STAINED_GLASS_PANE");
        GuiUtil.fillWithRandom(gui, list3);
        gui.setItem(11, new GuiItem(GuiItemType.ITEM, itemStack, () -> {
            UpgradeGui.upgradeGenerator(player, generatorLocation, block);
            player.closeInventory();
        }));
        gui.setItem(15, new GuiItem(GuiItemType.ITEM, itemStack2, () -> {
            UpgradeGui.upgradeAllGenerators(player, generatorLocation, block);
            player.closeInventory();
        }));
        player.openInventory(gui.getInventory());
    }

    private static void upgradeAllGenerators(Player player, LocationsData.GeneratorLocation generatorLocation, Block block) {
        LocationsData locationsData = instance.getLocationsData();
        GeneratorsData.Generator generator = generatorLocation.getGeneratorObject();
        GeneratorsData.Generator generator2 = instance.getGeneratorsData().getGenerator(generator.tier() + 1);
        if (generator2 == null) {
            Messages.REACHED_MAX_TIER.format(new Object[0]).send((CommandSender)player);
            return;
        }
        double d = instance.getGeneratorsData().getUpgradePrice(generator, generator2.tier()) * (double)generatorLocation.getBlockLocations().size();
        if (d > instance.getEcon().getBalance((OfflinePlayer)player)) {
            Messages.NOT_ENOUGH_MONEY.format("currentBalance", instance.getEcon().getBalance((OfflinePlayer)player), "price", instance.getEcon().format(d)).send((CommandSender)player);
            XSound.ENTITY_VILLAGER_NO.play((Entity)player);
            return;
        }
        EconomyResponse economyResponse = instance.getEcon().withdrawPlayer((OfflinePlayer)player, d);
        if (!economyResponse.transactionSuccess()) {
            player.sendMessage(ChatUtil.translate("Sorry, we were unable to process your transaction. Reason: " + economyResponse.errorMessage));
            return;
        }
        for (Block block2 : generatorLocation.getBlockLocations()) {
            block2.setType(generator2.blockType().getType());
        }
        locationsData.removeLocation(generatorLocation);
        for (Block block2 : generatorLocation.getBlockLocations()) {
            locationsData.createLocation((OfflinePlayer)player, generator2.tier(), block2);
        }
        UpgradeGui.spawnFirework(generatorLocation.getCenter());
        Messages.SUCCESSFULLY_UPGRADED.format("tier", generator2.tier()).send((CommandSender)player);
    }

    public static void upgradeGenerator(Player player, LocationsData.GeneratorLocation generatorLocation, Block block) {
        LocationsData locationsData = instance.getLocationsData();
        GeneratorsData.Generator generator = generatorLocation.getGeneratorObject();
        GeneratorsData.Generator generator2 = instance.getGeneratorsData().getGenerator(generator.tier() + 1);
        if (generator2 == null) {
            Messages.REACHED_MAX_TIER.format(new Object[0]).send((CommandSender)player);
            return;
        }
        double d = instance.getGeneratorsData().getUpgradePrice(generator, generator2.tier());
        if (d > instance.getEcon().getBalance((OfflinePlayer)player)) {
            Messages.NOT_ENOUGH_MONEY.format("currentBalance", instance.getEcon().getBalance((OfflinePlayer)player), "price", instance.getEcon().format(d)).send((CommandSender)player);
            XSound.ENTITY_VILLAGER_NO.play((Entity)player);
            return;
        }
        EconomyResponse economyResponse = instance.getEcon().withdrawPlayer((OfflinePlayer)player, d);
        if (!economyResponse.transactionSuccess()) {
            player.sendMessage(ChatUtil.translate("Sorry, we were unable to process your transaction. Reason: " + economyResponse.errorMessage));
            return;
        }
        ArrayList<Block> arrayList = new ArrayList<Block>(generatorLocation.getBlockLocations());
        arrayList.remove(block);
        locationsData.removeLocation(generatorLocation);
        for (Block block2 : arrayList) {
            locationsData.createLocation((OfflinePlayer)player, generator.tier(), block2);
        }
        block.setType(generator2.blockType().getType());
        locationsData.createLocation((OfflinePlayer)player, generator2.tier(), block);
        UpgradeGui.spawnFirework(generatorLocation.getCenter());
        Messages.SUCCESSFULLY_UPGRADED.format("tier", generator2.tier()).send((CommandSender)player);
    }

    private static void spawnFirework(Location location) {
        FileConfiguration fileConfiguration = GensPlus.getInstance().getConfig();
        XSound.matchXSound(fileConfiguration.getString(Config.PARTICLES_SOUND.getPath())).orElse(XSound.ENTITY_FIREWORK_ROCKET_BLAST).play(location);
        if (!fileConfiguration.getBoolean(Config.PARTICLES_ENABLED.getPath())) {
            return;
        }
        String string = fileConfiguration.getString(Config.PARTICLES_TYPE.getPath());
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_9)) {
            location.getWorld().spawnParticle(Particle.valueOf((String)string), location.add(0.0, -1.0, 0.0), 70);
        }
    }
}

