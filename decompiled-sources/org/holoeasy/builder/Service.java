/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package org.holoeasy.builder;

import kotlin.Metadata;
import kotlin.Pair;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.jvm.JvmOverloads;
import kotlin.jvm.internal.Intrinsics;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.holoeasy.builder.HologramConfig;
import org.holoeasy.builder.interfaces.HologramConfigGroup;
import org.holoeasy.hologram.IHologramLoader;
import org.holoeasy.hologram.TextBlockStandardLoader;
import org.holoeasy.line.BlockLine;
import org.holoeasy.line.ClickableTextLine;
import org.holoeasy.line.ILine;
import org.holoeasy.line.ITextLine;
import org.holoeasy.line.ItemLine;
import org.holoeasy.line.TextLine;
import org.holoeasy.reactive.MutableState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000h\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001(B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fJ\u0014\u0010\u0010\u001a\u00020\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0011J\u000e\u0010\u0012\u001a\u00020\r2\u0006\u0010\u0013\u001a\u00020\u0014J\u0012\u0010\u0015\u001a\u00020\r2\n\u0010\u0015\u001a\u0006\u0012\u0002\b\u00030\u0016J\b\u0010\u0017\u001a\u00020\u0005H\u0002J\u0012\u0010\u0018\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u00010\tJ\u000e\u0010\u0019\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fJ\u0014\u0010\u001a\u001a\u00020\r2\f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0011JG\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u001e\u001a\u00020\u001f2\b\b\u0002\u0010 \u001a\u00020!2\n\b\u0002\u0010\"\u001a\u0004\u0018\u00010#2\n\b\u0002\u0010$\u001a\u0004\u0018\u00010#2\u000e\b\u0002\u0010%\u001a\b\u0012\u0002\b\u0003\u0018\u00010&H\u0007\u00a2\u0006\u0002\u0010'R\u0017\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R#\u0010\b\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u00010\t0\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\u0007\u00a8\u0006)"}, d2={"Lorg/holoeasy/builder/Service;", "", "()V", "staticHologram", "Ljava/lang/ThreadLocal;", "Lorg/holoeasy/builder/HologramConfig;", "getStaticHologram", "()Ljava/lang/ThreadLocal;", "staticPool", "Lkotlin/Pair;", "Lorg/holoeasy/builder/Service$RegistrationType;", "getStaticPool", "blockline", "", "block", "Lorg/bukkit/inventory/ItemStack;", "blocklineMutable", "Lorg/holoeasy/reactive/MutableState;", "config", "configGroup", "Lorg/holoeasy/builder/interfaces/HologramConfigGroup;", "customLine", "Lorg/holoeasy/line/ILine;", "getStaticHolo", "getStaticRegistration", "itemline", "itemlineMutable", "item", "textline", "Lorg/holoeasy/line/ITextLine;", "text", "", "clickable", "", "minHitDistance", "", "maxHitDistance", "args", "", "(Ljava/lang/String;ZLjava/lang/Float;Ljava/lang/Float;[Ljava/lang/Object;)Lorg/holoeasy/line/ITextLine;", "RegistrationType", "holoeasy-core"})
public final class Service {
    @NotNull
    public static final Service INSTANCE = new Service();
    @NotNull
    private static final ThreadLocal<Pair<RegistrationType, Object>> staticPool = new ThreadLocal();
    @NotNull
    private static final ThreadLocal<HologramConfig> staticHologram = new ThreadLocal();

    private Service() {
    }

    @NotNull
    public final ThreadLocal<Pair<RegistrationType, Object>> getStaticPool() {
        return staticPool;
    }

    @NotNull
    public final ThreadLocal<HologramConfig> getStaticHologram() {
        return staticHologram;
    }

    @NotNull
    public final Pair<RegistrationType, Object> getStaticRegistration() {
        Pair<RegistrationType, Object> pair = staticPool.get();
        if (pair == null) {
            throw new IllegalStateException("hologram block must be inside a registerHolograms block");
        }
        Pair<RegistrationType, Object> pair2 = pair;
        return pair2;
    }

    private final HologramConfig getStaticHolo() {
        HologramConfig hologramConfig = staticHologram.get();
        if (hologramConfig == null) {
            throw new RuntimeException("You must call config() inside hologram block");
        }
        HologramConfig hologramConfig2 = hologramConfig;
        return hologramConfig2;
    }

    public final void config(@NotNull HologramConfigGroup hologramConfigGroup) {
        Intrinsics.checkNotNullParameter(hologramConfigGroup, "configGroup");
        HologramConfig hologramConfig = this.getStaticHolo();
        hologramConfigGroup.configure(hologramConfig);
        IHologramLoader iHologramLoader = hologramConfig.loader;
        if (iHologramLoader == null) {
            iHologramLoader = new TextBlockStandardLoader();
        }
        hologramConfig.loader = iHologramLoader;
    }

    @JvmOverloads
    @NotNull
    public final ITextLine textline(@NotNull String string, boolean bl, @Nullable Float f, @Nullable Float f2, @Nullable Object[] objectArray) {
        Intrinsics.checkNotNullParameter(string, "text");
        HologramConfig hologramConfig = this.getStaticHolo();
        if (f == null || f2 == null) {
            Plugin plugin = hologramConfig.plugin;
            Intrinsics.checkNotNull(plugin);
            TextLine textLine = new TextLine(plugin, string, objectArray, bl);
            hologramConfig.lines.add(textLine);
            return textLine;
        }
        Object object = hologramConfig.plugin;
        Intrinsics.checkNotNull(object);
        TextLine textLine = new TextLine((Plugin)object, string, objectArray, false);
        object = new ClickableTextLine(textLine, f.floatValue(), f2.floatValue());
        hologramConfig.lines.add((ILine<?>)object);
        return (ITextLine)object;
    }

    public static /* synthetic */ ITextLine textline$default(Service service, String string, boolean bl, Float f, Float f2, Object[] objectArray, int n, Object object) {
        if ((n & 2) != 0) {
            bl = false;
        }
        if ((n & 4) != 0) {
            f = null;
        }
        if ((n & 8) != 0) {
            f2 = null;
        }
        if ((n & 0x10) != 0) {
            objectArray = null;
        }
        return service.textline(string, bl, f, f2, objectArray);
    }

    public final void itemline(@NotNull ItemStack itemStack) {
        Intrinsics.checkNotNullParameter(itemStack, "block");
        HologramConfig hologramConfig = this.getStaticHolo();
        Plugin plugin = hologramConfig.plugin;
        Intrinsics.checkNotNullExpressionValue(plugin, "plugin");
        ItemLine itemLine = new ItemLine(plugin, itemStack);
        hologramConfig.lines.add(itemLine);
    }

    public final void itemlineMutable(@NotNull MutableState<ItemStack> mutableState) {
        Intrinsics.checkNotNullParameter(mutableState, "item");
        HologramConfig hologramConfig = this.getStaticHolo();
        Plugin plugin = hologramConfig.plugin;
        Intrinsics.checkNotNullExpressionValue(plugin, "plugin");
        ItemLine itemLine = new ItemLine(plugin, mutableState);
        hologramConfig.lines.add(itemLine);
    }

    public final void blockline(@NotNull ItemStack itemStack) {
        Intrinsics.checkNotNullParameter(itemStack, "block");
        HologramConfig hologramConfig = this.getStaticHolo();
        Plugin plugin = hologramConfig.plugin;
        Intrinsics.checkNotNullExpressionValue(plugin, "plugin");
        BlockLine blockLine = new BlockLine(plugin, itemStack);
        hologramConfig.lines.add(blockLine);
    }

    public final void blocklineMutable(@NotNull MutableState<ItemStack> mutableState) {
        Intrinsics.checkNotNullParameter(mutableState, "block");
        HologramConfig hologramConfig = this.getStaticHolo();
        Plugin plugin = hologramConfig.plugin;
        Intrinsics.checkNotNullExpressionValue(plugin, "plugin");
        BlockLine blockLine = new BlockLine(plugin, mutableState);
        hologramConfig.lines.add(blockLine);
    }

    public final void customLine(@NotNull ILine<?> iLine) {
        Intrinsics.checkNotNullParameter(iLine, "customLine");
        HologramConfig hologramConfig = this.getStaticHolo();
        hologramConfig.lines.add(iLine);
    }

    @JvmOverloads
    @NotNull
    public final ITextLine textline(@NotNull String string, boolean bl, @Nullable Float f, @Nullable Float f2) {
        Intrinsics.checkNotNullParameter(string, "text");
        return Service.textline$default(this, string, bl, f, f2, null, 16, null);
    }

    @JvmOverloads
    @NotNull
    public final ITextLine textline(@NotNull String string, boolean bl, @Nullable Float f) {
        Intrinsics.checkNotNullParameter(string, "text");
        return Service.textline$default(this, string, bl, f, null, null, 24, null);
    }

    @JvmOverloads
    @NotNull
    public final ITextLine textline(@NotNull String string, boolean bl) {
        Intrinsics.checkNotNullParameter(string, "text");
        return Service.textline$default(this, string, bl, null, null, null, 28, null);
    }

    @JvmOverloads
    @NotNull
    public final ITextLine textline(@NotNull String string) {
        Intrinsics.checkNotNullParameter(string, "text");
        return Service.textline$default(this, string, false, null, null, null, 30, null);
    }

    @Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0004\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004\u00a8\u0006\u0005"}, d2={"Lorg/holoeasy/builder/Service$RegistrationType;", "", "(Ljava/lang/String;I)V", "PLUGIN", "POOL", "holoeasy-core"})
    public static final class RegistrationType
    extends Enum<RegistrationType> {
        public static final /* enum */ RegistrationType PLUGIN = new RegistrationType();
        public static final /* enum */ RegistrationType POOL = new RegistrationType();
        private static final /* synthetic */ RegistrationType[] $VALUES;
        private static final /* synthetic */ EnumEntries $ENTRIES;

        public static RegistrationType[] values() {
            return (RegistrationType[])$VALUES.clone();
        }

        public static RegistrationType valueOf(String string) {
            return Enum.valueOf(RegistrationType.class, string);
        }

        @NotNull
        public static EnumEntries<RegistrationType> getEntries() {
            return $ENTRIES;
        }

        static {
            $VALUES = registrationTypeArray = new RegistrationType[]{RegistrationType.PLUGIN, RegistrationType.POOL};
            $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
        }
    }
}

