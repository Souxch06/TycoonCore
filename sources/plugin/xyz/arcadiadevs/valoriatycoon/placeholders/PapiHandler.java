package xyz.arcadiadevs.valoriatycoon.placeholders;

import lombok.Generated;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import xyz.arcadiadevs.valoriatycoon.models.LocationsData;
import xyz.arcadiadevs.valoriatycoon.models.PlayerData;
import xyz.arcadiadevs.valoriatycoon.tasks.EventLoop;
import xyz.arcadiadevs.valoriatycoon.utils.PlayerUtil;
import xyz.arcadiadevs.valoriatycoon.utils.TimeUtil;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;

public class PapiHandler
extends PlaceholderExpansion {
    private final Plugin plugin;
    private final LocationsData locationsData;
    private final PlayerData playerData;

    public boolean canRegister() {
        return true;
    }

    @NotNull
    public String getIdentifier() {
        return "valoriatycoon";
    }

    @NotNull
    public String getName() {
        return "ValoriaTycoon";
    }

    @NotNull
    public String getAuthor() {
        return this.plugin.getDescription().getAuthors().toString();
    }

    @NotNull
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    public String onRequest(OfflinePlayer offlinePlayer, String string) {
        boolean bl = Config.LIMIT_PER_PLAYER_USE_COMMANDS.getBoolean();
        boolean bl2 = Config.LIMIT_PER_PLAYER_USE_PERMISSIONS.getBoolean();
        return switch (string) {
            case "event_timer" -> {
                long var6_6 = EventLoop.getActiveEvent().endTime() - System.currentTimeMillis();
                yield TimeUtil.millisToTime(var6_6);
            }
            case "event_name" -> {
                if (EventLoop.getActiveEvent().event() == null) {
                    yield "Aucun événement";
                }
                yield EventLoop.getActiveEvent().event().getName();
            }
            case "gen_limit" -> {
                if (!Config.LIMIT_PER_PLAYER_ENABLED.getBoolean()) {
                    yield Config.LIMIT_PER_PLAYER_UNLIMITED_PLACEHOLDER.getString();
                }
                if (bl2) {
                    yield PlayerUtil.getGeneratorLimitPerPlayer(offlinePlayer.getPlayer()).toString();
                }
                if (bl) {
                    yield String.valueOf(this.playerData.getData(offlinePlayer.getUniqueId()).getLimit());
                }
                yield Config.LIMIT_PER_PLAYER_DEFAULT_LIMIT.getString();
            }
            case "gen_placed" -> this.locationsData.getGeneratorsCountByPlayer(offlinePlayer.getPlayer()).toString();
            case "sell_multiplier" -> PlayerUtil.getMultiplier(offlinePlayer.getPlayer()).toString();
            default -> throw new IllegalStateException("Valeur inattendue : " + string);
        };
    }

    public boolean persist() {
        return true;
    }

    @Generated
    public PapiHandler(Plugin plugin, LocationsData locationsData, PlayerData playerData) {
        this.plugin = plugin;
        this.locationsData = locationsData;
        this.playerData = playerData;
    }
}

