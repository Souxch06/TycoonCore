package org.holoeasy.line;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ClickEvent {
    public void onClick(@NotNull Player var1);
}

