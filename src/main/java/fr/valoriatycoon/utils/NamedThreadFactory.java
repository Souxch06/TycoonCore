package fr.valoriatycoon.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** Creates identifiable plugin-owned threads with an uncaught exception handler. */
public final class NamedThreadFactory implements ThreadFactory {
    private final String prefix;
    private final AtomicInteger sequence = new AtomicInteger();
    private final Thread.UncaughtExceptionHandler exceptionHandler;

    public NamedThreadFactory(String prefix, Thread.UncaughtExceptionHandler exceptionHandler) {
        this.prefix = prefix;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public Thread newThread(Runnable task) {
        Thread thread = new Thread(task, prefix + '-' + sequence.incrementAndGet());
        thread.setDaemon(false);
        thread.setUncaughtExceptionHandler(exceptionHandler);
        return thread;
    }
}
