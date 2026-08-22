package fr.valoriatycoon.economy;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Monotonic per-player limiter used to prevent command spam and accidental duplicate payments. */
public final class PaymentRateLimiter {
    private final ConcurrentHashMap<UUID, Long> deadlines = new ConcurrentHashMap<>();

    /**
     * Attempts to consume one payment slot.
     *
     * @return zero when allowed, otherwise the number of whole seconds to wait (rounded up)
     */
    public long tryAcquire(UUID playerId, Duration cooldown) {
        long cooldownNanos = cooldown.toNanos();
        if (cooldownNanos <= 0) {
            return 0;
        }
        long now = System.nanoTime();
        long currentDeadline = deadlines.getOrDefault(playerId, 0L);
        if (currentDeadline > now) {
            return Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(currentDeadline - now - 1L) + 1L);
        }
        deadlines.put(playerId, saturatingAdd(now, cooldownNanos));
        return 0;
    }

    public void remove(UUID playerId) {
        deadlines.remove(playerId);
    }

    private long saturatingAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
