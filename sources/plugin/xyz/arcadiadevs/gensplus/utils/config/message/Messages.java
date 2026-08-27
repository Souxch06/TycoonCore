/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
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
    NO_PERMISSION("no-permission", "&cErreur> &7Vous n'avez pas la permission de faire cela !"),
    PLUGIN_RELOADED("plugin-reloaded", "&9GensPlus> &7Plugin rechargé !"),
    PLAYER_NOT_FOUND("player-not-found", "&cErreur> &7Joueur introuvable !"),
    TOO_FAST("too-fast", "&cErreur> &7Vous faites cela trop vite !"),
    INVALID_GENERATOR_TIER("invalid-generator-tier", "&cErreur> &7Palier de générateur invalide !"),
    INVALID_FORMAT("invalid-format", "&cErreur> &7Format invalide !"),
    GENERATOR_GIVEN("generator-given", "&9GensPlus> &7Vous avez donné &a%amount% &7générateur(s) de palier &a%tier% &7à &a%targetPlayer%"),
    GENERATOR_RECEIVED("generator-received", "&9GensPlus> &7Vous avez reçu &a%amount% &7générateur(s) de palier &a%tier%"),
    LIMIT_REACHED("limit-reached", "&cErreur> &7Vous avez atteint la limite de &c%limit% &7générateurs !"),
    EVENT_STARTED("event-started", "&9GensPlus> &7%event% a commencé et se terminera dans &e&n%time%&7 !"),
    EVENT_ENDED("event-ended", "&9GensPlus> &7%event% est terminé. Un nouvel événement commencera dans &e&n%time%&7 !"),
    EVENT_FORCE_ENDED("event-force-ended", "&9GensPlus> &7L'événement a été arrêté de force ! Un nouvel événement commencera dans &e&n%time%&7 !"),
    EVENT_ALREADY_RUNNING("event-already-running", "&cErreur> &7Un événement est déjà en cours !"),
    EVENT_NOT_FOUND("event-not-found", "&cErreur> &7Événement introuvable !"),
    SUCCESSFULLY_UPGRADED("successfully-upgraded", "&9GensPlus> &7Votre générateur a été amélioré au palier &a%tier% &7avec succès !"),
    SUCCESSFULLY_SOLD("successfully-sold", "&9GensPlus> &7Drops vendus avec succès pour &a%price%"),
    SUCCESSFULLY_SOLD_ACTION_BAR("successfully-sold-action-bar", "&cVous avez vendu &a%amount% &cdrops pour &a%price%&c !"),
    NOT_ENOUGH_MONEY("not-enough-money", "&cErreur> &7Vous n'avez pas assez d'argent pour faire cela ! (%currentBalance%/&a%price%&7)"),
    NOTHING_TO_SELL("nothing-to-sell", "&cErreur> &7Vous n'avez aucun drop à vendre !"),
    SUCCESSFULLY_DESTROYED("successfully-destroyed", "&9GensPlus> &7Générateur détruit avec succès !"),
    SUCCESSFULLY_PLACED("successfully-placed", "&9GensPlus> &7Générateur de palier %tier% placé avec succès !"),
    SUCCESSFULLY_BOUGHT("successfully-bought", "&9GensPlus> &7Générateur de palier %tier% acheté avec succès pour %price% !"),
    REACHED_MAX_TIER("reached-max-tier", "&cErreur> &7Vous avez atteint le palier maximum du générateur !"),
    NOT_ENOUGH_ARGUMENTS("not-enough-arguments", "&cErreur> &7Arguments insuffisants !"),
    INVALID_AMOUNT("invalid-amount", "&cErreur> &7Montant invalide !"),
    GENERATOR_GIVEN_ALL("generator-given-all", "&9GensPlus> &7Vous avez donné &a%amount% &7générateur(s) de palier &a%tier% &7à tous les joueurs ! (&a%count%&7)"),
    DEFAULT_MESSAGE("default-message", "&9GensPlus> &7Ce serveur utilise GensPlus &av%version%"),
    CANNOT_PLACE_IN_WORLD("cannot-place-in-world", "&cErreur> &7Vous ne pouvez pas placer de générateur dans ce monde !"),
    NOT_YOUR_GENERATOR_DESTROY("not-your-generator-destroy", "&cErreur> &7Vous ne pouvez pas détruire un générateur qui ne vous appartient pas !"),
    NOT_YOUR_GENERATOR_UPGRADE("not-your-generator-upgrade", "&cErreur> &7Vous ne pouvez pas améliorer un générateur qui ne vous appartient pas !"),
    ONLY_PLAYER_CAN_EXECUTE_COMMAND("only-player-can-execute-command", "&cErreur> &7Seul un joueur peut exécuter cette commande !"),
    SELL_WAND_GIVEN("sell-wand-given", "&9GensPlus> &7Vous avez donné une baguette de vente !"),
    SELL_WAND_RECEIVED("sell-wand-received", "&9GensPlus> &7Vous avez reçu une baguette de vente !"),
    UPGRADE_WAND_GIVEN("upgrade-wand-given", "&9GensPlus> &7Vous avez donné une baguette d'amélioration !"),
    UPGRADE_WAND_RECEIVED("upgrade-wand-received", "&9GensPlus> &7Vous avez reçu une baguette d'amélioration !"),
    WAND_BROKE("wand-broke", "&cErreur> &7Votre baguette s'est cassée !"),
    LIMIT_UPDATED("limit-updated", "&9GensPlus> &7Vous avez défini la limite de générateurs de %player% à &a%limit%&7 !");

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

