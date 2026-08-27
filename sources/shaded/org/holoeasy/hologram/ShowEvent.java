package org.holoeasy.hologram;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ShowEvent {
    public void onShow(@NotNull Player var1);
}

