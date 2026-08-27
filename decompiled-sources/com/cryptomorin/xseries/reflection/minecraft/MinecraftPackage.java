/*
 * Decompiled with CFR 0.152.
 */
package com.cryptomorin.xseries.reflection.minecraft;

import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.jvm.classes.PackageHandle;
import org.intellij.lang.annotations.Pattern;

public enum MinecraftPackage implements PackageHandle
{
    NMS(XReflection.NMS_PACKAGE),
    CB(XReflection.CRAFTBUKKIT_PACKAGE),
    BUKKIT("org.bukkit"),
    SPIGOT("org.spigotmc");

    private final String packageId;

    private MinecraftPackage(String string2) {
        this.packageId = string2;
    }

    @Override
    public String packageId() {
        return this.name();
    }

    @Override
    public String getBasePackageName() {
        return this.packageId;
    }

    @Override
    public String getPackage(@Pattern(value="(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*") String string) {
        if (string.startsWith(".") || string.endsWith(".")) {
            throw new IllegalArgumentException("Package name must not start or end with a dot: " + string + " (" + this + ')');
        }
        if (!string.isEmpty() && (this != NMS || XReflection.supports(17))) {
            return this.packageId + '.' + string;
        }
        return this.packageId;
    }
}

