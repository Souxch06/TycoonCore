package fr.valoriatycoon.tycoon;

import org.bukkit.generator.ChunkGenerator;

/** Empty, stateless generator; islands are created only inside allocated plot bounds. */
public final class TycoonVoidGenerator extends ChunkGenerator {
    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs() { return false; }
    @Override public boolean shouldGenerateStructures() { return false; }
}
