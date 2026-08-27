/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 */
package com.cryptomorin.xseries.reflection;

import com.cryptomorin.xseries.reflection.XReflection;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public interface ReflectiveHandle<T>
extends Cloneable {
    @ApiStatus.Experimental
    public ReflectiveHandle<T> clone();

    default public boolean exists() {
        try {
            this.reflect();
            return true;
        }
        catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    @Nullable
    @ApiStatus.Obsolete
    default public ReflectiveOperationException catchError() {
        try {
            this.reflect();
            return null;
        }
        catch (ReflectiveOperationException ex) {
            return ex;
        }
    }

    @Nonnull
    default public T unreflect() {
        try {
            return this.reflect();
        }
        catch (ReflectiveOperationException e) {
            throw XReflection.throwCheckedException(e);
        }
    }

    @Nullable
    default public T reflectOrNull() {
        try {
            return this.reflect();
        }
        catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Nonnull
    public T reflect() throws ReflectiveOperationException;
}

