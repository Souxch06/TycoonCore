package org.holoeasy.packet.metadata.text;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.ClosedRange;
import kotlin.ranges.RangesKt;
import org.holoeasy.ext.StructureModifierExtKt;
import org.holoeasy.packet.PacketBuilderKt;
import org.holoeasy.packet.metadata.text.IMetadataTextPacket;
import org.holoeasy.util.VersionEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\"\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u000e2\u0006\u0010\u000f\u001a\u00020\u0010H\u0016R\"\u0010\u0003\u001a\u0010\u0012\f\b\u0001\u0012\b\u0012\u0004\u0012\u00020\u00060\u00050\u00048VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\b\u00a8\u0006\u0011"}, d2={"Lorg/holoeasy/packet/metadata/text/MetadataTextPacketA;", "Lorg/holoeasy/packet/metadata/text/IMetadataTextPacket;", "()V", "versionSupport", "", "Lkotlin/ranges/ClosedRange;", "Lorg/holoeasy/util/VersionEnum;", "getVersionSupport", "()[Lkotlin/ranges/ClosedRange;", "metadata", "Lcom/comphenix/protocol/events/PacketContainer;", "entityId", "", "nameTag", "", "invisible", "", "holoeasy-core"})
public final class MetadataTextPacketA
implements IMetadataTextPacket {
    @NotNull
    public static final MetadataTextPacketA INSTANCE = new MetadataTextPacketA();

    private MetadataTextPacketA() {
    }

    @Override
    @NotNull
    public ClosedRange<VersionEnum>[] getVersionSupport() {
        ClosedRange[] closedRangeArray = new ClosedRange[]{RangesKt.rangeTo((Comparable)VersionEnum.V1_8, (Comparable)VersionEnum.V1_8)};
        return closedRangeArray;
    }

    @Override
    @NotNull
    public PacketContainer metadata(int n, @Nullable String string, boolean bl) {
        WrappedDataWatcher wrappedDataWatcher = new WrappedDataWatcher();
        if (bl) {
            wrappedDataWatcher.setObject(0, (Object)32);
        }
        if (string != null) {
            wrappedDataWatcher.setObject(2, (Object)string);
            wrappedDataWatcher.setObject(3, (Object)1);
        }
        PacketType packetType = PacketType.Play.Server.ENTITY_METADATA;
        Intrinsics.checkNotNullExpressionValue(packetType, "ENTITY_METADATA");
        return PacketBuilderKt.packet(packetType, (Function1<? super PacketContainer, Unit>)new Function1<PacketContainer, Unit>(n, wrappedDataWatcher){
            final /* synthetic */ int $entityId;
            final /* synthetic */ WrappedDataWatcher $watcher;
            {
                this.$entityId = n;
                this.$watcher = wrappedDataWatcher;
                super(1);
            }

            public final void invoke(@NotNull PacketContainer packetContainer) {
                Intrinsics.checkNotNullParameter(packetContainer, "$this$packet");
                StructureModifier structureModifier = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier, 0, this.$entityId);
                StructureModifier structureModifier2 = packetContainer.getWatchableCollectionModifier();
                Intrinsics.checkNotNullExpressionValue(structureModifier2, "getWatchableCollectionModifier(...)");
                StructureModifierExtKt.set(structureModifier2, 0, this.$watcher.getWatchableObjects());
            }
        });
    }

    @Override
    public boolean isCurrentVersion() {
        return IMetadataTextPacket.DefaultImpls.isCurrentVersion(this);
    }
}

