package fr.valoriatycoon.farm.regeneration;

/** Persistable delayed block restoration. Block data uses Bukkit's canonical string format. */
public record PendingBlockRegeneration(
        BlockPosition position,
        String blockData,
        long dueAtEpochMillis
) implements Comparable<PendingBlockRegeneration> {
    @Override
    public int compareTo(PendingBlockRegeneration other) {
        return Long.compare(dueAtEpochMillis, other.dueAtEpochMillis);
    }
}
