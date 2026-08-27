package com.cryptomorin.xseries.profiles.exceptions;

public class ProfileException
extends RuntimeException {
    public ProfileException() {
    }

    public ProfileException(String string) {
        super(string);
    }

    public ProfileException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public ProfileException(Throwable throwable) {
        super(throwable);
    }

    public ProfileException(String string, Throwable throwable, boolean bl, boolean bl2) {
        super(string, throwable, bl, bl2);
    }
}

