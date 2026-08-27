package org.holoeasy.builder;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.holoeasy.hologram.IHologramLoader;
import org.holoeasy.hologram.TextBlockStandardLoader;
import org.holoeasy.line.ILine;
import org.jetbrains.annotations.NotNull;

public class HologramConfig {
    @NotNull
    final Plugin plugin;
    @NotNull
    final Location location;
    final List<ILine<?>> lines = new ArrayList();
    public IHologramLoader loader = new TextBlockStandardLoader();

    HologramConfig(@NotNull Plugin plugin, @NotNull Location location) {
        this.plugin = plugin;
        this.location = location;
    }
}

