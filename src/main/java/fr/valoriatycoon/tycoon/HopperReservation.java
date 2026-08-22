package fr.valoriatycoon.tycoon;

import java.util.concurrent.CompletableFuture;

/** Immediate limit decision plus asynchronous persistence completion. */
public record HopperReservation(boolean accepted, CompletableFuture<Void> persistence) {
    public static HopperReservation rejected() {
        return new HopperReservation(false, CompletableFuture.completedFuture(null));
    }
}
