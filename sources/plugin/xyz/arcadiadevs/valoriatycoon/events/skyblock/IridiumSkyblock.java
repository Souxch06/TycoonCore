package xyz.arcadiadevs.valoriatycoon.events.skyblock;

import com.iridium.iridiumskyblock.api.IslandDeleteEvent;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Generated;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import xyz.arcadiadevs.valoriatycoon.models.LocationsData;

public class IridiumSkyblock
implements Listener {
    private LocationsData locationsData;

    @EventHandler
    public void onIslandRemoveIridium(IslandDeleteEvent islandDeleteEvent) {
        CopyOnWriteArrayList<LocationsData.GeneratorLocation> copyOnWriteArrayList = this.locationsData.locations();
        copyOnWriteArrayList.removeIf(generatorLocation -> generatorLocation.getIslandId().equals(String.valueOf(islandDeleteEvent.getIsland().getId())));
    }

    @Generated
    public IridiumSkyblock(LocationsData locationsData) {
        this.locationsData = locationsData;
    }
}

