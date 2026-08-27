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
        if (CommandsTabCompletion.matches(command.getName(), "valoriatycoon", "vt", "vtc", "valoria", "tycoon") || CommandsTabCompletion.matches(string2, "valoriatycoon", "vt", "vtc", "valoria", "tycoon")) {
            if (stringArray.length == 1) {
                if (!bl && !commandSender.hasPermission(Permissions.ADMIN.getPermission(new String[0]))) {
                    return null;
                }
                return List.of("help", "h", "list", "l", "give", "g", "giveall", "ga", "wand", "w", "setlimit", "sl", "addlimit", "al", "startevent", "se", "stopevent", "ee", "reload", "rl");
            }
            if (CommandsTabCompletion.matches(stringArray[0], "reload", "rl", "r") && !bl && !commandSender.hasPermission(Permissions.GENERATOR_RELOAD.getPermission(new String[0]))) {
                return null;
            }
            if (CommandsTabCompletion.matches(stringArray[0], "setlimit", "sl")) {
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
            if (CommandsTabCompletion.matches(stringArray[0], "addlimit", "al")) {
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
            if (CommandsTabCompletion.matches(stringArray[0], "startevent", "se", "start")) {
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
            if (CommandsTabCompletion.matches(stringArray[0], "stopevent", "ee", "stop") && !bl && !commandSender.hasPermission(Permissions.STOP_EVENT.getPermission(new String[0]))) {
                return null;
            }
            if (CommandsTabCompletion.hasSubCommand(stringArray, "give", "g")) {
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
            if (CommandsTabCompletion.hasSubCommand(stringArray, "giveall", "ga")) {
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
            if (CommandsTabCompletion.hasSubCommand(stringArray, "wand", "w")) {
                if (!bl && !commandSender.hasPermission(Permissions.GIVE_WAND.getPermission(new String[0]))) {
                    return null;
                }
                if (stringArray.length == 2) {
                    return List.of("sell", "s");
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
        if (CommandsTabCompletion.matches(command.getName(), "selldrops", "sd", "sell", "sellall", "sa", "sellhand", "sh", "sellgui", "sg", "vendre") || CommandsTabCompletion.matches(string2, "selldrops", "sd", "sell", "sellall", "sa", "sellhand", "sh", "sellgui", "sg", "vendre")) {
            if (!Config.SELL_COMMAND_ENABLED.getBoolean()) {
                return null;
            }
            if (!(commandSender.hasPermission(Permissions.GENERATOR_DROPS_SELL_ALL.getPermission(new String[0])) && commandSender.hasPermission(Permissions.GENERATOR_DROPS_SELL_HAND.getPermission(new String[0])) && commandSender.hasPermission(Permissions.GENERATOR_DROPS_SELL_GUI.getPermission(new String[0])))) {
                return null;
            }
            if (stringArray.length == 1) {
                return List.of("hand", "h", "all", "a", "gui", "g");
            }
            return null;
        }
        return null;
    }


    private static boolean matches(String string, String ... stringArray) {
        if (string == null) {
            return false;
        }
        String[] stringArray2 = stringArray;
        int n = stringArray.length;
        int n2 = 0;
        while (n2 < n) {
            if (string.equalsIgnoreCase(stringArray2[n2])) {
                return true;
            }
            ++n2;
        }
        return false;
    }

    private static boolean hasSubCommand(String[] stringArray, String ... stringArray2) {
        return Arrays.stream(stringArray).anyMatch(string -> CommandsTabCompletion.matches(string, stringArray2));
    }

    @Generated
    public CommandsTabCompletion(GeneratorsData generatorsData) {
        this.generatorsData = generatorsData;
    }
}

