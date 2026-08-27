package org.holoeasy.packet.spawn;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import java.util.UUID;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.ClosedRange;
import kotlin.ranges.RangesKt;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.holoeasy.ext.StructureModifierExtKt;
import org.holoeasy.packet.PacketBuilderKt;
import org.holoeasy.packet.spawn.ISpawnPacket;
import org.holoeasy.util.VersionEnum;
import org.holoeasy.util.VersionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J*\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u00102\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012H\u0016R\"\u0010\u0003\u001a\u0010\u0012\f\b\u0001\u0012\b\u0012\u0004\u0012\u00020\u00060\u00050\u00048VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\b\u00a8\u0006\u0013"}, d2={"Lorg/holoeasy/packet/spawn/SpawnPacketB;", "Lorg/holoeasy/packet/spawn/ISpawnPacket;", "()V", "versionSupport", "", "Lkotlin/ranges/ClosedRange;", "Lorg/holoeasy/util/VersionEnum;", "getVersionSupport", "()[Lkotlin/ranges/ClosedRange;", "spawn", "Lcom/comphenix/protocol/events/PacketContainer;", "entityId", "", "entityType", "Lorg/bukkit/entity/EntityType;", "location", "Lorg/bukkit/Location;", "plugin", "Lorg/bukkit/plugin/Plugin;", "holoeasy-core"})
public final class SpawnPacketB
implements ISpawnPacket {
    @NotNull
    public static final SpawnPacketB INSTANCE = new SpawnPacketB();

    private SpawnPacketB() {
    }

    @Override
    @NotNull
    public ClosedRange<VersionEnum>[] getVersionSupport() {
        ClosedRange[] closedRangeArray = new ClosedRange[]{RangesKt.rangeTo((Comparable)VersionEnum.V1_9, (Comparable)VersionEnum.V1_15)};
        return closedRangeArray;
    }

    @Override
    @NotNull
    public PacketContainer spawn(int n, @NotNull EntityType entityType, @NotNull Location location, @Nullable Plugin plugin) {
        Intrinsics.checkNotNullParameter(entityType, "entityType");
        Intrinsics.checkNotNullParameter(location, "location");
        int n2 = 1;
        PacketType packetType = PacketType.Play.Server.SPAWN_ENTITY_LIVING;
        Intrinsics.checkNotNullExpressionValue(packetType, "SPAWN_ENTITY_LIVING");
        return PacketBuilderKt.packet(packetType, (Function1<? super PacketContainer, Unit>)new Function1<PacketContainer, Unit>(n, entityType, n2, location){
            final /* synthetic */ int $entityId;
            final /* synthetic */ EntityType $entityType;
            final /* synthetic */ int $extraData;
            final /* synthetic */ Location $location;
            {
                this.$entityId = n;
                this.$entityType = entityType;
                this.$extraData = n2;
                this.$location = location;
                super(1);
            }

            public final void invoke(@NotNull PacketContainer packetContainer) {
                Intrinsics.checkNotNullParameter(packetContainer, "$this$packet");
                packetContainer.getModifier().writeDefaults();
                StructureModifier structureModifier = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier, 0, this.$entityId);
                StructureModifier structureModifier2 = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier2, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier2, 1, this.$entityType == EntityType.ARMOR_STAND ? VersionUtil.INSTANCE.getCLEAN_VERSION().getArmorstandId() : VersionUtil.INSTANCE.getCLEAN_VERSION().getDroppedItemId());
                StructureModifier structureModifier3 = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier3, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier3, 2, this.$extraData);
                StructureModifier structureModifier4 = packetContainer.getUUIDs();
                Intrinsics.checkNotNullExpressionValue(structureModifier4, "getUUIDs(...)");
                StructureModifierExtKt.set(structureModifier4, 0, UUID.randomUUID());
                StructureModifier structureModifier5 = packetContainer.getDoubles();
                Intrinsics.checkNotNullExpressionValue(structureModifier5, "getDoubles(...)");
                StructureModifierExtKt.set(structureModifier5, 0, this.$location.getX());
                StructureModifier structureModifier6 = packetContainer.getDoubles();
                Intrinsics.checkNotNullExpressionValue(structureModifier6, "getDoubles(...)");
                StructureModifierExtKt.set(structureModifier6, 1, this.$location.getY());
                StructureModifier structureModifier7 = packetContainer.getDoubles();
                Intrinsics.checkNotNullExpressionValue(structureModifier7, "getDoubles(...)");
                StructureModifierExtKt.set(structureModifier7, 2, this.$location.getZ());
            }
        });
    }

    @Override
    public boolean isCurrentVersion() {
        return ISpawnPacket.DefaultImpls.isCurrentVersion(this);
    }
}

