package org.holoeasy.ext;

import kotlin.Metadata;

@Metadata(mv={1, 9, 0}, k=2, xi=48, d1={"\u0000\u0016\n\u0000\n\u0002\u0010\u0005\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0003\"\u0018\u0010\u0000\u001a\u00020\u0001*\u00020\u00028@X\u0080\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0003\u0010\u0004\"\u0018\u0010\u0005\u001a\u00020\u0006*\u00020\u00028@X\u0080\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\b\u00a8\u0006\t"}, d2={"compressAngle", "", "", "getCompressAngle", "(D)B", "fixCoordinate", "", "getFixCoordinate", "(D)I", "holoeasy-core"})
public final class DoubleExtKt {
    public static final byte getCompressAngle(double d) {
        return (byte)(d * (double)256.0f / (double)360.0f);
    }

    public static final int getFixCoordinate(double d) {
        return (int)Math.floor(d * 32.0);
    }
}

