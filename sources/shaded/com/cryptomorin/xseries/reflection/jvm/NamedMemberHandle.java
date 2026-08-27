/*
 * Decompiled with CFR 0.152.
 */
package com.cryptomorin.xseries.reflection.jvm;

import com.cryptomorin.xseries.reflection.jvm.MemberHandle;
import com.cryptomorin.xseries.reflection.jvm.NamedReflectiveHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.ClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftMapping;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;

public abstract class NamedMemberHandle
extends MemberHandle
implements NamedReflectiveHandle {
    protected final Set<String> names = new HashSet<String>(5);

    @Override
    @NotNull
    public Set<String> getPossibleNames() {
        return this.names;
    }

    protected NamedMemberHandle(ClassHandle classHandle) {
        super(classHandle);
    }

    public NamedMemberHandle map(MinecraftMapping minecraftMapping, @Pattern(value="\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*") String string) {
        this.names.add(string);
        return this;
    }

    public NamedMemberHandle named(String ... stringArray) {
        this.names.addAll(Arrays.asList(stringArray));
        return this;
    }

    @Override
    public abstract NamedMemberHandle clone();
}

