package org.holoeasy.util;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0003\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u001e\u0010\u0003\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00050\u00042\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tJ\u001c\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tJ(\u0010\u000b\u001a\b\u0012\u0004\u0012\u0002H\f0\u0004\"\u0004\b\u0000\u0010\f2\u0006\u0010\u0006\u001a\u00020\u00072\f\u0010\r\u001a\b\u0012\u0004\u0012\u0002H\f0\u000eJ(\u0010\u000f\u001a\b\u0012\u0004\u0012\u0002H\f0\u0004\"\u0004\b\u0000\u0010\f2\u0006\u0010\u0006\u001a\u00020\u00072\f\u0010\r\u001a\b\u0012\u0004\u0012\u0002H\f0\u000eJ<\u0010\u0010\u001a\u0012\u0012\u0004\u0012\u0002H\f\u0012\b\b\u0000\u0012\u0004\u0018\u00010\u00120\u0011\"\u0004\b\u0000\u0010\f2\u0006\u0010\u0006\u001a\u00020\u00072\u0016\u0010\u0013\u001a\u0012\u0012\u0004\u0012\u0002H\f\u0012\b\b\u0000\u0012\u0004\u0018\u00010\u00120\u0011\u00a8\u0006\u0014"}, d2={"Lorg/holoeasy/util/BukkitFuture;", "", "()V", "runAsync", "Ljava/util/concurrent/CompletableFuture;", "Ljava/lang/Void;", "plugin", "Lorg/bukkit/plugin/Plugin;", "runnable", "Ljava/lang/Runnable;", "runSync", "supplyAsync", "T", "supplier", "Ljava/util/function/Supplier;", "supplySync", "sync", "Ljava/util/function/BiConsumer;", "", "action", "holoeasy-core"})
public final class BukkitFuture {
    @NotNull
    public static final BukkitFuture INSTANCE = new BukkitFuture();

    private BukkitFuture() {
    }

    @NotNull
    public final <T> CompletableFuture<T> supplyAsync(@NotNull Plugin plugin, @NotNull Supplier<T> supplier) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        Intrinsics.checkNotNullParameter(supplier, "supplier");
        CompletableFuture completableFuture = new CompletableFuture();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> BukkitFuture.supplyAsync$lambda$0(completableFuture, supplier));
        return completableFuture;
    }

    @NotNull
    public final CompletableFuture<Void> runAsync(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        Intrinsics.checkNotNullParameter(runnable, "runnable");
        CompletableFuture<Void> completableFuture = new CompletableFuture<Void>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> BukkitFuture.runAsync$lambda$1(runnable, completableFuture));
        return completableFuture;
    }

    @NotNull
    public final <T> CompletableFuture<T> supplySync(@NotNull Plugin plugin, @NotNull Supplier<T> supplier) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        Intrinsics.checkNotNullParameter(supplier, "supplier");
        CompletableFuture<T> completableFuture = new CompletableFuture<T>();
        if (Bukkit.isPrimaryThread()) {
            try {
                completableFuture.complete(supplier.get());
            }
            catch (Throwable throwable) {
                completableFuture.completeExceptionally(throwable);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> BukkitFuture.supplySync$lambda$2(completableFuture, supplier));
        }
        return completableFuture;
    }

    @NotNull
    public final CompletableFuture<Void> runSync(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        Intrinsics.checkNotNullParameter(runnable, "runnable");
        CompletableFuture<Void> completableFuture = new CompletableFuture<Void>();
        if (Bukkit.isPrimaryThread()) {
            try {
                runnable.run();
                completableFuture.complete(null);
            }
            catch (Throwable throwable) {
                completableFuture.completeExceptionally(throwable);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> BukkitFuture.runSync$lambda$3(runnable, completableFuture));
        }
        return completableFuture;
    }

    @NotNull
    public final <T> BiConsumer<T, ? super Throwable> sync(@NotNull Plugin plugin, @NotNull BiConsumer<T, ? super Throwable> biConsumer) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        Intrinsics.checkNotNullParameter(biConsumer, "action");
        return (arg_0, arg_1) -> BukkitFuture.sync$lambda$5(plugin, biConsumer, arg_0, arg_1);
    }

    private static final void supplyAsync$lambda$0(CompletableFuture completableFuture, Supplier supplier) {
        Intrinsics.checkNotNullParameter(completableFuture, "$future");
        Intrinsics.checkNotNullParameter(supplier, "$supplier");
        try {
            completableFuture.complete(supplier.get());
        }
        catch (Throwable throwable) {
            completableFuture.completeExceptionally(throwable);
        }
    }

    private static final void runAsync$lambda$1(Runnable runnable, CompletableFuture completableFuture) {
        Intrinsics.checkNotNullParameter(runnable, "$runnable");
        Intrinsics.checkNotNullParameter(completableFuture, "$future");
        try {
            runnable.run();
            completableFuture.complete(null);
        }
        catch (Throwable throwable) {
            completableFuture.completeExceptionally(throwable);
        }
    }

    private static final void supplySync$lambda$2(CompletableFuture completableFuture, Supplier supplier) {
        Intrinsics.checkNotNullParameter(completableFuture, "$future");
        Intrinsics.checkNotNullParameter(supplier, "$supplier");
        try {
            completableFuture.complete(supplier.get());
        }
        catch (Throwable throwable) {
            completableFuture.completeExceptionally(throwable);
        }
    }

    private static final void runSync$lambda$3(Runnable runnable, CompletableFuture completableFuture) {
        Intrinsics.checkNotNullParameter(runnable, "$runnable");
        Intrinsics.checkNotNullParameter(completableFuture, "$future");
        try {
            runnable.run();
            completableFuture.complete(null);
        }
        catch (Throwable throwable) {
            completableFuture.completeExceptionally(throwable);
        }
    }

    private static final void sync$lambda$5$lambda$4(BiConsumer biConsumer, Object object, Throwable throwable) {
        Intrinsics.checkNotNullParameter(biConsumer, "$action");
        biConsumer.accept(object, throwable);
    }

    private static final void sync$lambda$5(Plugin plugin, BiConsumer biConsumer, Object object, Throwable throwable) {
        Intrinsics.checkNotNullParameter(plugin, "$plugin");
        Intrinsics.checkNotNullParameter(biConsumer, "$action");
        INSTANCE.runSync(plugin, () -> BukkitFuture.sync$lambda$5$lambda$4(biConsumer, object, throwable));
    }
}

