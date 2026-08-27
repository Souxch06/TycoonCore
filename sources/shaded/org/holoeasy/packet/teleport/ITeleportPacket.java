/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.comphenix.protocol.events.PacketContainer
 *  org.bukkit.Location
 */
package org.holoeasy.packet.teleport;

import com.comphenix.protocol.events.PacketContainer;
import kotlin.Metadata;
import org.bukkit.Location;
import org.holoeasy.packet.IPacket;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H&\u00a8\u0006\b"}, d2={"Lorg/holoeasy/packet/teleport/ITeleportPacket;", "Lorg/holoeasy/packet/IPacket;", "teleport", "Lcom/comphenix/protocol/events/PacketContainer;", "entityId", "", "location", "Lorg/bukkit/Location;", "holoeasy-core"})
public interface ITeleportPacket
extends IPacket {
    @NotNull
    public PacketContainer teleport(int var1, @NotNull Location var2);

    @Metadata(mv={1, 9, 0}, k=3, xi=48)
    public static final class DefaultImpls {
        public static boolean isCurrentVersion(@NotNull ITeleportPacket iTeleportPacket) {
            return IPacket.DefaultImpls.isCurrentVersion(iTeleportPacket);
        }
    }
}

