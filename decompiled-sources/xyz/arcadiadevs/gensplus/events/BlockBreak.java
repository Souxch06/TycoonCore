/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.block.Block
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 */
package xyz.arcadiadevs.gensplus.events;

import com.cryptomorin.xseries.XMaterial;
import java.util.ArrayList;
import lombok.Generated;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import xyz.arcadiadevs.gensplus.GensPlus;
import xyz.arcadiadevs.gensplus.models.GeneratorsData;
import xyz.arcadiadevs.gensplus.models.LocationsData;
import xyz.arcadiadevs.gensplus.utils.ServerVersion;
import xyz.arcadiadevs.gensplus.utils.config.Config;
import xyz.arcadiadevs.gensplus.utils.config.Permissions;
import xyz.arcadiadevs.gensplus.utils.config.message.Messages;

public class BlockBreak
implements Listener {
    private LocationsData locationsData;
    private GeneratorsData generatorsData;

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent blockBreakEvent) {
        Block block2 = blockBreakEvent.getBlock();
        LocationsData.GeneratorLocation generatorLocation = this.locationsData.getGeneratorLocation(block2);
        if (generatorLocation == null) {
            return;
        }
        if (generatorLocation.getPlacedBy() != blockBreakEvent.getPlayer() && !blockBreakEvent.getPlayer().isOp() && !blockBreakEvent.getPlayer().hasPermission(Permissions.ADMIN.getPermission(new String[0]))) {
            Messages.NOT_YOUR_GENERATOR_UPGRADE.format(new Object[0]).send((CommandSender)blockBreakEvent.getPlayer());
            blockBreakEvent.setCancelled(true);
            return;
        }
        GeneratorsData.Generator generator = this.generatorsData.getGenerator(generatorLocation.getGenerator());
        OfflinePlayer offlinePlayer = generatorLocation.getPlacedBy();
        int n = generatorLocation.getGenerator();
        ArrayList<Block> arrayList = generatorLocation.getBlockLocations();
        if (GensPlus.getInstance().getConfig().getBoolean(Config.INSTANT_PICKUP.getPath())) {
            generator.giveItem((Player)offlinePlayer);
        } else {
            generator.dropItem(blockBreakEvent.getPlayer(), block2.getLocation());
        }
        arrayList.remove(block2);
        this.locationsData.removeLocation(generatorLocation);
        if (Config.DEVELOPER_OPTIONS.getBoolean()) {
            GensPlus.getInstance().getLogger().info("[BLOCKBREAK] 3. Removing location: " + String.valueOf(generatorLocation));
        }
        arrayList.forEach(block -> {
            LocationsData.GeneratorLocation generatorLocation = this.locationsData.getGeneratorLocation((Block)block);
            if (generatorLocation != null) {
                return;
            }
            this.locationsData.createLocation(offlinePlayer, n, (Block)block);
        });
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_12)) {
            blockBreakEvent.setDropItems(false);
        } else {
            blockBreakEvent.setCancelled(true);
            blockBreakEvent.getBlock().setType(XMaterial.AIR.parseMaterial());
        }
        Messages.SUCCESSFULLY_DESTROYED.format(new Object[0]).send((CommandSender)blockBreakEvent.getPlayer());
    }

    @Generated
    public BlockBreak(LocationsData locationsData, GeneratorsData generatorsData) {
        this.locationsData = locationsData;
        this.generatorsData = generatorsData;
    }
}

