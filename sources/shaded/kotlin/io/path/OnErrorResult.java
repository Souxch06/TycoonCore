/*
 * Decompiled with CFR 0.152.
 */
package kotlin.io.path;

import kotlin.Metadata;
import kotlin.SinceKotlin;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.io.path.ExperimentalPathApi;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0004\b\u0087\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004\u00a8\u0006\u0005"}, d2={"Lkotlin/io/path/OnErrorResult;", "", "(Ljava/lang/String;I)V", "SKIP_SUBTREE", "TERMINATE", "kotlin-stdlib-jdk7"})
@ExperimentalPathApi
@SinceKotlin(version="1.8")
public final class OnErrorResult
extends Enum<OnErrorResult> {
    public static final /* enum */ OnErrorResult SKIP_SUBTREE = new OnErrorResult();
    public static final /* enum */ OnErrorResult TERMINATE = new OnErrorResult();
    private static final /* synthetic */ OnErrorResult[] $VALUES;
    private static final /* synthetic */ EnumEntries $ENTRIES;

    public static OnErrorResult[] values() {
        return (OnErrorResult[])$VALUES.clone();
    }

    public static OnErrorResult valueOf(String string) {
        return Enum.valueOf(OnErrorResult.class, string);
    }

    @NotNull
    public static EnumEntries<OnErrorResult> getEntries() {
        return $ENTRIES;
    }

    static {
        $VALUES = onErrorResultArray = new OnErrorResult[]{OnErrorResult.SKIP_SUBTREE, OnErrorResult.TERMINATE};
        $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
    }
}

