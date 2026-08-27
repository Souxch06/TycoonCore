/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.errorprone.annotations.CanIgnoreReturnValue
 */
package com.cryptomorin.xseries.profiles.mojang;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;

@ApiStatus.Internal
public final class RateLimiter {
    private final ConcurrentLinkedQueue<Long> requests = new ConcurrentLinkedQueue();
    private final int maxRequests;
    private final long per;

    RateLimiter(int n, Duration duration) {
        this.maxRequests = n;
        this.per = duration.toMillis();
    }

    @CanIgnoreReturnValue
    private @Unmodifiable ConcurrentLinkedQueue<Long> getRequests() {
        long l;
        long l2;
        if (this.requests.isEmpty()) {
            return this.requests;
        }
        long l3 = System.currentTimeMillis();
        Iterator<Long> iterator2 = this.requests.iterator();
        while (iterator2.hasNext() && (l2 = l3 - (l = iterator2.next().longValue())) > this.per) {
            iterator2.remove();
        }
        return this.requests;
    }

    public int getRemainingRequests() {
        return Math.max(0, this.maxRequests - this.getRequests().size());
    }

    public int getEffectiveRequestsCount() {
        return this.getRequests().size();
    }

    public void instantRateLimit() {
        long l = System.currentTimeMillis();
        for (int i = 0; i < this.getRemainingRequests(); ++i) {
            this.requests.add(l);
        }
    }

    public boolean acquire() {
        if (this.getRemainingRequests() <= 0) {
            return false;
        }
        this.requests.add(System.currentTimeMillis());
        return true;
    }

    public Duration timeUntilNextFreeRequest() {
        if (this.getRemainingRequests() == 0) {
            long l = System.currentTimeMillis();
            long l2 = this.requests.peek();
            long l3 = l - l2;
            return Duration.ofMillis(this.per - l3);
        }
        return Duration.ZERO;
    }

    public synchronized void acquireOrWait() {
        long l = this.timeUntilNextFreeRequest().toMillis();
        if (l == 0L) {
            return;
        }
        try {
            Thread.sleep(l);
        }
        catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }
    }

    public String toString() {
        return this.getClass().getSimpleName() + "[total=" + this.getRequests().size() + ", remaining=" + this.getRemainingRequests() + ", maxRequests=" + this.maxRequests + ", per=" + this.per + ']';
    }
}

