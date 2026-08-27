package xyz.arcadiadevs.valoriatycoon.events;

import java.util.List;
import lombok.Generated;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import xyz.arcadiadevs.valoriatycoon.models.LocationsData;

public class PistonEvent
implements Listener {
    private final LocationsData locationsData;

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPistonExtend(BlockPistonExtendEvent blockPistonExtendEvent) {
        List list = blockPistonExtendEvent.getBlocks();
        for (Block block : list) {
            LocationsData.GeneratorLocation generatorLocation = this.locationsData.getGeneratorLocation(block);
            if (generatorLocation == null) continue;
            blockPistonExtendEvent.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPistonRetract(BlockPistonRetractEvent blockPistonRetractEvent) {
        List list = blockPistonRetractEvent.getBlocks();
        for (Block block : list) {
            LocationsData.GeneratorLocation generatorLocation = this.locationsData.getGeneratorLocation(block);
            if (generatorLocation == null) continue;
            blockPistonRetractEvent.setCancelled(true);
            return;
        }
    }

    @Generated
    public PistonEvent(LocationsData locationsData) {
        this.locationsData = locationsData;
    }
}

