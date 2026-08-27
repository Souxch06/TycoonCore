/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 */
package com.cryptomorin.xseries.reflection.jvm;

import java.util.Set;
import javax.annotation.Nonnull;

public interface NamedReflectiveHandle {
    @Nonnull
    public Set<String> getPossibleNames();
}

