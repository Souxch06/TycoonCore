/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Strings
 *  javax.annotation.Nonnull
 */
package com.cryptomorin.xseries.reflection.jvm.classes;

import com.cryptomorin.xseries.reflection.ReflectiveNamespace;
import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.jvm.classes.ClassHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.PackageHandle;
import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import org.intellij.lang.annotations.Pattern;

public class DynamicClassHandle
extends ClassHandle {
    protected ClassHandle parent;
    protected String packageName;
    protected final Set<String> classNames = new HashSet<String>(5);
    protected int array;

    public DynamicClassHandle(ReflectiveNamespace reflectiveNamespace) {
        super(reflectiveNamespace);
    }

    public DynamicClassHandle inPackage(@Nonnull @Pattern(value="(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*") String string) {
        Objects.requireNonNull(string, "Null package name");
        this.packageName = string;
        return this;
    }

    public DynamicClassHandle inPackage(@Nonnull PackageHandle packageHandle) {
        return this.inPackage(packageHandle, "");
    }

    public DynamicClassHandle inPackage(@Nonnull PackageHandle packageHandle, @Nonnull @Pattern(value="(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*") String string) {
        Objects.requireNonNull(packageHandle, "Null package handle type");
        Objects.requireNonNull(string, "Null package handle name");
        if (this.parent != null) {
            throw new IllegalStateException("Cannot change package of an inner class: " + packageHandle + " -> " + string);
        }
        this.packageName = packageHandle.getPackage(string);
        return this;
    }

    public DynamicClassHandle named(String ... stringArray) {
        Objects.requireNonNull(stringArray);
        for (String string : this.classNames) {
            Objects.requireNonNull(string, () -> "Cannot add null class name from: " + Arrays.toString(stringArray) + " to " + this);
        }
        this.classNames.addAll(Arrays.asList(stringArray));
        return this;
    }

    public String[] reflectClassNames() {
        if (this.parent == null) {
            Objects.requireNonNull(this.packageName, "Package name is null");
        }
        String[] stringArray = new String[this.classNames.size()];
        Class clazz = this.parent == null ? null : (Class)XReflection.of((Class)this.parent.unreflect()).asArray(0).unreflect();
        int n = 0;
        for (String string : this.classNames) {
            String string2 = clazz == null ? this.packageName + '.' + string : clazz.getName() + '$' + string;
            if (this.array != 0) {
                string2 = Strings.repeat((String)"[", (int)this.array) + 'L' + string2 + ';';
            }
            stringArray[n++] = string2;
        }
        return stringArray;
    }

    @Override
    public DynamicClassHandle clone() {
        DynamicClassHandle dynamicClassHandle = new DynamicClassHandle(this.namespace);
        dynamicClassHandle.array = this.array;
        dynamicClassHandle.parent = this.parent;
        dynamicClassHandle.packageName = this.packageName;
        dynamicClassHandle.classNames.addAll(this.classNames);
        return dynamicClassHandle;
    }

    @Override
    public Class<?> reflect() {
        String[] stringArray = this.reflectClassNames();
        if (stringArray.length == 0) {
            throw new IllegalStateException("No class name specified for " + this);
        }
        Throwable throwable = null;
        for (String string : stringArray) {
            try {
                return Class.forName(string);
            }
            catch (ClassNotFoundException classNotFoundException) {
                if (throwable == null) {
                    throwable = new ClassNotFoundException("None of the classes were found");
                }
                throwable.addSuppressed(classNotFoundException);
            }
        }
        throw (ClassNotFoundException)XReflection.relativizeSuppressedExceptions(throwable);
    }

    @Override
    public DynamicClassHandle asArray(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Array dimension cannot be negative: " + n);
        }
        this.array = n;
        return this;
    }

    @Override
    public boolean isArray() {
        return this.array > 0;
    }

    @Override
    public Set<String> getPossibleNames() {
        return this.classNames;
    }
}

