package org.holoeasy.packet.metadata.text;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import kotlin.Metadata;
import kotlin.ranges.ClosedRange;
import kotlin.ranges.RangesKt;
import org.holoeasy.ext.PacketContainerExtKt;
import org.holoeasy.ext.WrappedDataWatcherExtKt;
import org.holoeasy.packet.metadata.text.IMetadataTextPacket;
import org.holoeasy.util.VersionEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\"\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u000e2\u0006\u0010\u000f\u001a\u00020\u0010H\u0016R\"\u0010\u0003\u001a\u0010\u0012\f\b\u0001\u0012\b\u0012\u0004\u0012\u00020\u00060\u00050\u00048VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\b\u00a8\u0006\u0011"}, d2={"Lorg/holoeasy/packet/metadata/text/MetadataTextPacketD;", "Lorg/holoeasy/packet/metadata/text/IMetadataTextPacket;", "()V", "versionSupport", "", "Lkotlin/ranges/ClosedRange;", "Lorg/holoeasy/util/VersionEnum;", "getVersionSupport", "()[Lkotlin/ranges/ClosedRange;", "metadata", "Lcom/comphenix/protocol/events/PacketContainer;", "entityId", "", "nameTag", "", "invisible", "", "holoeasy-core"})
public final class MetadataTextPacketD
implements IMetadataTextPacket {
    @NotNull
    public static final MetadataTextPacketD INSTANCE = new MetadataTextPacketD();

    private MetadataTextPacketD() {
    }

    @Override
    @NotNull
    public ClosedRange<VersionEnum>[] getVersionSupport() {
        ClosedRange[] closedRangeArray = new ClosedRange[]{RangesKt.rangeTo((Comparable)VersionEnum.V1_19, (Comparable)VersionEnum.V1_19)};
        return closedRangeArray;
    }

    @Override
    @NotNull
    public PacketContainer metadata(int n, @Nullable String string, boolean bl) {
        PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packetContainer.getIntegers().write(0, (Object)n);
        WrappedDataWatcher wrappedDataWatcher = new WrappedDataWatcher();
        if (bl) {
            WrappedDataWatcherExtKt.setByte(wrappedDataWatcher, 0, (byte)32);
        }
        if (string != null) {
            WrappedDataWatcherExtKt.setChatComponent(wrappedDataWatcher, 2, string);
            WrappedDataWatcherExtKt.setBool(wrappedDataWatcher, 3, true);
        }
        PacketContainerExtKt.parse119(packetContainer, wrappedDataWatcher);
        return packetContainer;
    }

    @Override
    public boolean isCurrentVersion() {
        return IMetadataTextPacket.DefaultImpls.isCurrentVersion(this);
    }
}

