/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 */
package org.holoeasy.hologram;

import java.util.Collection;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.hologram.IHologramLoader;
import org.holoeasy.line.ILine;
import org.holoeasy.line.ITextLine;
import org.holoeasy.line.TextLine;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J)\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0012\u0010\u0007\u001a\u000e\u0012\n\b\u0001\u0012\u0006\u0012\u0002\b\u00030\t0\bH\u0016\u00a2\u0006\u0002\u0010\nJ1\u0010\u000b\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0012\u0010\u0007\u001a\u000e\u0012\n\b\u0001\u0012\u0006\u0012\u0002\b\u00030\t0\b2\u0006\u0010\f\u001a\u00020\rH\u0002\u00a2\u0006\u0002\u0010\u000eJ\u0010\u0010\u000f\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0016\u00a8\u0006\u0010"}, d2={"Lorg/holoeasy/hologram/TextSequentialLoader;", "Lorg/holoeasy/hologram/IHologramLoader;", "()V", "load", "", "hologram", "Lorg/holoeasy/hologram/Hologram;", "lines", "", "Lorg/holoeasy/line/ILine;", "(Lorg/holoeasy/hologram/Hologram;[Lorg/holoeasy/line/ILine;)V", "set", "add", "", "(Lorg/holoeasy/hologram/Hologram;[Lorg/holoeasy/line/ILine;Z)V", "teleport", "holoeasy-core"})
@ApiStatus.Experimental
@SourceDebugExtension(value={"SMAP\nTextSequentialLoader.kt\nKotlin\n*S Kotlin\n*F\n+ 1 TextSequentialLoader.kt\norg/holoeasy/hologram/TextSequentialLoader\n+ 2 ArraysJVM.kt\nkotlin/collections/ArraysKt__ArraysJVMKt\n+ 3 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,42:1\n37#2,2:43\n1855#3,2:45\n*S KotlinDebug\n*F\n+ 1 TextSequentialLoader.kt\norg/holoeasy/hologram/TextSequentialLoader\n*L\n15#1:43,2\n32#1:45,2\n*E\n"})
public final class TextSequentialLoader
implements IHologramLoader {
    @Override
    public void load(@NotNull Hologram hologram, @NotNull ILine<?>[] iLineArray) {
        Intrinsics.checkNotNullParameter(hologram, "hologram");
        Intrinsics.checkNotNullParameter(iLineArray, "lines");
        this.set(hologram, iLineArray, true);
    }

    @Override
    public void teleport(@NotNull Hologram hologram) {
        Intrinsics.checkNotNullParameter(hologram, "hologram");
        Collection collection = hologram.getLines();
        boolean bl = false;
        Collection collection2 = collection;
        this.set(hologram, collection2.toArray(new ILine[0]), false);
    }

    private final void set(Hologram hologram, ILine<?>[] iLineArray, boolean bl) {
        Location location = hologram.getLocation().clone();
        Intrinsics.checkNotNullExpressionValue(location, "clone(...)");
        Location location2 = location;
        block3: for (ILine<?> iLine : iLineArray) {
            switch (WhenMappings.$EnumSwitchMapping$0[iLine.getType().ordinal()]) {
                case 1: 
                case 2: {
                    Intrinsics.checkNotNull(iLine, "null cannot be cast to non-null type org.holoeasy.line.ITextLine");
                    TextLine textLine = ((ITextLine)iLine).getTextLine();
                    Location location3 = location2.clone();
                    Intrinsics.checkNotNullExpressionValue(location3, "clone(...)");
                    textLine.setLocation(location3);
                    if (bl) {
                        hologram.getLines().add(0, textLine);
                    } else {
                        Iterable iterable = hologram.getSeeingPlayers();
                        boolean bl2 = false;
                        for (Object t : iterable) {
                            Player player = (Player)t;
                            boolean bl3 = false;
                            textLine.teleport(player);
                        }
                    }
                    location2.setZ(location2.getZ() + 0.175 * (double)textLine.getObj().length());
                    continue block3;
                }
                default: {
                    throw new RuntimeException("This method load supports only TextLine & TextALine & ClickableTextLine.");
                }
            }
        }
    }

    @Metadata(mv={1, 9, 0}, k=3, xi=48)
    public final class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] nArray = new int[ILine.Type.values().length];
            try {
                nArray[ILine.Type.TEXT_LINE.ordinal()] = 1;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[ILine.Type.CLICKABLE_TEXT_LINE.ordinal()] = 2;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            $EnumSwitchMapping$0 = nArray;
        }
    }
}

