package org.holoeasy.builder.interfaces;

import org.holoeasy.builder.HologramConfig;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface HologramConfigGroup {
    public void configure(@NotNull HologramConfig var1);
}

