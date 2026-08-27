/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 */
package com.cryptomorin.xseries.reflection.jvm.classes;

import javax.annotation.Nonnull;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus;

public interface PackageHandle {
    @Language(value="RegExp")
    @ApiStatus.Internal
    public static final String JAVA_PACKAGE_PATTERN = "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
    @Language(value="RegExp")
    @ApiStatus.Internal
    public static final String JAVA_IDENTIFIER_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";

    @Nonnull
    public String packageId();

    @Nonnull
    public String getBasePackageName();

    @Nonnull
    public String getPackage(@Nonnull String var1);
}

