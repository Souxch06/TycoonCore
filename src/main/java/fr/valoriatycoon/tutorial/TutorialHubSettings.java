package fr.valoriatycoon.tutorial;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable settings for the informational academy reached with /warp tuto. */
public record TutorialHubSettings(
        boolean enabled,
        String worldName,
        int refreshIntervalTicks,
        float viewRange,
        int lineWidth,
        boolean shadowed,
        boolean defaultBackground,
        Map<String, Panel> panels
) {
    public TutorialHubSettings {
        worldName = Objects.requireNonNull(worldName, "worldName").trim();
        panels = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(panels, "panels")));
        if (worldName.isBlank()
                || refreshIntervalTicks < 20 || refreshIntervalTicks > 72_000
                || !Float.isFinite(viewRange) || viewRange < 0.1F || viewRange > 4.0F
                || lineWidth < 80 || lineWidth > 1_024
                || panels.size() < 8 || panels.size() > 24) {
            throw new IllegalArgumentException("Invalid tutorial hub settings");
        }
    }

    /** One multiline TextDisplay positioned relative to Valoria's authoritative spawn. */
    public record Panel(
            double offsetX,
            double offsetY,
            double offsetZ,
            List<String> lines
    ) {
        public Panel {
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
            if (!Double.isFinite(offsetX) || !Double.isFinite(offsetY) || !Double.isFinite(offsetZ)
                    || Math.abs(offsetX) > 64.0 || offsetY < 0.5 || offsetY > 16.0
                    || Math.abs(offsetZ) > 160.0 || lines.size() < 2 || lines.size() > 12
                    || lines.stream().anyMatch(line -> line == null || line.isBlank() || line.length() > 512)) {
                throw new IllegalArgumentException("Invalid tutorial hub panel");
            }
        }
    }
}
