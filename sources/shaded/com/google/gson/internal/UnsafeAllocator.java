package com.google.gson.internal;

import com.google.gson.internal.ConstructorConstructor;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class UnsafeAllocator {
    public static final UnsafeAllocator INSTANCE = UnsafeAllocator.create();

    public abstract <T> T newInstance(Class<T> var1);

    private static void assertInstantiable(Class<?> clazz) {
        String string = ConstructorConstructor.checkInstantiable(clazz);
        if (string != null) {
            throw new AssertionError((Object)("UnsafeAllocator is used for non-instantiable type: " + string));
        }
    }

    private static UnsafeAllocator create() {
        try {
            Class<?> clazz = Class.forName("sun.misc.Unsafe");
            Field field = clazz.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            final Object object = field.get(null);
            final Method method = clazz.getMethod("allocateInstance", Class.class);
            return new UnsafeAllocator(){

                @Override
                public <T> T newInstance(Class<T> clazz) {
                    UnsafeAllocator.assertInstantiable(clazz);
                    return (T)method.invoke(object, clazz);
                }
            };
        }
        catch (Exception exception) {
            try {
                Method method = ObjectStreamClass.class.getDeclaredMethod("getConstructorId", Class.class);
                method.setAccessible(true);
                final int n = (Integer)method.invoke(null, Object.class);
                final Method method2 = ObjectStreamClass.class.getDeclaredMethod("newInstance", Class.class, Integer.TYPE);
                method2.setAccessible(true);
                return new UnsafeAllocator(){

                    @Override
                    public <T> T newInstance(Class<T> clazz) {
                        UnsafeAllocator.assertInstantiable(clazz);
                        return (T)method2.invoke(null, clazz, n);
                    }
                };
            }
            catch (Exception exception2) {
                try {
                    final Method method = ObjectInputStream.class.getDeclaredMethod("newInstance", Class.class, Class.class);
                    method.setAccessible(true);
                    return new UnsafeAllocator(){

                        @Override
                        public <T> T newInstance(Class<T> clazz) {
                            UnsafeAllocator.assertInstantiable(clazz);
                            return (T)method.invoke(null, clazz, Object.class);
                        }
                    };
                }
                catch (Exception exception3) {
                    return new UnsafeAllocator(){

                        @Override
                        public <T> T newInstance(Class<T> clazz) {
                            throw new UnsupportedOperationException("Cannot allocate " + clazz + ". Usage of JDK sun.misc.Unsafe is enabled, but it could not be used. Make sure your runtime is configured correctly.");
                        }
                    };
                }
            }
        }
    }
}

