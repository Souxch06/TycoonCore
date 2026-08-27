/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.comphenix.protocol.events.PacketContainer
 */
package org.holoeasy.packet.delete;

import com.comphenix.protocol.events.PacketContainer;
import kotlin.Metadata;
import org.holoeasy.packet.IPacket;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\bf\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&\u00a8\u0006\u0006"}, d2={"Lorg/holoeasy/packet/delete/IDeletePacket;", "Lorg/holoeasy/packet/IPacket;", "delete", "Lcom/comphenix/protocol/events/PacketContainer;", "entityId", "", "holoeasy-core"})
public interface IDeletePacket
extends IPacket {
    @NotNull
    public PacketContainer delete(int var1);

    @Metadata(mv={1, 9, 0}, k=3, xi=48)
    public static final class DefaultImpls {
        public static boolean isCurrentVersion(@NotNull IDeletePacket iDeletePacket) {
            return IPacket.DefaultImpls.isCurrentVersion(iDeletePacket);
        }
    }
}

