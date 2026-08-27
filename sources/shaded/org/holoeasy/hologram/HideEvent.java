package org.holoeasy.hologram;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface HideEvent {
    public void onHide(@NotNull Player var1);
}

