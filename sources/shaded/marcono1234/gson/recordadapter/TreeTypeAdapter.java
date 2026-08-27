package marcono1234.gson.recordadapter;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.lang.reflect.Type;

class TreeTypeAdapter<T>
extends TypeAdapter<T> {
    private final JsonSerializer<T> serializer;
    private final JsonDeserializer<T> deserializer;
    private final Gson gson;
    private final TypeToken<T> type;
    private final TypeAdapter<JsonElement> jsonElementAdapter;
    private final GsonContext context;
    private volatile TypeAdapter<T> delegate;

    TreeTypeAdapter(JsonSerializer<T> jsonSerializer, JsonDeserializer<T> jsonDeserializer, Gson gson, TypeToken<T> typeToken) {
        this.serializer = jsonSerializer;
        this.deserializer = jsonDeserializer;
        this.gson = gson;
        this.type = typeToken;
        this.jsonElementAdapter = gson.getAdapter(JsonElement.class);
        this.context = new GsonContext(gson);
    }

    private TypeAdapter<T> delegate() {
        TypeAdapter<T> typeAdapter = this.delegate;
        if (typeAdapter == null) {
            this.delegate = typeAdapter = this.gson.getAdapter(this.type);
        }
        return typeAdapter;
    }

    @Override
    public void write(JsonWriter jsonWriter, T t) {
        if (this.serializer == null) {
            this.delegate().write(jsonWriter, t);
        } else {
            JsonElement jsonElement = this.serializer.serialize(t, this.type.getType(), this.context);
            this.jsonElementAdapter.write(jsonWriter, jsonElement);
        }
    }

    @Override
    public T read(JsonReader jsonReader) {
        if (this.deserializer == null) {
            return this.delegate().read(jsonReader);
        }
        JsonElement jsonElement = this.jsonElementAdapter.read(jsonReader);
        return this.deserializer.deserialize(jsonElement, this.type.getType(), this.context);
    }

    private static class GsonContext
    implements JsonSerializationContext,
    JsonDeserializationContext {
        private final Gson gson;

        private GsonContext(Gson gson) {
            this.gson = gson;
        }

        @Override
        public <T> T deserialize(JsonElement jsonElement, Type type) {
            return this.gson.fromJson(jsonElement, type);
        }

        @Override
        public JsonElement serialize(Object object) {
            return this.gson.toJsonTree(object);
        }

        @Override
        public JsonElement serialize(Object object, Type type) {
            return this.gson.toJsonTree(object, type);
        }
    }
}

