package org.holoeasy.pool;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.holoeasy.builder.interfaces.HologramRegisterGroup;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.pool.IHologramPool;
import org.holoeasy.pool.KeyAlreadyExistsException;
import org.holoeasy.pool.NoValueForKeyException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0010%\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\u00020\u00012\u00020\u0002B\u0015\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007J\u0010\u0010\u0010\u001a\u00020\u000b2\u0006\u0010\u0011\u001a\u00020\nH\u0016J\u0010\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u0015H\u0007J\u0010\u0010\u0016\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u0017H\u0007J\b\u0010\u0018\u001a\u00020\u0013H\u0002J\u0012\u0010\u0019\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\u0011\u001a\u00020\nH\u0016J\u0010\u0010\u001a\u001a\u00020\u00132\u0006\u0010\u001b\u001a\u00020\u000bH\u0016R\u001d\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u000b0\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0014\u0010\u0003\u001a\u00020\u0004X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2={"Lorg/holoeasy/pool/HologramPool;", "Lorg/bukkit/event/Listener;", "Lorg/holoeasy/pool/IHologramPool;", "plugin", "Lorg/bukkit/plugin/Plugin;", "spawnDistance", "", "(Lorg/bukkit/plugin/Plugin;D)V", "holograms", "", "Ljava/util/UUID;", "Lorg/holoeasy/hologram/Hologram;", "getHolograms", "()Ljava/util/Map;", "getPlugin", "()Lorg/bukkit/plugin/Plugin;", "get", "id", "handleQuit", "", "event", "Lorg/bukkit/event/player/PlayerQuitEvent;", "handleRespawn", "Lorg/bukkit/event/player/PlayerRespawnEvent;", "hologramTick", "remove", "takeCareOf", "value", "holoeasy-core"})
@SourceDebugExtension(value={"SMAP\nHologramPool.kt\nKotlin\n*S Kotlin\n*F\n+ 1 HologramPool.kt\norg/holoeasy/pool/HologramPool\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,110:1\n766#2:111\n857#2,2:112\n1855#2,2:114\n766#2:116\n857#2,2:117\n1855#2,2:119\n*S KotlinDebug\n*F\n+ 1 HologramPool.kt\norg/holoeasy/pool/HologramPool\n*L\n62#1:111\n62#1:112,2\n63#1:114,2\n71#1:116\n71#1:117,2\n72#1:119,2\n*E\n"})
public final class HologramPool
implements Listener,
IHologramPool {
    @NotNull
    private final Plugin plugin;
    private final double spawnDistance;
    @NotNull
    private final Map<UUID, Hologram> holograms;

    public HologramPool(@NotNull Plugin plugin, double d) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        this.plugin = plugin;
        this.spawnDistance = d;
        this.holograms = new ConcurrentHashMap();
        Bukkit.getPluginManager().registerEvents((Listener)this, this.getPlugin());
        this.hologramTick();
    }

    @Override
    @NotNull
    public Plugin getPlugin() {
        return this.plugin;
    }

    @NotNull
    public final Map<UUID, Hologram> getHolograms() {
        return this.holograms;
    }

    @Override
    @NotNull
    public Hologram get(@NotNull UUID uUID) {
        Intrinsics.checkNotNullParameter(uUID, "id");
        Hologram hologram = this.holograms.get(uUID);
        if (hologram == null) {
            throw new NoValueForKeyException(uUID);
        }
        return hologram;
    }

    @Override
    public void takeCareOf(@NotNull Hologram hologram) {
        Intrinsics.checkNotNullParameter(hologram, "value");
        if (this.holograms.containsKey(hologram.getId())) {
            throw new KeyAlreadyExistsException(hologram.getId());
        }
        this.holograms.put(hologram.getId(), hologram);
    }

    @Override
    @Nullable
    public Hologram remove(@NotNull UUID uUID) {
        Intrinsics.checkNotNullParameter(uUID, "id");
        Hologram hologram = this.holograms.remove(uUID);
        if (hologram != null) {
            Hologram hologram2 = hologram;
            boolean bl = false;
            for (Player player : hologram2.getSeeingPlayers()) {
                hologram2.hide(player);
            }
            return hologram2;
        }
        return null;
    }

    @EventHandler
    public final void handleRespawn(@NotNull PlayerRespawnEvent playerRespawnEvent) {
        Intrinsics.checkNotNullParameter(playerRespawnEvent, "event");
        Player player = playerRespawnEvent.getPlayer();
        Intrinsics.checkNotNullExpressionValue(player, "getPlayer(...)");
        Player player2 = player;
        Iterable iterable = this.holograms.values();
        boolean bl = false;
        Iterable iterable2 = iterable;
        Collection collection2 = new ArrayList();
        boolean bl2 = false;
        Iterator iterator2 = iterable2.iterator();
        while (iterator2.hasNext()) {
            Object t = iterator2.next();
            Hologram hologram = (Hologram)t;
            boolean bl3 = false;
            if (!hologram.isShownFor(player2)) continue;
            collection2.add(t);
        }
        iterable = (List)collection2;
        bl = false;
        for (Collection collection2 : iterable) {
            Hologram hologram = (Hologram)((Object)collection2);
            boolean bl4 = false;
            hologram.hide(player2);
        }
    }

    @EventHandler
    public final void handleQuit(@NotNull PlayerQuitEvent playerQuitEvent) {
        Intrinsics.checkNotNullParameter(playerQuitEvent, "event");
        Player player = playerQuitEvent.getPlayer();
        Intrinsics.checkNotNullExpressionValue(player, "getPlayer(...)");
        Player player2 = player;
        Iterable iterable = this.holograms.values();
        boolean bl = false;
        Iterable iterable2 = iterable;
        Collection collection2 = new ArrayList();
        boolean bl2 = false;
        Iterator iterator2 = iterable2.iterator();
        while (iterator2.hasNext()) {
            Object t = iterator2.next();
            Hologram hologram = (Hologram)t;
            boolean bl3 = false;
            if (!hologram.isShownFor(player2)) continue;
            collection2.add(t);
        }
        iterable = (List)collection2;
        bl = false;
        for (Collection collection2 : iterable) {
            Hologram hologram = (Hologram)((Object)collection2);
            boolean bl4 = false;
            hologram.getSeeingPlayers().remove(player2);
        }
    }

    private final void hologramTick() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this.getPlugin(), () -> HologramPool.hologramTick$lambda$5(this), 20L, 2L);
    }

    @Override
    public void registerHolograms(@NotNull HologramRegisterGroup hologramRegisterGroup) {
        IHologramPool.DefaultImpls.registerHolograms(this, hologramRegisterGroup);
    }

    private static final void hologramTick$lambda$5(HologramPool hologramPool) {
        Intrinsics.checkNotNullParameter(hologramPool, "this$0");
        for (Player player : ImmutableList.copyOf((Collection)Bukkit.getOnlinePlayers())) {
            for (Hologram hologram : hologramPool.holograms.values()) {
                boolean bl;
                Location location;
                Location location2 = hologram.getLocation();
                Intrinsics.checkNotNullExpressionValue(player.getLocation(), "getLocation(...)");
                Intrinsics.checkNotNull(player);
                boolean bl2 = hologram.isShownFor(player);
                if (!Intrinsics.areEqual(location2.getWorld(), location.getWorld())) {
                    if (!bl2) continue;
                    hologram.hide(player);
                    continue;
                }
                World world = location2.getWorld();
                Intrinsics.checkNotNull(world);
                if (!world.isChunkLoaded(location2.getBlockX() >> 4, location2.getBlockZ() >> 4) && bl2) {
                    hologram.hide(player);
                    continue;
                }
                boolean bl3 = bl = location2.distanceSquared(location) <= hologramPool.spawnDistance;
                if (!bl && bl2) {
                    hologram.hide(player);
                    continue;
                }
                if (!bl || bl2) continue;
                hologram.show(player);
            }
        }
    }
}

