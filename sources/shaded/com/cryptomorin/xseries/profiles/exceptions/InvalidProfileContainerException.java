package com.cryptomorin.xseries.profiles.exceptions;

import com.cryptomorin.xseries.profiles.exceptions.ProfileException;

public final class InvalidProfileContainerException
extends ProfileException {
    public InvalidProfileContainerException(String string) {
        super(string);
    }

    public InvalidProfileContainerException(String string, Throwable throwable) {
        super(string, throwable);
    }
}

