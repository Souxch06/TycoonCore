/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.comphenix.protocol.events.PacketContainer
 */
package org.holoeasy.packet.velocity;

import com.comphenix.protocol.events.PacketContainer;
import kotlin.Metadata;
import org.holoeasy.packet.IPacket;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\bf\u0018\u00002\u00020\u0001J(\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00052\u0006\u0010\u0007\u001a\u00020\u00052\u0006\u0010\b\u001a\u00020\u0005H&\u00a8\u0006\t"}, d2={"Lorg/holoeasy/packet/velocity/IVelocityPacket;", "Lorg/holoeasy/packet/IPacket;", "velocity", "Lcom/comphenix/protocol/events/PacketContainer;", "entityId", "", "x", "y", "z", "holoeasy-core"})
public interface IVelocityPacket
extends IPacket {
    @NotNull
    public PacketContainer velocity(int var1, int var2, int var3, int var4);

    @Metadata(mv={1, 9, 0}, k=3, xi=48)
    public static final class DefaultImpls {
        public static boolean isCurrentVersion(@NotNull IVelocityPacket iVelocityPacket) {
            return IPacket.DefaultImpls.isCurrentVersion(iVelocityPacket);
        }
    }
}

