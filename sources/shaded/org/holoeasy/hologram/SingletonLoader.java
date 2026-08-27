/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 */
package org.holoeasy.hologram;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.hologram.IHologramLoader;
import org.holoeasy.line.ILine;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J)\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0012\u0010\u0007\u001a\u000e\u0012\n\b\u0001\u0012\u0006\u0012\u0002\b\u00030\t0\bH\u0016\u00a2\u0006\u0002\u0010\nJ\u0010\u0010\u000b\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0016\u00a8\u0006\f"}, d2={"Lorg/holoeasy/hologram/SingletonLoader;", "Lorg/holoeasy/hologram/IHologramLoader;", "()V", "load", "", "hologram", "Lorg/holoeasy/hologram/Hologram;", "lines", "", "Lorg/holoeasy/line/ILine;", "(Lorg/holoeasy/hologram/Hologram;[Lorg/holoeasy/line/ILine;)V", "teleport", "holoeasy-core"})
@SourceDebugExtension(value={"SMAP\nSingletonLoader.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SingletonLoader.kt\norg/holoeasy/hologram/SingletonLoader\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,27:1\n1855#2,2:28\n*S KotlinDebug\n*F\n+ 1 SingletonLoader.kt\norg/holoeasy/hologram/SingletonLoader\n*L\n24#1:28,2\n*E\n"})
public final class SingletonLoader
implements IHologramLoader {
    @Override
    public void load(@NotNull Hologram hologram, @NotNull ILine<?>[] iLineArray) {
        Intrinsics.checkNotNullParameter(hologram, "hologram");
        Intrinsics.checkNotNullParameter(iLineArray, "lines");
        if (iLineArray.length > 1) {
            throw new RuntimeException("Hologram '" + hologram.getId() + "' has more than 1 line.");
        }
        Location location = hologram.getLocation().clone();
        Intrinsics.checkNotNullExpressionValue(location, "clone(...)");
        Location location2 = location;
        ILine<?> iLine = iLineArray[0];
        iLine.setLocation(location2);
        hologram.getLines().add(iLine);
    }

    @Override
    public void teleport(@NotNull Hologram hologram) {
        Intrinsics.checkNotNullParameter(hologram, "hologram");
        ILine<?> iLine = hologram.getLines().get(0);
        Location location = hologram.getLocation().clone();
        Intrinsics.checkNotNullExpressionValue(location, "clone(...)");
        iLine.setLocation(location);
        Iterable iterable = hologram.getSeeingPlayers();
        boolean bl = false;
        for (Object t : iterable) {
            Player player = (Player)t;
            boolean bl2 = false;
            iLine.teleport(player);
        }
    }
}

