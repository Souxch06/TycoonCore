package fr.valoriatycoon.crates;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Reconciles exactly one physical Quest key per persisted quest completion. */
@FunctionalInterface
public interface QuestKeyRewardSink {
    CompletableFuture<Void> synchronize(
            UUID playerId,
            String questId,
            long totalCompletions
    );
}
