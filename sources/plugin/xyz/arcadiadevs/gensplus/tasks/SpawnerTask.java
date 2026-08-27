/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package xyz.arcadiadevs.gensplus.tasks;

import java.util.HashMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.arcadiadevs.gensplus.GensPlus;
import xyz.arcadiadevs.gensplus.models.GeneratorsData;
import xyz.arcadiadevs.gensplus.models.LocationsData;
import xyz.arcadiadevs.gensplus.models.events.ActiveEvent;
import xyz.arcadiadevs.gensplus.models.events.SpeedEvent;
import xyz.arcadiadevs.gensplus.tasks.EventLoop;

public class SpawnerTask
extends BukkitRunnable {
    private final List<LocationsData.GeneratorLocation> blockData;
    private final GeneratorsData generatorsData;
    private HashMap<GeneratorsData.Generator, Long> genNextSpawn;

    public SpawnerTask(List<LocationsData.GeneratorLocation> list, GeneratorsData generatorsData) {
        this.blockData = list;
        this.generatorsData = generatorsData;
        this.initialize();
    }

    private void initialize() {
        this.genNextSpawn = new HashMap();
        for (GeneratorsData.Generator generator : this.generatorsData.generators()) {
            this.genNextSpawn.put(generator, System.currentTimeMillis() + (long)generator.speed());
        }
    }

    public void run() {
        for (GeneratorsData.Generator generator : this.generatorsData.generators()) {
            if (this.genNextSpawn.get(generator) > System.currentTimeMillis()) continue;
            List<LocationsData.GeneratorLocation> list = this.blockData.stream().filter(generatorLocation -> generatorLocation.getGenerator().intValue() == generator.tier()).toList();
            Bukkit.getScheduler().runTask((Plugin)GensPlus.getInstance(), () -> {
                for (LocationsData.GeneratorLocation generatorLocation : list) {
                    generatorLocation.spawn();
                }
            });
            ActiveEvent activeEvent = EventLoop.getActiveEvent();
            long l = (long)(activeEvent.event() instanceof SpeedEvent ? (double)EventLoop.getActiveEvent().event().getMultiplier() : 1.0);
            this.genNextSpawn.put(generator, System.currentTimeMillis() + (long)generator.speed() / l * 1000L);
        }
    }
}

