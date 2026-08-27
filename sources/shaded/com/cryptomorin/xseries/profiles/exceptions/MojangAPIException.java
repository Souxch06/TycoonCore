package com.cryptomorin.xseries.profiles.exceptions;

import com.cryptomorin.xseries.profiles.exceptions.ProfileException;

public class MojangAPIException
extends ProfileException {
    public MojangAPIException(String string) {
        super(string);
    }

    public MojangAPIException(String string, Throwable throwable) {
        super(string, throwable);
    }
}

