package fr.valoriatycoon.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentRateLimiterTest {
    @Test
    void rejectsImmediateDuplicateAndCanBeCleared() {
        PaymentRateLimiter limiter = new PaymentRateLimiter();
        UUID playerId = UUID.randomUUID();

        assertEquals(0L, limiter.tryAcquire(playerId, Duration.ofSeconds(3)));
        assertTrue(limiter.tryAcquire(playerId, Duration.ofSeconds(3)) >= 1L);
        limiter.remove(playerId);
        assertEquals(0L, limiter.tryAcquire(playerId, Duration.ofSeconds(3)));
    }

    @Test
    void disabledCooldownNeverStoresADeadline() {
        PaymentRateLimiter limiter = new PaymentRateLimiter();
        UUID playerId = UUID.randomUUID();
        assertEquals(0L, limiter.tryAcquire(playerId, Duration.ZERO));
        assertEquals(0L, limiter.tryAcquire(playerId, Duration.ZERO));
    }
}
