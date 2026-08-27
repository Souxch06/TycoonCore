package com.cryptomorin.xseries.reflection.minecraft;

import com.cryptomorin.xseries.reflection.ReflectiveNamespace;
import com.cryptomorin.xseries.reflection.jvm.classes.DynamicClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftMapping;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftPackage;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.Pattern;

public class MinecraftClassHandle
extends DynamicClassHandle {
    public MinecraftClassHandle(ReflectiveNamespace reflectiveNamespace) {
        super(reflectiveNamespace);
    }

    public MinecraftClassHandle inPackage(MinecraftPackage minecraftPackage) {
        super.inPackage(minecraftPackage);
        return this;
    }

    public MinecraftClassHandle inPackage(MinecraftPackage minecraftPackage, @Pattern(value="(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*") String string) {
        super.inPackage(minecraftPackage, string);
        return this;
    }

    @Override
    public MinecraftClassHandle inner(@Language(value="Java") String string) {
        return this.inner(this.namespace.ofMinecraft(string));
    }

    @Override
    public MinecraftClassHandle named(String ... stringArray) {
        super.named(stringArray);
        return this;
    }

    public MinecraftClassHandle map(MinecraftMapping minecraftMapping, @Pattern(value="\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*") String string) {
        this.classNames.add(string);
        return this;
    }

    @Override
    public MinecraftClassHandle clone() {
        MinecraftClassHandle minecraftClassHandle = new MinecraftClassHandle(this.namespace);
        minecraftClassHandle.array = this.array;
        minecraftClassHandle.parent = this.parent;
        minecraftClassHandle.packageName = this.packageName;
        minecraftClassHandle.classNames.addAll(this.classNames);
        return minecraftClassHandle;
    }
}

