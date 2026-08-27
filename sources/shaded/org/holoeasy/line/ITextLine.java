/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package org.holoeasy.line;

import java.util.Collection;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.bukkit.entity.Player;
import org.holoeasy.animation.Animations;
import org.holoeasy.line.ClickEvent;
import org.holoeasy.line.ILine;
import org.holoeasy.line.TextLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0011\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001J\u0010\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012H&J\u0010\u0010\u0013\u001a\u00020\u00022\u0006\u0010\u0014\u001a\u00020\u0015H&R\u0018\u0010\u0003\u001a\b\u0012\u0002\b\u0003\u0018\u00010\u0004X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0005\u0010\u0006R\u0012\u0010\u0007\u001a\u00020\bX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\t\u0010\nR\u0012\u0010\u000b\u001a\u00020\fX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\r\u0010\u000e\u00a8\u0006\u0016"}, d2={"Lorg/holoeasy/line/ITextLine;", "Lorg/holoeasy/line/ILine;", "", "args", "", "getArgs", "()[Ljava/lang/Object;", "clickable", "", "getClickable", "()Z", "textLine", "Lorg/holoeasy/line/TextLine;", "getTextLine", "()Lorg/holoeasy/line/TextLine;", "onClick", "", "clickEvent", "Lorg/holoeasy/line/ClickEvent;", "parse", "player", "Lorg/bukkit/entity/Player;", "holoeasy-core"})
public interface ITextLine
extends ILine<String> {
    public boolean getClickable();

    @NotNull
    public TextLine getTextLine();

    @Nullable
    public Object[] getArgs();

    @NotNull
    public String parse(@NotNull Player var1);

    public void onClick(@NotNull ClickEvent var1);

    @Metadata(mv={1, 9, 0}, k=3, xi=48)
    public static final class DefaultImpls {
        public static void update(@NotNull ITextLine iTextLine, @NotNull Collection<? extends Player> collection) {
            Intrinsics.checkNotNullParameter(collection, "seeingPlayers");
            ILine.DefaultImpls.update(iTextLine, collection);
        }

        public static void setAnimation(@NotNull ITextLine iTextLine, @NotNull Animations animations) {
            Intrinsics.checkNotNullParameter((Object)animations, "animation");
            ILine.DefaultImpls.setAnimation(iTextLine, animations);
        }

        public static void cancelAnimation(@NotNull ITextLine iTextLine) {
            ILine.DefaultImpls.cancelAnimation(iTextLine);
        }
    }
}

