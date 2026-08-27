/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.bukkit.block.Block
 *  org.bukkit.scheduler.BukkitRunnable
 */
package xyz.arcadiadevs.gensplus.tasks;

import java.util.ArrayList;
import lombok.Generated;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.arcadiadevs.gensplus.GensPlus;
import xyz.arcadiadevs.gensplus.models.GeneratorsData;
import xyz.arcadiadevs.gensplus.models.LocationsData;
import xyz.arcadiadevs.gensplus.utils.SkyblockUtil;
import xyz.arcadiadevs.gensplus.utils.config.Config;

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
                    GensPlus.getInstance().getLogger().info("[CLEANUPTASK] 1. Removing location: " + String.valueOf(generatorLocation));
                }
                arrayList.remove(block);
                this.locationsData.removeLocation(generatorLocation);
            }
            if (generatorLocation.getWorld().isChunkLoaded(0, 0)) continue;
            generatorLocation.getSimplifiedBlockLocations().removeIf(simplifiedLocation -> simplifiedLocation.getLocation() == null || simplifiedLocation.getLocation().getBlock().getType() != generator.blockType().getType());
            if (!generatorLocation.getBlockLocations().isEmpty()) continue;
            if (Config.DEVELOPER_OPTIONS.getBoolean()) {
                GensPlus.getInstance().getLogger().info("[CLEANUPTASK] 2. Removing location: " + String.valueOf(generatorLocation));
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

