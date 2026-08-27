package com.cryptomorin.xseries.reflection.jvm;

import com.cryptomorin.xseries.reflection.ReflectiveHandle;
import com.cryptomorin.xseries.reflection.jvm.MemberHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.ClassHandle;
import com.cryptomorin.xseries.reflection.parser.ReflectionParser;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ConstructorMemberHandle
extends MemberHandle {
    protected Class<?>[] parameterTypes = new Class[0];

    public ConstructorMemberHandle(ClassHandle classHandle) {
        super(classHandle);
    }

    public ConstructorMemberHandle parameters(Class<?> ... classArray) {
        this.parameterTypes = classArray;
        return this;
    }

    public ConstructorMemberHandle parameters(ClassHandle ... classHandleArray) {
        this.parameterTypes = (Class[])Arrays.stream(classHandleArray).map(ReflectiveHandle::unreflect).toArray(Class[]::new);
        return this;
    }

    @Override
    public MethodHandle reflect() {
        if (this.isFinal) {
            throw new UnsupportedOperationException("Constructor cannot be final: " + this);
        }
        if (this.makeAccessible) {
            return this.clazz.getNamespace().getLookup().unreflectConstructor((Constructor<?>)this.reflectJvm());
        }
        return this.clazz.getNamespace().getLookup().findConstructor((Class)this.clazz.unreflect(), MethodType.methodType(Void.TYPE, this.parameterTypes));
    }

    @Override
    public ConstructorMemberHandle signature(String string) {
        return new ReflectionParser(string).imports(this.clazz.getNamespace()).parseConstructor(this);
    }

    public Constructor<?> reflectJvm() {
        return this.handleAccessible(((Class)this.clazz.unreflect()).getDeclaredConstructor(this.parameterTypes));
    }

    @Override
    public ConstructorMemberHandle clone() {
        ConstructorMemberHandle constructorMemberHandle = new ConstructorMemberHandle(this.clazz);
        constructorMemberHandle.parameterTypes = this.parameterTypes;
        constructorMemberHandle.isFinal = this.isFinal;
        constructorMemberHandle.makeAccessible = this.makeAccessible;
        return constructorMemberHandle;
    }

    public String toString() {
        String string = this.getClass().getSimpleName() + '{';
        if (this.makeAccessible) {
            string = string + "protected/private ";
        }
        string = string + this.clazz.toString() + ' ';
        string = string + '(' + Arrays.stream(this.parameterTypes).map(Class::getSimpleName).collect(Collectors.joining(", ")) + ')';
        return string + '}';
    }
}

