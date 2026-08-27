/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 */
package com.cryptomorin.xseries.reflection;

import com.cryptomorin.xseries.reflection.ReflectiveHandle;
import com.cryptomorin.xseries.reflection.XReflection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AggregateReflectiveHandle<T, H extends ReflectiveHandle<T>>
implements ReflectiveHandle<T> {
    private final List<Callable<H>> handles;
    private Consumer<H> handleModifier;

    protected AggregateReflectiveHandle(Collection<Callable<H>> collection) {
        this.handles = new ArrayList<Callable<H>>(collection.size());
        this.handles.addAll(collection);
    }

    public AggregateReflectiveHandle<T, H> or(@Nonnull H h) {
        return this.or(() -> h);
    }

    public AggregateReflectiveHandle<T, H> or(@Nonnull Callable<H> callable) {
        this.handles.add(callable);
        return this;
    }

    public AggregateReflectiveHandle<T, H> modify(@Nullable Consumer<H> consumer) {
        this.handleModifier = consumer;
        return this;
    }

    public AggregateReflectiveHandle<T, H> clone() {
        AggregateReflectiveHandle<T, H> aggregateReflectiveHandle = new AggregateReflectiveHandle<T, H>(new ArrayList<Callable<H>>(this.handles));
        aggregateReflectiveHandle.handleModifier = this.handleModifier;
        return aggregateReflectiveHandle;
    }

    @Override
    public T reflect() {
        Throwable throwable = null;
        for (Callable<H> callable : this.handles) {
            ReflectiveHandle reflectiveHandle;
            try {
                reflectiveHandle = (ReflectiveHandle)callable.call();
            }
            catch (Throwable throwable2) {
                if (throwable == null) {
                    throwable = new ClassNotFoundException("None of the aggregate handles were successful");
                }
                throwable.addSuppressed(throwable2);
                continue;
            }
            if (this.handleModifier != null) {
                this.handleModifier.accept(reflectiveHandle);
            }
            try {
                return reflectiveHandle.reflect();
            }
            catch (Throwable throwable3) {
                if (throwable == null) {
                    throwable = new ClassNotFoundException("None of the aggregate handles were successful");
                }
                throwable.addSuppressed(throwable3);
            }
        }
        throw (ClassNotFoundException)XReflection.relativizeSuppressedExceptions(throwable);
    }
}

