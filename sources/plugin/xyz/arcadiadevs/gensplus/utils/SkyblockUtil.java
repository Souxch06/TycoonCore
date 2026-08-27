/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI
 *  com.bgsoftware.superiorskyblock.api.island.Island
 *  com.iridium.iridiumskyblock.api.IridiumSkyblockAPI
 *  com.iridium.iridiumskyblock.database.Island
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  world.bentobox.bentobox.BentoBox
 *  world.bentobox.bentobox.api.addons.request.AddonRequestBuilder
 *  world.bentobox.bentobox.database.objects.Island
 */
package xyz.arcadiadevs.gensplus.utils;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.iridium.iridiumskyblock.api.IridiumSkyblockAPI;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.request.AddonRequestBuilder;
import world.bentobox.bentobox.database.objects.Island;
import xyz.arcadiadevs.gensplus.utils.config.Config;
import xyz.arcadiadevs.gensplus.utils.config.objects.GensPerLevel;

public class SkyblockUtil {
    public static long calculateLimit(Player player) {
        int n;
        long l = SkyblockUtil.getIslandLevel(player.getLocation(), player);
        long l2 = 0L;
        List<GensPerLevel> list = GensPerLevel.factory(Config.LIMIT_PER_ISLAND_GENS_PER_LEVEL.getStringList());
        int n2 = n = list.stream().mapToInt(GensPerLevel::from).min().orElse(0);
        while ((long)n2 <= l) {
            int n3 = n2;
            GensPerLevel gensPerLevel2 = list.stream().filter(gensPerLevel -> gensPerLevel.isIn(n3)).findFirst().orElse(null);
            if (gensPerLevel2 != null) {
                l2 += (long)gensPerLevel2.gain();
            }
            ++n2;
        }
        return l2;
    }

    public static String getIslandId(Location location) {
        String string = null;
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("BentoBox")) {
                string = SkyblockUtil.getIdBentobox(location);
            } else if (Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
                string = SkyblockUtil.getIdSuperiorSkyblock(location);
            } else if (Bukkit.getPluginManager().isPluginEnabled("IridiumSkyblock")) {
                string = SkyblockUtil.getIdIridiumSkyblock(location);
            }
        }
        catch (NullPointerException nullPointerException) {
            return string;
        }
        return string;
    }

    @Nullable
    public static String getIdSuperiorSkyblock(Location location) {
        com.bgsoftware.superiorskyblock.api.island.Island island = SuperiorSkyblockAPI.getIslandAt((Location)location);
        if (island == null) {
            return null;
        }
        return island.getUniqueId().toString();
    }

    @Nullable
    public static String getIdBentobox(Location location) {
        return ((Island)BentoBox.getInstance().getIslands().getIslandAt(location).orElse(null)).getUniqueId();
    }

    @Nullable
    public static String getIdIridiumSkyblock(Location location) {
        return String.valueOf(((com.iridium.iridiumskyblock.database.Island)IridiumSkyblockAPI.getInstance().getIslandViaLocation(location).orElse(null)).getId());
    }

    @Nullable
    public static Long getIslandLevel(Location location, Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("BentoBox")) {
            return SkyblockUtil.getLevelBentobox(location, player);
        }
        if (Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
            return SkyblockUtil.getLevelSuperiorSkyblock(location);
        }
        if (Bukkit.getPluginManager().isPluginEnabled("IridiumSkyblock")) {
            return SkyblockUtil.getLevelIridiumSkyblock(location);
        }
        return null;
    }

    @Nullable
    public static Long getLevelBentobox(Location location, Player player) {
        try {
            return (long)((Long)new AddonRequestBuilder().addon("Level").label("island-level").addMetaData("world-name", (Object)location.getWorld().getName()).addMetaData("player", (Object)player.getUniqueId()).request());
        }
        catch (NullPointerException nullPointerException) {
            return null;
        }
    }

    @Nullable
    public static Long getLevelSuperiorSkyblock(Location location) {
        String string = SkyblockUtil.getIslandId(location);
        if (string == null) {
            return null;
        }
        return SuperiorSkyblockAPI.getGrid().getIslandByUUID(UUID.fromString(string)).getIslandLevel().longValue();
    }

    @Nullable
    public static Long getLevelIridiumSkyblock(Location location) {
        String string = SkyblockUtil.getIslandId(location);
        if (string == null) {
            return null;
        }
        return ((com.iridium.iridiumskyblock.database.Island)IridiumSkyblockAPI.getInstance().getIslandById(Integer.parseInt(string)).orElseThrow()).getLevel();
    }
}

