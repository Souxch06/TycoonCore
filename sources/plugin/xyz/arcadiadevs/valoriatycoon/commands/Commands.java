package xyz.arcadiadevs.valoriatycoon.commands;

import com.awaitquality.api.spigot.chat.ChatUtil;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;
import xyz.arcadiadevs.valoriatycoon.guis.GeneratorsGui;
import xyz.arcadiadevs.valoriatycoon.guis.ListGui;
import xyz.arcadiadevs.valoriatycoon.guis.SellGui;
import xyz.arcadiadevs.valoriatycoon.models.GeneratorsData;
import xyz.arcadiadevs.valoriatycoon.models.PlayerData;
import xyz.arcadiadevs.valoriatycoon.models.events.Event;
import xyz.arcadiadevs.valoriatycoon.tasks.EventLoop;
import xyz.arcadiadevs.valoriatycoon.utils.ItemUtil;
import xyz.arcadiadevs.valoriatycoon.utils.SellUtil;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;
import xyz.arcadiadevs.valoriatycoon.utils.config.Permissions;
import xyz.arcadiadevs.valoriatycoon.utils.config.message.Messages;

public class Commands
implements CommandExecutor {
    private final GeneratorsData generatorsData;
    private final PlayerData playerData;
    private final List<Event> events;
    private long lastReload = 0L;

    public Commands(GeneratorsData generatorsData, PlayerData playerData, List<Event> list) {
        this.generatorsData = generatorsData;
        this.playerData = playerData;
        this.events = list;
    }

    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String string, @NotNull String[] stringArray) {
        Object object;
        boolean bl = commandSender.hasPermission(Permissions.ADMIN.getPermission(new String[0]));
        if (command.getName().equalsIgnoreCase("valoriatycoon")) {
            Object object2;
            if (stringArray.length == 0) {
                Messages.DEFAULT_MESSAGE.format("version", ValoriaTycoon.getInstance().getDescription().getVersion()).send(commandSender);
                return true;
            }
            if (stringArray[0].equalsIgnoreCase("help")) {
                ChatUtil.sendMessage(commandSender, "&9Commandes ValoriaTycoon :");
                ChatUtil.sendMessage(commandSender, "&7- /valoriatycoon : affiche la version du plugin");
                ChatUtil.sendMessage(commandSender, "&7- /valoriatycoon give <joueur> <palier> [quantité] : donne un générateur à un joueur");
                ChatUtil.sendMessage(commandSender, "&7- /valoriatycoon giveall <palier> [quantité] : donne un générateur à tous les joueurs");
                ChatUtil.sendMessage(commandSender, "&7- /valoriatycoon wand sell <joueur> <utilisations> <multiplicateur> : donne une baguette de vente à un joueur");
                ChatUtil.sendMessage(commandSender, "&7- /valoriatycoon setlimit <joueur> <limite> : définit la limite de générateurs d'un joueur");
                ChatUtil.sendMessage(commandSender, "&7- /sell : ouvre l'interface de vente");
                ChatUtil.sendMessage(commandSender, "&7- /sell hand/all : vend les drops en main ou dans votre inventaire");
                ChatUtil.sendMessage(commandSender, "&7- /gen : affiche tous les générateurs");
                return true;
            }
            if (stringArray[0].equalsIgnoreCase("list")) {
                if (!bl) {
                    Messages.NO_PERMISSION.format(new Object[0]).send(commandSender);
                    return true;
                }
                Player player = (Player)commandSender;
                ListGui.open(player.getPlayer());
                return true;
            }
            if (stringArray[0].equalsIgnoreCase("setlimit")) {
                if (!bl) {
                    Messages.NO_PERMISSION.format(new Object[0]).send(commandSender);
                    return true;
                }
                if (stringArray.length < 3) {
                    Messages.NOT_ENOUGH_ARGUMENTS.format(new Object[0]).send(commandSender);
                    return true;
                }
                if (!stringArray[2].matches("\\d+")) {
                    Messages.INVALID_FORMAT.format(new Object[0]).send(commandSender);
                    return true;
                }
                Player player = Bukkit.getPlayer((String)stringArray[1]);
                if (player == null) {
                    Messages.PLAYER_NOT_FOUND.format(new Object[0]).send(commandSender);
                    return true;
                }
                PlayerData.Data data = this.playerData.getData(player.getUniqueId());
                data.setLimit(Integer.parseInt(stringArray[2]));
                Messages.LIMIT_UPDATED.format("limit", stringArray[2], "player", player.getName()).send(commandSender);
                return true;
            }
            if (stringArray[0].equalsIgnoreCase("addlimit")) {
                if (!bl) {
                    Messages.NO_PERMISSION.format(new Object[0]).send(commandSender);
                    return true;
                }
                if (stringArray.length < 3) {
                    Messages.NOT_ENOUGH_ARGUMENTS.format(new Object[0]).send(commandSender);
                    return true;
                }
                if (!stringArray[2].matches("\\d+")) {
                    Messages.INVALID_FORMAT.format(new Object[0]).send(commandSender);
                    return true;
                }
                object = Bukkit.getPlayer((String)stringArray[1]);
                if (object == null) {
                    Messages.PLAYER_NOT_FOUND.format(new Object[0]).send(commandSender);
                    return true;
                }
                object2 = this.playerData.getData(object.getUniqueId());
                PlayerData.Data.addToLimit((PlayerData.Data)object2, Integer.parseInt(stringArray[2]));
                Messages.LIMIT_UPDATED.format("limit", ((PlayerData.Data)object2).getLimit(), "player", object.getName()).send(commandSender);
            }
            if (stringArray[0].equalsIgnoreCase("wand")) {
                if (stringArray.length < 2) {
                    Messages.NOT_ENOUGH_ARGUMENTS.format(new Object[0]).send(commandSender);
                    return true;
                }
                if (stringArray[1].equalsIgnoreCase("sell")) {
                    if (!commandSender.hasPermission(Permissions.GIVE_WAND.getPermission(new String[0]))) {
                        Messages.NO_PERMISSION.format(new Object[0]).send(commandSender);
                        return true;
                    }
                    if (stringArray.length < 5) {
                        Messages.NOT_ENOUGH_ARGUMENTS.format(new Object[0]).send(commandSender);
                        return true;
                    }
                    if (!stringArray[3].matches("-?\\d+") || !stringArray[4].matches("\\d+\\.?\\d*")) {
                        Messages.INVALID_FORMAT.format(new Object[0]).send(commandSender);
                        return true;
                    }
                    object = Bukkit.getPlayer((String)stringArray[2]);
                    object.getInventory().addItem(new ItemStack[]{ItemUtil.getSellWand(Integer.parseInt(stringArray[3]), Double.parseDouble(stringArray[4]))});
                    Messages.SELL_WAND_GIVEN.format(new Object[0]).send(commandSender);
                    Messages.SELL_WAND_RECEIVED.format(new Object[0]).send((CommandSender)object);
                    return true;
                }
                return true;
            }
            if (stringArray[0].equalsIgnoreCase("startevent")) {
                if (stringArray.length < 2) {
                    Messages.NOT_ENOUGH_ARGUMENTS.format(new Object[0]).send(commandSender);
                    return true;
                }
                if (EventLoop.getActiveEvent().event() != null) {
                    Messages.EVENT_ALREADY_RUNNING.format(new Object[0]).send(commandSender);
                    return true;
                }
                object = String.join((CharSequence)" ", stringArray).substring(11);
                object2 = this.events.stream().filter(arg_0 -> Commands.lambda$0((String)object, arg_0)).findFirst().orElse(null);
                if (object2 == null) {
                    Messages.EVENT_NOT_FOUND.format(new Object[0]).send(commandSender);
                    return true;
                }
                EventLoop.setNextEvent((Event)object2);
            }
            if (stringArray[0].equalsIgnoreCase("stopevent")) {
                EventLoop.stopEvent();
            }
            if (stringArray[0].equalsIgnoreCase("reload")) {
                long l;
                if (!bl && !commandSender.hasPermission(Permissions.GENERATOR_RELOAD.getPermission(new String[0]))) {
                    Messages.NO_PERMISSION.format(new Object[0]).send(commandSender);
                    return true;
                }
                long l2 = System.currentTimeMillis();
                if (l2 - this.lastReload < (l = 5000L)) {
                    Messages.TOO_FAST.format(new Object[0]).send(commandSender);
                    return true;
                }
                ValoriaTycoon.getInstance().reloadPlugin();
                Messages.PLUGIN_RELOADED.format(new Object[0]).send(commandSender);
                this.lastReload = l2;
                return true;
            }
            if (stringArray[0].equalsIgnoreCase("give")) {
                GeneratorsData.Generator generator;
                int n;
                if (!bl && !commandSender.hasPermission(Permissions.GENERATOR_GIVE.getPermission(new String[0]))) {
                    Messages.NO_PERMISSION.format(new Object[0]).send(commandSender);
                    return true;
                }
                if (stringArray.length < 3) {
                    Messages.NOT_ENOUGH_ARGUMENTS.format(new Object[0]).send(commandSender);
                    return true;
                }
                object = Bukkit.getPlayer((String)stringArray[1]);
                if (object == null) {
                    Messages.PLAYER_NOT_FOUND.format(new Object[0]).send(commandSender);
                    return true;
                }
                try {
                    n = Integer.parseInt(stringArray[2]);
                }
                catch (NumberFormatException numberFormatException) {
                    Messages.INVALID_GENERATOR_TIER.format(new Object[0]).send(commandSender);
                    return true;
                }
                int n2 = 1;
                if (stringArray.length >= 4) {
                    try {
                        n2 = Integer.parseInt(stringArray[3]);
                    }
                    catch (NumberFormatException numberFormatException) {
                        Messages.INVALID_AMOUNT.format(new Object[0]).send(commandSender);
                        return true;
                    }
                }
                if ((generator = this.generatorsData.getGenerator(n)) == null) {
                    Messages.INVALID_GENERATOR_TIER.format(new Object[0]).send(commandSender);
                    return true;
                }
                int n3 = 0;
                while (n3 < n2) {
                    generator.giveItem((Player)object);
                    ++n3;
                }
                Messages.GENERATOR_GIVEN.format("targetPlayer", object.getName(), "tier", String.valueOf(n), "amount", String.valueOf(n2)).send(commandSender);
                Messages.GENERATOR_RECEIVED.format("tier", String.valueOf(n), "amount", String.valueOf(n2)).send((CommandSender)object);
                return true;
            }
            if (stringArray[0].equalsIgnoreCase("giveall")) {
                GeneratorsData.Generator generator;
                int n;
                if (!bl && !commandSender.hasPermission(Permissions.GENERATOR_GIVE_ALL.getPermission(new String[0]))) {
                    Messages.NO_PERMISSION.format(new Object[0]).send(commandSender);
                    return true;
                }
                if (stringArray.length < 2) {
                    Messages.NOT_ENOUGH_ARGUMENTS.format(new Object[0]).send(commandSender);
                    return true;
                }
                try {
                    n = Integer.parseInt(stringArray[1]);
                }
                catch (NumberFormatException numberFormatException) {
                    Messages.INVALID_GENERATOR_TIER.format(new Object[0]).send(commandSender);
                    return true;
                }
                int n4 = 1;
                if (stringArray.length >= 3) {
                    try {
                        n4 = Integer.parseInt(stringArray[2]);
                    }
                    catch (NumberFormatException numberFormatException) {
                        Messages.INVALID_AMOUNT.format(new Object[0]).send(commandSender);
                        return true;
                    }
                }
                if ((generator = this.generatorsData.getGenerator(n)) == null) {
                    Messages.INVALID_GENERATOR_TIER.format(new Object[0]).send(commandSender);
                    return true;
                }
                int n5 = 0;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    int n6 = 0;
                    while (n6 < n4) {
                        generator.giveItem(player);
                        ++n6;
                    }
                    ++n5;
                }
                Messages.GENERATOR_GIVEN_ALL.format("tier", String.valueOf(n), "amount", String.valueOf(n4), "count", String.valueOf(n5)).send(commandSender);
                return true;
            }
        }
        if (command.getName().equalsIgnoreCase("generators") || string.equalsIgnoreCase("gen")) {
            if (!(commandSender instanceof Player)) {
                Messages.ONLY_PLAYER_CAN_EXECUTE_COMMAND.format(new Object[0]).send(commandSender);
                return true;
            }
            object = (Player)commandSender;
            if (!bl && !commandSender.hasPermission(Permissions.GENERATORS_GUI.getPermission(new String[0]))) {
                Messages.NO_PERMISSION.format(new Object[0]).send(commandSender);
                return true;
            }
            GeneratorsGui.open(object);
            return true;
        }
        if (command.getName().equalsIgnoreCase("selldrops") || string.equalsIgnoreCase("sell")) {
            if (!(commandSender instanceof Player)) {
                Messages.ONLY_PLAYER_CAN_EXECUTE_COMMAND.format(new Object[0]).send(commandSender);
                return true;
            }
            object = (Player)commandSender;
            if (!ValoriaTycoon.getInstance().getConfig().getBoolean(Config.SELL_COMMAND_ENABLED.getPath())) {
                return true;
            }
            if (stringArray.length == 0) {
                if (string.equalsIgnoreCase("sell")) {
                    if (!commandSender.hasPermission(Permissions.GENERATOR_DROPS_SELL_GUI.getPermission(new String[0]))) {
                        Messages.NO_PERMISSION.format(new Object[0]).send(commandSender);
                        return true;
                    }
                    SellGui.open(object);
                    return true;
                }
                Messages.NOT_ENOUGH_ARGUMENTS.format(new Object[0]).send(commandSender);
                return true;
            }
            if (stringArray[0].equalsIgnoreCase("all")) {
                if (!commandSender.hasPermission(Permissions.GENERATOR_DROPS_SELL_ALL.getPermission(new String[0]))) {
                    Messages.NO_PERMISSION.format(new Object[0]).send(commandSender);
                    return true;
                }
                SellUtil.sellAll(object, (Inventory)object.getInventory(), new boolean[0]);
                return true;
            }
            if (stringArray[0].equalsIgnoreCase("hand")) {
                if (!commandSender.hasPermission(Permissions.GENERATOR_DROPS_SELL_HAND.getPermission(new String[0]))) {
                    Messages.NO_PERMISSION.format(new Object[0]).send(commandSender);
                    return true;
                }
                SellUtil.sellHand(object);
                return true;
            }
            if (stringArray[0].equalsIgnoreCase("gui")) {
                if (!commandSender.hasPermission(Permissions.GENERATOR_DROPS_SELL_GUI.getPermission(new String[0]))) {
                    Messages.NO_PERMISSION.format(new Object[0]).send(commandSender);
                    return true;
                }
                SellGui.open(object);
                return true;
            }
        }
        return true;
    }

    private static /* synthetic */ boolean lambda$0(String string, Event event) {
        return event.getName().equalsIgnoreCase(string);
    }
}

