package com.cryptomorin.xseries.reflection;

import com.cryptomorin.xseries.reflection.jvm.classes.ClassHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.DynamicClassHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.StaticClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftClassHandle;
import com.cryptomorin.xseries.reflection.parser.ReflectionParser;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus;

public class ReflectiveNamespace {
    private final Map<String, Class<?>> imports = new HashMap();
    private final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private final Set<ClassHandle> handles = Collections.newSetFromMap(new IdentityHashMap());

    protected ReflectiveNamespace() {
    }

    public ReflectiveNamespace imports(Class<?> ... classArray) {
        for (Class<?> clazz : classArray) {
            this.imports(clazz.getSimpleName(), clazz);
        }
        return this;
    }

    public ReflectiveNamespace imports(@Nonnull String string, @Nonnull Class<?> clazz) {
        Objects.requireNonNull(string);
        Objects.requireNonNull(clazz);
        this.imports.put(string, clazz);
        return this;
    }

    @Nonnull
    @ApiStatus.Internal
    public Map<String, Class<?>> getImports() {
        for (ClassHandle classHandle : this.handles) {
            Class clazz = (Class)classHandle.reflectOrNull();
            if (clazz == null) continue;
            for (String string : classHandle.getPossibleNames()) {
                this.imports.put(string, clazz);
            }
        }
        return this.imports;
    }

    @Nonnull
    @ApiStatus.Internal
    public MethodHandles.Lookup getLookup() {
        return this.lookup;
    }

    public StaticClassHandle of(Class<?> clazz) {
        this.imports(clazz);
        return new StaticClassHandle(this, clazz);
    }

    @ApiStatus.Internal
    public void link(ClassHandle classHandle) {
        if (classHandle.getNamespace() != this) {
            throw new IllegalArgumentException("Not the same namespace");
        }
        this.handles.add(classHandle);
    }

    public DynamicClassHandle classHandle(@Language(value="Java") String string) {
        DynamicClassHandle dynamicClassHandle = new DynamicClassHandle(this);
        return new ReflectionParser(string).imports(this).parseClass(dynamicClassHandle);
    }

    public MinecraftClassHandle ofMinecraft(@Language(value="Java") String string) {
        MinecraftClassHandle minecraftClassHandle = new MinecraftClassHandle(this);
        return new ReflectionParser(string).imports(this).parseClass(minecraftClassHandle);
    }
}

