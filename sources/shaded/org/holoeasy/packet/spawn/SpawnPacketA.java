/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.comphenix.protocol.PacketType
 *  com.comphenix.protocol.PacketType$Play$Server
 *  com.comphenix.protocol.events.PacketContainer
 *  com.comphenix.protocol.reflect.StructureModifier
 *  com.comphenix.protocol.wrappers.WrappedDataWatcher
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.plugin.Plugin
 */
package org.holoeasy.packet.spawn;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import java.util.concurrent.CompletableFuture;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.ClosedRange;
import kotlin.ranges.RangesKt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.holoeasy.ext.StructureModifierExtKt;
import org.holoeasy.packet.PacketBuilderKt;
import org.holoeasy.packet.spawn.ISpawnPacket;
import org.holoeasy.util.BukkitFuture;
import org.holoeasy.util.VersionEnum;
import org.holoeasy.util.VersionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000J\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\f2\u0006\u0010\u000e\u001a\u00020\u000fH\u0002J*\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u00172\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u0016R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\"\u0010\u0005\u001a\u0010\u0012\f\b\u0001\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\u00068VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\t\u0010\n\u00a8\u0006\u0018"}, d2={"Lorg/holoeasy/packet/spawn/SpawnPacketA;", "Lorg/holoeasy/packet/spawn/ISpawnPacket;", "()V", "defaultDataWatcher", "Lcom/comphenix/protocol/wrappers/WrappedDataWatcher;", "versionSupport", "", "Lkotlin/ranges/ClosedRange;", "Lorg/holoeasy/util/VersionEnum;", "getVersionSupport", "()[Lkotlin/ranges/ClosedRange;", "loadDefaultWatcher", "Ljava/util/concurrent/CompletableFuture;", "Ljava/lang/Void;", "plugin", "Lorg/bukkit/plugin/Plugin;", "spawn", "Lcom/comphenix/protocol/events/PacketContainer;", "entityId", "", "entityType", "Lorg/bukkit/entity/EntityType;", "location", "Lorg/bukkit/Location;", "holoeasy-core"})
public final class SpawnPacketA
implements ISpawnPacket {
    @NotNull
    public static final SpawnPacketA INSTANCE = new SpawnPacketA();
    @Nullable
    private static WrappedDataWatcher defaultDataWatcher;

    private SpawnPacketA() {
    }

    private final CompletableFuture<Void> loadDefaultWatcher(Plugin plugin) {
        return BukkitFuture.INSTANCE.runSync(plugin, SpawnPacketA::loadDefaultWatcher$lambda$0);
    }

    @Override
    @NotNull
    public ClosedRange<VersionEnum>[] getVersionSupport() {
        ClosedRange[] closedRangeArray = new ClosedRange[]{RangesKt.rangeTo((Comparable)VersionEnum.V1_8, (Comparable)VersionEnum.V1_8)};
        return closedRangeArray;
    }

    @Override
    @NotNull
    public PacketContainer spawn(int n, @NotNull EntityType entityType, @NotNull Location location, @Nullable Plugin plugin) {
        Intrinsics.checkNotNullParameter(entityType, "entityType");
        Intrinsics.checkNotNullParameter(location, "location");
        PacketType packetType = PacketType.Play.Server.SPAWN_ENTITY_LIVING;
        Intrinsics.checkNotNullExpressionValue(packetType, "SPAWN_ENTITY_LIVING");
        return PacketBuilderKt.packet(packetType, (Function1<? super PacketContainer, Unit>)new Function1<PacketContainer, Unit>(n, location, plugin){
            final /* synthetic */ int $entityId;
            final /* synthetic */ Location $location;
            final /* synthetic */ Plugin $plugin;
            {
                this.$entityId = n;
                this.$location = location;
                this.$plugin = plugin;
                super(1);
            }

            public final void invoke(@NotNull PacketContainer packetContainer) {
                Intrinsics.checkNotNullParameter(packetContainer, "$this$packet");
                StructureModifier structureModifier = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier, 0, this.$entityId);
                StructureModifier structureModifier2 = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier2, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier2, 1, VersionUtil.INSTANCE.getCLEAN_VERSION().getArmorstandId());
                StructureModifier structureModifier3 = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier3, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier3, 2, (int)(this.$location.getX() * (double)32));
                StructureModifier structureModifier4 = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier4, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier4, 3, (int)(this.$location.getY() * (double)32));
                StructureModifier structureModifier5 = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier5, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier5, 4, (int)(this.$location.getZ() * (double)32));
                if (SpawnPacketA.access$getDefaultDataWatcher$p() == null) {
                    Plugin plugin = this.$plugin;
                    if (plugin == null) {
                        throw new RuntimeException("Plugin cannot be null");
                    }
                    SpawnPacketA.access$loadDefaultWatcher(SpawnPacketA.INSTANCE, plugin).join();
                }
                StructureModifier structureModifier6 = packetContainer.getDataWatcherModifier();
                Intrinsics.checkNotNullExpressionValue(structureModifier6, "getDataWatcherModifier(...)");
                StructureModifierExtKt.set(structureModifier6, 0, SpawnPacketA.access$getDefaultDataWatcher$p());
            }
        });
    }

    @Override
    public boolean isCurrentVersion() {
        return ISpawnPacket.DefaultImpls.isCurrentVersion(this);
    }

    private static final void loadDefaultWatcher$lambda$0() {
        World world = (World)Bukkit.getWorlds().get(0);
        Entity entity = world.spawnEntity(new Location(world, 0.0, 256.0, 0.0), EntityType.ARMOR_STAND);
        Intrinsics.checkNotNullExpressionValue(entity, "spawnEntity(...)");
        Entity entity2 = entity;
        entity2.remove();
    }

    public static final /* synthetic */ WrappedDataWatcher access$getDefaultDataWatcher$p() {
        return defaultDataWatcher;
    }

    public static final /* synthetic */ CompletableFuture access$loadDefaultWatcher(SpawnPacketA spawnPacketA, Plugin plugin) {
        return spawnPacketA.loadDefaultWatcher(plugin);
    }
}

