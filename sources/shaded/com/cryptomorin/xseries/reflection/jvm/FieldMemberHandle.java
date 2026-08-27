package com.cryptomorin.xseries.reflection.jvm;

import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.jvm.FlaggedNamedMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.MethodMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.ClassHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.DynamicClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftMapping;
import com.cryptomorin.xseries.reflection.parser.ReflectionParser;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Objects;
import org.intellij.lang.annotations.Pattern;

public class FieldMemberHandle
extends FlaggedNamedMemberHandle {
    public static final MethodHandle MODIFIERS_FIELD;
    public static final DynamicClassHandle VarHandle;
    public static final MethodHandle VAR_HANDLE_SET;
    private static final Object MODIFIERS_VAR_HANDLE;
    protected Boolean getter;

    public FieldMemberHandle(ClassHandle classHandle) {
        super(classHandle);
    }

    public FieldMemberHandle getter() {
        this.getter = true;
        return this;
    }

    @Override
    public FieldMemberHandle asStatic() {
        super.asStatic();
        return this;
    }

    public FieldMemberHandle asFinal() {
        this.isFinal = true;
        return this;
    }

    @Override
    public FieldMemberHandle makeAccessible() {
        super.makeAccessible();
        return this;
    }

    public FieldMemberHandle setter() {
        this.getter = false;
        return this;
    }

    @Override
    public FieldMemberHandle returns(Class<?> clazz) {
        super.returns(clazz);
        return this;
    }

    @Override
    public FieldMemberHandle returns(ClassHandle classHandle) {
        super.returns(classHandle);
        return this;
    }

    @Override
    public FieldMemberHandle clone() {
        FieldMemberHandle fieldMemberHandle = new FieldMemberHandle(this.clazz);
        fieldMemberHandle.returnType = this.returnType;
        fieldMemberHandle.getter = this.getter;
        fieldMemberHandle.isFinal = this.isFinal;
        fieldMemberHandle.makeAccessible = this.makeAccessible;
        fieldMemberHandle.names.addAll(this.names);
        return fieldMemberHandle;
    }

    @Override
    public MethodHandle reflect() {
        Field field = this.reflectJvm();
        if (this.getter.booleanValue()) {
            return this.clazz.getNamespace().getLookup().unreflectGetter(field);
        }
        return this.clazz.getNamespace().getLookup().unreflectSetter(field);
    }

    @Override
    public FieldMemberHandle signature(String string) {
        return new ReflectionParser(string).imports(this.clazz.getNamespace()).parseField(this);
    }

    @Override
    public FieldMemberHandle map(MinecraftMapping minecraftMapping, @Pattern(value="\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*") String string) {
        super.map(minecraftMapping, string);
        return this;
    }

    @Override
    public FieldMemberHandle named(String ... stringArray) {
        super.named(stringArray);
        return this;
    }

    @Override
    protected <T extends AccessibleObject> T handleAccessible(T t) {
        block6: {
            if ((t = super.handleAccessible(t)) == null) {
                return null;
            }
            if (this.isFinal && this.isStatic) {
                try {
                    int n = ((Member)((Object)t)).getModifiers() & 0xFFFFFFEF;
                    if (MODIFIERS_VAR_HANDLE != null) {
                        VAR_HANDLE_SET.invoke(MODIFIERS_VAR_HANDLE, t, n);
                        break block6;
                    }
                    if (MODIFIERS_FIELD != null) {
                        MODIFIERS_FIELD.invoke(t, n);
                        break block6;
                    }
                    throw new IllegalAccessException("Current Java version doesn't support modifying final fields. " + this);
                }
                catch (Throwable throwable) {
                    throw new ReflectiveOperationException("Cannot unfinal field " + this, throwable);
                }
            }
        }
        return t;
    }

    public Field reflectJvm() {
        Objects.requireNonNull(this.returnType, "Return type not specified");
        Objects.requireNonNull(this.getter, "Not specified whether the method is a getter or setter");
        if (this.names.isEmpty()) {
            throw new IllegalStateException("No names specified");
        }
        Throwable throwable = null;
        Field field = null;
        Class clazz = (Class)this.clazz.reflect();
        for (String string : this.names) {
            if (field != null) break;
            try {
                field = clazz.getDeclaredField(string);
                if (field.getType() != this.returnType) {
                    throw new NoSuchFieldException("Field named '" + string + "' was found but the types don't match: " + field + " != " + this);
                }
                if (!this.isFinal || Modifier.isFinal(field.getModifiers())) continue;
                throw new NoSuchFieldException("Field named '" + string + "' was found but it's not final: " + field + " != " + this);
            }
            catch (NoSuchFieldException noSuchFieldException) {
                field = null;
                if (throwable == null) {
                    throwable = new NoSuchFieldException("None of the fields were found for " + this);
                }
                throwable.addSuppressed(noSuchFieldException);
            }
        }
        if (field == null) {
            throw (NoSuchFieldException)XReflection.relativizeSuppressedExceptions(throwable);
        }
        return this.handleAccessible(field);
    }

    public String toString() {
        String string = this.getClass().getSimpleName() + '{';
        if (this.makeAccessible) {
            string = string + "protected/private ";
        }
        if (this.isFinal) {
            string = string + "final ";
        }
        if (this.returnType != null) {
            string = string + this.returnType.getSimpleName() + ' ';
        }
        string = string + String.join((CharSequence)"/", this.names);
        return string + '}';
    }

    static {
        VarHandle = XReflection.classHandle().inPackage("java.lang.invoke").named("VarHandle");
        VAR_HANDLE_SET = (MethodHandle)((MethodMemberHandle)VarHandle.method().named("set").returns((Class)Void.TYPE)).parameters(Object[].class).reflectOrNull();
        Object var0 = null;
        MethodHandle methodHandle = null;
        try {
            methodHandle = (MethodHandle)XReflection.of(Field.class).field().setter().named("modifiers").returns((Class)Integer.TYPE).unreflect();
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            VarHandle.reflect();
            MethodHandle methodHandle2 = ((MethodMemberHandle)XReflection.of(MethodHandles.class).method().named("privateLookupIn").returns((Class)MethodHandles.Lookup.class)).parameters(Class.class, MethodHandles.Lookup.class).reflect();
            MethodHandle methodHandle3 = ((MethodMemberHandle)VarHandle.method().named("findVarHandle").returns((Class)MethodHandles.Lookup.class)).parameters(Class.class, String.class, Class.class).reflect();
            MethodHandles.Lookup lookup = methodHandle2.invoke(Field.class, MethodHandles.lookup());
            methodHandle3.invoke(lookup, Field.class, "modifiers", Integer.TYPE);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        MODIFIERS_VAR_HANDLE = var0;
        MODIFIERS_FIELD = methodHandle;
    }
}

