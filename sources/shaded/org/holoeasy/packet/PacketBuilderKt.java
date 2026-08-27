package org.holoeasy.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=2, xi=48, d1={"\u0000\u001c\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\u0018\u0002\n\u0000\u001a'\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0017\u0010\u0004\u001a\u0013\u0012\u0004\u0012\u00020\u0001\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\u0002\b\u0007\u00a8\u0006\b"}, d2={"packet", "Lcom/comphenix/protocol/events/PacketContainer;", "type", "Lcom/comphenix/protocol/PacketType;", "initializer", "Lkotlin/Function1;", "", "Lkotlin/ExtensionFunctionType;", "holoeasy-core"})
public final class PacketBuilderKt {
    @NotNull
    public static final PacketContainer packet(@NotNull PacketType packetType, @NotNull Function1<? super PacketContainer, Unit> function1) {
        Intrinsics.checkNotNullParameter(packetType, "type");
        Intrinsics.checkNotNullParameter(function1, "initializer");
        PacketContainer packetContainer = new PacketContainer(packetType);
        function1.invoke((PacketContainer)packetContainer);
        return packetContainer;
    }
}

