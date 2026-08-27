/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package org.holoeasy.pool;

import java.util.UUID;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.bukkit.plugin.Plugin;
import org.holoeasy.builder.HologramBuilder;
import org.holoeasy.builder.interfaces.HologramRegisterGroup;
import org.holoeasy.hologram.Hologram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\bf\u0018\u00002\u00020\u0001J\u0010\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH&J\u0010\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\rH\u0016J\u0012\u0010\u000e\u001a\u0004\u0018\u00010\u00072\u0006\u0010\b\u001a\u00020\tH&J\u0010\u0010\u000f\u001a\u00020\u000b2\u0006\u0010\u0010\u001a\u00020\u0007H&R\u0012\u0010\u0002\u001a\u00020\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0004\u0010\u0005\u00a8\u0006\u0011"}, d2={"Lorg/holoeasy/pool/IHologramPool;", "", "plugin", "Lorg/bukkit/plugin/Plugin;", "getPlugin", "()Lorg/bukkit/plugin/Plugin;", "get", "Lorg/holoeasy/hologram/Hologram;", "id", "Ljava/util/UUID;", "registerHolograms", "", "registerGroup", "Lorg/holoeasy/builder/interfaces/HologramRegisterGroup;", "remove", "takeCareOf", "value", "holoeasy-core"})
public interface IHologramPool {
    @NotNull
    public Plugin getPlugin();

    @NotNull
    public Hologram get(@NotNull UUID var1);

    public void takeCareOf(@NotNull Hologram var1);

    @Nullable
    public Hologram remove(@NotNull UUID var1);

    public void registerHolograms(@NotNull HologramRegisterGroup var1);

    @Metadata(mv={1, 9, 0}, k=3, xi=48)
    public static final class DefaultImpls {
        public static void registerHolograms(@NotNull IHologramPool iHologramPool, @NotNull HologramRegisterGroup hologramRegisterGroup) {
            Intrinsics.checkNotNullParameter(hologramRegisterGroup, "registerGroup");
            HologramBuilder.registerHolograms(iHologramPool, hologramRegisterGroup);
        }
    }
}

