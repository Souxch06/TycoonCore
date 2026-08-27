package com.cryptomorin.xseries.reflection.jvm.classes;

import com.cryptomorin.xseries.reflection.ReflectiveHandle;
import com.cryptomorin.xseries.reflection.ReflectiveNamespace;
import com.cryptomorin.xseries.reflection.jvm.ConstructorMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.EnumMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.FieldMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.MethodMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.NamedReflectiveHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.DynamicClassHandle;
import com.cryptomorin.xseries.reflection.parser.ReflectionParser;
import java.util.Objects;
import org.intellij.lang.annotations.Language;

public abstract class ClassHandle
implements ReflectiveHandle<Class<?>>,
NamedReflectiveHandle {
    protected final ReflectiveNamespace namespace;

    protected ClassHandle(ReflectiveNamespace reflectiveNamespace) {
        this.namespace = reflectiveNamespace;
        reflectiveNamespace.link(this);
    }

    public abstract ClassHandle asArray(int var1);

    public final ClassHandle asArray() {
        return this.asArray(1);
    }

    public abstract boolean isArray();

    public DynamicClassHandle inner(@Language(value="Java") String string) {
        return this.inner(this.namespace.classHandle(string));
    }

    public <T extends DynamicClassHandle> T inner(T t) {
        Objects.requireNonNull(t, "Inner handle is null");
        if (this == t) {
            throw new IllegalArgumentException("Same instance: " + this);
        }
        t.parent = this;
        this.namespace.link(this);
        return t;
    }

    public int getDimensionCount() {
        int n = -1;
        Class<?> clazz = (Class<?>)this.reflectOrNull();
        if (clazz == null) {
            return n;
        }
        do {
            clazz = clazz.getComponentType();
            ++n;
        } while (clazz != null);
        return n;
    }

    public ReflectiveNamespace getNamespace() {
        return this.namespace;
    }

    public MethodMemberHandle method() {
        return new MethodMemberHandle(this);
    }

    public MethodMemberHandle method(@Language(value="Java") String string) {
        return this.createParser(string).parseMethod(this.method());
    }

    public EnumMemberHandle enums() {
        return new EnumMemberHandle(this);
    }

    public FieldMemberHandle field() {
        return new FieldMemberHandle(this);
    }

    public FieldMemberHandle field(@Language(value="Java") String string) {
        return this.createParser(string).parseField(this.field());
    }

    public ConstructorMemberHandle constructor(@Language(value="Java") String string) {
        return this.createParser(string).parseConstructor(this.constructor());
    }

    public ConstructorMemberHandle constructor() {
        return new ConstructorMemberHandle(this);
    }

    public ConstructorMemberHandle constructor(Class<?> ... classArray) {
        return this.constructor().parameters(classArray);
    }

    public ConstructorMemberHandle constructor(ClassHandle ... classHandleArray) {
        return this.constructor().parameters(classHandleArray);
    }

    private ReflectionParser createParser(@Language(value="Java") String string) {
        return new ReflectionParser(string).imports(this.namespace);
    }

    public abstract ClassHandle clone();
}

