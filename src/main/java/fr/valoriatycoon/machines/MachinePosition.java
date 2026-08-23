package fr.valoriatycoon.machines;

/** Stable machine block coordinate key. */
public record MachinePosition(String worldName, int x, int y, int z) {
    public int chunkX() { return x >> 4; }
    public int chunkZ() { return z >> 4; }
}
