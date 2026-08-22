package fr.valoriatycoon.warps;

import java.util.concurrent.CompletableFuture;

/** Immediate cooldown/availability decision plus an asynchronous Paper teleport result. */
public record WarpTeleportAttempt(
        boolean accepted,
        long waitSeconds,
        CompletableFuture<Boolean> completion
) {
    public static WarpTeleportAttempt rejected(long waitSeconds) {
        return new WarpTeleportAttempt(false, waitSeconds, CompletableFuture.completedFuture(false));
    }
}
