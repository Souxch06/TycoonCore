/*
 * Decompiled with CFR 0.152.
 */
package com.cryptomorin.xseries.profiles.builder;

import com.cryptomorin.xseries.profiles.builder.ProfileInstruction;
import com.cryptomorin.xseries.profiles.exceptions.ProfileChangeException;

public final class ProfileFallback<T> {
    private final ProfileInstruction<T> instruction;
    private T object;
    private final ProfileChangeException error;

    public ProfileFallback(ProfileInstruction<T> profileInstruction, T t, ProfileChangeException profileChangeException) {
        this.instruction = profileInstruction;
        this.object = t;
        this.error = profileChangeException;
    }

    public T getObject() {
        return this.object;
    }

    public ProfileInstruction<T> getInstruction() {
        return this.instruction;
    }

    public void setObject(T t) {
        this.object = t;
    }

    public ProfileChangeException getError() {
        return this.error;
    }
}

