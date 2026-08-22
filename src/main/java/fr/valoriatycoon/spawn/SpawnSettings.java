package fr.valoriatycoon.spawn;

import java.util.List;
import java.util.Objects;
import org.bukkit.Material;

/** Immutable spawn.yml snapshot for the generated medieval hub. */
public record SpawnSettings(
        String worldName,
        long seed,
        int groundY,
        int islandRadius,
        double borderSize,
        int protectionRadius,
        boolean teleportOnFirstJoin,
        int spawnX,
        int spawnZ,
        float spawnYaw,
        List<PortalDefinition> portals
) {
    public SpawnSettings {
        if (worldName == null || worldName.isBlank()
                || groundY < -32 || groundY > 280
                || islandRadius < 64 || islandRadius > 512
                || borderSize < islandRadius * 2.0 + 32.0
                || protectionRadius < islandRadius
                || protectionRadius >= borderSize / 2.0
                || Math.hypot(spawnX, spawnZ) > islandRadius - 8.0) {
            throw new IllegalArgumentException("Invalid generated spawn settings");
        }
        portals = List.copyOf(portals);
        if (portals.size() != 4 || portals.stream().anyMatch(portal ->
                Math.hypot(portal.centerX(), portal.centerZ()) > islandRadius - 8.0)) {
            throw new IllegalArgumentException("The medieval spawn requires four arches inside the island");
        }
    }

    /** Decorative arch that opens one public farm destination. */
    public record PortalDefinition(
            String farmId,
            String displayName,
            int centerX,
            int centerZ,
            Axis axis,
            Material frameMaterial,
            Material accentMaterial
    ) {
        public PortalDefinition {
            farmId = Objects.requireNonNull(farmId, "farmId");
            displayName = Objects.requireNonNull(displayName, "displayName");
            axis = Objects.requireNonNull(axis, "axis");
            frameMaterial = Objects.requireNonNull(frameMaterial, "frameMaterial");
            accentMaterial = Objects.requireNonNull(accentMaterial, "accentMaterial");
        }

        public boolean contains(int x, int z) {
            return Math.abs(x - centerX) <= 5 && Math.abs(z - centerZ) <= 5;
        }
    }

    public enum Axis {
        X,
        Z
    }
}
