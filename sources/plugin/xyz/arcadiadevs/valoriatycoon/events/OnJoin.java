/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  lombok.Generated
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.inventory.Inventory
 */
package xyz.arcadiadevs.valoriatycoon.events;

import lombok.Generated;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import xyz.arcadiadevs.valoriatycoon.models.GeneratorsData;
import xyz.arcadiadevs.valoriatycoon.models.PlayerData;
import xyz.arcadiadevs.valoriatycoon.utils.ItemUtil;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;

public class OnJoin
implements Listener {
    private final GeneratorsData generatorsData;
    private final PlayerData playerData;
    private final FileConfiguration config;

    @EventHandler
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        ItemUtil.upgradeGens((Inventory)playerJoinEvent.getPlayer().getInventory());
        if (this.playerData.getData(playerJoinEvent.getPlayer().getUniqueId()) == null) {
            this.playerData.create(playerJoinEvent.getPlayer().getUniqueId(), Config.LIMIT_PER_PLAYER_DEFAULT_LIMIT.getInt());
        }
        if (!this.config.getBoolean(Config.ON_JOIN_ENABLED.getPath())) {
            return;
        }
        if (playerJoinEvent.getPlayer().hasPlayedBefore()) {
            return;
        }
        Player player = playerJoinEvent.getPlayer();
        int n = Config.ON_JOIN_GENERATOR_TIER.getInt();
        int n2 = Config.ON_JOIN_GENERATOR_AMOUNT.getInt();
        this.generatorsData.giveItemByTier(player, n, n2);
    }

    @Generated
    public OnJoin(GeneratorsData generatorsData, PlayerData playerData, FileConfiguration fileConfiguration) {
        this.generatorsData = generatorsData;
        this.playerData = playerData;
        this.config = fileConfiguration;
    }
}

