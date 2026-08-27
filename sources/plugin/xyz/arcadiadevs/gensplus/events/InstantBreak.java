/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  lombok.Generated
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.block.Block
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.EquipmentSlot
 */
package xyz.arcadiadevs.gensplus.events;

import java.util.ArrayList;
import lombok.Generated;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import xyz.arcadiadevs.gensplus.GensPlus;
import xyz.arcadiadevs.gensplus.models.GeneratorsData;
import xyz.arcadiadevs.gensplus.models.LocationsData;
import xyz.arcadiadevs.gensplus.utils.ServerVersion;
import xyz.arcadiadevs.gensplus.utils.config.Config;
import xyz.arcadiadevs.gensplus.utils.config.message.Messages;

public class InstantBreak
implements Listener {
    private final LocationsData locationsData;
    private final GeneratorsData generatorsData;

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent playerInteractEvent) {
        if (ServerVersion.isServerVersionAbove(ServerVersion.V1_8) && playerInteractEvent.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = playerInteractEvent.getPlayer();
        Block block2 = playerInteractEvent.getClickedBlock();
        if (block2 == null) {
            return;
        }
        if (playerInteractEvent.getAction() != Action.LEFT_CLICK_BLOCK || !player.isSneaking()) {
            return;
        }
        LocationsData.GeneratorLocation generatorLocation = this.locationsData.getGeneratorLocation(block2);
        if (generatorLocation == null) {
            return;
        }
        if (generatorLocation.getPlacedBy() != playerInteractEvent.getPlayer() && !player.isOp()) {
            Messages.NOT_YOUR_GENERATOR_DESTROY.format(new Object[0]).send((CommandSender)playerInteractEvent.getPlayer());
            return;
        }
        GeneratorsData.Generator generator = this.generatorsData.getGenerator(generatorLocation.getGenerator());
        if (generator == null) {
            return;
        }
        if (!generator.instantBreak()) {
            return;
        }
        int n = generatorLocation.getGenerator();
        ArrayList<Block> arrayList = generatorLocation.getBlockLocations();
        block2.setType(Material.AIR);
        if (GensPlus.getInstance().getConfig().getBoolean(Config.INSTANT_PICKUP.getPath())) {
            generator.giveItem(player);
        } else {
            generator.dropItem(playerInteractEvent.getPlayer(), block2.getLocation());
        }
        arrayList.remove(block2);
        this.locationsData.removeLocation(generatorLocation);
        arrayList.forEach(block -> {
            LocationsData.GeneratorLocation generatorLocation = this.locationsData.getGeneratorLocation((Block)block);
            if (generatorLocation != null) {
                return;
            }
            this.locationsData.createLocation((OfflinePlayer)player, n, (Block)block);
        });
        Messages.SUCCESSFULLY_DESTROYED.format(new Object[0]).send((CommandSender)playerInteractEvent.getPlayer());
        playerInteractEvent.setCancelled(true);
    }

    @Generated
    public InstantBreak(LocationsData locationsData, GeneratorsData generatorsData) {
        this.locationsData = locationsData;
        this.generatorsData = generatorsData;
    }
}

