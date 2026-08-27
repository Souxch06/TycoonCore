package org.holoeasy.reactive;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.holoeasy.reactive.Observer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000@\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\b\u0018\u0000*\u0004\b\u0000\u0010\u00012\u00020\u0002B\r\u0012\u0006\u0010\u0003\u001a\u00028\u0000\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u0007J\u000e\u0010\f\u001a\u00028\u0000H\u00c2\u0003\u00a2\u0006\u0002\u0010\rJ\u001e\u0010\u000e\u001a\b\u0012\u0004\u0012\u00028\u00000\u00002\b\b\u0002\u0010\u0003\u001a\u00028\u0000H\u00c6\u0001\u00a2\u0006\u0002\u0010\u000fJ\u0013\u0010\u0010\u001a\u00020\u00112\b\u0010\u0012\u001a\u0004\u0018\u00010\u0002H\u00d6\u0003J\u000b\u0010\u0013\u001a\u00028\u0000\u00a2\u0006\u0002\u0010\rJ\t\u0010\u0014\u001a\u00020\u0015H\u00d6\u0001J\b\u0010\u0016\u001a\u00020\nH\u0002J\u000e\u0010\u0017\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u0007J\u0013\u0010\u0018\u001a\u00020\n2\u0006\u0010\u0019\u001a\u00028\u0000\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u001a\u001a\u00020\u001bH\u00d6\u0001J\u001a\u0010\u001c\u001a\u00020\n2\u0012\u0010\u001d\u001a\u000e\u0012\u0004\u0012\u00028\u0000\u0012\u0004\u0012\u00028\u00000\u001eR\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0003\u001a\u00028\u0000X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\b\u00a8\u0006\u001f"}, d2={"Lorg/holoeasy/reactive/MutableState;", "T", "", "value", "(Ljava/lang/Object;)V", "observers", "", "Lorg/holoeasy/reactive/Observer;", "Ljava/lang/Object;", "addObserver", "", "observer", "component1", "()Ljava/lang/Object;", "copy", "(Ljava/lang/Object;)Lorg/holoeasy/reactive/MutableState;", "equals", "", "other", "get", "hashCode", "", "notifyObservers", "removeObserver", "set", "newValue", "toString", "", "update", "newFun", "Ljava/util/function/Function;", "holoeasy-core"})
public final class MutableState<T> {
    private T value;
    @NotNull
    private final List<Observer> observers;

    public MutableState(T t) {
        this.value = t;
        this.observers = new ArrayList();
    }

    public final T get() {
        return this.value;
    }

    public final void set(T t) {
        this.value = t;
        this.notifyObservers();
    }

    public final void update(@NotNull Function<T, T> function) {
        Intrinsics.checkNotNullParameter(function, "newFun");
        this.set(function.apply(this.get()));
    }

    public final void addObserver(@NotNull Observer observer) {
        Intrinsics.checkNotNullParameter(observer, "observer");
        this.observers.add(observer);
    }

    public final void removeObserver(@NotNull Observer observer) {
        Intrinsics.checkNotNullParameter(observer, "observer");
        this.observers.remove(observer);
    }

    private final void notifyObservers() {
        for (Observer observer : this.observers) {
            observer.observerUpdate();
        }
    }

    private final T component1() {
        return this.value;
    }

    @NotNull
    public final MutableState<T> copy(T t) {
        return new MutableState<T>(t);
    }

    public static /* synthetic */ MutableState copy$default(MutableState mutableState, Object object, int n, Object object2) {
        if ((n & 1) != 0) {
            object = mutableState.value;
        }
        return mutableState.copy(object);
    }

    @NotNull
    public String toString() {
        return "MutableState(value=" + this.value + ')';
    }

    public int hashCode() {
        return this.value == null ? 0 : this.value.hashCode();
    }

    public boolean equals(@Nullable Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MutableState)) {
            return false;
        }
        MutableState mutableState = (MutableState)object;
        return Intrinsics.areEqual(this.value, mutableState.value);
    }
}

