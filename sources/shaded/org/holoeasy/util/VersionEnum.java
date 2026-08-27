package org.holoeasy.util;

import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\u0010\u000f\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0019\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u00012\b\u0012\u0004\u0012\u00020\u00000\u0002B\u0007\b\u0012\u00a2\u0006\u0002\u0010\u0003B\u0017\b\u0002\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0007R\u001a\u0010\u0004\u001a\u00020\u0005X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u001a\u0010\u0006\u001a\u00020\u0005X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\t\"\u0004\b\r\u0010\u000bj\u0002\b\u000ej\u0002\b\u000fj\u0002\b\u0010j\u0002\b\u0011j\u0002\b\u0012j\u0002\b\u0013j\u0002\b\u0014j\u0002\b\u0015j\u0002\b\u0016j\u0002\b\u0017j\u0002\b\u0018j\u0002\b\u0019j\u0002\b\u001aj\u0002\b\u001bj\u0002\b\u001cj\u0002\b\u001d\u00a8\u0006\u001e"}, d2={"Lorg/holoeasy/util/VersionEnum;", "", "", "(Ljava/lang/String;I)V", "armorstandId", "", "droppedItemId", "(Ljava/lang/String;III)V", "getArmorstandId", "()I", "setArmorstandId", "(I)V", "getDroppedItemId", "setDroppedItemId", "MOCKBUK", "V1_8", "V1_9", "V1_10", "V1_11", "V1_12", "V1_13", "V1_14", "V1_15", "V1_16", "V1_17", "V1_18", "V1_19", "V1_20", "V1_21", "LATEST", "holoeasy-core"})
public final class VersionEnum
extends Enum<VersionEnum>
implements Comparable<VersionEnum> {
    private int armorstandId;
    private int droppedItemId;
    public static final /* enum */ VersionEnum MOCKBUK = new VersionEnum();
    public static final /* enum */ VersionEnum V1_8 = new VersionEnum(30, 1);
    public static final /* enum */ VersionEnum V1_9 = new VersionEnum(30, 1);
    public static final /* enum */ VersionEnum V1_10 = new VersionEnum(30, 1);
    public static final /* enum */ VersionEnum V1_11 = new VersionEnum(30, 1);
    public static final /* enum */ VersionEnum V1_12 = new VersionEnum(30, 1);
    public static final /* enum */ VersionEnum V1_13 = new VersionEnum(1, 32);
    public static final /* enum */ VersionEnum V1_14 = new VersionEnum(1, 34);
    public static final /* enum */ VersionEnum V1_15 = new VersionEnum(1, 35);
    public static final /* enum */ VersionEnum V1_16 = new VersionEnum(1, 37);
    public static final /* enum */ VersionEnum V1_17 = new VersionEnum(1, 41);
    public static final /* enum */ VersionEnum V1_18 = new VersionEnum(1, 41);
    public static final /* enum */ VersionEnum V1_19 = new VersionEnum();
    public static final /* enum */ VersionEnum V1_20 = new VersionEnum();
    public static final /* enum */ VersionEnum V1_21 = new VersionEnum();
    public static final /* enum */ VersionEnum LATEST = new VersionEnum();
    private static final /* synthetic */ VersionEnum[] $VALUES;
    private static final /* synthetic */ EnumEntries $ENTRIES;

    private VersionEnum(int n2, int n3) {
        this.armorstandId = n2;
        this.droppedItemId = n3;
    }

    public final int getArmorstandId() {
        return this.armorstandId;
    }

    public final void setArmorstandId(int n) {
        this.armorstandId = n;
    }

    public final int getDroppedItemId() {
        return this.droppedItemId;
    }

    public final void setDroppedItemId(int n) {
        this.droppedItemId = n;
    }

    private VersionEnum() {
        this(-1, -1);
    }

    public static VersionEnum[] values() {
        return (VersionEnum[])$VALUES.clone();
    }

    public static VersionEnum valueOf(String string) {
        return Enum.valueOf(VersionEnum.class, string);
    }

    @NotNull
    public static EnumEntries<VersionEnum> getEntries() {
        return $ENTRIES;
    }

    static {
        $VALUES = versionEnumArray = new VersionEnum[]{VersionEnum.MOCKBUK, VersionEnum.V1_8, VersionEnum.V1_9, VersionEnum.V1_10, VersionEnum.V1_11, VersionEnum.V1_12, VersionEnum.V1_13, VersionEnum.V1_14, VersionEnum.V1_15, VersionEnum.V1_16, VersionEnum.V1_17, VersionEnum.V1_18, VersionEnum.V1_19, VersionEnum.V1_20, VersionEnum.V1_21, VersionEnum.LATEST};
        $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
    }
}

