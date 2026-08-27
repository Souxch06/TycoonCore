package xyz.arcadiadevs.valoriatycoon.events;

import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import xyz.arcadiadevs.valoriatycoon.models.GeneratorsData;
import xyz.arcadiadevs.valoriatycoon.models.LocationsData;

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

