package xyz.arcadiadevs.valoriatycoon.events.skyblock;

import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Generated;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import xyz.arcadiadevs.valoriatycoon.models.LocationsData;

public class Bentobox
implements Listener {
    private LocationsData locationsData;

    @EventHandler
    public void onIslandRemoveBentoBox(IslandDeleteEvent islandDeleteEvent) {
        CopyOnWriteArrayList<LocationsData.GeneratorLocation> copyOnWriteArrayList = this.locationsData.locations();
        copyOnWriteArrayList.removeIf(generatorLocation -> generatorLocation.getIslandId().equals(islandDeleteEvent.getIsland().getUniqueId()));
    }

    @Generated
    public Bentobox(LocationsData locationsData) {
        this.locationsData = locationsData;
    }
}

