package org.holoeasy.util;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=2, xi=48, d1={"\u0000\n\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\"\u0014\u0010\u0000\u001a\u00020\u00018@X\u0080\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0002\u0010\u0003\"\u0014\u0010\u0004\u001a\u00020\u00018@X\u0080\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0005\u0010\u0003\"\u0014\u0010\u0006\u001a\u00020\u00018@X\u0080\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\u0003\"\u0014\u0010\b\u001a\u00020\u00018@X\u0080\u0004\u00a2\u0006\u0006\u001a\u0004\b\t\u0010\u0003\u00a8\u0006\n"}, d2={"BOOL_SERIALIZER", "Lcom/comphenix/protocol/wrappers/WrappedDataWatcher$Serializer;", "getBOOL_SERIALIZER", "()Lcom/comphenix/protocol/wrappers/WrappedDataWatcher$Serializer;", "BYTE_SERIALIZER", "getBYTE_SERIALIZER", "ITEM_SERIALIZER", "getITEM_SERIALIZER", "STRING_SERIALIZER", "getSTRING_SERIALIZER", "holoeasy-core"})
public final class SerializersKt {
    @NotNull
    public static final WrappedDataWatcher.Serializer getBYTE_SERIALIZER() {
        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Byte.class);
        Intrinsics.checkNotNullExpressionValue(serializer, "get(...)");
        return serializer;
    }

    @NotNull
    public static final WrappedDataWatcher.Serializer getBOOL_SERIALIZER() {
        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Boolean.class);
        Intrinsics.checkNotNullExpressionValue(serializer, "get(...)");
        return serializer;
    }

    @NotNull
    public static final WrappedDataWatcher.Serializer getSTRING_SERIALIZER() {
        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(String.class);
        Intrinsics.checkNotNullExpressionValue(serializer, "get(...)");
        return serializer;
    }

    @NotNull
    public static final WrappedDataWatcher.Serializer getITEM_SERIALIZER() {
        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.getItemStackSerializer((boolean)false);
        Intrinsics.checkNotNullExpressionValue(serializer, "getItemStackSerializer(...)");
        return serializer;
    }
}

