/*
 * Decompiled with CFR 0.152.
 */
package com.cryptomorin.xseries.reflection.jvm;

import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.jvm.MemberHandle;
import com.cryptomorin.xseries.reflection.jvm.NamedMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.ClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftMapping;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnumMemberHandle
extends NamedMemberHandle {
    public EnumMemberHandle(ClassHandle classHandle) {
        super(classHandle);
    }

    @Override
    public EnumMemberHandle map(MinecraftMapping minecraftMapping, @Pattern(value="\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*") String string) {
        super.map(minecraftMapping, string);
        return this;
    }

    @Override
    public EnumMemberHandle named(String ... stringArray) {
        super.named(stringArray);
        return this;
    }

    @Override
    @ApiStatus.Obsolete
    public MemberHandle signature(String string) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    @ApiStatus.Obsolete
    public MethodHandle unreflect() {
        return (MethodHandle)super.unreflect();
    }

    @Override
    @ApiStatus.Obsolete
    @Nullable
    public MethodHandle reflectOrNull() {
        return (MethodHandle)super.reflectOrNull();
    }

    @Override
    @ApiStatus.Obsolete
    public MethodHandle reflect() {
        Field field = this.reflectJvm();
        return this.clazz.getNamespace().getLookup().unreflectGetter(field);
    }

    @Nullable
    public Object getEnumConstant() {
        try {
            return this.reflectJvm().get(null);
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            throw XReflection.throwCheckedException(reflectiveOperationException);
        }
    }

    public Field reflectJvm() {
        if (this.names.isEmpty()) {
            throw new IllegalStateException("No enum names specified");
        }
        Throwable throwable = null;
        Field field = null;
        Class clazz = (Class)this.clazz.reflect();
        for (String string : this.names) {
            if (field != null) break;
            try {
                field = clazz.getDeclaredField(string);
                if (field.isEnumConstant()) continue;
                throw new NoSuchFieldException("Field named '" + string + "' was found but it's not an enum constant " + this);
            }
            catch (NoSuchFieldException noSuchFieldException) {
                field = null;
                if (throwable == null) {
                    throwable = new NoSuchFieldException("None of the enums were found for " + this);
                }
                throwable.addSuppressed(noSuchFieldException);
            }
        }
        if (field == null) {
            throw (NoSuchFieldException)XReflection.relativizeSuppressedExceptions(throwable);
        }
        return this.handleAccessible(field);
    }

    @Override
    @ApiStatus.Obsolete
    public EnumMemberHandle clone() {
        throw new UnsupportedOperationException();
    }
}

