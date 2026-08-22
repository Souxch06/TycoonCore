package fr.valoriatycoon.crates;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable physical ItemDisplay/Interaction crate station settings. */
public record CrateStationSettings(
        boolean enabled,
        String worldName,
        int refreshIntervalTicks,
        float interactionWidth,
        float interactionHeight,
        double labelOffsetY,
        boolean effectsEnabled,
        int effectIntervalTicks,
        double effectViewDistance,
        Map<CrateStationType, Station> stations
) {
    public CrateStationSettings {
        worldName = Objects.requireNonNull(worldName, "worldName").trim();
        stations = Objects.requireNonNull(stations, "stations");
        stations = Collections.unmodifiableMap(new EnumMap<>(stations));
        if (worldName.isBlank() || refreshIntervalTicks < 20
                || !Float.isFinite(interactionWidth) || interactionWidth <= 0.0F || interactionWidth > 4.0F
                || !Float.isFinite(interactionHeight) || interactionHeight <= 0.0F || interactionHeight > 4.0F
                || !Double.isFinite(labelOffsetY) || labelOffsetY < 0.5 || labelOffsetY > 5.0
                || effectIntervalTicks < 1 || effectIntervalTicks > 40
                || !Double.isFinite(effectViewDistance)
                || effectViewDistance < 8.0 || effectViewDistance > 96.0
                || stations.size() != CrateStationType.values().length) {
            throw new IllegalArgumentException("Invalid physical crate station settings");
        }
    }

    public Station station(CrateStationType type) {
        Station station = stations.get(type);
        if (station == null) {
            throw new IllegalArgumentException("Missing physical crate station: " + type);
        }
        return station;
    }

    /** Position relative to the generated /spawn anchor, client model path and ambient effect. */
    public record Station(
            double offsetX,
            double offsetY,
            double offsetZ,
            float yaw,
            String itemModel,
            Effect effect
    ) {
        public Station {
            itemModel = Objects.requireNonNull(itemModel, "itemModel").trim();
            effect = Objects.requireNonNull(effect, "effect");
            if (!Double.isFinite(offsetX) || !Double.isFinite(offsetY) || !Double.isFinite(offsetZ)
                    || !Float.isFinite(yaw) || itemModel.isBlank()
                    || Math.abs(offsetX) > 80.0 || Math.abs(offsetY) > 16.0 || Math.abs(offsetZ) > 80.0) {
                throw new IllegalArgumentException("Invalid physical crate station position/model");
            }
        }
    }

    /** Colored orbital particles and subtle model movement for one crate station. */
    public record Effect(
            int primaryRgb,
            int secondaryRgb,
            int particleCount,
            float particleSize,
            double orbitRadius,
            double orbitSpeed,
            double bobHeight,
            double yawSwayDegrees
    ) {
        public Effect {
            if (primaryRgb < 0 || primaryRgb > 0xFFFFFF
                    || secondaryRgb < 0 || secondaryRgb > 0xFFFFFF
                    || particleCount < 1 || particleCount > 12
                    || !Float.isFinite(particleSize) || particleSize < 0.25F || particleSize > 2.0F
                    || !Double.isFinite(orbitRadius) || orbitRadius < 0.25 || orbitRadius > 2.0
                    || !Double.isFinite(orbitSpeed) || orbitSpeed < 0.001 || orbitSpeed > 0.25
                    || !Double.isFinite(bobHeight) || bobHeight < 0.0 || bobHeight > 0.35
                    || !Double.isFinite(yawSwayDegrees)
                    || yawSwayDegrees < 0.0 || yawSwayDegrees > 15.0) {
                throw new IllegalArgumentException("Invalid physical crate ambient effect");
            }
        }
    }
}
