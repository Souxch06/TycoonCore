package org.holoeasy.animation;

import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.jvm.functions.Function1;
import org.bukkit.scheduler.BukkitTask;
import org.holoeasy.line.ILine;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u001f\b\u0002\u0012\u0016\u0010\u0002\u001a\u0012\u0012\b\u0012\u0006\u0012\u0002\b\u00030\u0004\u0012\u0004\u0012\u00020\u00050\u0003\u00a2\u0006\u0002\u0010\u0006R!\u0010\u0002\u001a\u0012\u0012\b\u0012\u0006\u0012\u0002\b\u00030\u0004\u0012\u0004\u0012\u00020\u00050\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bj\u0002\b\t\u00a8\u0006\n"}, d2={"Lorg/holoeasy/animation/Animations;", "", "task", "Lkotlin/Function1;", "Lorg/holoeasy/line/ILine;", "Lorg/bukkit/scheduler/BukkitTask;", "(Ljava/lang/String;ILkotlin/jvm/functions/Function1;)V", "getTask", "()Lkotlin/jvm/functions/Function1;", "CIRCLE", "holoeasy-core"})
public final class Animations
extends Enum<Animations> {
    @NotNull
    private final Function1<ILine<?>, BukkitTask> task;
    public static final /* enum */ Animations CIRCLE = new Animations(1.INSTANCE);
    private static final /* synthetic */ Animations[] $VALUES;
    private static final /* synthetic */ EnumEntries $ENTRIES;

    private Animations(Function1<? super ILine<?>, ? extends BukkitTask> function1) {
        this.task = function1;
    }

    @NotNull
    public final Function1<ILine<?>, BukkitTask> getTask() {
        return this.task;
    }

    public static Animations[] values() {
        return (Animations[])$VALUES.clone();
    }

    public static Animations valueOf(String string) {
        return Enum.valueOf(Animations.class, string);
    }

    @NotNull
    public static EnumEntries<Animations> getEntries() {
        return $ENTRIES;
    }

    static {
        $VALUES = animationsArray = new Animations[]{Animations.CIRCLE};
        $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
    }
}

