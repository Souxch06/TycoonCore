/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package xyz.arcadiadevs.gensplus.utils;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.arcadiadevs.gensplus.GensPlus;
import xyz.arcadiadevs.gensplus.utils.ServerVersion;
import xyz.arcadiadevs.gensplus.utils.config.Config;
import xyz.arcadiadevs.gensplus.utils.config.Permissions;

public class PlayerUtil {
    private static final FileConfiguration config = GensPlus.getInstance().getConfig();

    private static Double getLimit(Player player, String string, String string3, String string4) {
        if (config.getBoolean(string3 + ".use-permissions")) {
            List<String> list = player.getEffectivePermissions().stream().map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission().toLowerCase()).filter(string2 -> string2.startsWith(string)).toList();
            if (list.isEmpty()) {
                return config.getDouble(string4);
            }
            return list.stream().map(string2 -> string2.substring(string.length()).replace(',', '.')).map(Double::parseDouble).max(Double::compareTo).orElse(config.getDouble(string4));
        }
        return config.getDouble(string4);
    }

    public static Double getMultiplier(Player player) {
        return PlayerUtil.getLimit(player, Permissions.SELL_MULTIPLIER.getPermission(new String[0]), "multiplier", Config.MULTIPLIER_DEFAULT_MULTIPLIER.getPath());
    }

    public static Integer getGeneratorLimitPerPlayer(Player player) {
        return PlayerUtil.getLimit(player, Permissions.GENERATOR_LIMIT.getPermission(new String[0]), "limits.per-player", Config.LIMIT_PER_PLAYER_DEFAULT_LIMIT.getPath()).intValue();
    }

    public static Integer getRadius(Player player) {
        return PlayerUtil.getLimit(player, Permissions.CHUNK_RADIUS.getPermission(new String[0]), "radius", Config.CHUNK_RADIUS_DEFAULT_RADIUS.getPath()).intValue();
    }

    public static ItemStack getHeldItem(Player player) {
        return ServerVersion.isServerVersionAbove(ServerVersion.V1_8) ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInHand();
    }

    public static ItemStack getOffHeldItem(Player player) {
        return ServerVersion.isServerVersionAbove(ServerVersion.V1_8) ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInHand();
    }
}

