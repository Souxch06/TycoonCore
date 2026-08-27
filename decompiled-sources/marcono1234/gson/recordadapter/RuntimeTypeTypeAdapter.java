/*
 * Decompiled with CFR 0.152.
 */
package marcono1234.gson.recordadapter;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

class RuntimeTypeTypeAdapter<T>
extends TypeAdapter<T> {
    private final Gson gson;
    private final TypeAdapter<T> delegate;

    RuntimeTypeTypeAdapter(Gson gson, TypeAdapter<T> typeAdapter) {
        this.gson = gson;
        this.delegate = typeAdapter;
    }

    @Override
    public void write(JsonWriter jsonWriter, T t) {
        if (t == null) {
            this.delegate.write(jsonWriter, null);
        } else {
            TypeAdapter<?> typeAdapter = this.gson.getAdapter(TypeToken.get(t.getClass()));
            typeAdapter.write(jsonWriter, t);
        }
    }

    @Override
    public T read(JsonReader jsonReader) {
        return this.delegate.read(jsonReader);
    }
}

