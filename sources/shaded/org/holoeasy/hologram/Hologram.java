package org.holoeasy.hologram;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.holoeasy.hologram.HideEvent;
import org.holoeasy.hologram.IHologramLoader;
import org.holoeasy.hologram.ShowEvent;
import org.holoeasy.line.ILine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000f\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\r\n\u0002\u0010#\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0007\n\u0002\u0010\u0011\n\u0002\b\u0007\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0013\u0010#\u001a\u00020$2\b\u0010%\u001a\u0004\u0018\u00010\u0001H\u0096\u0002J\b\u0010&\u001a\u00020'H\u0016J\u000e\u0010(\u001a\u00020)2\u0006\u0010*\u001a\u00020\u001eJ\u000e\u0010+\u001a\u00020$2\u0006\u0010*\u001a\u00020\u001eJ!\u0010,\u001a\u0002H-\"\f\b\u0000\u0010-*\u0006\u0012\u0002\b\u00030\u000b2\u0006\u0010.\u001a\u00020'\u00a2\u0006\u0002\u0010/J'\u00100\u001a\u00020)2\u001a\u0010\u0012\u001a\u000e\u0012\n\b\u0001\u0012\u0006\u0012\u0002\b\u00030\u000b01\"\u0006\u0012\u0002\b\u00030\u000b\u00a2\u0006\u0002\u00102J\u000e\u00103\u001a\u00020\u00002\u0006\u0010\f\u001a\u00020\rJ\u000e\u00104\u001a\u00020\u00002\u0006\u0010!\u001a\u00020\"J\u000e\u00105\u001a\u00020)2\u0006\u0010*\u001a\u00020\u001eJ\u000e\u00106\u001a\u00020)2\u0006\u00107\u001a\u00020\u0005R\u0018\u0010\t\u001a\f\u0012\b\u0012\u0006\u0012\u0002\b\u00030\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\f\u001a\u0004\u0018\u00010\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u000e\u001a\u00020\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u001b\u0010\u0012\u001a\f\u0012\b\u0012\u0006\u0012\u0002\b\u00030\u000b0\n8F\u00a2\u0006\u0006\u001a\u0004\b\u0013\u0010\u0014R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u001e\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0017\u001a\u00020\u0005@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u001bR\u0017\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001e0\u001d\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010 R\u0010\u0010!\u001a\u0004\u0018\u00010\"X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u00068"}, d2={"Lorg/holoeasy/hologram/Hologram;", "", "plugin", "Lorg/bukkit/plugin/Plugin;", "location", "Lorg/bukkit/Location;", "loader", "Lorg/holoeasy/hologram/IHologramLoader;", "(Lorg/bukkit/plugin/Plugin;Lorg/bukkit/Location;Lorg/holoeasy/hologram/IHologramLoader;)V", "hLines", "", "Lorg/holoeasy/line/ILine;", "hideEvent", "Lorg/holoeasy/hologram/HideEvent;", "id", "Ljava/util/UUID;", "getId", "()Ljava/util/UUID;", "lines", "getLines", "()Ljava/util/List;", "getLoader", "()Lorg/holoeasy/hologram/IHologramLoader;", "<set-?>", "getLocation", "()Lorg/bukkit/Location;", "getPlugin", "()Lorg/bukkit/plugin/Plugin;", "seeingPlayers", "", "Lorg/bukkit/entity/Player;", "getSeeingPlayers", "()Ljava/util/Set;", "showEvent", "Lorg/holoeasy/hologram/ShowEvent;", "equals", "", "other", "hashCode", "", "hide", "", "player", "isShownFor", "lineAt", "T", "index", "(I)Lorg/holoeasy/line/ILine;", "load", "", "([Lorg/holoeasy/line/ILine;)V", "onHide", "onShow", "show", "teleport", "to", "holoeasy-core"})
@SourceDebugExtension(value={"SMAP\nHologram.kt\nKotlin\n*S Kotlin\n*F\n+ 1 Hologram.kt\norg/holoeasy/hologram/Hologram\n+ 2 _Arrays.kt\nkotlin/collections/ArraysKt___ArraysKt\n*L\n1#1,92:1\n13309#2,2:93\n*S KotlinDebug\n*F\n+ 1 Hologram.kt\norg/holoeasy/hologram/Hologram\n*L\n46#1:93,2\n*E\n"})
public final class Hologram {
    @NotNull
    private final Plugin plugin;
    @NotNull
    private final IHologramLoader loader;
    @NotNull
    private final UUID id;
    @NotNull
    private Location location;
    @NotNull
    private final List<ILine<?>> hLines;
    @NotNull
    private final Set<Player> seeingPlayers;
    @Nullable
    private ShowEvent showEvent;
    @Nullable
    private HideEvent hideEvent;

    public Hologram(@NotNull Plugin plugin, @NotNull Location location, @NotNull IHologramLoader iHologramLoader) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        Intrinsics.checkNotNullParameter(location, "location");
        Intrinsics.checkNotNullParameter(iHologramLoader, "loader");
        this.plugin = plugin;
        this.loader = iHologramLoader;
        UUID uUID = UUID.randomUUID();
        Intrinsics.checkNotNull(uUID);
        this.id = uUID;
        this.location = location;
        this.hLines = new CopyOnWriteArrayList();
        ConcurrentHashMap.KeySetView keySetView = ConcurrentHashMap.newKeySet();
        Intrinsics.checkNotNullExpressionValue(keySetView, "newKeySet(...)");
        this.seeingPlayers = keySetView;
    }

    @NotNull
    public final Plugin getPlugin() {
        return this.plugin;
    }

    @NotNull
    public final IHologramLoader getLoader() {
        return this.loader;
    }

    @NotNull
    public final UUID getId() {
        return this.id;
    }

    @NotNull
    public final Location getLocation() {
        return this.location;
    }

    @NotNull
    public final List<ILine<?>> getLines() {
        return this.hLines;
    }

    @NotNull
    public final Set<Player> getSeeingPlayers() {
        return this.seeingPlayers;
    }

    @NotNull
    public final <T extends ILine<?>> T lineAt(int n) {
        ILine<?> iLine = this.hLines.get(n);
        Intrinsics.checkNotNull(iLine, "null cannot be cast to non-null type T of org.holoeasy.hologram.Hologram.lineAt");
        return (T)iLine;
    }

    @NotNull
    public final Hologram onShow(@NotNull ShowEvent showEvent) {
        Intrinsics.checkNotNullParameter(showEvent, "showEvent");
        this.showEvent = showEvent;
        return this;
    }

    @NotNull
    public final Hologram onHide(@NotNull HideEvent hideEvent) {
        Intrinsics.checkNotNullParameter(hideEvent, "hideEvent");
        this.hideEvent = hideEvent;
        return this;
    }

    public final void load(ILine<?> ... iLineArray) {
        Intrinsics.checkNotNullParameter(iLineArray, "lines");
        this.hLines.clear();
        ILine<?>[] iLineArray2 = iLineArray;
        boolean bl = false;
        int n = iLineArray2.length;
        for (int i = 0; i < n; ++i) {
            ILine<?> iLine;
            ILine<?> iLine2 = iLine = iLineArray2[i];
            boolean bl2 = false;
            iLine2.getPvt().setHologram(this);
        }
        this.loader.load(this, iLineArray);
    }

    public final void teleport(@NotNull Location location) {
        Intrinsics.checkNotNullParameter(location, "to");
        Location location2 = location.clone();
        Intrinsics.checkNotNullExpressionValue(location2, "clone(...)");
        this.location = location2;
        this.loader.teleport(this);
    }

    public final boolean isShownFor(@NotNull Player player) {
        Intrinsics.checkNotNullParameter(player, "player");
        return this.seeingPlayers.contains(player);
    }

    public final void show(@NotNull Player player) {
        block1: {
            Intrinsics.checkNotNullParameter(player, "player");
            this.seeingPlayers.add(player);
            for (ILine<?> iLine : this.hLines) {
                iLine.show(player);
            }
            ShowEvent showEvent = this.showEvent;
            if (showEvent == null) break block1;
            showEvent.onShow(player);
        }
    }

    public final void hide(@NotNull Player player) {
        block1: {
            Intrinsics.checkNotNullParameter(player, "player");
            for (ILine<?> iLine : this.hLines) {
                iLine.hide(player);
            }
            this.seeingPlayers.remove(player);
            HideEvent hideEvent = this.hideEvent;
            if (hideEvent == null) break block1;
            hideEvent.onHide(player);
        }
    }

    public boolean equals(@Nullable Object object) {
        if (this == object) {
            return true;
        }
        Object object2 = object;
        if (!Intrinsics.areEqual(this.getClass(), object2 != null ? object2.getClass() : null)) {
            return false;
        }
        Intrinsics.checkNotNull(object, "null cannot be cast to non-null type org.holoeasy.hologram.Hologram");
        Hologram cfr_ignored_0 = (Hologram)object;
        return Intrinsics.areEqual(this.id, ((Hologram)object).id);
    }

    public int hashCode() {
        return this.id.hashCode();
    }
}

