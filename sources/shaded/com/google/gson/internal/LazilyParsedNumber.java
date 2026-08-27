package com.google.gson.internal;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.math.BigDecimal;

public final class LazilyParsedNumber
extends Number {
    private final String value;

    public LazilyParsedNumber(String string) {
        this.value = string;
    }

    @Override
    public int intValue() {
        try {
            return Integer.parseInt(this.value);
        }
        catch (NumberFormatException numberFormatException) {
            try {
                return (int)Long.parseLong(this.value);
            }
            catch (NumberFormatException numberFormatException2) {
                return new BigDecimal(this.value).intValue();
            }
        }
    }

    @Override
    public long longValue() {
        try {
            return Long.parseLong(this.value);
        }
        catch (NumberFormatException numberFormatException) {
            return new BigDecimal(this.value).longValue();
        }
    }

    @Override
    public float floatValue() {
        return Float.parseFloat(this.value);
    }

    @Override
    public double doubleValue() {
        return Double.parseDouble(this.value);
    }

    public String toString() {
        return this.value;
    }

    private Object writeReplace() {
        return new BigDecimal(this.value);
    }

    private void readObject(ObjectInputStream objectInputStream) {
        throw new InvalidObjectException("Deserialization is unsupported");
    }

    public int hashCode() {
        return this.value.hashCode();
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof LazilyParsedNumber) {
            LazilyParsedNumber lazilyParsedNumber = (LazilyParsedNumber)object;
            return this.value == lazilyParsedNumber.value || this.value.equals(lazilyParsedNumber.value);
        }
        return false;
    }
}

