/*
 * Decompiled with CFR 0.152.
 */
package com.cryptomorin.xseries.reflection;

import com.cryptomorin.xseries.reflection.jvm.NamedReflectiveHandle;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public interface ReflectiveMapping {
    public boolean shouldBeChecked();

    public String category();

    public String name();

    public String process(NamedReflectiveHandle var1, String var2);
}

