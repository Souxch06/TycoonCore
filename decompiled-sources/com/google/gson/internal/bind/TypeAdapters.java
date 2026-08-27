/*
 * Decompiled with CFR 0.152.
 */
package com.google.gson.internal.bind;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

public final class TypeAdapters {
    public static final TypeAdapter<Class> CLASS = new TypeAdapter<Class>(){

        @Override
        public void write(JsonWriter jsonWriter, Class clazz) {
            throw new UnsupportedOperationException("Attempted to serialize java.lang.Class: " + clazz.getName() + ". Forgot to register a type adapter?");
        }

        @Override
        public Class read(JsonReader jsonReader) {
            throw new UnsupportedOperationException("Attempted to deserialize a java.lang.Class. Forgot to register a type adapter?");
        }
    }.nullSafe();
    public static final TypeAdapterFactory CLASS_FACTORY = TypeAdapters.newFactory(Class.class, CLASS);
    public static final TypeAdapter<BitSet> BIT_SET = new TypeAdapter<BitSet>(){

        @Override
        public BitSet read(JsonReader jsonReader) {
            BitSet bitSet = new BitSet();
            jsonReader.beginArray();
            int n = 0;
            JsonToken jsonToken = jsonReader.peek();
            while (jsonToken != JsonToken.END_ARRAY) {
                boolean bl;
                switch (jsonToken) {
                    case NUMBER: 
                    case STRING: {
                        int n2 = jsonReader.nextInt();
                        if (n2 == 0) {
                            bl = false;
                            break;
                        }
                        if (n2 == 1) {
                            bl = true;
                            break;
                        }
                        throw new JsonSyntaxException("Invalid bitset value " + n2 + ", expected 0 or 1; at path " + jsonReader.getPreviousPath());
                    }
                    case BOOLEAN: {
                        bl = jsonReader.nextBoolean();
                        break;
                    }
                    default: {
                        throw new JsonSyntaxException("Invalid bitset value type: " + (Object)((Object)jsonToken) + "; at path " + jsonReader.getPath());
                    }
                }
                if (bl) {
                    bitSet.set(n);
                }
                ++n;
                jsonToken = jsonReader.peek();
            }
            jsonReader.endArray();
            return bitSet;
        }

        @Override
        public void write(JsonWriter jsonWriter, BitSet bitSet) {
            jsonWriter.beginArray();
            int n = bitSet.length();
            for (int i = 0; i < n; ++i) {
                int n2 = bitSet.get(i) ? 1 : 0;
                jsonWriter.value(n2);
            }
            jsonWriter.endArray();
        }
    }.nullSafe();
    public static final TypeAdapterFactory BIT_SET_FACTORY = TypeAdapters.newFactory(BitSet.class, BIT_SET);
    public static final TypeAdapter<Boolean> BOOLEAN = new TypeAdapter<Boolean>(){

        @Override
        public Boolean read(JsonReader jsonReader) {
            JsonToken jsonToken = jsonReader.peek();
            if (jsonToken == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            if (jsonToken == JsonToken.STRING) {
                return Boolean.parseBoolean(jsonReader.nextString());
            }
            return jsonReader.nextBoolean();
        }

        @Override
        public void write(JsonWriter jsonWriter, Boolean bl) {
            jsonWriter.value(bl);
        }
    };
    public static final TypeAdapter<Boolean> BOOLEAN_AS_STRING = new TypeAdapter<Boolean>(){

        @Override
        public Boolean read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return Boolean.valueOf(jsonReader.nextString());
        }

        @Override
        public void write(JsonWriter jsonWriter, Boolean bl) {
            jsonWriter.value(bl == null ? "null" : bl.toString());
        }
    };
    public static final TypeAdapterFactory BOOLEAN_FACTORY = TypeAdapters.newFactory(Boolean.TYPE, Boolean.class, BOOLEAN);
    public static final TypeAdapter<Number> BYTE = new TypeAdapter<Number>(){

        @Override
        public Number read(JsonReader jsonReader) {
            int n;
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            try {
                n = jsonReader.nextInt();
            }
            catch (NumberFormatException numberFormatException) {
                throw new JsonSyntaxException(numberFormatException);
            }
            if (n > 255 || n < -128) {
                throw new JsonSyntaxException("Lossy conversion from " + n + " to byte; at path " + jsonReader.getPreviousPath());
            }
            return (byte)n;
        }

        @Override
        public void write(JsonWriter jsonWriter, Number number) {
            if (number == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(number.byteValue());
            }
        }
    };
    public static final TypeAdapterFactory BYTE_FACTORY = TypeAdapters.newFactory(Byte.TYPE, Byte.class, BYTE);
    public static final TypeAdapter<Number> SHORT = new TypeAdapter<Number>(){

        @Override
        public Number read(JsonReader jsonReader) {
            int n;
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            try {
                n = jsonReader.nextInt();
            }
            catch (NumberFormatException numberFormatException) {
                throw new JsonSyntaxException(numberFormatException);
            }
            if (n > 65535 || n < Short.MIN_VALUE) {
                throw new JsonSyntaxException("Lossy conversion from " + n + " to short; at path " + jsonReader.getPreviousPath());
            }
            return (short)n;
        }

        @Override
        public void write(JsonWriter jsonWriter, Number number) {
            if (number == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(number.shortValue());
            }
        }
    };
    public static final TypeAdapterFactory SHORT_FACTORY = TypeAdapters.newFactory(Short.TYPE, Short.class, SHORT);
    public static final TypeAdapter<Number> INTEGER = new TypeAdapter<Number>(){

        @Override
        public Number read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            try {
                return jsonReader.nextInt();
            }
            catch (NumberFormatException numberFormatException) {
                throw new JsonSyntaxException(numberFormatException);
            }
        }

        @Override
        public void write(JsonWriter jsonWriter, Number number) {
            if (number == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(number.intValue());
            }
        }
    };
    public static final TypeAdapterFactory INTEGER_FACTORY = TypeAdapters.newFactory(Integer.TYPE, Integer.class, INTEGER);
    public static final TypeAdapter<AtomicInteger> ATOMIC_INTEGER = new TypeAdapter<AtomicInteger>(){

        @Override
        public AtomicInteger read(JsonReader jsonReader) {
            try {
                return new AtomicInteger(jsonReader.nextInt());
            }
            catch (NumberFormatException numberFormatException) {
                throw new JsonSyntaxException(numberFormatException);
            }
        }

        @Override
        public void write(JsonWriter jsonWriter, AtomicInteger atomicInteger) {
            jsonWriter.value(atomicInteger.get());
        }
    }.nullSafe();
    public static final TypeAdapterFactory ATOMIC_INTEGER_FACTORY = TypeAdapters.newFactory(AtomicInteger.class, ATOMIC_INTEGER);
    public static final TypeAdapter<AtomicBoolean> ATOMIC_BOOLEAN = new TypeAdapter<AtomicBoolean>(){

        @Override
        public AtomicBoolean read(JsonReader jsonReader) {
            return new AtomicBoolean(jsonReader.nextBoolean());
        }

        @Override
        public void write(JsonWriter jsonWriter, AtomicBoolean atomicBoolean) {
            jsonWriter.value(atomicBoolean.get());
        }
    }.nullSafe();
    public static final TypeAdapterFactory ATOMIC_BOOLEAN_FACTORY = TypeAdapters.newFactory(AtomicBoolean.class, ATOMIC_BOOLEAN);
    public static final TypeAdapter<AtomicIntegerArray> ATOMIC_INTEGER_ARRAY = new TypeAdapter<AtomicIntegerArray>(){

        @Override
        public AtomicIntegerArray read(JsonReader jsonReader) {
            int n;
            ArrayList<Integer> arrayList = new ArrayList<Integer>();
            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                try {
                    n = jsonReader.nextInt();
                    arrayList.add(n);
                }
                catch (NumberFormatException numberFormatException) {
                    throw new JsonSyntaxException(numberFormatException);
                }
            }
            jsonReader.endArray();
            n = arrayList.size();
            AtomicIntegerArray atomicIntegerArray = new AtomicIntegerArray(n);
            for (int i = 0; i < n; ++i) {
                atomicIntegerArray.set(i, (Integer)arrayList.get(i));
            }
            return atomicIntegerArray;
        }

        @Override
        public void write(JsonWriter jsonWriter, AtomicIntegerArray atomicIntegerArray) {
            jsonWriter.beginArray();
            int n = atomicIntegerArray.length();
            for (int i = 0; i < n; ++i) {
                jsonWriter.value(atomicIntegerArray.get(i));
            }
            jsonWriter.endArray();
        }
    }.nullSafe();
    public static final TypeAdapterFactory ATOMIC_INTEGER_ARRAY_FACTORY = TypeAdapters.newFactory(AtomicIntegerArray.class, ATOMIC_INTEGER_ARRAY);
    public static final TypeAdapter<Number> LONG = new TypeAdapter<Number>(){

        @Override
        public Number read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            try {
                return jsonReader.nextLong();
            }
            catch (NumberFormatException numberFormatException) {
                throw new JsonSyntaxException(numberFormatException);
            }
        }

        @Override
        public void write(JsonWriter jsonWriter, Number number) {
            if (number == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(number.longValue());
            }
        }
    };
    public static final TypeAdapter<Number> FLOAT = new TypeAdapter<Number>(){

        @Override
        public Number read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return Float.valueOf((float)jsonReader.nextDouble());
        }

        @Override
        public void write(JsonWriter jsonWriter, Number number) {
            if (number == null) {
                jsonWriter.nullValue();
            } else {
                Number number2 = number instanceof Float ? (Number)number : (Number)Float.valueOf(number.floatValue());
                jsonWriter.value(number2);
            }
        }
    };
    public static final TypeAdapter<Number> DOUBLE = new TypeAdapter<Number>(){

        @Override
        public Number read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return jsonReader.nextDouble();
        }

        @Override
        public void write(JsonWriter jsonWriter, Number number) {
            if (number == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(number.doubleValue());
            }
        }
    };
    public static final TypeAdapter<Character> CHARACTER = new TypeAdapter<Character>(){

        @Override
        public Character read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            String string = jsonReader.nextString();
            if (string.length() != 1) {
                throw new JsonSyntaxException("Expecting character, got: " + string + "; at " + jsonReader.getPreviousPath());
            }
            return Character.valueOf(string.charAt(0));
        }

        @Override
        public void write(JsonWriter jsonWriter, Character c) {
            jsonWriter.value(c == null ? null : String.valueOf(c));
        }
    };
    public static final TypeAdapterFactory CHARACTER_FACTORY = TypeAdapters.newFactory(Character.TYPE, Character.class, CHARACTER);
    public static final TypeAdapter<String> STRING = new TypeAdapter<String>(){

        @Override
        public String read(JsonReader jsonReader) {
            JsonToken jsonToken = jsonReader.peek();
            if (jsonToken == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            if (jsonToken == JsonToken.BOOLEAN) {
                return Boolean.toString(jsonReader.nextBoolean());
            }
            return jsonReader.nextString();
        }

        @Override
        public void write(JsonWriter jsonWriter, String string) {
            jsonWriter.value(string);
        }
    };
    public static final TypeAdapter<BigDecimal> BIG_DECIMAL = new TypeAdapter<BigDecimal>(){

        @Override
        public BigDecimal read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            String string = jsonReader.nextString();
            try {
                return new BigDecimal(string);
            }
            catch (NumberFormatException numberFormatException) {
                throw new JsonSyntaxException("Failed parsing '" + string + "' as BigDecimal; at path " + jsonReader.getPreviousPath(), numberFormatException);
            }
        }

        @Override
        public void write(JsonWriter jsonWriter, BigDecimal bigDecimal) {
            jsonWriter.value(bigDecimal);
        }
    };
    public static final TypeAdapter<BigInteger> BIG_INTEGER = new TypeAdapter<BigInteger>(){

        @Override
        public BigInteger read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            String string = jsonReader.nextString();
            try {
                return new BigInteger(string);
            }
            catch (NumberFormatException numberFormatException) {
                throw new JsonSyntaxException("Failed parsing '" + string + "' as BigInteger; at path " + jsonReader.getPreviousPath(), numberFormatException);
            }
        }

        @Override
        public void write(JsonWriter jsonWriter, BigInteger bigInteger) {
            jsonWriter.value(bigInteger);
        }
    };
    public static final TypeAdapter<LazilyParsedNumber> LAZILY_PARSED_NUMBER = new TypeAdapter<LazilyParsedNumber>(){

        @Override
        public LazilyParsedNumber read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return new LazilyParsedNumber(jsonReader.nextString());
        }

        @Override
        public void write(JsonWriter jsonWriter, LazilyParsedNumber lazilyParsedNumber) {
            jsonWriter.value(lazilyParsedNumber);
        }
    };
    public static final TypeAdapterFactory STRING_FACTORY = TypeAdapters.newFactory(String.class, STRING);
    public static final TypeAdapter<StringBuilder> STRING_BUILDER = new TypeAdapter<StringBuilder>(){

        @Override
        public StringBuilder read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return new StringBuilder(jsonReader.nextString());
        }

        @Override
        public void write(JsonWriter jsonWriter, StringBuilder stringBuilder) {
            jsonWriter.value(stringBuilder == null ? null : stringBuilder.toString());
        }
    };
    public static final TypeAdapterFactory STRING_BUILDER_FACTORY = TypeAdapters.newFactory(StringBuilder.class, STRING_BUILDER);
    public static final TypeAdapter<StringBuffer> STRING_BUFFER = new TypeAdapter<StringBuffer>(){

        @Override
        public StringBuffer read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return new StringBuffer(jsonReader.nextString());
        }

        @Override
        public void write(JsonWriter jsonWriter, StringBuffer stringBuffer) {
            jsonWriter.value(stringBuffer == null ? null : stringBuffer.toString());
        }
    };
    public static final TypeAdapterFactory STRING_BUFFER_FACTORY = TypeAdapters.newFactory(StringBuffer.class, STRING_BUFFER);
    public static final TypeAdapter<URL> URL = new TypeAdapter<URL>(){

        @Override
        public URL read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            String string = jsonReader.nextString();
            return "null".equals(string) ? null : new URL(string);
        }

        @Override
        public void write(JsonWriter jsonWriter, URL uRL) {
            jsonWriter.value(uRL == null ? null : uRL.toExternalForm());
        }
    };
    public static final TypeAdapterFactory URL_FACTORY = TypeAdapters.newFactory(URL.class, URL);
    public static final TypeAdapter<URI> URI = new TypeAdapter<URI>(){

        @Override
        public URI read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            try {
                String string = jsonReader.nextString();
                return "null".equals(string) ? null : new URI(string);
            }
            catch (URISyntaxException uRISyntaxException) {
                throw new JsonIOException(uRISyntaxException);
            }
        }

        @Override
        public void write(JsonWriter jsonWriter, URI uRI) {
            jsonWriter.value(uRI == null ? null : uRI.toASCIIString());
        }
    };
    public static final TypeAdapterFactory URI_FACTORY = TypeAdapters.newFactory(URI.class, URI);
    public static final TypeAdapter<InetAddress> INET_ADDRESS = new TypeAdapter<InetAddress>(){

        @Override
        public InetAddress read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return InetAddress.getByName(jsonReader.nextString());
        }

        @Override
        public void write(JsonWriter jsonWriter, InetAddress inetAddress) {
            jsonWriter.value(inetAddress == null ? null : inetAddress.getHostAddress());
        }
    };
    public static final TypeAdapterFactory INET_ADDRESS_FACTORY = TypeAdapters.newTypeHierarchyFactory(InetAddress.class, INET_ADDRESS);
    public static final TypeAdapter<UUID> UUID = new TypeAdapter<UUID>(){

        @Override
        public UUID read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            String string = jsonReader.nextString();
            try {
                return java.util.UUID.fromString(string);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                throw new JsonSyntaxException("Failed parsing '" + string + "' as UUID; at path " + jsonReader.getPreviousPath(), illegalArgumentException);
            }
        }

        @Override
        public void write(JsonWriter jsonWriter, UUID uUID) {
            jsonWriter.value(uUID == null ? null : uUID.toString());
        }
    };
    public static final TypeAdapterFactory UUID_FACTORY = TypeAdapters.newFactory(UUID.class, UUID);
    public static final TypeAdapter<Currency> CURRENCY = new TypeAdapter<Currency>(){

        @Override
        public Currency read(JsonReader jsonReader) {
            String string = jsonReader.nextString();
            try {
                return Currency.getInstance(string);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                throw new JsonSyntaxException("Failed parsing '" + string + "' as Currency; at path " + jsonReader.getPreviousPath(), illegalArgumentException);
            }
        }

        @Override
        public void write(JsonWriter jsonWriter, Currency currency) {
            jsonWriter.value(currency.getCurrencyCode());
        }
    }.nullSafe();
    public static final TypeAdapterFactory CURRENCY_FACTORY = TypeAdapters.newFactory(Currency.class, CURRENCY);
    public static final TypeAdapter<Calendar> CALENDAR = new TypeAdapter<Calendar>(){
        private static final String YEAR = "year";
        private static final String MONTH = "month";
        private static final String DAY_OF_MONTH = "dayOfMonth";
        private static final String HOUR_OF_DAY = "hourOfDay";
        private static final String MINUTE = "minute";
        private static final String SECOND = "second";

        @Override
        public Calendar read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            jsonReader.beginObject();
            int n = 0;
            int n2 = 0;
            int n3 = 0;
            int n4 = 0;
            int n5 = 0;
            int n6 = 0;
            while (jsonReader.peek() != JsonToken.END_OBJECT) {
                String string = jsonReader.nextName();
                int n7 = jsonReader.nextInt();
                if (YEAR.equals(string)) {
                    n = n7;
                    continue;
                }
                if (MONTH.equals(string)) {
                    n2 = n7;
                    continue;
                }
                if (DAY_OF_MONTH.equals(string)) {
                    n3 = n7;
                    continue;
                }
                if (HOUR_OF_DAY.equals(string)) {
                    n4 = n7;
                    continue;
                }
                if (MINUTE.equals(string)) {
                    n5 = n7;
                    continue;
                }
                if (!SECOND.equals(string)) continue;
                n6 = n7;
            }
            jsonReader.endObject();
            return new GregorianCalendar(n, n2, n3, n4, n5, n6);
        }

        @Override
        public void write(JsonWriter jsonWriter, Calendar calendar) {
            if (calendar == null) {
                jsonWriter.nullValue();
                return;
            }
            jsonWriter.beginObject();
            jsonWriter.name(YEAR);
            jsonWriter.value(calendar.get(1));
            jsonWriter.name(MONTH);
            jsonWriter.value(calendar.get(2));
            jsonWriter.name(DAY_OF_MONTH);
            jsonWriter.value(calendar.get(5));
            jsonWriter.name(HOUR_OF_DAY);
            jsonWriter.value(calendar.get(11));
            jsonWriter.name(MINUTE);
            jsonWriter.value(calendar.get(12));
            jsonWriter.name(SECOND);
            jsonWriter.value(calendar.get(13));
            jsonWriter.endObject();
        }
    };
    public static final TypeAdapterFactory CALENDAR_FACTORY = TypeAdapters.newFactoryForMultipleTypes(Calendar.class, GregorianCalendar.class, CALENDAR);
    public static final TypeAdapter<Locale> LOCALE = new TypeAdapter<Locale>(){

        @Override
        public Locale read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            String string = jsonReader.nextString();
            StringTokenizer stringTokenizer = new StringTokenizer(string, "_");
            String string2 = null;
            String string3 = null;
            String string4 = null;
            if (stringTokenizer.hasMoreElements()) {
                string2 = stringTokenizer.nextToken();
            }
            if (stringTokenizer.hasMoreElements()) {
                string3 = stringTokenizer.nextToken();
            }
            if (stringTokenizer.hasMoreElements()) {
                string4 = stringTokenizer.nextToken();
            }
            if (string3 == null && string4 == null) {
                return new Locale(string2);
            }
            if (string4 == null) {
                return new Locale(string2, string3);
            }
            return new Locale(string2, string3, string4);
        }

        @Override
        public void write(JsonWriter jsonWriter, Locale locale) {
            jsonWriter.value(locale == null ? null : locale.toString());
        }
    };
    public static final TypeAdapterFactory LOCALE_FACTORY = TypeAdapters.newFactory(Locale.class, LOCALE);
    public static final TypeAdapter<JsonElement> JSON_ELEMENT = new TypeAdapter<JsonElement>(){

        private JsonElement tryBeginNesting(JsonReader jsonReader, JsonToken jsonToken) {
            switch (jsonToken) {
                case BEGIN_ARRAY: {
                    jsonReader.beginArray();
                    return new JsonArray();
                }
                case BEGIN_OBJECT: {
                    jsonReader.beginObject();
                    return new JsonObject();
                }
            }
            return null;
        }

        private JsonElement readTerminal(JsonReader jsonReader, JsonToken jsonToken) {
            switch (jsonToken) {
                case STRING: {
                    return new JsonPrimitive(jsonReader.nextString());
                }
                case NUMBER: {
                    String string = jsonReader.nextString();
                    return new JsonPrimitive(new LazilyParsedNumber(string));
                }
                case BOOLEAN: {
                    return new JsonPrimitive(jsonReader.nextBoolean());
                }
                case NULL: {
                    jsonReader.nextNull();
                    return JsonNull.INSTANCE;
                }
            }
            throw new IllegalStateException("Unexpected token: " + (Object)((Object)jsonToken));
        }

        @Override
        public JsonElement read(JsonReader jsonReader) {
            if (jsonReader instanceof JsonTreeReader) {
                return ((JsonTreeReader)jsonReader).nextJsonElement();
            }
            JsonToken jsonToken = jsonReader.peek();
            JsonElement jsonElement = this.tryBeginNesting(jsonReader, jsonToken);
            if (jsonElement == null) {
                return this.readTerminal(jsonReader, jsonToken);
            }
            ArrayDeque<JsonElement> arrayDeque = new ArrayDeque<JsonElement>();
            while (true) {
                if (jsonReader.hasNext()) {
                    JsonElement jsonElement2;
                    boolean bl;
                    String string = null;
                    if (jsonElement instanceof JsonObject) {
                        string = jsonReader.nextName();
                    }
                    boolean bl2 = bl = (jsonElement2 = this.tryBeginNesting(jsonReader, jsonToken = jsonReader.peek())) != null;
                    if (jsonElement2 == null) {
                        jsonElement2 = this.readTerminal(jsonReader, jsonToken);
                    }
                    if (jsonElement instanceof JsonArray) {
                        ((JsonArray)jsonElement).add(jsonElement2);
                    } else {
                        ((JsonObject)jsonElement).add(string, jsonElement2);
                    }
                    if (!bl) continue;
                    arrayDeque.addLast(jsonElement);
                    jsonElement = jsonElement2;
                    continue;
                }
                if (jsonElement instanceof JsonArray) {
                    jsonReader.endArray();
                } else {
                    jsonReader.endObject();
                }
                if (arrayDeque.isEmpty()) {
                    return jsonElement;
                }
                jsonElement = (JsonElement)arrayDeque.removeLast();
            }
        }

        @Override
        public void write(JsonWriter jsonWriter, JsonElement jsonElement) {
            if (jsonElement == null || jsonElement.isJsonNull()) {
                jsonWriter.nullValue();
            } else if (jsonElement.isJsonPrimitive()) {
                JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
                if (jsonPrimitive.isNumber()) {
                    jsonWriter.value(jsonPrimitive.getAsNumber());
                } else if (jsonPrimitive.isBoolean()) {
                    jsonWriter.value(jsonPrimitive.getAsBoolean());
                } else {
                    jsonWriter.value(jsonPrimitive.getAsString());
                }
            } else if (jsonElement.isJsonArray()) {
                jsonWriter.beginArray();
                for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
                    this.write(jsonWriter, jsonElement2);
                }
                jsonWriter.endArray();
            } else if (jsonElement.isJsonObject()) {
                jsonWriter.beginObject();
                for (Map.Entry<String, JsonElement> entry : jsonElement.getAsJsonObject().entrySet()) {
                    jsonWriter.name(entry.getKey());
                    this.write(jsonWriter, entry.getValue());
                }
                jsonWriter.endObject();
            } else {
                throw new IllegalArgumentException("Couldn't write " + jsonElement.getClass());
            }
        }
    };
    public static final TypeAdapterFactory JSON_ELEMENT_FACTORY = TypeAdapters.newTypeHierarchyFactory(JsonElement.class, JSON_ELEMENT);
    public static final TypeAdapterFactory ENUM_FACTORY = new TypeAdapterFactory(){

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            Class<T> clazz = typeToken.getRawType();
            if (!Enum.class.isAssignableFrom(clazz) || clazz == Enum.class) {
                return null;
            }
            if (!clazz.isEnum()) {
                clazz = clazz.getSuperclass();
            }
            EnumTypeAdapter<T> enumTypeAdapter = new EnumTypeAdapter<T>(clazz);
            return enumTypeAdapter;
        }
    };

    private TypeAdapters() {
        throw new UnsupportedOperationException();
    }

    public static <TT> TypeAdapterFactory newFactory(final TypeToken<TT> typeToken, final TypeAdapter<TT> typeAdapter) {
        return new TypeAdapterFactory(){

            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken2) {
                return typeToken2.equals(typeToken) ? typeAdapter : null;
            }
        };
    }

    public static <TT> TypeAdapterFactory newFactory(final Class<TT> clazz, final TypeAdapter<TT> typeAdapter) {
        return new TypeAdapterFactory(){

            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
                return typeToken.getRawType() == clazz ? typeAdapter : null;
            }

            public String toString() {
                return "Factory[type=" + clazz.getName() + ",adapter=" + typeAdapter + "]";
            }
        };
    }

    public static <TT> TypeAdapterFactory newFactory(final Class<TT> clazz, final Class<TT> clazz2, final TypeAdapter<? super TT> typeAdapter) {
        return new TypeAdapterFactory(){

            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
                Class<T> clazz3 = typeToken.getRawType();
                return clazz3 == clazz || clazz3 == clazz2 ? typeAdapter : null;
            }

            public String toString() {
                return "Factory[type=" + clazz2.getName() + "+" + clazz.getName() + ",adapter=" + typeAdapter + "]";
            }
        };
    }

    public static <TT> TypeAdapterFactory newFactoryForMultipleTypes(final Class<TT> clazz, final Class<? extends TT> clazz2, final TypeAdapter<? super TT> typeAdapter) {
        return new TypeAdapterFactory(){

            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
                Class<T> clazz3 = typeToken.getRawType();
                return clazz3 == clazz || clazz3 == clazz2 ? typeAdapter : null;
            }

            public String toString() {
                return "Factory[type=" + clazz.getName() + "+" + clazz2.getName() + ",adapter=" + typeAdapter + "]";
            }
        };
    }

    public static <T1> TypeAdapterFactory newTypeHierarchyFactory(final Class<T1> clazz, final TypeAdapter<T1> typeAdapter) {
        return new TypeAdapterFactory(){

            public <T2> TypeAdapter<T2> create(Gson gson, TypeToken<T2> typeToken) {
                final Class<T2> clazz2 = typeToken.getRawType();
                if (!clazz.isAssignableFrom(clazz2)) {
                    return null;
                }
                return new TypeAdapter<T1>(){

                    @Override
                    public void write(JsonWriter jsonWriter, T1 T1) {
                        typeAdapter.write(jsonWriter, T1);
                    }

                    @Override
                    public T1 read(JsonReader jsonReader) {
                        Object t = typeAdapter.read(jsonReader);
                        if (t != null && !clazz2.isInstance(t)) {
                            throw new JsonSyntaxException("Expected a " + clazz2.getName() + " but was " + t.getClass().getName() + "; at path " + jsonReader.getPreviousPath());
                        }
                        return t;
                    }
                };
            }

            public String toString() {
                return "Factory[typeHierarchy=" + clazz.getName() + ",adapter=" + typeAdapter + "]";
            }
        };
    }

    private static final class EnumTypeAdapter<T extends Enum<T>>
    extends TypeAdapter<T> {
        private final Map<String, T> nameToConstant = new HashMap<String, T>();
        private final Map<String, T> stringToConstant = new HashMap<String, T>();
        private final Map<T, String> constantToName = new HashMap<T, String>();

        public EnumTypeAdapter(final Class<T> clazz) {
            try {
                Field[] fieldArray;
                for (Field field : fieldArray = AccessController.doPrivileged(new PrivilegedAction<Field[]>(){

                    @Override
                    public Field[] run() {
                        AccessibleObject[] accessibleObjectArray = clazz.getDeclaredFields();
                        ArrayList<Field> arrayList = new ArrayList<Field>(accessibleObjectArray.length);
                        for (Field field : accessibleObjectArray) {
                            if (!field.isEnumConstant()) continue;
                            arrayList.add(field);
                        }
                        AccessibleObject[] accessibleObjectArray2 = arrayList.toArray(new Field[0]);
                        AccessibleObject.setAccessible(accessibleObjectArray2, true);
                        return accessibleObjectArray2;
                    }
                })) {
                    Enum enum_ = (Enum)field.get(null);
                    String string = enum_.name();
                    String string2 = enum_.toString();
                    SerializedName serializedName = field.getAnnotation(SerializedName.class);
                    if (serializedName != null) {
                        string = serializedName.value();
                        for (String string3 : serializedName.alternate()) {
                            this.nameToConstant.put(string3, enum_);
                        }
                    }
                    this.nameToConstant.put(string, enum_);
                    this.stringToConstant.put(string2, enum_);
                    this.constantToName.put(enum_, string);
                }
            }
            catch (IllegalAccessException illegalAccessException) {
                throw new AssertionError((Object)illegalAccessException);
            }
        }

        @Override
        public T read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            String string = jsonReader.nextString();
            Enum enum_ = (Enum)this.nameToConstant.get(string);
            return (T)(enum_ == null ? (Enum)this.stringToConstant.get(string) : enum_);
        }

        @Override
        public void write(JsonWriter jsonWriter, T t) {
            jsonWriter.value(t == null ? null : this.constantToName.get(t));
        }
    }
}

