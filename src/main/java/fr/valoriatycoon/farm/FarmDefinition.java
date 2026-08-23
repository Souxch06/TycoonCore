package fr.valoriatycoon.farm;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.bukkit.Material;

/** Immutable validated definition of one generated shared farm world. */
public record FarmDefinition(
        String id,
        boolean enabled,
        FarmType type,
        String worldName,
        long seed,
        double borderSize,
        int spawnProtectionRadius,
        float spawnYaw,
        Material menuIcon,
        int menuSlot,
        String menuName,
        List<String> menuLore,
        Set<Material> breakableBlocks,
        Duration defaultRegenerationDelay,
        Map<Material, Duration> regenerationDelays,
        Map<Material, Long> sellPricesCents,
        FarmGenerationSettings generation
) {
    public FarmDefinition {
        menuLore = List.copyOf(menuLore);
        breakableBlocks = Set.copyOf(breakableBlocks);
        regenerationDelays = Map.copyOf(regenerationDelays);
        sellPricesCents = Map.copyOf(sellPricesCents);
    }

    public Duration regenerationDelay(Material material) {
        return regenerationDelays.getOrDefault(material, defaultRegenerationDelay);
    }

    public long sellPrice(Material droppedMaterial) {
        return sellPricesCents.getOrDefault(droppedMaterial, 0L);
    }

    public List<FarmZoneDefinition> zones() {
        return generation.zones();
    }

    public Optional<FarmZoneDefinition> zone(int index) {
        for (FarmZoneDefinition zone : zones()) {
            if (zone.index() == index) {
                return Optional.of(zone);
            }
        }
        return Optional.empty();
    }

    public Optional<FarmZoneDefinition> zoneAt(int x, int z) {
        for (FarmZoneDefinition zone : zones()) {
            if (zone.contains(x, z)) {
                return Optional.of(zone);
            }
        }
        return Optional.empty();
    }

    public boolean zoned() {
        return !zones().isEmpty();
    }

    public List<FarmBridgeDefinition> bridges() {
        if (zones().size() < 2) {
            return List.of();
        }
        java.util.ArrayList<FarmBridgeDefinition> bridges = new java.util.ArrayList<>();
        for (int index = 0; index < zones().size() - 1; index++) {
            FarmZoneDefinition from = zones().get(index);
            FarmZoneDefinition to = zones().get(index + 1);
            bridges.add(new FarmBridgeDefinition(
                    from.index(),
                    to.index(),
                    from.maximumX() + 1,
                    to.minimumX() - 1,
                    (from.centerZ() + to.centerZ()) / 2,
                    generation.bridgeWidth(),
                    to.requiredRank()
            ));
        }
        return List.copyOf(bridges);
    }

    /** Returns the connecting bridge whose traversable deck contains the coordinates. */
    public Optional<FarmBridgeDefinition> bridgeAt(int x, int z) {
        for (int index = 0; index < zones().size() - 1; index++) {
            FarmZoneDefinition from = zones().get(index);
            FarmZoneDefinition to = zones().get(index + 1);
            int minimumX = from.maximumX() + 1;
            int maximumX = to.minimumX() - 1;
            int centerZ = (from.centerZ() + to.centerZ()) / 2;
            int halfWidth = generation.bridgeWidth() / 2;
            if (x >= minimumX
                    && x <= maximumX
                    && Math.abs(z - centerZ) <= halfWidth) {
                return Optional.of(new FarmBridgeDefinition(
                        from.index(),
                        to.index(),
                        minimumX,
                        maximumX,
                        centerZ,
                        generation.bridgeWidth(),
                        to.requiredRank()
                ));
            }
        }
        return Optional.empty();
    }

    /** Required rank for an island or connecting bridge at the supplied coordinates. */
    public OptionalInt requiredRankAt(int x, int z) {
        Optional<FarmZoneDefinition> zone = zoneAt(x, z);
        if (zone.isPresent()) {
            return OptionalInt.of(zone.get().requiredRank());
        }
        return bridgeAt(x, z)
                .map(bridge -> OptionalInt.of(bridge.requiredRank()))
                .orElseGet(OptionalInt::empty);
    }
}
