package fr.valoriatycoon.farm;

import java.util.List;
import java.util.Objects;
import org.bukkit.Material;

/** One bounded, rank-gated resource zone inside a generated public farm world. */
public record FarmZoneDefinition(
        int index,
        String id,
        int requiredRank,
        int centerX,
        int centerZ,
        int size,
        Shape shape,
        int menuSlot,
        Material menuIcon,
        String menuName,
        List<String> menuLore,
        Resource resource
) {
    public FarmZoneDefinition {
        if (index < 1 || id == null || id.isBlank() || requiredRank < 0 || size < 16) {
            throw new IllegalArgumentException("Invalid farm zone definition");
        }
        shape = Objects.requireNonNull(shape, "shape");
        menuIcon = Objects.requireNonNull(menuIcon, "menuIcon");
        menuName = Objects.requireNonNull(menuName, "menuName");
        menuLore = List.copyOf(menuLore);
        resource = Objects.requireNonNull(resource, "resource");
    }

    public boolean contains(int x, int z) {
        if (shape == Shape.CIRCLE) {
            long dx = (long) x - centerX;
            long dz = (long) z - centerZ;
            double radius = size / 2.0 - 0.5;
            return dx * dx + dz * dz <= radius * radius;
        }
        return x >= minimumX() && x <= maximumX() && z >= minimumZ() && z <= maximumZ();
    }

    public double normalizedDistance(int x, int z) {
        return Math.min(1.0, Math.hypot(x - centerX, z - centerZ) / (size / 2.0));
    }

    public int minimumX() {
        return centerX - size / 2;
    }

    public int maximumX() {
        return minimumX() + size - 1;
    }

    public int minimumZ() {
        return centerZ - size / 2;
    }

    public int maximumZ() {
        return minimumZ() + size - 1;
    }

    public enum Shape {
        SQUARE,
        CIRCLE
    }

    /** Type-specific blocks generated in the zone. */
    public sealed interface Resource permits MineResource, FieldsResource, ForestResource {
    }

    public record MineResource(
            Material filler,
            List<FarmGenerationSettings.OreRule> ores
    ) implements Resource {
        public MineResource {
            filler = Objects.requireNonNull(filler, "filler");
            ores = List.copyOf(ores);
        }
    }

    public record FieldsResource(Material crop) implements Resource {
        public FieldsResource {
            crop = Objects.requireNonNull(crop, "crop");
        }
    }

    public record ForestResource(Material log, Material leaves) implements Resource {
        public ForestResource {
            log = Objects.requireNonNull(log, "log");
            leaves = Objects.requireNonNull(leaves, "leaves");
        }
    }
}
