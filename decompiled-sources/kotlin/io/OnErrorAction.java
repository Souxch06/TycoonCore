/*
 * Decompiled with CFR 0.152.
 */
package kotlin.io;

import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0004\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004\u00a8\u0006\u0005"}, d2={"Lkotlin/io/OnErrorAction;", "", "(Ljava/lang/String;I)V", "SKIP", "TERMINATE", "kotlin-stdlib"})
public final class OnErrorAction
extends Enum<OnErrorAction> {
    public static final /* enum */ OnErrorAction SKIP = new OnErrorAction();
    public static final /* enum */ OnErrorAction TERMINATE = new OnErrorAction();
    private static final /* synthetic */ OnErrorAction[] $VALUES;
    private static final /* synthetic */ EnumEntries $ENTRIES;

    public static OnErrorAction[] values() {
        return (OnErrorAction[])$VALUES.clone();
    }

    public static OnErrorAction valueOf(String string) {
        return Enum.valueOf(OnErrorAction.class, string);
    }

    @NotNull
    public static EnumEntries<OnErrorAction> getEntries() {
        return $ENTRIES;
    }

    static {
        $VALUES = onErrorActionArray = new OnErrorAction[]{OnErrorAction.SKIP, OnErrorAction.TERMINATE};
        $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
    }
}

