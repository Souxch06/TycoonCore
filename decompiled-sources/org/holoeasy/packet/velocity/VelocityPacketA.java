/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.comphenix.protocol.PacketType
 *  com.comphenix.protocol.PacketType$Play$Server
 *  com.comphenix.protocol.events.PacketContainer
 *  com.comphenix.protocol.reflect.StructureModifier
 */
package org.holoeasy.packet.velocity;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.ClosedRange;
import kotlin.ranges.RangesKt;
import org.holoeasy.ext.StructureModifierExtKt;
import org.holoeasy.packet.PacketBuilderKt;
import org.holoeasy.packet.velocity.IVelocityPacket;
import org.holoeasy.util.VersionEnum;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J(\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\f2\u0006\u0010\u000e\u001a\u00020\f2\u0006\u0010\u000f\u001a\u00020\fH\u0016R\"\u0010\u0003\u001a\u0010\u0012\f\b\u0001\u0012\b\u0012\u0004\u0012\u00020\u00060\u00050\u00048VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\b\u00a8\u0006\u0010"}, d2={"Lorg/holoeasy/packet/velocity/VelocityPacketA;", "Lorg/holoeasy/packet/velocity/IVelocityPacket;", "()V", "versionSupport", "", "Lkotlin/ranges/ClosedRange;", "Lorg/holoeasy/util/VersionEnum;", "getVersionSupport", "()[Lkotlin/ranges/ClosedRange;", "velocity", "Lcom/comphenix/protocol/events/PacketContainer;", "entityId", "", "x", "y", "z", "holoeasy-core"})
public final class VelocityPacketA
implements IVelocityPacket {
    @NotNull
    public static final VelocityPacketA INSTANCE = new VelocityPacketA();

    private VelocityPacketA() {
    }

    @Override
    @NotNull
    public ClosedRange<VersionEnum>[] getVersionSupport() {
        ClosedRange[] closedRangeArray = new ClosedRange[]{RangesKt.rangeTo((Comparable)VersionEnum.V1_8, (Comparable)VersionEnum.LATEST)};
        return closedRangeArray;
    }

    @Override
    @NotNull
    public PacketContainer velocity(int n, int n2, int n3, int n4) {
        PacketType packetType = PacketType.Play.Server.ENTITY_VELOCITY;
        Intrinsics.checkNotNullExpressionValue(packetType, "ENTITY_VELOCITY");
        return PacketBuilderKt.packet(packetType, (Function1<? super PacketContainer, Unit>)new Function1<PacketContainer, Unit>(n, n2, n3, n4){
            final /* synthetic */ int $entityId;
            final /* synthetic */ int $x;
            final /* synthetic */ int $y;
            final /* synthetic */ int $z;
            {
                this.$entityId = n;
                this.$x = n2;
                this.$y = n3;
                this.$z = n4;
                super(1);
            }

            public final void invoke(@NotNull PacketContainer packetContainer) {
                Intrinsics.checkNotNullParameter(packetContainer, "$this$packet");
                StructureModifier structureModifier = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier, 0, this.$entityId);
                StructureModifier structureModifier2 = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier2, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier2, 1, this.$x);
                StructureModifier structureModifier3 = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier3, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier3, 2, this.$y);
                StructureModifier structureModifier4 = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier4, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier4, 3, this.$z);
            }
        });
    }

    @Override
    public boolean isCurrentVersion() {
        return IVelocityPacket.DefaultImpls.isCurrentVersion(this);
    }
}

