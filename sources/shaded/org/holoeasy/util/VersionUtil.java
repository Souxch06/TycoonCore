/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 */
package org.holoeasy.util;

import java.util.List;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.StringsKt;
import org.bukkit.Bukkit;
import org.holoeasy.util.VersionEnum;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0007\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u0004J\u000e\u0010\f\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u0004J\u0016\u0010\r\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u0004J\u000e\u0010\u0010\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u0004R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2={"Lorg/holoeasy/util/VersionUtil;", "", "()V", "CLEAN_VERSION", "Lorg/holoeasy/util/VersionEnum;", "getCLEAN_VERSION", "()Lorg/holoeasy/util/VersionEnum;", "VERSION", "", "isAbove", "", "ve", "isBelow", "isBetween", "ve1", "ve2", "isCompatible", "holoeasy-core"})
@SourceDebugExtension(value={"SMAP\nVersionUtil.kt\nKotlin\n*S Kotlin\n*F\n+ 1 VersionUtil.kt\norg/holoeasy/util/VersionUtil\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,148:1\n1#2:149\n*E\n"})
public final class VersionUtil {
    @NotNull
    public static final VersionUtil INSTANCE = new VersionUtil();
    @NotNull
    private static final String VERSION;
    @NotNull
    private static final VersionEnum CLEAN_VERSION;

    private VersionUtil() {
    }

    @NotNull
    public final VersionEnum getCLEAN_VERSION() {
        return CLEAN_VERSION;
    }

    public final boolean isCompatible(@NotNull VersionEnum versionEnum) {
        Intrinsics.checkNotNullParameter(versionEnum, "ve");
        return CLEAN_VERSION == versionEnum;
    }

    public final boolean isAbove(@NotNull VersionEnum versionEnum) {
        Intrinsics.checkNotNullParameter(versionEnum, "ve");
        return CLEAN_VERSION.ordinal() >= versionEnum.ordinal();
    }

    public final boolean isBelow(@NotNull VersionEnum versionEnum) {
        Intrinsics.checkNotNullParameter(versionEnum, "ve");
        return CLEAN_VERSION.ordinal() <= versionEnum.ordinal();
    }

    public final boolean isBetween(@NotNull VersionEnum versionEnum, @NotNull VersionEnum versionEnum2) {
        Intrinsics.checkNotNullParameter(versionEnum, "ve1");
        Intrinsics.checkNotNullParameter(versionEnum2, "ve2");
        return this.isAbove(versionEnum) && this.isBelow(versionEnum2);
    }

    static {
        String string = Bukkit.getServer().getBukkitVersion();
        Intrinsics.checkNotNullExpressionValue(string, "getBukkitVersion(...)");
        String string2 = string;
        Object object = new String[]{"-"};
        String string3 = (String)StringsKt.split$default((CharSequence)string2, object, false, 0, 6, null).get(0);
        object = new String[]{"."};
        List list = StringsKt.split$default((CharSequence)string3, object, false, 0, 6, null);
        boolean bl = false;
        VERSION = 'V' + (String)list.get(0) + '_' + (String)list.get(1);
        try {
            object = VersionEnum.valueOf(VERSION);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            object = VersionEnum.LATEST;
        }
        CLEAN_VERSION = object;
    }
}

