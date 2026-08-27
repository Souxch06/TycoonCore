/*
 * Decompiled with CFR 0.152.
 */
package marcono1234.gson.recordadapter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Optional;

public interface JsonAdapterCreator {
    public static final JsonAdapterCreator DEFAULT_CONSTRUCTOR_INVOKER = new JsonAdapterCreator(){

        @Override
        public Optional<Object> create(Class<?> clazz) {
            Constructor<?> constructor;
            int n = clazz.getModifiers();
            if (Modifier.isAbstract(n) || !Modifier.isStatic(n)) {
                return Optional.empty();
            }
            try {
                constructor = clazz.getConstructor(new Class[0]);
            }
            catch (NoSuchMethodException noSuchMethodException) {
                return Optional.empty();
            }
            try {
                constructor.setAccessible(true);
                return Optional.of(constructor.newInstance(new Object[0]));
            }
            catch (IllegalAccessException | InaccessibleObjectException exception) {
                throw new AdapterCreationException("Default constructor of " + clazz + " is not accessible; open it to this library or register a custom JsonAdapterCreator", exception);
            }
            catch (InstantiationException instantiationException) {
                throw new AdapterCreationException("Failed invoking default constructor for " + clazz, instantiationException);
            }
            catch (InvocationTargetException invocationTargetException) {
                throw new AdapterCreationException("Failed invoking default constructor for " + clazz, invocationTargetException.getCause());
            }
        }

        public String toString() {
            return "DEFAULT_CONSTRUCTOR_INVOKER";
        }
    };

    public Optional<Object> create(Class<?> var1) throws AdapterCreationException;

    public static class AdapterCreationException
    extends Exception {
        private static final long serialVersionUID = 1L;

        public AdapterCreationException(String string) {
            super(string);
        }

        public AdapterCreationException(String string, Throwable throwable) {
            super(string, throwable);
        }
    }
}

