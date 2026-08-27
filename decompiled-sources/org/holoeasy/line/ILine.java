/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package org.holoeasy.line;

import java.util.Collection;
import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.jvm.internal.Intrinsics;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.holoeasy.animation.Animations;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.reactive.Observer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000T\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u001e\n\u0002\b\u0003\bf\u0018\u0000*\u0004\b\u0000\u0010\u00012\u00020\u0002:\u0002-.J\b\u0010\u001e\u001a\u00020\u001fH\u0016J\u0010\u0010 \u001a\u00020\u001f2\u0006\u0010!\u001a\u00020\"H&J\u0010\u0010#\u001a\u00020\u001f2\u0006\u0010$\u001a\u00020%H\u0016J\u0010\u0010&\u001a\u00020\u001f2\u0006\u0010'\u001a\u00020\bH&J\u0010\u0010(\u001a\u00020\u001f2\u0006\u0010!\u001a\u00020\"H&J\u0010\u0010)\u001a\u00020\u001f2\u0006\u0010!\u001a\u00020\"H&J\u0016\u0010*\u001a\u00020\u001f2\f\u0010+\u001a\b\u0012\u0004\u0012\u00020\"0,H\u0016J\u0010\u0010*\u001a\u00020\u001f2\u0006\u0010!\u001a\u00020\"H&R\u0012\u0010\u0003\u001a\u00020\u0004X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0005\u0010\u0006R\u0014\u0010\u0007\u001a\u0004\u0018\u00010\bX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\t\u0010\nR\u0018\u0010\u000b\u001a\u00028\u0000X\u00a6\u000e\u00a2\u0006\f\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u0012\u0010\u0010\u001a\u00020\u0011X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0012\u0010\u0013R\u0018\u0010\u0014\u001a\u00020\u0015X\u00a6\u000e\u00a2\u0006\f\u001a\u0004\b\u0016\u0010\u0017\"\u0004\b\u0018\u0010\u0019R\u0012\u0010\u001a\u001a\u00020\u001bX\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u001c\u0010\u001d\u00a8\u0006/"}, d2={"Lorg/holoeasy/line/ILine;", "T", "", "entityId", "", "getEntityId", "()I", "location", "Lorg/bukkit/Location;", "getLocation", "()Lorg/bukkit/Location;", "obj", "getObj", "()Ljava/lang/Object;", "setObj", "(Ljava/lang/Object;)V", "plugin", "Lorg/bukkit/plugin/Plugin;", "getPlugin", "()Lorg/bukkit/plugin/Plugin;", "pvt", "Lorg/holoeasy/line/ILine$PrivateConfig;", "getPvt", "()Lorg/holoeasy/line/ILine$PrivateConfig;", "setPvt", "(Lorg/holoeasy/line/ILine$PrivateConfig;)V", "type", "Lorg/holoeasy/line/ILine$Type;", "getType", "()Lorg/holoeasy/line/ILine$Type;", "cancelAnimation", "", "hide", "player", "Lorg/bukkit/entity/Player;", "setAnimation", "animation", "Lorg/holoeasy/animation/Animations;", "setLocation", "value", "show", "teleport", "update", "seeingPlayers", "", "PrivateConfig", "Type", "holoeasy-core"})
public interface ILine<T> {
    @NotNull
    public Plugin getPlugin();

    @NotNull
    public Type getType();

    public int getEntityId();

    @Nullable
    public Location getLocation();

    public T getObj();

    public void setObj(T var1);

    @NotNull
    public PrivateConfig getPvt();

    public void setPvt(@NotNull PrivateConfig var1);

    public void setLocation(@NotNull Location var1);

    public void hide(@NotNull Player var1);

    public void teleport(@NotNull Player var1);

    public void show(@NotNull Player var1);

    public void update(@NotNull Player var1);

    public void update(@NotNull Collection<? extends Player> var1);

    public void setAnimation(@NotNull Animations var1);

    public void cancelAnimation();

    @Metadata(mv={1, 9, 0}, k=3, xi=48)
    public static final class DefaultImpls {
        public static <T> void update(@NotNull ILine<T> iLine, @NotNull Collection<? extends Player> collection) {
            Intrinsics.checkNotNullParameter(collection, "seeingPlayers");
            for (Player player : collection) {
                iLine.update(player);
            }
        }

        public static <T> void setAnimation(@NotNull ILine<T> iLine, @NotNull Animations animations) {
            Intrinsics.checkNotNullParameter((Object)animations, "animation");
            iLine.cancelAnimation();
            iLine.getPvt().setAnimationTask(animations.getTask().invoke(iLine));
        }

        public static <T> void cancelAnimation(@NotNull ILine<T> iLine) {
            BukkitTask bukkitTask = iLine.getPvt().getAnimationTask();
            if (bukkitTask != null) {
                bukkitTask.cancel();
            }
            iLine.getPvt().setAnimationTask(null);
        }
    }

    @Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u0011\u0012\n\u0010\u0002\u001a\u0006\u0012\u0002\b\u00030\u0003\u00a2\u0006\u0002\u0010\u0004J\r\u0010\u0011\u001a\u0006\u0012\u0002\b\u00030\u0003H\u00c2\u0003J\u0017\u0010\u0012\u001a\u00020\u00002\f\b\u0002\u0010\u0002\u001a\u0006\u0012\u0002\b\u00030\u0003H\u00c6\u0001J\u0013\u0010\u0013\u001a\u00020\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u0016H\u00d6\u0003J\t\u0010\u0017\u001a\u00020\u0018H\u00d6\u0001J\b\u0010\u0019\u001a\u00020\u001aH\u0016J\t\u0010\u001b\u001a\u00020\u001cH\u00d6\u0001R\u001c\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0007\u0010\b\"\u0004\b\t\u0010\nR\u001a\u0010\u000b\u001a\u00020\fX\u0086.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010R\u0012\u0010\u0002\u001a\u0006\u0012\u0002\b\u00030\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001d"}, d2={"Lorg/holoeasy/line/ILine$PrivateConfig;", "Lorg/holoeasy/reactive/Observer;", "line", "Lorg/holoeasy/line/ILine;", "(Lorg/holoeasy/line/ILine;)V", "animationTask", "Lorg/bukkit/scheduler/BukkitTask;", "getAnimationTask", "()Lorg/bukkit/scheduler/BukkitTask;", "setAnimationTask", "(Lorg/bukkit/scheduler/BukkitTask;)V", "hologram", "Lorg/holoeasy/hologram/Hologram;", "getHologram", "()Lorg/holoeasy/hologram/Hologram;", "setHologram", "(Lorg/holoeasy/hologram/Hologram;)V", "component1", "copy", "equals", "", "other", "", "hashCode", "", "observerUpdate", "", "toString", "", "holoeasy-core"})
    public static final class PrivateConfig
    implements Observer {
        @NotNull
        private final ILine<?> line;
        public Hologram hologram;
        @Nullable
        private BukkitTask animationTask;

        public PrivateConfig(@NotNull ILine<?> iLine) {
            Intrinsics.checkNotNullParameter(iLine, "line");
            this.line = iLine;
        }

        @NotNull
        public final Hologram getHologram() {
            Hologram hologram = this.hologram;
            if (hologram != null) {
                return hologram;
            }
            Intrinsics.throwUninitializedPropertyAccessException("hologram");
            return null;
        }

        public final void setHologram(@NotNull Hologram hologram) {
            Intrinsics.checkNotNullParameter(hologram, "<set-?>");
            this.hologram = hologram;
        }

        @Nullable
        public final BukkitTask getAnimationTask() {
            return this.animationTask;
        }

        public final void setAnimationTask(@Nullable BukkitTask bukkitTask) {
            this.animationTask = bukkitTask;
        }

        @Override
        public void observerUpdate() {
            Hologram hologram = this.getHologram();
            boolean bl = false;
            this.line.update((Collection<Player>)hologram.getSeeingPlayers());
        }

        private final ILine<?> component1() {
            return this.line;
        }

        @NotNull
        public final PrivateConfig copy(@NotNull ILine<?> iLine) {
            Intrinsics.checkNotNullParameter(iLine, "line");
            return new PrivateConfig(iLine);
        }

        public static /* synthetic */ PrivateConfig copy$default(PrivateConfig privateConfig, ILine iLine, int n, Object object) {
            if ((n & 1) != 0) {
                iLine = privateConfig.line;
            }
            return privateConfig.copy(iLine);
        }

        @NotNull
        public String toString() {
            return "PrivateConfig(line=" + this.line + ')';
        }

        public int hashCode() {
            return this.line.hashCode();
        }

        public boolean equals(@Nullable Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof PrivateConfig)) {
                return false;
            }
            PrivateConfig privateConfig = (PrivateConfig)object;
            return Intrinsics.areEqual(this.line, privateConfig.line);
        }
    }

    @Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0007\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007\u00a8\u0006\b"}, d2={"Lorg/holoeasy/line/ILine$Type;", "", "(Ljava/lang/String;I)V", "EXTERNAL", "TEXT_LINE", "CLICKABLE_TEXT_LINE", "ITEM_LINE", "BLOCK_LINE", "holoeasy-core"})
    public static final class Type
    extends Enum<Type> {
        public static final /* enum */ Type EXTERNAL = new Type();
        public static final /* enum */ Type TEXT_LINE = new Type();
        @ApiStatus.Experimental
        public static final /* enum */ Type CLICKABLE_TEXT_LINE = new Type();
        public static final /* enum */ Type ITEM_LINE = new Type();
        @ApiStatus.Experimental
        public static final /* enum */ Type BLOCK_LINE = new Type();
        private static final /* synthetic */ Type[] $VALUES;
        private static final /* synthetic */ EnumEntries $ENTRIES;

        public static Type[] values() {
            return (Type[])$VALUES.clone();
        }

        public static Type valueOf(String string) {
            return Enum.valueOf(Type.class, string);
        }

        @NotNull
        public static EnumEntries<Type> getEntries() {
            return $ENTRIES;
        }

        static {
            $VALUES = typeArray = new Type[]{Type.EXTERNAL, Type.TEXT_LINE, Type.CLICKABLE_TEXT_LINE, Type.ITEM_LINE, Type.BLOCK_LINE};
            $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
        }
    }
}

