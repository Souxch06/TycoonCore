package com.cryptomorin.xseries.profiles.mojang;

import com.cryptomorin.xseries.profiles.ProfilesCore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class PlayerProfileFetcherThread
implements ThreadFactory {
    public static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, new PlayerProfileFetcherThread());
    private static final AtomicInteger COUNT = new AtomicInteger();

    @Override
    public Thread newThread(@Nonnull Runnable runnable) {
        Thread thread3 = new Thread(runnable);
        thread3.setName("Profile Lookup Executor #" + COUNT.getAndIncrement());
        thread3.setUncaughtExceptionHandler((thread2, throwable) -> ProfilesCore.LOGGER.error("Uncaught exception in thread {}", (Object)thread2.getName(), (Object)throwable));
        return thread3;
    }
}

