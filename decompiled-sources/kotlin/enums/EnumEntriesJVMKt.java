/*
 * Decompiled with CFR 0.152.
 */
package kotlin.enums;

import kotlin.ExperimentalStdlibApi;
import kotlin.Metadata;
import kotlin.PublishedApi;
import kotlin.SinceKotlin;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.jvm.internal.Intrinsics;

@Metadata(mv={1, 9, 0}, k=2, xi=48, d1={"\u0000\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0010\n\u0000\u001a!\u0010\u0000\u001a\b\u0012\u0004\u0012\u0002H\u00020\u0001\"\u0010\b\u0000\u0010\u0002\u0018\u0001*\b\u0012\u0004\u0012\u0002H\u00020\u0003H\u0081\b\u00a8\u0006\u0004"}, d2={"enumEntriesIntrinsic", "Lkotlin/enums/EnumEntries;", "T", "", "kotlin-stdlib"})
public final class EnumEntriesJVMKt {
    @SinceKotlin(version="1.9")
    @ExperimentalStdlibApi
    @PublishedApi
    public static final /* synthetic */ <T extends Enum<T>> EnumEntries<T> enumEntriesIntrinsic() {
        boolean bl = false;
        Intrinsics.reifiedOperationMarker(5, "T");
        return EnumEntriesKt.enumEntries((Enum[])new Enum[0]);
    }
}

