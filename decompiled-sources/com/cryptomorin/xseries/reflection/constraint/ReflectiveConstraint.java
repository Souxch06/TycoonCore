/*
 * Decompiled with CFR 0.152.
 */
package com.cryptomorin.xseries.reflection.constraint;

import com.cryptomorin.xseries.reflection.ReflectiveHandle;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public interface ReflectiveConstraint {
    public String category();

    public String name();

    public boolean appliesTo(ReflectiveHandle<?> var1);
}

