package marcono1234.gson.recordadapter;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.Until;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.lang.annotation.Annotation;
import java.lang.constant.Constable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import marcono1234.gson.recordadapter.ComponentTypeHelper;
import marcono1234.gson.recordadapter.JsonAdapterCreator;
import marcono1234.gson.recordadapter.RecordComponentNamingStrategy;
import marcono1234.gson.recordadapter.RecordTypeAdapterException;
import marcono1234.gson.recordadapter.RuntimeTypeTypeAdapter;
import marcono1234.gson.recordadapter.TreeTypeAdapter;

public class RecordTypeAdapterFactory
implements TypeAdapterFactory {
    private static final boolean DEFAULT_SERIALIZE_RUNTIME_COMPONENT_TYPES = false;
    private static final boolean DEFAULT_ALLOW_MISSING_COMPONENT_VALUES = false;
    private static final boolean DEFAULT_ALLOW_UNKNOWN_PROPERTIES = true;
    private static final boolean DEFAULT_ALLOW_DUPLICATE_COMPONENT_VALUES = false;
    private static final boolean DEFAULT_ALLOW_JSON_NULL_FOR_PRIMITIVES = false;
    private static final RecordComponentNamingStrategy DEFAULT_NAMING_STRATEGY = RecordComponentNamingStrategy.IDENTITY;
    private static final JsonAdapterCreator DEFAULT_JSON_ADAPTER_CREATOR = JsonAdapterCreator.DEFAULT_CONSTRUCTOR_INVOKER;
    private static final List<Class<? extends Annotation>> UNSUPPORTED_FIELD_ANNOTATIONS = List.of(Expose.class, Since.class, Until.class);
    public static final RecordTypeAdapterFactory DEFAULT = RecordTypeAdapterFactory.builder().create();
    private final boolean serializeRuntimeComponentTypes;
    private final boolean allowMissingComponentValues;
    private final boolean allowUnknownProperties;
    private final boolean allowDuplicateComponentValues;
    private final boolean allowJsonNullForPrimitives;
    private final RecordComponentNamingStrategy namingStrategy;
    private final List<JsonAdapterCreator> jsonAdapterCreators;
    private static final Byte DEFAULT_BYTE = 0;
    private static final Short DEFAULT_SHORT = 0;
    private static final Integer DEFAULT_INT = 0;
    private static final Long DEFAULT_LONG = 0L;
    private static final Float DEFAULT_FLOAT = Float.valueOf(0.0f);
    private static final Double DEFAULT_DOUBLE = 0.0;
    private static final Character DEFAULT_CHAR = Character.valueOf('\u0000');

    @Deprecated
    public RecordTypeAdapterFactory() {
        this(false, false, true, false, false, DEFAULT_NAMING_STRATEGY, List.of(DEFAULT_JSON_ADAPTER_CREATOR));
    }

    public static Builder builder() {
        return new Builder();
    }

    private RecordTypeAdapterFactory(boolean bl, boolean bl2, boolean bl3, boolean bl4, boolean bl5, RecordComponentNamingStrategy recordComponentNamingStrategy, List<JsonAdapterCreator> list) {
        this.serializeRuntimeComponentTypes = bl;
        this.allowMissingComponentValues = bl2;
        this.allowUnknownProperties = bl3;
        this.allowDuplicateComponentValues = bl4;
        this.allowJsonNullForPrimitives = bl5;
        this.namingStrategy = recordComponentNamingStrategy;
        this.jsonAdapterCreators = list;
        assert (!list.isEmpty());
    }

    private static Field getComponentField(RecordComponent recordComponent) {
        try {
            return recordComponent.getDeclaringRecord().getDeclaredField(recordComponent.getName());
        }
        catch (NoSuchFieldException noSuchFieldException) {
            throw new RecordTypeAdapterException("Unexpected: Failed finding component field for " + recordComponent);
        }
    }

    private ComponentNames getComponentNames(RecordComponent recordComponent) {
        SerializedName serializedName = RecordTypeAdapterFactory.getComponentField(recordComponent).getAnnotation(SerializedName.class);
        SerializedName serializedName2 = recordComponent.getAccessor().getAnnotation(SerializedName.class);
        if (serializedName == null) {
            if (serializedName2 != null) {
                throw new RecordTypeAdapterException("@SerializedName on accessor method is not supported; place it on the corresponding record component instead");
            }
            String string = Objects.requireNonNull(this.namingStrategy.translateName(recordComponent));
            return new ComponentNames(string, Set.of(string));
        }
        if (serializedName2 != null && !serializedName.equals(serializedName2)) {
            throw new RecordTypeAdapterException("Using different @SerializedName on accessor than on corresponding record component is not supported");
        }
        String string = serializedName.value();
        String[] stringArray = serializedName.alternate();
        if (stringArray.length == 0) {
            return new ComponentNames(string, Set.of(string));
        }
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>();
        linkedHashSet.add(string);
        for (String string2 : stringArray) {
            if (linkedHashSet.add(string2)) continue;
            throw new RecordTypeAdapterException("Duplicate property name '" + string2 + "' for " + RecordTypeAdapterFactory.getComponentDisplayString(recordComponent));
        }
        return new ComponentNames(string, linkedHashSet);
    }

    private static boolean needsRuntimeTypeTypeAdapter(Type type) {
        if (type instanceof Class) {
            Class clazz = (Class)type;
            return !clazz.isPrimitive() && !Modifier.isFinal(clazz.getModifiers());
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            return RecordTypeAdapterFactory.needsRuntimeTypeTypeAdapter(parameterizedType.getRawType());
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType)type;
            return RecordTypeAdapterFactory.needsRuntimeTypeTypeAdapter(genericArrayType.getGenericComponentType());
        }
        return true;
    }

    private TypeAdapter<?> getAdapter(RecordComponent recordComponent, Type type, Gson gson) {
        TypeAdapter<Object> typeAdapter;
        Object object;
        TypeToken<?> typeToken = TypeToken.get(type);
        Field field = RecordTypeAdapterFactory.getComponentField(recordComponent);
        List<Class> list = UNSUPPORTED_FIELD_ANNOTATIONS.stream().filter(field::isAnnotationPresent).toList();
        if (!list.isEmpty()) {
            String string = list.stream().map(clazz -> "@" + clazz.getSimpleName()).collect(Collectors.joining(", "));
            throw new RecordTypeAdapterException("Unsupported annotations on component " + RecordTypeAdapterFactory.getComponentDisplayString(recordComponent) + ": " + string);
        }
        JsonAdapter jsonAdapter = field.getAnnotation(JsonAdapter.class);
        if (jsonAdapter == null) {
            TypeAdapter<?> typeAdapter2 = gson.getAdapter(typeToken);
            if (this.serializeRuntimeComponentTypes && RecordTypeAdapterFactory.needsRuntimeTypeTypeAdapter(type)) {
                return new RuntimeTypeTypeAdapter(gson, typeAdapter2);
            }
            return typeAdapter2;
        }
        Class<?> clazz2 = jsonAdapter.value();
        JsonAdapterCreator object2 = null;
        Object object3 = null;
        for (JsonAdapterCreator object4 : this.jsonAdapterCreators) {
            try {
                object = object4.create(clazz2);
            }
            catch (JsonAdapterCreator.AdapterCreationException jsonDeserializer) {
                throw new RecordTypeAdapterException("Creator " + object4 + " failed creating instance of adapter " + clazz2 + " for " + RecordTypeAdapterFactory.getComponentDisplayString(recordComponent), jsonDeserializer);
            }
            if (!((Optional)object).isPresent()) continue;
            object3 = ((Optional)object).get();
            object2 = object4;
            break;
        }
        if (object2 == null) {
            typeAdapter = this.jsonAdapterCreators.stream().map(Object::toString).collect(Collectors.joining(", "));
            throw new RecordTypeAdapterException("None of the creators can create an instance of adapter " + clazz2 + " for " + RecordTypeAdapterFactory.getComponentDisplayString(recordComponent) + "; registered creators: " + typeAdapter);
        }
        if (object3 instanceof TypeAdapter) {
            typeAdapter = (TypeAdapter)object3;
        } else if (object3 instanceof TypeAdapterFactory) {
            TypeAdapterFactory typeAdapterFactory = (TypeAdapterFactory)object3;
            typeAdapter = typeAdapterFactory.create(gson, typeToken);
            if (typeAdapter == null) {
                throw new RecordTypeAdapterException("Factory " + typeAdapterFactory + " of type " + typeAdapterFactory.getClass().getName() + " does not support type " + typeToken + " of component " + RecordTypeAdapterFactory.getComponentDisplayString(recordComponent));
            }
        } else if (object3 instanceof JsonSerializer || object3 instanceof JsonDeserializer) {
            object = object3 instanceof JsonSerializer ? (JsonSerializer)object3 : null;
            JsonDeserializer jsonDeserializer = object3 instanceof JsonDeserializer ? (JsonDeserializer)object3 : null;
            TreeTypeAdapter treeTypeAdapter = new TreeTypeAdapter((JsonSerializer<?>)object, jsonDeserializer, gson, typeToken);
            typeAdapter = treeTypeAdapter;
        } else {
            throw new RecordTypeAdapterException("Adapter " + object3 + " of type " + object3.getClass().getName() + " created by " + object2 + " for " + RecordTypeAdapterFactory.getComponentDisplayString(recordComponent) + " is not supported");
        }
        return jsonAdapter.nullSafe() ? typeAdapter.nullSafe() : typeAdapter;
    }

    private static String getComponentDisplayString(RecordComponent recordComponent) {
        return recordComponent.getDeclaringRecord().getName() + "." + recordComponent.getName();
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        int n;
        final Class<T> clazz = typeToken.getRawType();
        if (!clazz.isRecord()) {
            return null;
        }
        final RecordComponent[] recordComponentArray = clazz.getRecordComponents();
        final Constructor<?> constructor = RecordTypeAdapterFactory.getCanonicalConstructor(clazz, recordComponentArray);
        Type[] typeArray = new Type[recordComponentArray.length];
        final Method[] methodArray = new Method[recordComponentArray.length];
        final String[] stringArray = new String[recordComponentArray.length];
        final HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        final TypeAdapter[] typeAdapterArray = new TypeAdapter[recordComponentArray.length];
        for (n = 0; n < recordComponentArray.length; ++n) {
            RecordComponent recordComponent = recordComponentArray[n];
            typeArray[n] = recordComponent.getGenericType();
            Method method = recordComponent.getAccessor();
            try {
                method.setAccessible(true);
            }
            catch (InaccessibleObjectException inaccessibleObjectException) {
                throw new RecordTypeAdapterException("Cannot access accessor method for " + RecordTypeAdapterFactory.getComponentDisplayString(recordComponent) + "; either change the visibility of the record class to `public` or open it to this library", inaccessibleObjectException);
            }
            methodArray[n] = method;
            ComponentNames componentNames = this.getComponentNames(recordComponent);
            String string = componentNames.serializationName;
            for (int i = 0; i < n; ++i) {
                if (!stringArray[i].equals(string)) continue;
                throw new RecordTypeAdapterException("Property name '" + string + "' for " + RecordTypeAdapterFactory.getComponentDisplayString(recordComponent) + " clashes with name of other component");
            }
            stringArray[n] = string;
            for (String string2 : componentNames.deserializationNames) {
                if (hashMap.put(string2, n) == null) continue;
                throw new RecordTypeAdapterException("Property name '" + string2 + "' for " + RecordTypeAdapterFactory.getComponentDisplayString(recordComponent) + " clashes with name of other component");
            }
        }
        typeArray = ComponentTypeHelper.resolveComponentTypes(typeToken, typeArray);
        for (n = 0; n < recordComponentArray.length; ++n) {
            typeAdapterArray[n] = this.getAdapter(recordComponentArray[n], typeArray[n], gson);
        }
        return new TypeAdapter<T>(){

            @Override
            public void write(JsonWriter jsonWriter, T t) {
                if (t == null) {
                    jsonWriter.nullValue();
                    return;
                }
                jsonWriter.beginObject();
                for (int i = 0; i < methodArray.length; ++i) {
                    Object object;
                    try {
                        object = methodArray[i].invoke(t, new Object[0]);
                    }
                    catch (IllegalAccessException | IllegalArgumentException | NullPointerException exception) {
                        throw new JsonParseException("Failed getting component value", exception);
                    }
                    catch (InvocationTargetException invocationTargetException) {
                        throw new JsonParseException("Failed getting component value", invocationTargetException.getCause());
                    }
                    jsonWriter.name(stringArray[i]);
                    TypeAdapter typeAdapter = typeAdapterArray[i];
                    typeAdapter.write(jsonWriter, object);
                }
                jsonWriter.endObject();
            }

            @Override
            public T read(JsonReader jsonReader) {
                Constable constable;
                if (jsonReader.peek() == JsonToken.NULL) {
                    jsonReader.skipValue();
                    return null;
                }
                Object[] objectArray = new Object[recordComponentArray.length];
                boolean[] blArray = new boolean[objectArray.length];
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String string = jsonReader.nextName();
                    constable = (Integer)hashMap.get(string);
                    if (constable == null) {
                        if (RecordTypeAdapterFactory.this.allowUnknownProperties) {
                            jsonReader.skipValue();
                            continue;
                        }
                        throw new JsonParseException("Unknown property '" + string + "' for " + clazz + " at JSON path " + jsonReader.getPath());
                    }
                    RecordComponent recordComponent = recordComponentArray[(Integer)constable];
                    if (!RecordTypeAdapterFactory.this.allowDuplicateComponentValues && blArray[(Integer)constable]) {
                        throw new JsonParseException("Duplicate value for " + RecordTypeAdapterFactory.getComponentDisplayString(recordComponent) + " provided by property '" + string + "' at JSON path " + jsonReader.getPath());
                    }
                    Class<?> clazz2 = recordComponent.getType();
                    boolean bl = clazz2.isPrimitive();
                    if (!RecordTypeAdapterFactory.this.allowJsonNullForPrimitives && bl && jsonReader.peek() == JsonToken.NULL) {
                        throw new JsonParseException("JSON null is not allowed for primitive " + RecordTypeAdapterFactory.getComponentDisplayString(recordComponent) + " provided by property '" + string + "' at JSON path " + jsonReader.getPath());
                    }
                    Object object = typeAdapterArray[(Integer)constable].read(jsonReader);
                    if (bl && object == null) {
                        object = RecordTypeAdapterFactory.getPrimitiveDefaultValue(clazz2);
                    }
                    objectArray[((Integer)constable).intValue()] = object;
                    blArray[((Integer)constable).intValue()] = true;
                }
                for (int i = 0; i < recordComponentArray.length; ++i) {
                    if (blArray[i]) continue;
                    if (!RecordTypeAdapterFactory.this.allowMissingComponentValues) {
                        throw new JsonParseException("Missing value for " + RecordTypeAdapterFactory.getComponentDisplayString(recordComponentArray[i]) + "; last property is at JSON path " + jsonReader.getPath());
                    }
                    constable = recordComponentArray[i].getType();
                    if (!((Class)constable).isPrimitive()) continue;
                    objectArray[i] = RecordTypeAdapterFactory.getPrimitiveDefaultValue(constable);
                }
                jsonReader.endObject();
                try {
                    Object t = constructor.newInstance(objectArray);
                    return t;
                }
                catch (IllegalAccessException | IllegalArgumentException | InstantiationException exception) {
                    throw new JsonParseException("Failed creating record instance for " + clazz, exception);
                }
                catch (InvocationTargetException invocationTargetException) {
                    throw new JsonParseException("Failed creating record instance for " + clazz, invocationTargetException.getCause());
                }
            }
        };
    }

    private static Constructor<?> getCanonicalConstructor(Class<?> clazz, RecordComponent[] recordComponentArray) {
        Constructor<?> constructor;
        Class[] classArray = new Class[recordComponentArray.length];
        for (int i = 0; i < recordComponentArray.length; ++i) {
            classArray[i] = recordComponentArray[i].getType();
        }
        try {
            constructor = clazz.getDeclaredConstructor(classArray);
        }
        catch (NoSuchMethodException noSuchMethodException) {
            throw new RecordTypeAdapterException("Unexpected: Failed finding canonical constructor for " + clazz, noSuchMethodException);
        }
        try {
            constructor.setAccessible(true);
        }
        catch (InaccessibleObjectException inaccessibleObjectException) {
            throw new RecordTypeAdapterException("Cannot access canonical constructor of " + clazz + "; either change the visibility of the record class to `public` or open it to this library", inaccessibleObjectException);
        }
        return constructor;
    }

    private static Object getPrimitiveDefaultValue(Class<?> clazz) {
        if (clazz == Byte.TYPE) {
            return DEFAULT_BYTE;
        }
        if (clazz == Short.TYPE) {
            return DEFAULT_SHORT;
        }
        if (clazz == Integer.TYPE) {
            return DEFAULT_INT;
        }
        if (clazz == Long.TYPE) {
            return DEFAULT_LONG;
        }
        if (clazz == Float.TYPE) {
            return DEFAULT_FLOAT;
        }
        if (clazz == Double.TYPE) {
            return DEFAULT_DOUBLE;
        }
        if (clazz == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (clazz == Character.TYPE) {
            return DEFAULT_CHAR;
        }
        throw new AssertionError((Object)("Not primitive: " + clazz));
    }

    public static class Builder {
        private boolean serializeRuntimeComponentTypes = false;
        private boolean allowMissingComponentValues = false;
        private boolean allowUnknownProperties = true;
        private boolean allowDuplicateComponentValues = false;
        private boolean allowJsonNullForPrimitives = false;
        private RecordComponentNamingStrategy namingStrategy = DEFAULT_NAMING_STRATEGY;
        private final List<JsonAdapterCreator> jsonAdapterCreators = new ArrayList<JsonAdapterCreator>();

        private Builder() {
            this.jsonAdapterCreators.add(DEFAULT_JSON_ADAPTER_CREATOR);
        }

        public Builder serializeRuntimeComponentTypes() {
            this.serializeRuntimeComponentTypes = true;
            return this;
        }

        public Builder allowMissingComponentValues() {
            this.allowMissingComponentValues = true;
            return this;
        }

        public Builder disallowUnknownProperties() {
            this.allowUnknownProperties = false;
            return this;
        }

        public Builder allowDuplicateComponentValues() {
            this.allowDuplicateComponentValues = true;
            return this;
        }

        public Builder allowJsonNullForPrimitiveComponents() {
            this.allowJsonNullForPrimitives = true;
            return this;
        }

        public Builder withComponentNamingStrategy(RecordComponentNamingStrategy recordComponentNamingStrategy) {
            this.namingStrategy = Objects.requireNonNull(recordComponentNamingStrategy);
            return this;
        }

        public Builder registerJsonAdapterCreator(JsonAdapterCreator jsonAdapterCreator) {
            this.jsonAdapterCreators.add(Objects.requireNonNull(jsonAdapterCreator));
            return this;
        }

        public RecordTypeAdapterFactory create() {
            ArrayList<JsonAdapterCreator> arrayList = new ArrayList<JsonAdapterCreator>(this.jsonAdapterCreators);
            Collections.reverse(arrayList);
            return new RecordTypeAdapterFactory(this.serializeRuntimeComponentTypes, this.allowMissingComponentValues, this.allowUnknownProperties, this.allowDuplicateComponentValues, this.allowJsonNullForPrimitives, this.namingStrategy, arrayList);
        }
    }

    private record ComponentNames(String serializationName, Set<String> deserializationNames) {
    }
}

