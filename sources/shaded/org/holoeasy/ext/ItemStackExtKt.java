package org.holoeasy.ext;

import com.comphenix.protocol.wrappers.BukkitConverters;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=2, xi=48, d1={"\u0000\f\n\u0000\n\u0002\u0010\u0000\n\u0002\u0018\u0002\n\u0000\u001a\f\u0010\u0000\u001a\u00020\u0001*\u00020\u0002H\u0000\u00a8\u0006\u0003"}, d2={"bukkitGeneric", "", "Lorg/bukkit/inventory/ItemStack;", "holoeasy-core"})
public final class ItemStackExtKt {
    @NotNull
    public static final Object bukkitGeneric(@NotNull ItemStack itemStack) {
        Intrinsics.checkNotNullParameter(itemStack, "<this>");
        Object object = BukkitConverters.getItemStackConverter().getGeneric((Object)itemStack);
        Intrinsics.checkNotNullExpressionValue(object, "getGeneric(...)");
        return object;
    }
}

