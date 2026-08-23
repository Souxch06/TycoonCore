package fr.valoriatycoon.farm;

import java.util.concurrent.CompletableFuture;

/** Immediate acceptance decision plus asynchronous Paper teleport completion. */
public record FarmTeleportAttempt(
        boolean accepted,
        long waitSeconds,
        CompletableFuture<Boolean> completion
) {
    public static FarmTeleportAttempt cooldown(long waitSeconds) {
        return new FarmTeleportAttempt(false, waitSeconds, CompletableFuture.completedFuture(false));
    }

    public static FarmTeleportAttempt accepted(CompletableFuture<Boolean> completion) {
        return new FarmTeleportAttempt(true, 0L, completion);
    }
}
