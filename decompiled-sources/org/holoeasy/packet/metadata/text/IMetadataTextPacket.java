/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.comphenix.protocol.events.PacketContainer
 */
package org.holoeasy.packet.metadata.text;

import com.comphenix.protocol.events.PacketContainer;
import kotlin.Metadata;
import org.holoeasy.packet.IPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0000\bf\u0018\u00002\u00020\u0001J$\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\b\u0010\u0006\u001a\u0004\u0018\u00010\u00072\b\b\u0002\u0010\b\u001a\u00020\tH&\u00a8\u0006\n"}, d2={"Lorg/holoeasy/packet/metadata/text/IMetadataTextPacket;", "Lorg/holoeasy/packet/IPacket;", "metadata", "Lcom/comphenix/protocol/events/PacketContainer;", "entityId", "", "nameTag", "", "invisible", "", "holoeasy-core"})
public interface IMetadataTextPacket
extends IPacket {
    @NotNull
    public PacketContainer metadata(int var1, @Nullable String var2, boolean var3);

    @Metadata(mv={1, 9, 0}, k=3, xi=48)
    public static final class DefaultImpls {
        public static /* synthetic */ PacketContainer metadata$default(IMetadataTextPacket iMetadataTextPacket, int n, String string, boolean bl, int n2, Object object) {
            if (object != null) {
                throw new UnsupportedOperationException("Super calls with default arguments not supported in this target, function: metadata");
            }
            if ((n2 & 4) != 0) {
                bl = true;
            }
            return iMetadataTextPacket.metadata(n, string, bl);
        }

        public static boolean isCurrentVersion(@NotNull IMetadataTextPacket iMetadataTextPacket) {
            return IPacket.DefaultImpls.isCurrentVersion(iMetadataTextPacket);
        }
    }
}

