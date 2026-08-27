package com.google.gson.internal.sql;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

final class SqlDateTypeAdapter
extends TypeAdapter<java.sql.Date> {
    static final TypeAdapterFactory FACTORY = new TypeAdapterFactory(){

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            return typeToken.getRawType() == java.sql.Date.class ? new SqlDateTypeAdapter() : null;
        }
    };
    private final DateFormat format = new SimpleDateFormat("MMM d, yyyy");

    private SqlDateTypeAdapter() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public java.sql.Date read(JsonReader jsonReader) {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }
        String string = jsonReader.nextString();
        try {
            Date date;
            SqlDateTypeAdapter sqlDateTypeAdapter = this;
            synchronized (sqlDateTypeAdapter) {
                date = this.format.parse(string);
            }
            return new java.sql.Date(date.getTime());
        }
        catch (ParseException parseException) {
            throw new JsonSyntaxException("Failed parsing '" + string + "' as SQL Date; at path " + jsonReader.getPreviousPath(), parseException);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void write(JsonWriter jsonWriter, java.sql.Date date) {
        String string;
        if (date == null) {
            jsonWriter.nullValue();
            return;
        }
        SqlDateTypeAdapter sqlDateTypeAdapter = this;
        synchronized (sqlDateTypeAdapter) {
            string = this.format.format(date);
        }
        jsonWriter.value(string);
    }
}

