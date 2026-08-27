package com.google.gson;

import com.google.gson.JsonParseException;

public final class JsonIOException
extends JsonParseException {
    private static final long serialVersionUID = 1L;

    public JsonIOException(String string) {
        super(string);
    }

    public JsonIOException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public JsonIOException(Throwable throwable) {
        super(throwable);
    }
}

