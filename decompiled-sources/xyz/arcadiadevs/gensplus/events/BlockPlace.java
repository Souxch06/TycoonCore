/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.inventory.ItemStack
 */
package xyz.arcadiadevs.gensplus.events;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import java.util.List;
import lombok.Generated;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import xyz.arcadiadevs.gensplus.models.LocationsData;
import xyz.arcadiadevs.gensplus.models.PlayerData;
import xyz.arcadiadevs.gensplus.utils.LimitUtil;
import xyz.arcadiadevs.gensplus.utils.SkyblockUtil;
import xyz.arcadiadevs.gensplus.utils.config.Config;
import xyz.arcadiadevs.gensplus.utils.config.message.Messages;

public class BlockPlace
implements Listener {
    private LocationsData locationsData;
    private PlayerData playerData;
    private FileConfiguration config;

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent blockPlaceEvent) {
        Player player = blockPlaceEvent.getPlayer();
        ItemStack itemStack = blockPlaceEvent.getItemInHand();
        if (Config.DEVELOPER_OPTIONS.getBoolean()) {
            player.sendMessage("[DEBUG] Placing item: " + String.valueOf(itemStack.getType()));
            player.sendMessage("[DEBUG] Has spawn NBT: " + NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "gensplus", "spawnitem", "tier"}));
            player.sendMessage("[DEBUG] Has block NBT: " + NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "gensplus", "blocktype", "tier"}));
            if (NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "gensplus", "blocktype", "tier"})) {
                player.sendMessage("[DEBUG] Block tier: " + NBTEditor.getInt(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "gensplus", "blocktype", "tier"}));
            }
        }
        if (NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "gensplus", "spawnitem", "tier"}) && !Config.CAN_DROPS_BE_PLACED.getBoolean()) {
            blockPlaceEvent.setCancelled(true);
            return;
        }
        List list = this.config.getStringList(Config.DISABLED_WORLDS.getPath());
        for (String string : list) {
            if (!blockPlaceEvent.getBlockPlaced().getWorld().getName().equals(string)) continue;
            if (!NBTEditor.contains(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "gensplus", "blocktype", "tier"})) {
                return;
            }
            Messages.CANNOT_PLACE_IN_WORLD.format("world", string).send((CommandSender)blockPlaceEvent.getPlayer());
            blockPlaceEvent.setCancelled(true);
            return;
        }
        int n = NBTEditor.getInt(itemStack, new Object[]{NBTEditor.CUSTOM_DATA, "gensplus", "blocktype", "tier"});
        boolean bl = Config.LIMIT_PER_PLAYER_ENABLED.getBoolean();
        int n2 = LimitUtil.calculateCombinedLimit((OfflinePlayer)player, this.playerData);
        if (Config.LIMIT_PER_ISLAND_ENABLED.getBoolean()) {
            int n3 = (int)SkyblockUtil.calculateLimit(player);
            String string = SkyblockUtil.getIslandId(blockPlaceEvent.getBlock().getLocation());
            if (this.locationsData.getGeneratorsCountByIsland(string) >= n3) {
                Messages.LIMIT_REACHED.format("limit", n3).send((CommandSender)player);
                blockPlaceEvent.setCancelled(true);
                return;
            }
        }
        if (Config.LIMIT_PER_ISLAND_ENABLED.getBoolean()) {
            n2 = (int)SkyblockUtil.calculateLimit(player);
        }
        if (bl && this.locationsData.getGeneratorsCountByPlayer(player) >= n2) {
            Messages.LIMIT_REACHED.format("limit", n2).send((CommandSender)player);
            blockPlaceEvent.setCancelled(true);
            return;
        }
        this.locationsData.createLocation((OfflinePlayer)player, n, blockPlaceEvent.getBlockPlaced());
        Messages.SUCCESSFULLY_PLACED.format("tier", n).send((CommandSender)player);
    }

    @Generated
    public BlockPlace(LocationsData locationsData, PlayerData playerData, FileConfiguration fileConfiguration) {
        this.locationsData = locationsData;
        this.playerData = playerData;
        this.config = fileConfiguration;
    }
}

