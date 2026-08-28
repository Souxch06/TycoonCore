package com.cryptomorin.xseries.reflection;

import com.cryptomorin.xseries.reflection.AggregateReflectiveHandle;
import com.cryptomorin.xseries.reflection.ReflectiveHandle;
import com.cryptomorin.xseries.reflection.ReflectiveNamespace;
import com.cryptomorin.xseries.reflection.VersionHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.DynamicClassHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.StaticClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftMapping;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftPackage;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;

public final class XReflection {
    @Nullable
    @ApiStatus.Internal
    public static final String NMS_VERSION = XReflection.findNMSVersionString();
    @ApiStatus.Internal
    public static final String XSERIES_VERSION = "11.2.0";
    public static final int MAJOR_NUMBER;
    public static final int MINOR_NUMBER;
    public static final int PATCH_NUMBER;
    @ApiStatus.Internal
    public static final String CRAFTBUKKIT_PACKAGE;
    @ApiStatus.Internal
    public static final String NMS_PACKAGE;
    @ApiStatus.Internal
    @ApiStatus.Experimental
    public static final Set<MinecraftMapping> SUPPORTED_MAPPINGS;

    @Nullable
    @ApiStatus.Internal
    public static String findNMSVersionString() {
        String string = null;
        for (Package package_ : Package.getPackages()) {
            String string2 = package_.getName();
            if (!string2.startsWith("org.bukkit.craftbukkit.v")) continue;
            string = package_.getName().split("\\.")[3];
            try {
                Class.forName("org.bukkit.craftbukkit." + string + ".entity.CraftPlayer");
                break;
            }
            catch (ClassNotFoundException classNotFoundException) {
                string = null;
            }
        }
        return string;
    }

    public static String getVersionInformation() {
        return "(NMS: " + (NMS_VERSION == null ? "Unknown NMS" : NMS_VERSION) + " | Parsed: " + MAJOR_NUMBER + '.' + MINOR_NUMBER + '.' + PATCH_NUMBER + " | Minecraft: " + Bukkit.getVersion() + " | Bukkit: " + Bukkit.getBukkitVersion() + ')';
    }

    @Nullable
    public static Integer getLatestPatchNumberOf(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Minor version must be positive: " + n);
        }
        int[] nArray = new int[]{1, 5, 2, 7, 2, 4, 10, 8, 4, 2, 2, 2, 2, 4, 2, 5, 1, 2, 4, 6, 0};
        if (n > nArray.length) {
            return null;
        }
        return nArray[n - 1];
    }

    private XReflection() {
    }

    public static <T> VersionHandle<T> v(int n, T t) {
        return new VersionHandle<T>(n, t);
    }

    public static <T> VersionHandle<T> v(int n, int n2, T t) {
        return new VersionHandle<T>(n, n2, t);
    }

    public static <T> VersionHandle<T> v(int n, Callable<T> callable) {
        return new VersionHandle<T>(n, callable);
    }

    public static <T> VersionHandle<T> v(int n, int n2, Callable<T> callable) {
        return new VersionHandle<T>(n, n2, callable);
    }

    public static boolean supports(int n) {
        return MINOR_NUMBER >= n;
    }

    public static boolean supports(int n, int n2, int n3) {
        if (n != 1) {
            throw new IllegalArgumentException("Invalid major number: " + n);
        }
        return XReflection.supports(n2, n3);
    }

    public static boolean supports(int n, int n2) {
        return MINOR_NUMBER == n ? PATCH_NUMBER >= n2 : XReflection.supports(n);
    }

    @Deprecated
    public static boolean supportsPatch(int n) {
        return PATCH_NUMBER >= n;
    }

    @Nonnull
    @Deprecated
    public static Class<?> getNMSClass(@Nullable String string, @Nonnull String string2) {
        if (string != null && XReflection.supports(17)) {
            string2 = string + '.' + string2;
        }
        try {
            return Class.forName(NMS_PACKAGE + '.' + string2);
        }
        catch (ClassNotFoundException classNotFoundException) {
            throw new RuntimeException(classNotFoundException);
        }
    }

    @Nonnull
    @Deprecated
    public static Class<?> getNMSClass(@Nonnull String string) {
        return XReflection.getNMSClass(null, string);
    }

    @Nonnull
    @Deprecated
    public static Class<?> getCraftClass(@Nonnull String string) {
        try {
            return Class.forName(CRAFTBUKKIT_PACKAGE + '.' + string);
        }
        catch (ClassNotFoundException classNotFoundException) {
            throw new RuntimeException(classNotFoundException);
        }
    }

    @Nonnull
    public static Class<?> toArrayClass(Class<?> clazz) {
        try {
            return Class.forName("[L" + clazz.getName() + ';');
        }
        catch (ClassNotFoundException classNotFoundException) {
            throw new RuntimeException("Cannot find array class for class: " + clazz, classNotFoundException);
        }
    }

    public static MinecraftClassHandle ofMinecraft() {
        return new MinecraftClassHandle(new ReflectiveNamespace());
    }

    public static DynamicClassHandle classHandle() {
        return new DynamicClassHandle(new ReflectiveNamespace());
    }

    public static StaticClassHandle of(Class<?> clazz) {
        return new StaticClassHandle(new ReflectiveNamespace(), clazz);
    }

    public static ReflectiveNamespace namespaced() {
        return new ReflectiveNamespace();
    }

    @SafeVarargs
    public static <T, H extends ReflectiveHandle<T>> AggregateReflectiveHandle<T, H> any(H ... HArray) {
        return new AggregateReflectiveHandle(Arrays.stream(HArray).map(reflectiveHandle -> () -> reflectiveHandle).collect(Collectors.toList()));
    }

    @SafeVarargs
    public static <T, H extends ReflectiveHandle<T>> AggregateReflectiveHandle<T, H> anyOf(Callable<H> ... callableArray) {
        return new AggregateReflectiveHandle(Arrays.asList(callableArray));
    }

    @ApiStatus.Experimental
    public static <T extends Throwable> T relativizeSuppressedExceptions(T t) {
        Objects.requireNonNull(t, "Cannot relativize null exception");
        StackTraceElement[] stackTraceElementArray = new StackTraceElement[]{};
        StackTraceElement[] stackTraceElementArray2 = t.getStackTrace();
        for (Throwable throwable : t.getSuppressed()) {
            StackTraceElement[] stackTraceElementArray3 = throwable.getStackTrace();
            ArrayList<StackTraceElement> arrayList = new ArrayList<StackTraceElement>(10);
            for (int i = 0; i < stackTraceElementArray3.length; ++i) {
                if (stackTraceElementArray2.length <= i) {
                    arrayList = null;
                    break;
                }
                StackTraceElement stackTraceElement = stackTraceElementArray2[i];
                StackTraceElement stackTraceElement2 = stackTraceElementArray3[i];
                if (stackTraceElement.equals(stackTraceElement2)) break;
                arrayList.add(stackTraceElement2);
            }
            if (arrayList == null) continue;
            throwable.setStackTrace(arrayList.toArray(stackTraceElementArray));
        }
        return t;
    }

    private static <T extends Throwable> void throwException(Throwable throwable) {
        throw throwable;
    }

    public static RuntimeException throwCheckedException(Throwable throwable) {
        Objects.requireNonNull(throwable, "Cannot throw null exception");
        XReflection.throwException(throwable);
        return null;
    }

    @ApiStatus.Experimental
    public static <T> CompletableFuture<T> stacktrace(@Nonnull CompletableFuture<T> completableFuture) {
        StackTraceElement[] stackTraceElementArray = Thread.currentThread().getStackTrace();
        return completableFuture.whenComplete((object, throwable) -> {
            if (throwable == null) {
                completableFuture.complete(object);
                return;
            }
            try {
                StackTraceElement[] stackTraceElementArray2;
                StackTraceElement[] stackTraceElementArray3 = throwable.getStackTrace();
                if (stackTraceElementArray3.length >= 3) {
                    stackTraceElementArray2 = new ArrayList(Arrays.asList(stackTraceElementArray3));
                    Collections.reverse(stackTraceElementArray2);
                    Iterator iterator2 = stackTraceElementArray2.iterator();
                    List<String> list = Arrays.asList("java.util.concurrent.CompletableFuture", "java.util.concurrent.ThreadPoolExecutor", "java.util.concurrent.ForkJoinTask", "java.util.concurrent.ForkJoinWorkerThread", "java.util.concurrent.ForkJoinPool");
                    List<String> list2 = Arrays.asList("postComplete", "encodeThrowable", "completeThrowable", "tryFire", "run", "runWorker", "scan", "exec", "doExec", "topLevelExec", "uniWhenComplete");
                    while (iterator2.hasNext()) {
                        StackTraceElement stackTraceElement = (StackTraceElement)iterator2.next();
                        String string = stackTraceElement.getClassName();
                        String string2 = stackTraceElement.getMethodName();
                        if (string.equals(Thread.class.getName())) continue;
                        if (!list.stream().anyMatch(string::startsWith)) break;
                        if (!list2.stream().anyMatch(string2::equals)) break;
                        iterator2.remove();
                    }
                    Collections.reverse(stackTraceElementArray2);
                    stackTraceElementArray3 = stackTraceElementArray2.toArray(new StackTraceElement[0]);
                }
                stackTraceElementArray2 = (StackTraceElement[])Arrays.stream(stackTraceElementArray).skip(2L).toArray(StackTraceElement[]::new);
                throwable.setStackTrace(XReflection.concatenate(stackTraceElementArray3, stackTraceElementArray2));
            }
            catch (Throwable throwable2) {
                throwable.addSuppressed(throwable2);
            }
            finally {
                completableFuture.completeExceptionally((Throwable)throwable);
            }
        });
    }

    @ApiStatus.Internal
    public static <T> T[] concatenate(T[] TArray, T[] TArray2) {
        int n = TArray.length;
        int n2 = TArray2.length;
        Object[] objectArray = (Object[])Array.newInstance(TArray.getClass().getComponentType(), n + n2);
        System.arraycopy(TArray, 0, objectArray, 0, n);
        System.arraycopy(TArray2, 0, objectArray, n, n2);
        return objectArray;
    }

    static {
        // Patch ValoriaTycoon: le motif d'origine "^(?<major>\d+)\.(?<minor>\d+)..." lit la version
        // Mineure APRÈS le point, ce qui est faux depuis le versionnage calendaire de Minecraft
        // ("26.2.build.112-stable" donnait MINOR_NUMBER=2 et donc supports(17)==false, d'où un
        // NMS_PACKAGE invalide et une ExceptionInInitializerError sur Paper 26.x). Le motif ignore
        // le préfixe "1." quand il est présent et garde MINOR_NUMBER/PATCH_NUMBER identiques sur
        // toutes les versions 1.x.
        Object object = Pattern.compile("^(?:1\\.)?(?<minor>(?<major>\\d{1,2}))(?:\\.(?<patch>\\d+))?").matcher(Bukkit.getBukkitVersion());
        if (((Matcher)object).find()) {
            try {
                String string = ((Matcher)object).group("patch");
                MAJOR_NUMBER = Integer.parseInt(((Matcher)object).group("major"));
                MINOR_NUMBER = Integer.parseInt(((Matcher)object).group("minor"));
                PATCH_NUMBER = Integer.parseInt(string == null || string.isEmpty() ? "0" : string);
            }
            catch (Throwable throwable) {
                throw new RuntimeException("Failed to parse minor number: " + object + ' ' + XReflection.getVersionInformation(), throwable);
            }
        } else {
            throw new IllegalStateException("Cannot parse server version: \"" + Bukkit.getBukkitVersion() + '\"');
        }
        CRAFTBUKKIT_PACKAGE = Bukkit.getServer().getClass().getPackage().getName();
        NMS_PACKAGE = XReflection.v(17, "net.minecraft").orElse("net.minecraft.server." + NMS_VERSION);
        if (XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "server.level").map(MinecraftMapping.MOJANG, "ServerPlayer").exists()) {
            SUPPORTED_MAPPINGS = EnumSet.of(MinecraftMapping.MOJANG);
        } else if (XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "server.level").map(MinecraftMapping.MOJANG, "EntityPlayer").exists()) {
            SUPPORTED_MAPPINGS = EnumSet.of(MinecraftMapping.SPIGOT, MinecraftMapping.OBFUSCATED);
        } else {
            object = XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "server.level").map(MinecraftMapping.MOJANG, "ServerPlayer").map(MinecraftMapping.SPIGOT, "EntityPlayer");
            throw new RuntimeException("Unknown Minecraft mapping " + XReflection.getVersionInformation(), object.catchError());
        }
    }
}

