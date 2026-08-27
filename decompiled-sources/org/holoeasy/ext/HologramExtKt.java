/*
 * Decompiled with CFR 0.152.
 */
package org.holoeasy.ext;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.line.ILine;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=2, xi=48, d1={"\u0000\u0012\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\u001a\u0019\u0010\u0000\u001a\u0006\u0012\u0002\b\u00030\u0001*\u00020\u00022\u0006\u0010\u0003\u001a\u00020\u0004H\u0086\u0002\u00a8\u0006\u0005"}, d2={"get", "Lorg/holoeasy/line/ILine;", "Lorg/holoeasy/hologram/Hologram;", "index", "", "holoeasy-core"})
public final class HologramExtKt {
    @NotNull
    public static final ILine<?> get(@NotNull Hologram hologram, int n) {
        Intrinsics.checkNotNullParameter(hologram, "<this>");
        return hologram.lineAt(n);
    }
}

