package fr.valoriatycoon.farm.regeneration;

/** Stable world/block key used by memory and SQLite regeneration indexes. */
public record BlockPosition(String worldName, int x, int y, int z) {
    public long chunkKey() {
        return ((long) (x >> 4) << 32) ^ ((z >> 4) & 0xffffffffL);
    }
}
