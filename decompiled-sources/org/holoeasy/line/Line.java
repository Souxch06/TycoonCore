/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.comphenix.protocol.events.PacketContainer
 *  org.bukkit.Location
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package org.holoeasy.line;

import com.comphenix.protocol.events.PacketContainer;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.holoeasy.ext.PacketContainerExtKt;
import org.holoeasy.packet.PacketType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\t\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u0000 \u001b2\u00020\u0001:\u0001\u001bB!\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\u0002\u0010\bJ\u000e\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0018J\u000e\u0010\u0019\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0018J\u000e\u0010\u001a\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0018R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000f\u0010\u0010\"\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014\u00a8\u0006\u001c"}, d2={"Lorg/holoeasy/line/Line;", "", "plugin", "Lorg/bukkit/plugin/Plugin;", "entityType", "Lorg/bukkit/entity/EntityType;", "location", "Lorg/bukkit/Location;", "(Lorg/bukkit/plugin/Plugin;Lorg/bukkit/entity/EntityType;Lorg/bukkit/Location;)V", "entityDestroyPacket", "Lcom/comphenix/protocol/events/PacketContainer;", "entityID", "", "getEntityID", "()I", "getLocation", "()Lorg/bukkit/Location;", "setLocation", "(Lorg/bukkit/Location;)V", "getPlugin", "()Lorg/bukkit/plugin/Plugin;", "destroy", "", "player", "Lorg/bukkit/entity/Player;", "spawn", "teleport", "Companion", "holoeasy-core"})
public final class Line {
    @NotNull
    public static final Companion Companion = new Companion(null);
    @NotNull
    private final Plugin plugin;
    @NotNull
    private final EntityType entityType;
    @Nullable
    private Location location;
    private final int entityID;
    @NotNull
    private final PacketContainer entityDestroyPacket;
    @NotNull
    private static final AtomicInteger IDs_COUNTER = new AtomicInteger(new Random().nextInt());

    public Line(@NotNull Plugin plugin, @NotNull EntityType entityType, @Nullable Location location) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        Intrinsics.checkNotNullParameter(entityType, "entityType");
        this.plugin = plugin;
        this.entityType = entityType;
        this.location = location;
        this.entityID = IDs_COUNTER.getAndIncrement();
        this.entityDestroyPacket = PacketType.INSTANCE.getDELETE().delete(this.entityID);
    }

    public /* synthetic */ Line(Plugin plugin, EntityType entityType, Location location, int n, DefaultConstructorMarker defaultConstructorMarker) {
        if ((n & 4) != 0) {
            location = null;
        }
        this(plugin, entityType, location);
    }

    @NotNull
    public final Plugin getPlugin() {
        return this.plugin;
    }

    @Nullable
    public final Location getLocation() {
        return this.location;
    }

    public final void setLocation(@Nullable Location location) {
        this.location = location;
    }

    public final int getEntityID() {
        return this.entityID;
    }

    public final void destroy(@NotNull Player player) {
        Intrinsics.checkNotNullParameter(player, "player");
        PacketContainerExtKt.invoke(this.entityDestroyPacket, player);
    }

    public final void spawn(@NotNull Player player) {
        Intrinsics.checkNotNullParameter(player, "player");
        Location location = this.location;
        if (location == null) {
            throw new RuntimeException("Forgot the location?");
        }
        PacketContainer packetContainer = PacketType.INSTANCE.getSPAWN().spawn(this.entityID, this.entityType, location, this.plugin);
        PacketContainerExtKt.invoke(packetContainer, player);
    }

    public final void teleport(@NotNull Player player) {
        Intrinsics.checkNotNullParameter(player, "player");
        Location location = this.location;
        if (location == null) {
            throw new RuntimeException("Forgot the location?");
        }
        PacketContainer packetContainer = PacketType.INSTANCE.getTELEPORT().teleport(this.entityID, location);
        PacketContainerExtKt.invoke(packetContainer, player);
    }

    @Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2={"Lorg/holoeasy/line/Line$Companion;", "", "()V", "IDs_COUNTER", "Ljava/util/concurrent/atomic/AtomicInteger;", "getIDs_COUNTER", "()Ljava/util/concurrent/atomic/AtomicInteger;", "holoeasy-core"})
    public static final class Companion {
        private Companion() {
        }

        @NotNull
        public final AtomicInteger getIDs_COUNTER() {
            return IDs_COUNTER;
        }

        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }
    }
}

