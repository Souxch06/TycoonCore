/*
 * Decompiled with CFR 0.152.
 */
package kotlin.contracts;

import kotlin.Metadata;
import kotlin.SinceKotlin;
import kotlin.contracts.ExperimentalContracts;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.internal.ContractsDsl;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0006\b\u0087\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006\u00a8\u0006\u0007"}, d2={"Lkotlin/contracts/InvocationKind;", "", "(Ljava/lang/String;I)V", "AT_MOST_ONCE", "AT_LEAST_ONCE", "EXACTLY_ONCE", "UNKNOWN", "kotlin-stdlib"})
@ContractsDsl
@ExperimentalContracts
@SinceKotlin(version="1.3")
public final class InvocationKind
extends Enum<InvocationKind> {
    @ContractsDsl
    public static final /* enum */ InvocationKind AT_MOST_ONCE = new InvocationKind();
    @ContractsDsl
    public static final /* enum */ InvocationKind AT_LEAST_ONCE = new InvocationKind();
    @ContractsDsl
    public static final /* enum */ InvocationKind EXACTLY_ONCE = new InvocationKind();
    @ContractsDsl
    public static final /* enum */ InvocationKind UNKNOWN = new InvocationKind();
    private static final /* synthetic */ InvocationKind[] $VALUES;
    private static final /* synthetic */ EnumEntries $ENTRIES;

    public static InvocationKind[] values() {
        return (InvocationKind[])$VALUES.clone();
    }

    public static InvocationKind valueOf(String string) {
        return Enum.valueOf(InvocationKind.class, string);
    }

    @NotNull
    public static EnumEntries<InvocationKind> getEntries() {
        return $ENTRIES;
    }

    static {
        $VALUES = invocationKindArray = new InvocationKind[]{InvocationKind.AT_MOST_ONCE, InvocationKind.AT_LEAST_ONCE, InvocationKind.EXACTLY_ONCE, InvocationKind.UNKNOWN};
        $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
    }
}

