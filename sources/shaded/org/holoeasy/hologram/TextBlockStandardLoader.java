package org.holoeasy.hologram;

import java.util.List;
import kotlin.Metadata;
import kotlin.collections.ArraysKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.hologram.IHologramLoader;
import org.holoeasy.line.ILine;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J)\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\u0012\u0010\u000b\u001a\u000e\u0012\n\b\u0001\u0012\u0006\u0012\u0002\b\u00030\r0\fH\u0016\u00a2\u0006\u0002\u0010\u000eJ\u0010\u0010\u000f\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH\u0016J$\u0010\u0010\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u0011\u001a\u00020\u00042\n\u0010\u0012\u001a\u0006\u0012\u0002\b\u00030\rH\u0002R\u0014\u0010\u0003\u001a\u00020\u0004X\u0086D\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0013"}, d2={"Lorg/holoeasy/hologram/TextBlockStandardLoader;", "Lorg/holoeasy/hologram/IHologramLoader;", "()V", "LINE_HEIGHT", "", "getLINE_HEIGHT", "()D", "load", "", "hologram", "Lorg/holoeasy/hologram/Hologram;", "lines", "", "Lorg/holoeasy/line/ILine;", "(Lorg/holoeasy/hologram/Hologram;[Lorg/holoeasy/line/ILine;)V", "teleport", "teleportLine", "destY", "tempLine", "holoeasy-core"})
@SourceDebugExtension(value={"SMAP\nTextBlockStandardLoader.kt\nKotlin\n*S Kotlin\n*F\n+ 1 TextBlockStandardLoader.kt\norg/holoeasy/hologram/TextBlockStandardLoader\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,96:1\n1855#2,2:97\n*S KotlinDebug\n*F\n+ 1 TextBlockStandardLoader.kt\norg/holoeasy/hologram/TextBlockStandardLoader\n*L\n94#1:97,2\n*E\n"})
public final class TextBlockStandardLoader
implements IHologramLoader {
    private final double LINE_HEIGHT;

    public TextBlockStandardLoader() {
        this.LINE_HEIGHT = 0.28;
    }

    public final double getLINE_HEIGHT() {
        return this.LINE_HEIGHT;
    }

    @Override
    public void load(@NotNull Hologram hologram, @NotNull ILine<?>[] iLineArray) {
        Intrinsics.checkNotNullParameter(hologram, "hologram");
        Intrinsics.checkNotNullParameter(iLineArray, "lines");
        Location location = hologram.getLocation().clone();
        Intrinsics.checkNotNullExpressionValue(location, "clone(...)");
        Location location2 = location;
        if (iLineArray.length == 1) {
            ILine<?> iLine = iLineArray[0];
            iLine.setLocation(location2);
            hologram.getLines().add(iLine);
            return;
        }
        ArraysKt.reverse(iLineArray);
        location2.subtract(0.0, this.LINE_HEIGHT, 0.0);
        int n = iLineArray.length;
        block8: for (int i = 0; i < n; ++i) {
            ILine<?> iLine = iLineArray[i];
            double d = this.LINE_HEIGHT;
            if (i > 0) {
                switch (WhenMappings.$EnumSwitchMapping$0[iLineArray[i - 1].getType().ordinal()]) {
                    case 1: {
                        d = -1.5;
                        break;
                    }
                    case 2: {
                        d = -0.19;
                    }
                }
            }
            switch (WhenMappings.$EnumSwitchMapping$0[iLine.getType().ordinal()]) {
                case 2: 
                case 4: 
                case 5: {
                    Location location3 = location2.add(0.0, d, 0.0).clone();
                    Intrinsics.checkNotNullExpressionValue(location3, "clone(...)");
                    iLine.setLocation(location3);
                    hologram.getLines().add(0, iLine);
                    continue block8;
                }
                case 1: {
                    Location location4 = location2.add(0.0, 0.6, 0.0).clone();
                    Intrinsics.checkNotNullExpressionValue(location4, "clone(...)");
                    iLine.setLocation(location4);
                    hologram.getLines().add(0, iLine);
                    continue block8;
                }
                default: {
                    throw new RuntimeException("This method load does not support line type " + iLine.getType().name());
                }
            }
        }
    }

    @Override
    public void teleport(@NotNull Hologram hologram) {
        double d;
        Intrinsics.checkNotNullParameter(hologram, "hologram");
        List<ILine<?>> list = hologram.getLines();
        ILine<?> iLine = list.get(0);
        Location location = iLine.getLocation();
        if (location == null) {
            throw new RuntimeException("First line has not a location");
        }
        double d2 = location.getY();
        double d3 = hologram.getLocation().getY() - this.LINE_HEIGHT;
        switch (WhenMappings.$EnumSwitchMapping$0[iLine.getType().ordinal()]) {
            case 2: 
            case 4: 
            case 5: {
                d = this.LINE_HEIGHT;
                break;
            }
            default: {
                d = 0.6;
            }
        }
        this.teleportLine(hologram, d3 += d, iLine);
        ILine<?> iLine2 = null;
        int n = list.size();
        for (int i = 1; i < n; ++i) {
            iLine2 = list.get(i);
            Location location2 = iLine2.getLocation();
            if (location2 == null) {
                throw new RuntimeException("Missing location of line " + iLine2);
            }
            this.teleportLine(hologram, d3 + Math.abs(d2 - location2.getY()), iLine2);
        }
    }

    private final void teleportLine(Hologram hologram, double d, ILine<?> iLine) {
        Location location = hologram.getLocation().clone();
        Intrinsics.checkNotNullExpressionValue(location, "clone(...)");
        Location location2 = location;
        location2.setY(d);
        iLine.setLocation(location2);
        Iterable iterable = hologram.getSeeingPlayers();
        boolean bl = false;
        for (Object t : iterable) {
            Player player = (Player)t;
            boolean bl2 = false;
            iLine.teleport(player);
        }
    }

    @Metadata(mv={1, 9, 0}, k=3, xi=48)
    public final class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] nArray = new int[ILine.Type.values().length];
            try {
                nArray[ILine.Type.ITEM_LINE.ordinal()] = 1;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[ILine.Type.BLOCK_LINE.ordinal()] = 2;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[ILine.Type.EXTERNAL.ordinal()] = 3;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[ILine.Type.TEXT_LINE.ordinal()] = 4;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[ILine.Type.CLICKABLE_TEXT_LINE.ordinal()] = 5;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            $EnumSwitchMapping$0 = nArray;
        }
    }
}

