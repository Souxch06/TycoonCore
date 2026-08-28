package org.holoeasy.packet.teleport;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.ClosedRange;
import kotlin.ranges.RangesKt;
import org.bukkit.Location;
import org.holoeasy.ext.DoubleExtKt;
import org.holoeasy.ext.StructureModifierExtKt;
import org.holoeasy.packet.PacketBuilderKt;
import org.holoeasy.packet.teleport.ITeleportPacket;
import org.holoeasy.util.VersionEnum;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000.\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0016R\"\u0010\u0003\u001a\u0010\u0012\f\b\u0001\u0012\b\u0012\u0004\u0012\u00020\u00060\u00050\u00048VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\b\u00a8\u0006\u000f"}, d2={"Lorg/holoeasy/packet/teleport/TeleportPacketA;", "Lorg/holoeasy/packet/teleport/ITeleportPacket;", "()V", "versionSupport", "", "Lkotlin/ranges/ClosedRange;", "Lorg/holoeasy/util/VersionEnum;", "getVersionSupport", "()[Lkotlin/ranges/ClosedRange;", "teleport", "Lcom/comphenix/protocol/events/PacketContainer;", "entityId", "", "location", "Lorg/bukkit/Location;", "holoeasy-core"})
public final class TeleportPacketA
implements ITeleportPacket {
    @NotNull
    public static final TeleportPacketA INSTANCE = new TeleportPacketA();

    private TeleportPacketA() {
    }

    @Override
    @NotNull
    public ClosedRange<VersionEnum>[] getVersionSupport() {
        ClosedRange[] closedRangeArray = new ClosedRange[]{RangesKt.rangeTo((Comparable)VersionEnum.V1_8, (Comparable)VersionEnum.V1_8)};
        return closedRangeArray;
    }

    @Override
    @NotNull
    public PacketContainer teleport(int n, @NotNull Location location) {
        Intrinsics.checkNotNullParameter(location, "location");
        PacketType packetType = PacketType.Play.Server.ENTITY_TELEPORT;
        Intrinsics.checkNotNullExpressionValue(packetType, "ENTITY_TELEPORT");
        return PacketBuilderKt.packet(packetType, (Function1<? super PacketContainer, Unit>)new Function1<PacketContainer, Unit>(n, location){
            final /* synthetic */ int $entityId;
            final /* synthetic */ Location $location;
            {
                this.$entityId = n;
                this.$location = location;
                super(1);
            }

            public final void invoke(@NotNull PacketContainer packetContainer) {
                Intrinsics.checkNotNullParameter(packetContainer, "$this$packet");
                StructureModifier structureModifier = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier, 0, this.$entityId);
                StructureModifier structureModifier2 = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier2, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier2, 1, DoubleExtKt.getFixCoordinate(this.$location.getX()));
                StructureModifier structureModifier3 = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier3, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier3, 2, DoubleExtKt.getFixCoordinate(this.$location.getY()));
                StructureModifier structureModifier4 = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier4, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier4, 3, DoubleExtKt.getFixCoordinate(this.$location.getZ()));
                StructureModifier structureModifier5 = packetContainer.getBytes();
                Intrinsics.checkNotNullExpressionValue(structureModifier5, "getBytes(...)");
                StructureModifierExtKt.set(structureModifier5, 0, DoubleExtKt.getCompressAngle(this.$location.getYaw()));
                StructureModifier structureModifier6 = packetContainer.getBytes();
                Intrinsics.checkNotNullExpressionValue(structureModifier6, "getBytes(...)");
                StructureModifierExtKt.set(structureModifier6, 1, DoubleExtKt.getCompressAngle(this.$location.getPitch()));
                StructureModifier structureModifier7 = packetContainer.getBooleans();
                Intrinsics.checkNotNullExpressionValue(structureModifier7, "getBooleans(...)");
                StructureModifierExtKt.set(structureModifier7, 0, false);
            }
        });
    }

    @Override
    public boolean isCurrentVersion() {
        return ITeleportPacket.DefaultImpls.isCurrentVersion(this);
    }
}

