package org.holoeasy.packet.metadata.item;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.ClosedRange;
import kotlin.ranges.RangesKt;
import org.bukkit.inventory.ItemStack;
import org.holoeasy.ext.ItemStackExtKt;
import org.holoeasy.ext.PacketContainerExtKt;
import org.holoeasy.packet.metadata.item.IMetadataItemPacket;
import org.holoeasy.util.SerializersKt;
import org.holoeasy.util.VersionEnum;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000.\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0016R\"\u0010\u0003\u001a\u0010\u0012\f\b\u0001\u0012\b\u0012\u0004\u0012\u00020\u00060\u00050\u00048VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\b\u00a8\u0006\u000f"}, d2={"Lorg/holoeasy/packet/metadata/item/MetadataItemPacketD;", "Lorg/holoeasy/packet/metadata/item/IMetadataItemPacket;", "()V", "versionSupport", "", "Lkotlin/ranges/ClosedRange;", "Lorg/holoeasy/util/VersionEnum;", "getVersionSupport", "()[Lkotlin/ranges/ClosedRange;", "metadata", "Lcom/comphenix/protocol/events/PacketContainer;", "entityId", "", "item", "Lorg/bukkit/inventory/ItemStack;", "holoeasy-core"})
public final class MetadataItemPacketD
implements IMetadataItemPacket {
    @NotNull
    public static final MetadataItemPacketD INSTANCE = new MetadataItemPacketD();

    private MetadataItemPacketD() {
    }

    @Override
    @NotNull
    public ClosedRange<VersionEnum>[] getVersionSupport() {
        ClosedRange[] closedRangeArray = new ClosedRange[]{RangesKt.rangeTo((Comparable)VersionEnum.V1_19, (Comparable)VersionEnum.V1_19)};
        return closedRangeArray;
    }

    @Override
    @NotNull
    public PacketContainer metadata(int n, @NotNull ItemStack itemStack) {
        Intrinsics.checkNotNullParameter(itemStack, "item");
        PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packetContainer.getIntegers().write(0, (Object)n);
        WrappedDataWatcher wrappedDataWatcher = new WrappedDataWatcher();
        WrappedDataWatcher.WrappedDataWatcherObject wrappedDataWatcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(5, SerializersKt.getBOOL_SERIALIZER());
        wrappedDataWatcher.setObject(wrappedDataWatcherObject, (Object)true);
        WrappedDataWatcher.WrappedDataWatcherObject wrappedDataWatcherObject2 = new WrappedDataWatcher.WrappedDataWatcherObject(8, SerializersKt.getITEM_SERIALIZER());
        wrappedDataWatcher.setObject(wrappedDataWatcherObject2, ItemStackExtKt.bukkitGeneric(itemStack));
        PacketContainerExtKt.parse119(packetContainer, wrappedDataWatcher);
        return packetContainer;
    }

    @Override
    public boolean isCurrentVersion() {
        return IMetadataItemPacket.DefaultImpls.isCurrentVersion(this);
    }
}

