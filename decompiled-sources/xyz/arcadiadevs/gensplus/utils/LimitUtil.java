/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 */
package xyz.arcadiadevs.gensplus.utils;

import org.bukkit.OfflinePlayer;
import xyz.arcadiadevs.gensplus.models.PlayerData;
import xyz.arcadiadevs.gensplus.utils.PlayerUtil;
import xyz.arcadiadevs.gensplus.utils.config.Config;

public class LimitUtil {
    public static int calculateCombinedLimit(OfflinePlayer offlinePlayer, PlayerData playerData) {
        int n = 0;
        boolean bl = Config.LIMIT_PER_PLAYER_USE_COMMANDS.getBoolean();
        boolean bl2 = Config.LIMIT_PER_PLAYER_USE_PERMISSIONS.getBoolean();
        if (bl2) {
            n = PlayerUtil.getGeneratorLimitPerPlayer(offlinePlayer.getPlayer());
        }
        if (bl) {
            int n2 = playerData.getData(offlinePlayer.getUniqueId()).getLimit();
            n = bl2 ? (n += n2) : n2;
        }
        return n;
    }
}

