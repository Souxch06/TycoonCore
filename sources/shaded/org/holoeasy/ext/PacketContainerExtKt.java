/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.comphenix.protocol.ProtocolLibrary
 *  com.comphenix.protocol.events.PacketContainer
 *  com.comphenix.protocol.utility.MinecraftVersion
 *  com.comphenix.protocol.wrappers.WrappedDataValue
 *  com.comphenix.protocol.wrappers.WrappedDataWatcher
 *  com.comphenix.protocol.wrappers.WrappedDataWatcher$WrappedDataWatcherObject
 *  com.comphenix.protocol.wrappers.WrappedWatchableObject
 *  com.google.common.collect.Lists
 *  org.bukkit.entity.Player
 */
package org.holoeasy.ext;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=2, xi=48, d1={"\u0000\u001c\n\u0000\n\u0002\u0010\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a\u0015\u0010\u0000\u001a\u00020\u0001*\u00020\u00022\u0006\u0010\u0003\u001a\u00020\u0004H\u0080\u0002\u001a\u0014\u0010\u0005\u001a\u00020\u0001*\u00020\u00022\u0006\u0010\u0006\u001a\u00020\u0007H\u0000\u001a\u0014\u0010\b\u001a\u00020\u0001*\u00020\u00022\u0006\u0010\u0003\u001a\u00020\u0004H\u0000\u00a8\u0006\t"}, d2={"invoke", "", "Lcom/comphenix/protocol/events/PacketContainer;", "player", "Lorg/bukkit/entity/Player;", "parse119", "watcher", "Lcom/comphenix/protocol/wrappers/WrappedDataWatcher;", "send", "holoeasy-core"})
public final class PacketContainerExtKt {
    public static final void send(@NotNull PacketContainer packetContainer, @NotNull Player player) {
        Intrinsics.checkNotNullParameter(packetContainer, "<this>");
        Intrinsics.checkNotNullParameter(player, "player");
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packetContainer);
    }

    public static final void invoke(@NotNull PacketContainer packetContainer, @NotNull Player player) {
        Intrinsics.checkNotNullParameter(packetContainer, "<this>");
        Intrinsics.checkNotNullParameter(player, "player");
        PacketContainerExtKt.send(packetContainer, player);
    }

    public static final void parse119(@NotNull PacketContainer packetContainer, @NotNull WrappedDataWatcher wrappedDataWatcher) {
        Intrinsics.checkNotNullParameter(packetContainer, "<this>");
        Intrinsics.checkNotNullParameter(wrappedDataWatcher, "watcher");
        if (MinecraftVersion.getCurrentVersion().isAtLeast(new MinecraftVersion("1.19.3"))) {
            ArrayList arrayList = Lists.newArrayList();
            Intrinsics.checkNotNullExpressionValue(arrayList, "newArrayList(...)");
            List list = arrayList;
            wrappedDataWatcher.getWatchableObjects().stream().filter(Objects::nonNull).forEach(arg_0 -> PacketContainerExtKt.parse119$lambda$0(new Function1<WrappedWatchableObject, Unit>((List<WrappedDataValue>)list){
                final /* synthetic */ List<WrappedDataValue> $wrappedDataValueList;
                {
                    this.$wrappedDataValueList = list;
                    super(1);
                }

                public final void invoke(WrappedWatchableObject wrappedWatchableObject) {
                    WrappedDataWatcher.WrappedDataWatcherObject wrappedDataWatcherObject = wrappedWatchableObject.getWatcherObject();
                    Intrinsics.checkNotNullExpressionValue(wrappedDataWatcherObject, "getWatcherObject(...)");
                    WrappedDataWatcher.WrappedDataWatcherObject wrappedDataWatcherObject2 = wrappedDataWatcherObject;
                    this.$wrappedDataValueList.add(new WrappedDataValue(wrappedDataWatcherObject2.getIndex(), wrappedDataWatcherObject2.getSerializer(), wrappedWatchableObject.getRawValue()));
                }
            }, arg_0));
            packetContainer.getDataValueCollectionModifier().write(0, (Object)list);
        } else {
            packetContainer.getWatchableCollectionModifier().write(0, (Object)wrappedDataWatcher.getWatchableObjects());
        }
    }

    private static final void parse119$lambda$0(Function1 function1, Object object) {
        Intrinsics.checkNotNullParameter(function1, "$tmp0");
        function1.invoke(object);
    }
}

