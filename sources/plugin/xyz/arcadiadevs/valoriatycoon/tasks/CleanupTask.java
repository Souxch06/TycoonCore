/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  lombok.Generated
 *  org.bukkit.block.Block
 *  org.bukkit.scheduler.BukkitRunnable
 */
package xyz.arcadiadevs.valoriatycoon.tasks;

import java.util.ArrayList;
import lombok.Generated;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;
import xyz.arcadiadevs.valoriatycoon.models.GeneratorsData;
import xyz.arcadiadevs.valoriatycoon.models.LocationsData;
import xyz.arcadiadevs.valoriatycoon.utils.SkyblockUtil;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;

public class CleanupTask
extends BukkitRunnable {
    private final LocationsData locationsData;

    public void run() {
        for (LocationsData.GeneratorLocation generatorLocation : this.locationsData.locations()) {
            if (generatorLocation.getBlockLocations().isEmpty()) {
                this.locationsData.removeLocation(generatorLocation);
                continue;
            }
            GeneratorsData.Generator generator = generatorLocation.getGeneratorObject();
            ArrayList<Block> arrayList = generatorLocation.getBlockLocations();
            for (Block block : generatorLocation.getBlockLocations()) {
                if (block.getType() == generator.blockType().getType()) continue;
                if (Config.DEVELOPER_OPTIONS.getBoolean()) {
                    ValoriaTycoon.getInstance().getLogger().info("[CLEANUPTASK] 1. Removing location: " + String.valueOf(generatorLocation));
                }
                arrayList.remove(block);
                this.locationsData.removeLocation(generatorLocation);
            }
            if (generatorLocation.getWorld().isChunkLoaded(0, 0)) continue;
            generatorLocation.getSimplifiedBlockLocations().removeIf(simplifiedLocation -> simplifiedLocation.getLocation() == null || simplifiedLocation.getLocation().getBlock().getType() != generator.blockType().getType());
            if (!generatorLocation.getBlockLocations().isEmpty()) continue;
            if (Config.DEVELOPER_OPTIONS.getBoolean()) {
                ValoriaTycoon.getInstance().getLogger().info("[CLEANUPTASK] 2. Removing location: " + String.valueOf(generatorLocation));
            }
            this.locationsData.removeLocation(generatorLocation);
        }
        this.updateGens();
    }

    private void updateGens() {
        for (LocationsData.GeneratorLocation generatorLocation : this.locationsData.locations()) {
            String string;
            Block block;
            if (generatorLocation.getBlockLocations().isEmpty() || (block = (Block)generatorLocation.getBlockLocations().stream().findAny().orElse(null)) == null || (string = SkyblockUtil.getIslandId(block.getLocation())) == null) continue;
            generatorLocation.setIslandId(string);
        }
    }

    @Generated
    public CleanupTask(LocationsData locationsData) {
        this.locationsData = locationsData;
    }
}

