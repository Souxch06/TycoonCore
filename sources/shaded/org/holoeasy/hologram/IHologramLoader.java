package org.holoeasy.hologram;

import kotlin.Metadata;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.line.ILine;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J)\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0012\u0010\u0006\u001a\u000e\u0012\n\b\u0001\u0012\u0006\u0012\u0002\b\u00030\b0\u0007H&\u00a2\u0006\u0002\u0010\tJ\u0010\u0010\n\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&\u00a8\u0006\u000b"}, d2={"Lorg/holoeasy/hologram/IHologramLoader;", "", "load", "", "hologram", "Lorg/holoeasy/hologram/Hologram;", "lines", "", "Lorg/holoeasy/line/ILine;", "(Lorg/holoeasy/hologram/Hologram;[Lorg/holoeasy/line/ILine;)V", "teleport", "holoeasy-core"})
public interface IHologramLoader {
    public void load(@NotNull Hologram var1, @NotNull ILine<?>[] var2);

    public void teleport(@NotNull Hologram var1);
}

