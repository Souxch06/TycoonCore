package org.holoeasy.packet.spawn;

import com.comphenix.protocol.events.PacketContainer;
import kotlin.Metadata;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.holoeasy.packet.IPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J,\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u000bH&\u00a8\u0006\f"}, d2={"Lorg/holoeasy/packet/spawn/ISpawnPacket;", "Lorg/holoeasy/packet/IPacket;", "spawn", "Lcom/comphenix/protocol/events/PacketContainer;", "entityId", "", "entityType", "Lorg/bukkit/entity/EntityType;", "location", "Lorg/bukkit/Location;", "plugin", "Lorg/bukkit/plugin/Plugin;", "holoeasy-core"})
public interface ISpawnPacket
extends IPacket {
    @NotNull
    public PacketContainer spawn(int var1, @NotNull EntityType var2, @NotNull Location var3, @Nullable Plugin var4);

    @Metadata(mv={1, 9, 0}, k=3, xi=48)
    public static final class DefaultImpls {
        public static /* synthetic */ PacketContainer spawn$default(ISpawnPacket iSpawnPacket, int n, EntityType entityType, Location location, Plugin plugin, int n2, Object object) {
            if (object != null) {
                throw new UnsupportedOperationException("Super calls with default arguments not supported in this target, function: spawn");
            }
            if ((n2 & 8) != 0) {
                plugin = null;
            }
            return iSpawnPacket.spawn(n, entityType, location, plugin);
        }

        public static boolean isCurrentVersion(@NotNull ISpawnPacket iSpawnPacket) {
            return IPacket.DefaultImpls.isCurrentVersion(iSpawnPacket);
        }
    }
}

