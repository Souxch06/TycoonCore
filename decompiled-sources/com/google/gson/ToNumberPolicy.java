/*
 * Decompiled with CFR 0.152.
 */
package com.google.gson;

import com.google.gson.JsonParseException;
import com.google.gson.ToNumberStrategy;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;
import java.math.BigDecimal;

public enum ToNumberPolicy implements ToNumberStrategy
{
    DOUBLE{

        @Override
        public Double readNumber(JsonReader jsonReader) {
            return jsonReader.nextDouble();
        }
    }
    ,
    LAZILY_PARSED_NUMBER{

        @Override
        public Number readNumber(JsonReader jsonReader) {
            return new LazilyParsedNumber(jsonReader.nextString());
        }
    }
    ,
    LONG_OR_DOUBLE{

        @Override
        public Number readNumber(JsonReader jsonReader) {
            String string = jsonReader.nextString();
            try {
                return Long.parseLong(string);
            }
            catch (NumberFormatException numberFormatException) {
                try {
                    Double d = Double.valueOf(string);
                    if ((d.isInfinite() || d.isNaN()) && !jsonReader.isLenient()) {
                        throw new MalformedJsonException("JSON forbids NaN and infinities: " + d + "; at path " + jsonReader.getPreviousPath());
                    }
                    return d;
                }
                catch (NumberFormatException numberFormatException2) {
                    throw new JsonParseException("Cannot parse " + string + "; at path " + jsonReader.getPreviousPath(), numberFormatException2);
                }
            }
        }
    }
    ,
    BIG_DECIMAL{

        @Override
        public BigDecimal readNumber(JsonReader jsonReader) {
            String string = jsonReader.nextString();
            try {
                return new BigDecimal(string);
            }
            catch (NumberFormatException numberFormatException) {
                throw new JsonParseException("Cannot parse " + string + "; at path " + jsonReader.getPreviousPath(), numberFormatException);
            }
        }
    };

}

