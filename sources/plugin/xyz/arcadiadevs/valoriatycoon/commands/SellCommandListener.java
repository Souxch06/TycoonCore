package xyz.arcadiadevs.valoriatycoon.commands;

import java.util.ArrayList;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import xyz.arcadiadevs.valoriatycoon.guis.SellGui;
import xyz.arcadiadevs.valoriatycoon.utils.SellUtil;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;
import xyz.arcadiadevs.valoriatycoon.utils.config.Permissions;
import xyz.arcadiadevs.valoriatycoon.utils.config.message.Messages;

public class SellCommandListener
implements Listener {
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent playerCommandPreprocessEvent) {
        String string;
        Player player = playerCommandPreprocessEvent.getPlayer();
        String[] stringArray = playerCommandPreprocessEvent.getMessage().split(" ");
        stringArray[0] = stringArray[0].toLowerCase();
        if (!Config.SELL_COMMAND_ENABLED.getBoolean()) {
            return;
        }
        ArrayList<String> arrayList = Config.SELL_COMMAND_ALLIASES.getStringList();
        if (!arrayList.contains(string = stringArray[0].replace("/", ""))) {
            return;
        }
        if (stringArray.length < 2) {
            if (string.equalsIgnoreCase("sell")) {
                if (!player.hasPermission(Permissions.GENERATOR_DROPS_SELL_GUI.getPermission(new String[0]))) {
                    Messages.NO_PERMISSION.format(new Object[0]).send((CommandSender)player);
                    playerCommandPreprocessEvent.setCancelled(true);
                    return;
                }
                SellGui.open(player);
                playerCommandPreprocessEvent.setCancelled(true);
                return;
            }
            Messages.NOT_ENOUGH_ARGUMENTS.format(new Object[0]).send((CommandSender)player);
            playerCommandPreprocessEvent.setCancelled(true);
            return;
        }
        if (stringArray[1].equalsIgnoreCase("all")) {
            if (!player.hasPermission(Permissions.GENERATOR_DROPS_SELL_ALL.getPermission(new String[0]))) {
                Messages.NO_PERMISSION.format(new Object[0]).send((CommandSender)player);
                return;
            }
            SellUtil.sellAll(player, (Inventory)player.getInventory(), new boolean[0]);
            playerCommandPreprocessEvent.setCancelled(true);
            return;
        }
        if (stringArray[1].equalsIgnoreCase("hand")) {
            if (!player.hasPermission(Permissions.GENERATOR_DROPS_SELL_HAND.getPermission(new String[0]))) {
                Messages.NO_PERMISSION.format(new Object[0]).send((CommandSender)player);
                return;
            }
            SellUtil.sellHand(player);
            playerCommandPreprocessEvent.setCancelled(true);
            return;
        }
        if (stringArray[1].equalsIgnoreCase("gui")) {
            if (!player.hasPermission(Permissions.GENERATOR_DROPS_SELL_GUI.getPermission(new String[0]))) {
                Messages.NO_PERMISSION.format(new Object[0]).send((CommandSender)player);
                return;
            }
            SellGui.open(player);
            playerCommandPreprocessEvent.setCancelled(true);
        }
    }
}

