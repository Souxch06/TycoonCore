/*
 * Decompiled with CFR 0.152.
 */
package com.cryptomorin.xseries.profiles.exceptions;

import com.cryptomorin.xseries.profiles.exceptions.MojangAPIException;

public final class MojangAPIRetryException
extends MojangAPIException {
    private final Reason reason;

    public MojangAPIRetryException(Reason reason, String string) {
        super(string);
        this.reason = reason;
    }

    public MojangAPIRetryException(Reason reason, String string, Throwable throwable) {
        super(string, throwable);
        this.reason = reason;
    }

    public Reason getReason() {
        return this.reason;
    }

    public static enum Reason {
        CONNECTION_RESET,
        CONNECTION_TIMEOUT,
        RATELIMITED;

    }
}

