/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package xyz.arcadiadevs.gensplus.utils.config.message;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Generated;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.arcadiadevs.gensplus.GensPlus;
import xyz.arcadiadevs.gensplus.utils.config.message.PlayerMessage;

public enum Messages {
    NO_PERMISSION("no-permission", "&cError> &7You don't have permission to do that!"),
    PLUGIN_RELOADED("plugin-reloaded", "&9GensPlus> &7Plugin reloaded!"),
    PLAYER_NOT_FOUND("player-not-found", "&cError> &7Player not found!"),
    TOO_FAST("too-fast", "&cError> &7You are doing that too fast!"),
    INVALID_GENERATOR_TIER("invalid-generator-tier", "&cError> &7Invalid generator tier!"),
    INVALID_FORMAT("invalid-format", "&cError> &7Invalid format!"),
    GENERATOR_GIVEN("generator-given", "&9GensPlus> &7You gave &a%amount% &7generator(s) of tier &a%tier% &7to &a%targetPlayer%"),
    GENERATOR_RECEIVED("generator-received", "&9GensPlus> &7You received &a%amount% &7generator(s) of tier &a%tier%"),
    LIMIT_REACHED("limit-reached", "&cError> &7You have reached the limit of &c%limit% &7generators!"),
    EVENT_STARTED("event-started", "&9GensPlus> &7%event% has started and will end in &e&n%time%!"),
    EVENT_ENDED("event-ended", "&9GensPlus> &7%event% has ended and a new event will be started in &e&n%time%!"),
    EVENT_FORCE_ENDED("event-force-ended", "&9GensPlus> &7Event has been force ended! New event will be started in &e&n%time%!"),
    EVENT_ALREADY_RUNNING("event-already-running", "&cError> &7An event is already running!"),
    EVENT_NOT_FOUND("event-not-found", "&cError> &7Event not found!"),
    SUCCESSFULLY_UPGRADED("successfully-upgraded", "&9GensPlus> &7Successfully upgraded your generator to tier &a%tier%!"),
    SUCCESSFULLY_SOLD("successfully-sold", "&9GensPlus> &7Successfully sold drops for &a%price%"),
    SUCCESSFULLY_SOLD_ACTION_BAR("successfully-sold-action-bar", "&cSuccessfully sold &a%amount% &cdrops for &a%price%&c!"),
    NOT_ENOUGH_MONEY("not-enough-money", "&cError> &7You don't have enough money to do that! (%currentBalance%/&a%price%&7)"),
    NOTHING_TO_SELL("nothing-to-sell", "&cError> &7You don't have any drops to sell!"),
    SUCCESSFULLY_DESTROYED("successfully-destroyed", "&9GensPlus> &7Successfully destroyed generator!"),
    SUCCESSFULLY_PLACED("successfully-placed", "&9GensPlus> &7Successfully placed generator tier %tier%!"),
    SUCCESSFULLY_BOUGHT("successfully-bought", "&9GensPlus> &7Successfully bought generator tier %tier% for %price%!"),
    REACHED_MAX_TIER("reached-max-tier", "&cError> &7You have reached the maximum tier of the generator!"),
    NOT_ENOUGH_ARGUMENTS("not-enough-arguments", "&cError> &7Not enough arguments!"),
    INVALID_AMOUNT("invalid-amount", "&cError> &7Invalid amount!"),
    GENERATOR_GIVEN_ALL("generator-given-all", "&9GensPlus> &7You gave &a%amount% &7generator(s) of tier &a%tier% &7to all players! (&a%count%&7)"),
    DEFAULT_MESSAGE("default-message", "&9GensPlus> &7This server is running GensPlus &av%version%"),
    CANNOT_PLACE_IN_WORLD("cannot-place-in-world", "&cError> &7You cannot place a generator in this world!"),
    NOT_YOUR_GENERATOR_DESTROY("not-your-generator-destroy", "&cError> &7You cannot destroy a generator that is not yours!"),
    NOT_YOUR_GENERATOR_UPGRADE("not-your-generator-upgrade", "&cError> &7You cannot upgrade a generator that is not yours!"),
    ONLY_PLAYER_CAN_EXECUTE_COMMAND("only-player-can-execute-command", "&cError> &7Only a player can execute this command!"),
    SELL_WAND_GIVEN("sell-wand-given", "&9GensPlus> &7You have been given a sell wand!"),
    SELL_WAND_RECEIVED("sell-wand-received", "&9GensPlus> &7You have received a sell wand!"),
    UPGRADE_WAND_GIVEN("upgrade-wand-given", "&9GensPlus> &7You have been given an upgrade wand!"),
    UPGRADE_WAND_RECEIVED("upgrade-wand-received", "&9GensPlus> &7You have received an upgrade wand!"),
    WAND_BROKE("wand-broke", "&cError> &7Your wand broke!"),
    LIMIT_UPDATED("limit-updated", "&9GensPlus> &7You have changed %player%'s gens limit to &a%limit%&7!");

    private final String key;
    private final String defaultMessage;
    private String message;

    public static void init() {
        File file = new File(GensPlus.getInstance().getDataFolder(), "messages.yml");
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)file);
        Messages[] messagesArray = Messages.values();
        int n = messagesArray.length;
        int n2 = 0;
        while (n2 < n) {
            Messages messages = messagesArray[n2];
            messages.message = yamlConfiguration.getString(messages.key, messages.defaultMessage);
            ++n2;
        }
        try {
            yamlConfiguration.save(file);
        }
        catch (IOException iOException) {
            iOException.printStackTrace();
        }
    }

    private Messages(String string2, String string3) {
        this.key = string2;
        this.defaultMessage = string3;
    }

    public String getPath() {
        return this.name().toLowerCase().replace("_", "-");
    }

    public List<String> getCached() {
        return new ArrayList<String>(Collections.singletonList(this.message));
    }

    public PlayerMessage format(Object ... objectArray) {
        return new PlayerMessage(this).format(objectArray);
    }

    public String getMessage(String ... stringArray) {
        String string = this.message;
        int n = 0;
        while (n < stringArray.length - 1) {
            string = string.replace(stringArray[n], stringArray[n + 1]);
            n += 2;
        }
        return string;
    }

    @Generated
    public String getDefaultMessage() {
        return this.defaultMessage;
    }
}

