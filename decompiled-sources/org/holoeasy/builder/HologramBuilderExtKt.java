/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package org.holoeasy.builder;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.bukkit.plugin.Plugin;
import org.holoeasy.builder.HologramBuilder;
import org.holoeasy.builder.interfaces.HologramRegisterGroup;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=2, xi=48, d1={"\u0000\u0016\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a!\u0010\u0000\u001a\u00020\u0001\"\b\b\u0000\u0010\u0002*\u00020\u0003*\u0002H\u00022\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u0007"}, d2={"registerHolograms", "", "T", "Lorg/bukkit/plugin/Plugin;", "registerGroup", "Lorg/holoeasy/builder/interfaces/HologramRegisterGroup;", "(Lorg/bukkit/plugin/Plugin;Lorg/holoeasy/builder/interfaces/HologramRegisterGroup;)V", "holoeasy-core"})
public final class HologramBuilderExtKt {
    public static final <T extends Plugin> void registerHolograms(@NotNull T t, @NotNull HologramRegisterGroup hologramRegisterGroup) {
        Intrinsics.checkNotNullParameter(t, "<this>");
        Intrinsics.checkNotNullParameter(hologramRegisterGroup, "registerGroup");
        HologramBuilder.registerHolograms(t, hologramRegisterGroup);
    }
}

