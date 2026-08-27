/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityExplodeEvent
 */
package xyz.arcadiadevs.gensplus.events;

import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import xyz.arcadiadevs.gensplus.models.GeneratorsData;
import xyz.arcadiadevs.gensplus.models.LocationsData;

public record EntityExplode(LocationsData locationsData, GeneratorsData generatorsData) implements Listener
{
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onEntityExplode(EntityExplodeEvent entityExplodeEvent) {
        List list = entityExplodeEvent.blockList();
        if (list.stream().anyMatch(block -> this.locationsData.getGeneratorLocation((Block)block) != null)) {
            entityExplodeEvent.setCancelled(true);
        }
    }
}

