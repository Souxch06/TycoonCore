package fr.valoriatycoon.tycoon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Chunk-keyed plot index avoiding scans of every Tycoon on protection events. */
public final class PlotSpatialIndex {
    private final Map<WorldChunkKey, List<Tycoon>> byChunk = new HashMap<>();

    public void clear() {
        byChunk.clear();
    }

    public void add(Tycoon tycoon) {
        for (int chunkX = tycoon.bounds().minimumX() >> 4; chunkX <= tycoon.bounds().maximumX() >> 4; chunkX++) {
            for (int chunkZ = tycoon.bounds().minimumZ() >> 4; chunkZ <= tycoon.bounds().maximumZ() >> 4; chunkZ++) {
                byChunk.computeIfAbsent(new WorldChunkKey(tycoon.worldName(), chunkX, chunkZ), ignored -> new ArrayList<>())
                        .add(tycoon);
            }
        }
    }

    public void replace(Tycoon previous, Tycoon updated) {
        remove(previous);
        add(updated);
    }

    public void remove(Tycoon tycoon) {
        for (int chunkX = tycoon.bounds().minimumX() >> 4; chunkX <= tycoon.bounds().maximumX() >> 4; chunkX++) {
            for (int chunkZ = tycoon.bounds().minimumZ() >> 4; chunkZ <= tycoon.bounds().maximumZ() >> 4; chunkZ++) {
                WorldChunkKey key = new WorldChunkKey(tycoon.worldName(), chunkX, chunkZ);
                List<Tycoon> entries = byChunk.get(key);
                if (entries != null) {
                    entries.removeIf(entry -> entry.id().equals(tycoon.id()));
                    if (entries.isEmpty()) {
                        byChunk.remove(key);
                    }
                }
            }
        }
    }

    public Optional<Tycoon> find(String worldName, int x, int z) {
        List<Tycoon> entries = byChunk.get(new WorldChunkKey(worldName, x >> 4, z >> 4));
        if (entries == null) {
            return Optional.empty();
        }
        return entries.stream().filter(tycoon -> tycoon.containsHorizontal(x, z)).findFirst();
    }

    private record WorldChunkKey(String worldName, int chunkX, int chunkZ) {
    }
}
