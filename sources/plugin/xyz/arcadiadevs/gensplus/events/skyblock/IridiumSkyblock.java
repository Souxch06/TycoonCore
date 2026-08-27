/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  com.iridium.iridiumskyblock.api.IslandDeleteEvent
 *  lombok.Generated
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 */
package xyz.arcadiadevs.gensplus.events.skyblock;

import com.iridium.iridiumskyblock.api.IslandDeleteEvent;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Generated;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import xyz.arcadiadevs.gensplus.models.LocationsData;

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

