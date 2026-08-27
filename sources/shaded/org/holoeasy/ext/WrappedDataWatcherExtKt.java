package org.holoeasy.ext;

import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import java.util.Optional;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.bukkit.inventory.ItemStack;
import org.holoeasy.ext.ItemStackExtKt;
import org.holoeasy.util.SerializersKt;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=2, xi=48, d1={"\u00002\n\u0000\n\u0002\u0010\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0005\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0000\n\u0000\u001a\u001c\u0010\u0000\u001a\u00020\u0001*\u00020\u00022\u0006\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0000\u001a\u001c\u0010\u0007\u001a\u00020\u0001*\u00020\u00022\u0006\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\bH\u0000\u001a\u001c\u0010\t\u001a\u00020\u0001*\u00020\u00022\u0006\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\nH\u0000\u001a\u001c\u0010\u000b\u001a\u00020\u0001*\u00020\u00022\u0006\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\fH\u0000\u001a\u001c\u0010\r\u001a\u00020\u0001*\u00020\u00022\u0006\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\nH\u0000\u001a\u001c\u0010\u000e\u001a\u00020\u0001*\u00020\u00022\u0006\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u000fH\u0000\u00a8\u0006\u0010"}, d2={"setBool", "", "Lcom/comphenix/protocol/wrappers/WrappedDataWatcher;", "index", "", "value", "", "setByte", "", "setChatComponent", "", "setItemStack", "Lorg/bukkit/inventory/ItemStack;", "setString", "setVectorSerializer", "", "holoeasy-core"})
public final class WrappedDataWatcherExtKt {
    public static final void setByte(@NotNull WrappedDataWatcher wrappedDataWatcher, int n, byte by) {
        Intrinsics.checkNotNullParameter(wrappedDataWatcher, "<this>");
        WrappedDataWatcher.WrappedDataWatcherObject wrappedDataWatcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(n, SerializersKt.getBYTE_SERIALIZER());
        wrappedDataWatcher.setObject(wrappedDataWatcherObject, (Object)by);
    }

    public static final void setString(@NotNull WrappedDataWatcher wrappedDataWatcher, int n, @NotNull String string) {
        Intrinsics.checkNotNullParameter(wrappedDataWatcher, "<this>");
        Intrinsics.checkNotNullParameter(string, "value");
        wrappedDataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(n, SerializersKt.getSTRING_SERIALIZER()), (Object)string);
    }

    public static final void setBool(@NotNull WrappedDataWatcher wrappedDataWatcher, int n, boolean bl) {
        Intrinsics.checkNotNullParameter(wrappedDataWatcher, "<this>");
        WrappedDataWatcher.WrappedDataWatcherObject wrappedDataWatcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(n, SerializersKt.getBOOL_SERIALIZER());
        wrappedDataWatcher.setObject(wrappedDataWatcherObject, (Object)bl);
    }

    public static final void setVectorSerializer(@NotNull WrappedDataWatcher wrappedDataWatcher, int n, @NotNull Object object) {
        Intrinsics.checkNotNullParameter(wrappedDataWatcher, "<this>");
        Intrinsics.checkNotNullParameter(object, "value");
        WrappedDataWatcher.WrappedDataWatcherObject wrappedDataWatcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(n, WrappedDataWatcher.Registry.getVectorSerializer());
        wrappedDataWatcher.setObject(wrappedDataWatcherObject, object);
    }

    public static final void setChatComponent(@NotNull WrappedDataWatcher wrappedDataWatcher, int n, @NotNull String string) {
        Intrinsics.checkNotNullParameter(wrappedDataWatcher, "<this>");
        Intrinsics.checkNotNullParameter(string, "value");
        Optional<Object> optional = Optional.of(WrappedChatComponent.fromChatMessage((String)string)[0].getHandle());
        Intrinsics.checkNotNullExpressionValue(optional, "of(...)");
        Optional<Object> optional2 = optional;
        wrappedDataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(n, WrappedDataWatcher.Registry.getChatComponentSerializer((boolean)true)), optional2);
    }

    public static final void setItemStack(@NotNull WrappedDataWatcher wrappedDataWatcher, int n, @NotNull ItemStack itemStack) {
        Intrinsics.checkNotNullParameter(wrappedDataWatcher, "<this>");
        Intrinsics.checkNotNullParameter(itemStack, "value");
        WrappedDataWatcher.WrappedDataWatcherObject wrappedDataWatcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(n, SerializersKt.getITEM_SERIALIZER());
        wrappedDataWatcher.setObject(wrappedDataWatcherObject, ItemStackExtKt.bukkitGeneric(itemStack));
    }
}

