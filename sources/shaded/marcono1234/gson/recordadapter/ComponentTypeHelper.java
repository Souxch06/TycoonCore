package marcono1234.gson.recordadapter;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;

class ComponentTypeHelper {
    private ComponentTypeHelper() {
    }

    private static Type getUltimateTypeVariableBound(TypeVariable<?> typeVariable) {
        Type type = typeVariable;
        while (type instanceof TypeVariable) {
            TypeVariable<?> typeVariable2 = type;
            type = typeVariable2.getBounds()[0];
        }
        return type;
    }

    private static Class<?> getRawType(Type type) {
        if (type instanceof Class) {
            Class clazz = (Class)type;
            return clazz;
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType)type;
            return ComponentTypeHelper.getRawType(genericArrayType.getGenericComponentType()).arrayType();
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            return (Class)parameterizedType.getRawType();
        }
        if (type instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable)type;
            return ComponentTypeHelper.getRawType(ComponentTypeHelper.getUltimateTypeVariableBound(typeVariable));
        }
        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType)type;
            return ComponentTypeHelper.getRawType(wildcardType.getUpperBounds()[0]);
        }
        throw new AssertionError((Object)("Unexpected type instance " + type + " of class " + type.getClass().getName()));
    }

    private static Type resolveTypeVariables(Type type, Function<TypeVariable<?>, Type> function) {
        if (type instanceof Class) {
            return type;
        }
        if (type instanceof GenericArrayType) {
            Type type2;
            GenericArrayType genericArrayType = (GenericArrayType)type;
            Type type3 = genericArrayType.getGenericComponentType();
            if (type3.equals(type2 = ComponentTypeHelper.resolveTypeVariables(type3, function))) {
                return genericArrayType;
            }
            return new GenericArrayTypeImpl(type2);
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            Type type4 = parameterizedType.getRawType();
            Type type5 = ComponentTypeHelper.resolveTypeVariables(type4, function);
            Type type6 = parameterizedType.getOwnerType();
            Type type7 = type6 == null ? null : ComponentTypeHelper.resolveTypeVariables(type6, function);
            Object[] objectArray = parameterizedType.getActualTypeArguments();
            Object[] objectArray2 = new Type[objectArray.length];
            for (int i = 0; i < objectArray.length; ++i) {
                objectArray2[i] = ComponentTypeHelper.resolveTypeVariables(objectArray[i], function);
            }
            if (type4.equals(type5) && Objects.equals(type6, type7) && Arrays.equals(objectArray, objectArray2)) {
                return parameterizedType;
            }
            return new ParameterizedTypeImpl(type5, type7, (Type[])objectArray2);
        }
        if (type instanceof WildcardType) {
            int n;
            WildcardType wildcardType = (WildcardType)type;
            Object[] objectArray = wildcardType.getLowerBounds();
            Object[] objectArray3 = new Type[objectArray.length];
            Object[] objectArray4 = wildcardType.getUpperBounds();
            Object[] objectArray5 = new Type[objectArray4.length];
            for (n = 0; n < objectArray.length; ++n) {
                objectArray3[n] = ComponentTypeHelper.resolveTypeVariables(objectArray[n], function);
            }
            for (n = 0; n < objectArray4.length; ++n) {
                objectArray5[n] = ComponentTypeHelper.resolveTypeVariables(objectArray4[n], function);
            }
            if (Arrays.equals(objectArray, objectArray3) && Arrays.equals(objectArray4, objectArray5)) {
                return wildcardType;
            }
            return new WildcardTypeImpl((Type[])objectArray3, (Type[])objectArray5);
        }
        if (type instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable)type;
            return function.apply(typeVariable);
        }
        throw new AssertionError((Object)("Unexpected type instance " + type + " of class " + type.getClass().getName()));
    }

    private static Type resolveTypeVariableRaw(TypeVariable<?> typeVariable) {
        Function function = new Function<TypeVariable<?>, Type>(){
            private final Set<TypeVariable<?>> visited = new HashSet();

            @Override
            public Type apply(TypeVariable<?> typeVariable) {
                Type type = ComponentTypeHelper.getUltimateTypeVariableBound(typeVariable);
                if (this.visited.add(typeVariable)) {
                    return ComponentTypeHelper.resolveTypeVariables(type, this);
                }
                return ComponentTypeHelper.getRawType(type);
            }
        };
        return ComponentTypeHelper.resolveTypeVariables(typeVariable, function);
    }

    private static Map<TypeVariable<?>, Type> getResolvedTypeArguments(TypeVariable<?>[] typeVariableArray, Type type) {
        final HashMap hashMap = new HashMap();
        if (type instanceof Class) {
            for (TypeVariable<?> typeVariable2 : typeVariableArray) {
                hashMap.put(typeVariable2, ComponentTypeHelper.resolveTypeVariableRaw(typeVariable2));
            }
            return hashMap;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            Type[] typeArray = parameterizedType.getActualTypeArguments();
            LinkedHashSet<TypeVariable> linkedHashSet = new LinkedHashSet<TypeVariable>();
            for (int i = 0; i < typeArray.length; ++i) {
                Type type2 = typeArray[i];
                TypeVariable typeVariable3 = typeVariableArray[i];
                if (type2 instanceof WildcardType) {
                    WildcardType wildcardType = (WildcardType)type2;
                    Type type3 = wildcardType.getUpperBounds()[0];
                    if (type3 != Object.class) {
                        hashMap.put(typeVariable3, ComponentTypeHelper.resolveTypeVariables(type3, typeVariable -> ComponentTypeHelper.resolveTypeVariableRaw(typeVariable)));
                        continue;
                    }
                    linkedHashSet.add(typeVariable3);
                    continue;
                }
                hashMap.put(typeVariable3, ComponentTypeHelper.resolveTypeVariables(type2, typeVariable -> ComponentTypeHelper.resolveTypeVariableRaw(typeVariable)));
            }
            Function function = new Function<TypeVariable<?>, Type>(){

                @Override
                public Type apply(TypeVariable<?> typeVariable) {
                    if (hashMap.containsKey(typeVariable)) {
                        Type type = (Type)hashMap.get(typeVariable);
                        if (type == null) {
                            return ComponentTypeHelper.getRawType(typeVariable);
                        }
                        return type;
                    }
                    hashMap.put(typeVariable, null);
                    Type type = typeVariable.getBounds()[0];
                    Type type2 = ComponentTypeHelper.resolveTypeVariables(type, this);
                    hashMap.put(typeVariable, type2);
                    return type2;
                }
            };
            for (TypeVariable typeVariable3 : linkedHashSet) {
                if (hashMap.containsKey(typeVariable3)) continue;
                function.apply(typeVariable3);
            }
            return hashMap;
        }
        throw new AssertionError((Object)("Unexpected type instance " + type + " of class " + type.getClass().getName()));
    }

    public static Type[] resolveComponentTypes(TypeToken<?> typeToken, Type[] typeArray) {
        Object object;
        TypeVariable<Class<?>>[] typeVariableArray = typeToken.getRawType().getTypeParameters();
        if (typeVariableArray.length == 0) {
            return typeArray;
        }
        Type type = typeToken.getType();
        if (type instanceof WildcardType) {
            object = (WildcardType)type;
            type = object.getUpperBounds()[0];
        }
        if (type instanceof TypeVariable) {
            object = (TypeVariable)type;
            type = ComponentTypeHelper.getUltimateTypeVariableBound(object);
        }
        object = ComponentTypeHelper.getResolvedTypeArguments(typeVariableArray, type);
        Type[] typeArray2 = new Type[typeArray.length];
        for (int i = 0; i < typeArray2.length; ++i) {
            typeArray2[i] = ComponentTypeHelper.resolveTypeVariables(typeArray[i], ((Map)object)::get);
        }
        return typeArray2;
    }

    static class GenericArrayTypeImpl
    implements GenericArrayType {
        private final Type componentType;

        GenericArrayTypeImpl(Type type) {
            this.componentType = Objects.requireNonNull(type);
        }

        @Override
        public Type getGenericComponentType() {
            return this.componentType;
        }

        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (object instanceof GenericArrayType) {
                GenericArrayType genericArrayType = (GenericArrayType)object;
                return Objects.equals(this.componentType, genericArrayType.getGenericComponentType());
            }
            return false;
        }

        public int hashCode() {
            return Objects.hashCode(this.componentType);
        }

        public String toString() {
            return this.componentType.getTypeName() + "[]";
        }
    }

    static class ParameterizedTypeImpl
    implements ParameterizedType {
        private final Type rawType;
        private final Type ownerType;
        private final Type[] typeArguments;

        ParameterizedTypeImpl(Type type, Type type2, Type[] typeArray) {
            this.rawType = Objects.requireNonNull(type);
            this.ownerType = type2;
            this.typeArguments = (Type[])typeArray.clone();
        }

        @Override
        public Type getRawType() {
            return this.rawType;
        }

        @Override
        public Type getOwnerType() {
            return this.ownerType;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return (Type[])this.typeArguments.clone();
        }

        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (object instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType)object;
                return Objects.equals(this.ownerType, parameterizedType.getOwnerType()) && Objects.equals(this.rawType, parameterizedType.getRawType()) && Arrays.equals(this.typeArguments, parameterizedType.getActualTypeArguments());
            }
            return false;
        }

        public int hashCode() {
            return Arrays.hashCode(this.typeArguments) ^ Objects.hashCode(this.ownerType) ^ Objects.hashCode(this.rawType);
        }

        public String toString() {
            Object object;
            StringBuilder stringBuilder = new StringBuilder();
            if (this.ownerType != null) {
                stringBuilder.append(this.ownerType.getTypeName()).append('$');
                Type[] typeArray = this.rawType;
                if (typeArray instanceof Class) {
                    object = (Class)typeArray;
                    stringBuilder.append(((Class)object).getSimpleName());
                } else {
                    stringBuilder.append(this.rawType.getTypeName());
                }
            } else {
                stringBuilder.append(this.rawType.getTypeName());
            }
            object = new StringJoiner(", ", "<", ">");
            ((StringJoiner)object).setEmptyValue("");
            for (Type type : this.typeArguments) {
                ((StringJoiner)object).add(type.getTypeName());
            }
            stringBuilder.append(((StringJoiner)object).toString());
            return stringBuilder.toString();
        }
    }

    static class WildcardTypeImpl
    implements WildcardType {
        private final Type[] lowerBounds;
        private final Type[] upperBounds;

        WildcardTypeImpl(Type[] typeArray, Type[] typeArray2) {
            typeArray = (Type[])typeArray.clone();
            typeArray2 = (Type[])typeArray2.clone();
            if (typeArray2.length == 0) {
                throw new IllegalArgumentException("At least Object is required as upper bound");
            }
            if (typeArray.length > 0 && (typeArray2.length != 1 || typeArray2[0] != Object.class)) {
                throw new IllegalArgumentException("Malformed bounds: lower=" + Arrays.toString(typeArray) + ", upper=" + Arrays.toString(typeArray2));
            }
            this.lowerBounds = typeArray;
            this.upperBounds = typeArray2;
        }

        @Override
        public Type[] getLowerBounds() {
            return (Type[])this.lowerBounds.clone();
        }

        @Override
        public Type[] getUpperBounds() {
            return (Type[])this.upperBounds.clone();
        }

        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (object instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType)object;
                return Arrays.equals(this.lowerBounds, wildcardType.getLowerBounds()) && Arrays.equals(this.upperBounds, wildcardType.getUpperBounds());
            }
            return false;
        }

        public int hashCode() {
            return Arrays.hashCode(this.lowerBounds) ^ Arrays.hashCode(this.upperBounds);
        }

        public String toString() {
            Type[] typeArray;
            StringBuilder stringBuilder = new StringBuilder();
            if (this.lowerBounds.length > 0) {
                stringBuilder.append("? super ");
                typeArray = this.lowerBounds;
            } else {
                if (this.upperBounds.length == 1 && this.upperBounds[0] == Object.class) {
                    return "?";
                }
                stringBuilder.append("? extends ");
                typeArray = this.upperBounds;
            }
            StringJoiner stringJoiner = new StringJoiner(" & ");
            for (Type type : typeArray) {
                stringJoiner.add(type.getTypeName());
            }
            stringBuilder.append(stringJoiner.toString());
            return stringBuilder.toString();
        }
    }
}

