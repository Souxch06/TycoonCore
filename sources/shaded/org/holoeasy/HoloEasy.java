package org.holoeasy;

import kotlin.Metadata;
import kotlin.jvm.JvmField;
import kotlin.jvm.JvmOverloads;
import kotlin.jvm.JvmStatic;
import kotlin.jvm.internal.Intrinsics;
import org.bukkit.plugin.Plugin;
import org.holoeasy.action.ClickAction;
import org.holoeasy.pool.HologramPool;
import org.holoeasy.pool.IHologramPool;
import org.holoeasy.pool.InteractiveHologramPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J8\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010\r\u001a\u00020\f2\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u0007J\u0018\u0010\u0010\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH\u0007R\u0012\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2={"Lorg/holoeasy/HoloEasy;", "", "()V", "useLastSupportedVersion", "", "startInteractivePool", "Lorg/holoeasy/pool/IHologramPool;", "plugin", "Lorg/bukkit/plugin/Plugin;", "spawnDistance", "", "minHitDistance", "", "maxHitDistance", "clickAction", "Lorg/holoeasy/action/ClickAction;", "startPool", "holoeasy-core"})
public final class HoloEasy {
    @NotNull
    public static final HoloEasy INSTANCE = new HoloEasy();
    @JvmField
    public static boolean useLastSupportedVersion;

    private HoloEasy() {
    }

    @JvmStatic
    @NotNull
    public static final IHologramPool startPool(@NotNull Plugin plugin, double d) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        HologramPool hologramPool = new HologramPool(plugin, d);
        return hologramPool;
    }

    @JvmStatic
    @JvmOverloads
    @NotNull
    public static final IHologramPool startInteractivePool(@NotNull Plugin plugin, double d, float f, float f2, @Nullable ClickAction clickAction) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        HologramPool hologramPool = new HologramPool(plugin, d);
        InteractiveHologramPool interactiveHologramPool = new InteractiveHologramPool(hologramPool, f, f2, clickAction);
        return interactiveHologramPool;
    }

    public static /* synthetic */ IHologramPool startInteractivePool$default(Plugin plugin, double d, float f, float f2, ClickAction clickAction, int n, Object object) {
        if ((n & 4) != 0) {
            f = 0.5f;
        }
        if ((n & 8) != 0) {
            f2 = 5.0f;
        }
        if ((n & 0x10) != 0) {
            clickAction = null;
        }
        return HoloEasy.startInteractivePool(plugin, d, f, f2, clickAction);
    }

    @JvmStatic
    @JvmOverloads
    @NotNull
    public static final IHologramPool startInteractivePool(@NotNull Plugin plugin, double d, float f, float f2) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        return HoloEasy.startInteractivePool$default(plugin, d, f, f2, null, 16, null);
    }

    @JvmStatic
    @JvmOverloads
    @NotNull
    public static final IHologramPool startInteractivePool(@NotNull Plugin plugin, double d, float f) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        return HoloEasy.startInteractivePool$default(plugin, d, f, 0.0f, null, 24, null);
    }

    @JvmStatic
    @JvmOverloads
    @NotNull
    public static final IHologramPool startInteractivePool(@NotNull Plugin plugin, double d) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        return HoloEasy.startInteractivePool$default(plugin, d, 0.0f, 0.0f, null, 28, null);
    }
}

