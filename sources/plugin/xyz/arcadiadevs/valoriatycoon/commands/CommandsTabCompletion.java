package xyz.arcadiadevs.valoriatycoon.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Generated;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.arcadiadevs.valoriatycoon.models.GeneratorsData;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;
import xyz.arcadiadevs.valoriatycoon.utils.config.Permissions;

public class CommandsTabCompletion
implements TabCompleter {
    private final GeneratorsData generatorsData;

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String string2, @NotNull String[] stringArray) {
        boolean bl = commandSender.hasPermission(Permissions.ADMIN.getPermission(new String[0]));
        if (command.getName().equalsIgnoreCase("valoriatycoon") || command.getName().equalsIgnoreCase("gens") || command.getName().equalsIgnoreCase("gp")) {
            if (stringArray.length == 1) {
                if (!bl && !commandSender.hasPermission(Permissions.ADMIN.getPermission(new String[0]))) {
                    return null;
                }
                return List.of("help", "list", "give", "giveall", "wand", "setlimit", "addlimit", "startevent", "stopevent", "reload");
            }
            if (stringArray[0].equalsIgnoreCase("reload") && !bl && !commandSender.hasPermission(Permissions.GENERATOR_RELOAD.getPermission(new String[0]))) {
                return null;
            }
            if (stringArray[0].equalsIgnoreCase("setlimit")) {
                if (!bl && !commandSender.hasPermission(Permissions.SET_LIMIT.getPermission(new String[0]))) {
                    return null;
                }
                if (stringArray.length == 2) {
                    ArrayList<String> arrayList = new ArrayList<String>();
                    for (Player player : commandSender.getServer().getOnlinePlayers()) {
                        arrayList.add(player.getName());
                    }
                    return arrayList;
                }
                if (stringArray.length == 3) {
                    return List.of("<limite>");
                }
            }
            if (stringArray[0].equalsIgnoreCase("addlimit")) {
                if (!bl && !commandSender.hasPermission(Permissions.ADD_LIMIT.getPermission(new String[0]))) {
                    return null;
                }
                if (stringArray.length == 2) {
                    ArrayList<String> arrayList = new ArrayList<String>();
                    for (Player player : commandSender.getServer().getOnlinePlayers()) {
                        arrayList.add(player.getName());
                    }
                    return arrayList;
                }
                if (stringArray.length == 3) {
                    return List.of("<limite>");
                }
            }
            if (stringArray[0].equalsIgnoreCase("startevent")) {
                if (!bl && !commandSender.hasPermission(Permissions.START_EVENT.getPermission(new String[0]))) {
                    return null;
                }
                if (stringArray.length == 2) {
                    ArrayList<String> arrayList = new ArrayList<String>();
                    if (Config.EVENTS_SPEED_EVENT_ENABLED.getBoolean()) {
                        arrayList.add(Config.EVENTS_SPEED_EVENT_NAME.getString());
                    }
                    if (Config.EVENTS_SELL_EVENT_ENABLED.getBoolean()) {
                        arrayList.add(Config.EVENTS_SELL_EVENT_NAME.getString());
                    }
                    if (Config.EVENTS_DROP_EVENT_ENABLED.getBoolean()) {
                        arrayList.add(Config.EVENTS_DROP_EVENT_NAME.getString());
                    }
                    return arrayList;
                }
            }
            if (stringArray[0].equalsIgnoreCase("stopevent") && !bl && !commandSender.hasPermission(Permissions.STOP_EVENT.getPermission(new String[0]))) {
                return null;
            }
            if (Arrays.stream(stringArray).anyMatch(string -> string.equalsIgnoreCase("give"))) {
                if (!bl && !commandSender.hasPermission(Permissions.GENERATOR_GIVE.getPermission(new String[0]))) {
                    return null;
                }
                if (stringArray.length == 2) {
                    ArrayList<String> arrayList = new ArrayList<String>();
                    for (Player player : commandSender.getServer().getOnlinePlayers()) {
                        arrayList.add(player.getName());
                    }
                    return arrayList;
                }
                if (stringArray.length == 3) {
                    List<Integer> list = this.generatorsData.getGenerators().stream().map(GeneratorsData.Generator::tier).toList();
                    return list.stream().map(String::valueOf).toList();
                }
                if (stringArray.length == 4) {
                    return List.of("[quantité]");
                }
            }
            if (Arrays.stream(stringArray).anyMatch(string -> string.equalsIgnoreCase("giveall"))) {
                if (!bl && !commandSender.hasPermission(Permissions.GENERATOR_GIVE_ALL.getPermission(new String[0]))) {
                    return null;
                }
                if (stringArray.length == 2) {
                    List<Integer> list = this.generatorsData.getGenerators().stream().map(GeneratorsData.Generator::tier).toList();
                    return list.stream().map(String::valueOf).toList();
                }
                if (stringArray.length == 3) {
                    return List.of("[quantité]");
                }
            }
            if (Arrays.stream(stringArray).anyMatch(string -> string.equalsIgnoreCase("wand"))) {
                if (!bl && !commandSender.hasPermission(Permissions.GIVE_WAND.getPermission(new String[0]))) {
                    return null;
                }
                if (stringArray.length == 2) {
                    return List.of("sell");
                }
                if (stringArray.length == 3) {
                    ArrayList<String> arrayList = new ArrayList<String>();
                    for (Player player : commandSender.getServer().getOnlinePlayers()) {
                        arrayList.add(player.getName());
                    }
                    return arrayList;
                }
                if (stringArray.length == 4) {
                    return List.of("<utilisations>", "-1");
                }
                if (stringArray.length == 5) {
                    return List.of("<multiplicateur>");
                }
            }
            return null;
        }
        if (command.getName().equalsIgnoreCase("selldrops") || string2.equalsIgnoreCase("sell")) {
            if (!Config.SELL_COMMAND_ENABLED.getBoolean()) {
                return null;
            }
            if (!(commandSender.hasPermission(Permissions.GENERATOR_DROPS_SELL_ALL.getPermission(new String[0])) && commandSender.hasPermission(Permissions.GENERATOR_DROPS_SELL_HAND.getPermission(new String[0])) && commandSender.hasPermission(Permissions.GENERATOR_DROPS_SELL_GUI.getPermission(new String[0])))) {
                return null;
            }
            if (stringArray.length == 1) {
                if (string2.equalsIgnoreCase("sell")) {
                    return List.of("hand", "all");
                }
                return List.of("hand", "all", "gui");
            }
            return null;
        }
        return null;
    }

    @Generated
    public CommandsTabCompletion(GeneratorsData generatorsData) {
        this.generatorsData = generatorsData;
    }
}

