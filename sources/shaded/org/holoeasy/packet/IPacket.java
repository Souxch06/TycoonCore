/*
 * Decompiled with CFR 0.152.
 */
package org.holoeasy.packet;

import kotlin.Metadata;
import kotlin.ranges.ClosedRange;
import org.holoeasy.util.VersionEnum;
import org.holoeasy.util.VersionUtil;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\bf\u0018\u00002\u00020\u0001J\b\u0010\b\u001a\u00020\tH\u0016R \u0010\u0002\u001a\u0010\u0012\f\b\u0001\u0012\b\u0012\u0004\u0012\u00020\u00050\u00040\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\n"}, d2={"Lorg/holoeasy/packet/IPacket;", "", "versionSupport", "", "Lkotlin/ranges/ClosedRange;", "Lorg/holoeasy/util/VersionEnum;", "getVersionSupport", "()[Lkotlin/ranges/ClosedRange;", "isCurrentVersion", "", "holoeasy-core"})
public interface IPacket {
    @NotNull
    public ClosedRange<VersionEnum>[] getVersionSupport();

    public boolean isCurrentVersion();

    @Metadata(mv={1, 9, 0}, k=3, xi=48)
    public static final class DefaultImpls {
        public static boolean isCurrentVersion(@NotNull IPacket iPacket) {
            for (ClosedRange<VersionEnum> closedRange : iPacket.getVersionSupport()) {
                if (!closedRange.contains((VersionEnum)((Comparable)VersionUtil.INSTANCE.getCLEAN_VERSION()))) continue;
                return true;
            }
            return false;
        }
    }
}

