package com.google.gson.internal.bind;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ReflectionAccessFilter;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.internal.Primitives;
import com.google.gson.internal.ReflectionAccessFilterHelper;
import com.google.gson.internal.bind.JsonAdapterAnnotationTypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapterRuntimeTypeWrapper;
import com.google.gson.internal.reflect.ReflectionHelper;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReflectiveTypeAdapterFactory
implements TypeAdapterFactory {
    private final ConstructorConstructor constructorConstructor;
    private final FieldNamingStrategy fieldNamingPolicy;
    private final Excluder excluder;
    private final JsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;
    private final List<ReflectionAccessFilter> reflectionFilters;

    public ReflectiveTypeAdapterFactory(ConstructorConstructor constructorConstructor, FieldNamingStrategy fieldNamingStrategy, Excluder excluder, JsonAdapterAnnotationTypeAdapterFactory jsonAdapterAnnotationTypeAdapterFactory, List<ReflectionAccessFilter> list) {
        this.constructorConstructor = constructorConstructor;
        this.fieldNamingPolicy = fieldNamingStrategy;
        this.excluder = excluder;
        this.jsonAdapterFactory = jsonAdapterAnnotationTypeAdapterFactory;
        this.reflectionFilters = list;
    }

    private boolean includeField(Field field, boolean bl) {
        return !this.excluder.excludeClass(field.getType(), bl) && !this.excluder.excludeField(field, bl);
    }

    private List<String> getFieldNames(Field field) {
        SerializedName serializedName = field.getAnnotation(SerializedName.class);
        if (serializedName == null) {
            String string = this.fieldNamingPolicy.translateName(field);
            return Collections.singletonList(string);
        }
        String string = serializedName.value();
        String[] stringArray = serializedName.alternate();
        if (stringArray.length == 0) {
            return Collections.singletonList(string);
        }
        ArrayList<String> arrayList = new ArrayList<String>(stringArray.length + 1);
        arrayList.add(string);
        Collections.addAll(arrayList, stringArray);
        return arrayList;
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        boolean bl;
        Class<T> clazz = typeToken.getRawType();
        if (!Object.class.isAssignableFrom(clazz)) {
            return null;
        }
        ReflectionAccessFilter.FilterResult filterResult = ReflectionAccessFilterHelper.getFilterResult(this.reflectionFilters, clazz);
        if (filterResult == ReflectionAccessFilter.FilterResult.BLOCK_ALL) {
            throw new JsonIOException("ReflectionAccessFilter does not permit using reflection for " + clazz + ". Register a TypeAdapter for this type or adjust the access filter.");
        }
        boolean bl2 = bl = filterResult == ReflectionAccessFilter.FilterResult.BLOCK_INACCESSIBLE;
        if (ReflectionHelper.isRecord(clazz)) {
            RecordAdapter<T> recordAdapter = new RecordAdapter<T>(clazz, this.getBoundFields(gson, typeToken, clazz, bl, true), bl);
            return recordAdapter;
        }
        ObjectConstructor<T> objectConstructor = this.constructorConstructor.get(typeToken);
        return new FieldReflectionAdapter<T>(objectConstructor, this.getBoundFields(gson, typeToken, clazz, bl, false));
    }

    private static <M extends AccessibleObject> void checkAccessible(Object object, M m) {
        if (!ReflectionAccessFilterHelper.canAccess(m, Modifier.isStatic(((Member)((Object)m)).getModifiers()) ? null : object)) {
            String string = ReflectionHelper.getAccessibleObjectDescription(m, true);
            throw new JsonIOException(string + " is not accessible and ReflectionAccessFilter does not permit making it accessible. Register a TypeAdapter for the declaring type, adjust the access filter or increase the visibility of the element and its declaring type.");
        }
    }

    private BoundField createBoundField(final Gson gson, Field field, final Method method, String string, final TypeToken<?> typeToken, boolean bl, boolean bl2, final boolean bl3) {
        boolean bl4;
        final boolean bl5 = Primitives.isPrimitive(typeToken.getRawType());
        int n = field.getModifiers();
        final boolean bl6 = Modifier.isStatic(n) && Modifier.isFinal(n);
        JsonAdapter jsonAdapter = field.getAnnotation(JsonAdapter.class);
        TypeAdapter<?> typeAdapter = null;
        if (jsonAdapter != null) {
            typeAdapter = this.jsonAdapterFactory.getTypeAdapter(this.constructorConstructor, gson, typeToken, jsonAdapter);
        }
        boolean bl7 = bl4 = typeAdapter != null;
        if (typeAdapter == null) {
            typeAdapter = gson.getAdapter(typeToken);
        }
        final TypeAdapter<?> typeAdapter2 = typeAdapter;
        return new BoundField(string, field, bl, bl2){

            @Override
            void write(JsonWriter jsonWriter, Object object) {
                Object object2;
                if (!this.serialized) {
                    return;
                }
                if (bl3) {
                    if (method == null) {
                        ReflectiveTypeAdapterFactory.checkAccessible(object, this.field);
                    } else {
                        ReflectiveTypeAdapterFactory.checkAccessible(object, method);
                    }
                }
                if (method != null) {
                    try {
                        object2 = method.invoke(object, new Object[0]);
                    }
                    catch (InvocationTargetException invocationTargetException) {
                        String string = ReflectionHelper.getAccessibleObjectDescription(method, false);
                        throw new JsonIOException("Accessor " + string + " threw exception", invocationTargetException.getCause());
                    }
                } else {
                    object2 = this.field.get(object);
                }
                if (object2 == object) {
                    return;
                }
                jsonWriter.name(this.name);
                TypeAdapter typeAdapter = bl4 ? typeAdapter2 : new TypeAdapterRuntimeTypeWrapper(gson, typeAdapter2, typeToken.getType());
                typeAdapter.write(jsonWriter, object2);
            }

            @Override
            void readIntoArray(JsonReader jsonReader, int n, Object[] objectArray) {
                Object t = typeAdapter2.read(jsonReader);
                if (t == null && bl5) {
                    throw new JsonParseException("null is not allowed as value for record component '" + this.fieldName + "' of primitive type; at path " + jsonReader.getPath());
                }
                objectArray[n] = t;
            }

            @Override
            void readIntoField(JsonReader jsonReader, Object object) {
                Object t = typeAdapter2.read(jsonReader);
                if (t != null || !bl5) {
                    if (bl3) {
                        ReflectiveTypeAdapterFactory.checkAccessible(object, this.field);
                    } else if (bl6) {
                        String string = ReflectionHelper.getAccessibleObjectDescription(this.field, false);
                        throw new JsonIOException("Cannot set value of 'static final' " + string);
                    }
                    this.field.set(object, t);
                }
            }
        };
    }

    private Map<String, BoundField> getBoundFields(Gson gson, TypeToken<?> typeToken, Class<?> clazz, boolean bl, boolean bl2) {
        LinkedHashMap<String, BoundField> linkedHashMap = new LinkedHashMap<String, BoundField>();
        if (clazz.isInterface()) {
            return linkedHashMap;
        }
        Class<?> clazz2 = clazz;
        while (clazz != Object.class) {
            Field[] fieldArray = clazz.getDeclaredFields();
            if (clazz != clazz2 && fieldArray.length > 0) {
                ReflectionAccessFilter.FilterResult filterResult = ReflectionAccessFilterHelper.getFilterResult(this.reflectionFilters, clazz);
                if (filterResult == ReflectionAccessFilter.FilterResult.BLOCK_ALL) {
                    throw new JsonIOException("ReflectionAccessFilter does not permit using reflection for " + clazz + " (supertype of " + clazz2 + "). Register a TypeAdapter for this type or adjust the access filter.");
                }
                bl = filterResult == ReflectionAccessFilter.FilterResult.BLOCK_INACCESSIBLE;
            }
            for (Field field : fieldArray) {
                Object object;
                boolean bl3 = this.includeField(field, true);
                boolean bl4 = this.includeField(field, false);
                if (!bl3 && !bl4) continue;
                Method method = null;
                if (bl2) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        bl4 = false;
                    } else {
                        method = ReflectionHelper.getAccessor(clazz, field);
                        if (!bl) {
                            ReflectionHelper.makeAccessible(method);
                        }
                        if (method.getAnnotation(SerializedName.class) != null && field.getAnnotation(SerializedName.class) == null) {
                            object = ReflectionHelper.getAccessibleObjectDescription(method, false);
                            throw new JsonIOException("@SerializedName on " + (String)object + " is not supported");
                        }
                    }
                }
                if (!bl && method == null) {
                    ReflectionHelper.makeAccessible(field);
                }
                object = $Gson$Types.resolve(typeToken.getType(), clazz, field.getGenericType());
                List<String> list = this.getFieldNames(field);
                BoundField boundField = null;
                int n = list.size();
                for (int i = 0; i < n; ++i) {
                    String string = list.get(i);
                    if (i != 0) {
                        bl3 = false;
                    }
                    BoundField boundField2 = this.createBoundField(gson, field, method, string, TypeToken.get((Type)object), bl3, bl4, bl);
                    BoundField boundField3 = linkedHashMap.put(string, boundField2);
                    if (boundField != null) continue;
                    boundField = boundField3;
                }
                if (boundField == null) continue;
                throw new IllegalArgumentException("Class " + clazz2.getName() + " declares multiple JSON fields named '" + boundField.name + "'; conflict is caused by fields " + ReflectionHelper.fieldToString(boundField.field) + " and " + ReflectionHelper.fieldToString(field));
            }
            typeToken = TypeToken.get($Gson$Types.resolve(typeToken.getType(), clazz, clazz.getGenericSuperclass()));
            clazz = typeToken.getRawType();
        }
        return linkedHashMap;
    }

    private static final class RecordAdapter<T>
    extends Adapter<T, Object[]> {
        static final Map<Class<?>, Object> PRIMITIVE_DEFAULTS = RecordAdapter.primitiveDefaults();
        private final Constructor<T> constructor;
        private final Object[] constructorArgsDefaults;
        private final Map<String, Integer> componentIndices = new HashMap<String, Integer>();

        RecordAdapter(Class<T> clazz, Map<String, BoundField> map, boolean bl) {
            super(map);
            this.constructor = ReflectionHelper.getCanonicalRecordConstructor(clazz);
            if (bl) {
                ReflectiveTypeAdapterFactory.checkAccessible(null, this.constructor);
            } else {
                ReflectionHelper.makeAccessible(this.constructor);
            }
            String[] stringArray = ReflectionHelper.getRecordComponentNames(clazz);
            for (int i = 0; i < stringArray.length; ++i) {
                this.componentIndices.put(stringArray[i], i);
            }
            Class<?>[] classArray = this.constructor.getParameterTypes();
            this.constructorArgsDefaults = new Object[classArray.length];
            for (int i = 0; i < classArray.length; ++i) {
                this.constructorArgsDefaults[i] = PRIMITIVE_DEFAULTS.get(classArray[i]);
            }
        }

        private static Map<Class<?>, Object> primitiveDefaults() {
            HashMap hashMap = new HashMap();
            hashMap.put(Byte.TYPE, (byte)0);
            hashMap.put(Short.TYPE, (short)0);
            hashMap.put(Integer.TYPE, 0);
            hashMap.put(Long.TYPE, 0L);
            hashMap.put(Float.TYPE, Float.valueOf(0.0f));
            hashMap.put(Double.TYPE, 0.0);
            hashMap.put(Character.TYPE, Character.valueOf('\u0000'));
            hashMap.put(Boolean.TYPE, false);
            return hashMap;
        }

        @Override
        Object[] createAccumulator() {
            return (Object[])this.constructorArgsDefaults.clone();
        }

        @Override
        void readField(Object[] objectArray, JsonReader jsonReader, BoundField boundField) {
            Integer n = this.componentIndices.get(boundField.fieldName);
            if (n == null) {
                throw new IllegalStateException("Could not find the index in the constructor '" + ReflectionHelper.constructorToString(this.constructor) + "' for field with name '" + boundField.fieldName + "', unable to determine which argument in the constructor the field corresponds to. This is unexpected behavior, as we expect the RecordComponents to have the same names as the fields in the Java class, and that the order of the RecordComponents is the same as the order of the canonical constructor parameters.");
            }
            boundField.readIntoArray(jsonReader, n, objectArray);
        }

        @Override
        T finalize(Object[] objectArray) {
            try {
                return this.constructor.newInstance(objectArray);
            }
            catch (IllegalAccessException illegalAccessException) {
                throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(illegalAccessException);
            }
            catch (IllegalArgumentException | InstantiationException exception) {
                throw new RuntimeException("Failed to invoke constructor '" + ReflectionHelper.constructorToString(this.constructor) + "' with args " + Arrays.toString(objectArray), exception);
            }
            catch (InvocationTargetException invocationTargetException) {
                throw new RuntimeException("Failed to invoke constructor '" + ReflectionHelper.constructorToString(this.constructor) + "' with args " + Arrays.toString(objectArray), invocationTargetException.getCause());
            }
        }
    }

    private static final class FieldReflectionAdapter<T>
    extends Adapter<T, T> {
        private final ObjectConstructor<T> constructor;

        FieldReflectionAdapter(ObjectConstructor<T> objectConstructor, Map<String, BoundField> map) {
            super(map);
            this.constructor = objectConstructor;
        }

        @Override
        T createAccumulator() {
            return this.constructor.construct();
        }

        @Override
        void readField(T t, JsonReader jsonReader, BoundField boundField) {
            boundField.readIntoField(jsonReader, t);
        }

        @Override
        T finalize(T t) {
            return t;
        }
    }

    public static abstract class Adapter<T, A>
    extends TypeAdapter<T> {
        final Map<String, BoundField> boundFields;

        Adapter(Map<String, BoundField> map) {
            this.boundFields = map;
        }

        @Override
        public void write(JsonWriter jsonWriter, T t) {
            if (t == null) {
                jsonWriter.nullValue();
                return;
            }
            jsonWriter.beginObject();
            try {
                for (BoundField boundField : this.boundFields.values()) {
                    boundField.write(jsonWriter, t);
                }
            }
            catch (IllegalAccessException illegalAccessException) {
                throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(illegalAccessException);
            }
            jsonWriter.endObject();
        }

        @Override
        public T read(JsonReader jsonReader) {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            A a = this.createAccumulator();
            try {
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String string = jsonReader.nextName();
                    BoundField boundField = this.boundFields.get(string);
                    if (boundField == null || !boundField.deserialized) {
                        jsonReader.skipValue();
                        continue;
                    }
                    this.readField(a, jsonReader, boundField);
                }
            }
            catch (IllegalStateException illegalStateException) {
                throw new JsonSyntaxException(illegalStateException);
            }
            catch (IllegalAccessException illegalAccessException) {
                throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(illegalAccessException);
            }
            jsonReader.endObject();
            return this.finalize(a);
        }

        abstract A createAccumulator();

        abstract void readField(A var1, JsonReader var2, BoundField var3);

        abstract T finalize(A var1);
    }

    static abstract class BoundField {
        final String name;
        final Field field;
        final String fieldName;
        final boolean serialized;
        final boolean deserialized;

        protected BoundField(String string, Field field, boolean bl, boolean bl2) {
            this.name = string;
            this.field = field;
            this.fieldName = field.getName();
            this.serialized = bl;
            this.deserialized = bl2;
        }

        abstract void write(JsonWriter var1, Object var2);

        abstract void readIntoArray(JsonReader var1, int var2, Object[] var3);

        abstract void readIntoField(JsonReader var1, Object var2);
    }
}

