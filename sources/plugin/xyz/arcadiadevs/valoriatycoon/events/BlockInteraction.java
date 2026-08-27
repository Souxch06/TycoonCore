package xyz.arcadiadevs.valoriatycoon.events;

import com.cryptomorin.xseries.XMaterial;
import java.util.Arrays;
import lombok.Generated;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import xyz.arcadiadevs.valoriatycoon.guis.UpgradeGui;
import xyz.arcadiadevs.valoriatycoon.models.GeneratorsData;
import xyz.arcadiadevs.valoriatycoon.models.LocationsData;
import xyz.arcadiadevs.valoriatycoon.utils.ServerVersion;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;
import xyz.arcadiadevs.valoriatycoon.utils.config.Permissions;
import xyz.arcadiadevs.valoriatycoon.utils.config.message.Messages;

public class BlockInteraction
implements Listener {
    private final LocationsData locationsData;
    private final FileConfiguration config;

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onBlockClick(PlayerInteractEvent playerInteractEvent) {
        if (ServerVersion.isServerVersionAbove(ServerVersion.V1_8) && playerInteractEvent.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = playerInteractEvent.getClickedBlock();
        Player player = playerInteractEvent.getPlayer();
        if (block == null) {
            return;
        }
        LocationsData.GeneratorLocation generatorLocation = this.locationsData.getGeneratorLocation(block);
        if (generatorLocation == null) {
            return;
        }
        boolean bl = Config.GENERATOR_UPGRADE_SNEAK.getBoolean();
        String string = Config.GENERATOR_UPGRADE_ACTION.getString();
        if (bl && !player.isSneaking() || playerInteractEvent.getAction() != Action.valueOf((String)string)) {
            return;
        }
        GeneratorsData.Generator generator = generatorLocation.getGeneratorObject();
        if (generator == null) {
            return;
        }
        playerInteractEvent.setCancelled(true);
        if (Config.GUIS_UPGRADE_GUI_ENABLED.getBoolean()) {
            UpgradeGui.open(player, generatorLocation, block);
        } else {
            if (generatorLocation.getPlacedBy() != player && !player.hasPermission(Permissions.ADMIN.getPermission(new String[0])) && !player.isOp()) {
                Messages.NOT_YOUR_GENERATOR_UPGRADE.format(new Object[0]).send((CommandSender)player);
                return;
            }
            UpgradeGui.upgradeGenerator(player, generatorLocation, block);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void beaconInteract(PlayerInteractEvent playerInteractEvent) {
        LocationsData.GeneratorLocation generatorLocation = this.locationsData.getGeneratorLocation(playerInteractEvent.getClickedBlock());
        if (generatorLocation == null) {
            return;
        }
        if (playerInteractEvent.getClickedBlock() != null && playerInteractEvent.getClickedBlock().getType() == Material.BEACON && generatorLocation.getGeneratorObject().blockType().getType() == Material.BEACON && playerInteractEvent.getAction() == Action.RIGHT_CLICK_BLOCK) {
            playerInteractEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void eggInteract(PlayerInteractEvent playerInteractEvent) {
        LocationsData.GeneratorLocation generatorLocation = this.locationsData.getGeneratorLocation(playerInteractEvent.getClickedBlock());
        if (generatorLocation == null) {
            return;
        }
        if (playerInteractEvent.getClickedBlock() != null && playerInteractEvent.getClickedBlock().getType() == XMaterial.DRAGON_EGG.parseMaterial() && generatorLocation.getGeneratorObject().blockType().getType() == XMaterial.DRAGON_EGG.parseMaterial()) {
            playerInteractEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onItemUse(PlayerInteractEvent playerInteractEvent) {
        LocationsData.GeneratorLocation generatorLocation = this.locationsData.getGeneratorLocation(playerInteractEvent.getClickedBlock());
        if (generatorLocation == null) {
            return;
        }
        if (playerInteractEvent.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack[] itemStackArray = new ItemStack[]{XMaterial.WOODEN_SHOVEL.parseItem(), XMaterial.STONE_SHOVEL.parseItem(), XMaterial.IRON_SHOVEL.parseItem(), XMaterial.GOLDEN_SHOVEL.parseItem(), XMaterial.DIAMOND_SHOVEL.parseItem(), XMaterial.NETHERITE_SHOVEL.parseItem()};
        ItemStack[] itemStackArray2 = new ItemStack[]{XMaterial.WOODEN_HOE.parseItem(), XMaterial.STONE_HOE.parseItem(), XMaterial.IRON_HOE.parseItem(), XMaterial.GOLDEN_HOE.parseItem(), XMaterial.DIAMOND_HOE.parseItem(), XMaterial.NETHERITE_HOE.parseItem()};
        if (Arrays.asList(itemStackArray).contains(playerInteractEvent.getItem()) || Arrays.asList(itemStackArray2).contains(playerInteractEvent.getItem())) {
            playerInteractEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onConcreteForm(BlockFormEvent blockFormEvent) {
        if (blockFormEvent.getNewState().getType().name().contains("CONCRETE")) {
            LocationsData.GeneratorLocation generatorLocation = this.locationsData.getGeneratorLocation(blockFormEvent.getBlock());
            if (generatorLocation == null) {
                return;
            }
            blockFormEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onCoralForm(BlockFadeEvent blockFadeEvent) {
        if (blockFadeEvent.getNewState().getType().name().contains("CORAL")) {
            LocationsData.GeneratorLocation generatorLocation = this.locationsData.getGeneratorLocation(blockFadeEvent.getBlock());
            if (generatorLocation == null) {
                return;
            }
            blockFadeEvent.setCancelled(true);
        }
    }

    @Generated
    public BlockInteraction(LocationsData locationsData, FileConfiguration fileConfiguration) {
        this.locationsData = locationsData;
        this.config = fileConfiguration;
    }
}

