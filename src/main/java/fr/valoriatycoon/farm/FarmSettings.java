package fr.valoriatycoon.farm;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;

/** Immutable snapshot of farms.yml. World-generation changes require a restart. */
public record FarmSettings(
        Menu menu,
        ZoneMenu zoneMenu,
        AutoSellMenu autoSellMenu,
        Teleport teleport,
        RankBarrier rankBarrier,
        Regeneration regeneration,
        AutoSell autoSell,
        Map<String, FarmDefinition> farms
) {
    public FarmSettings {
        farms = Collections.unmodifiableMap(new LinkedHashMap<>(farms));
    }

    public Optional<FarmDefinition> farm(String id) {
        return Optional.ofNullable(farms.get(id));
    }

    public record Menu(int size, String title) {
    }

    public record ZoneMenu(
            int size,
            String title,
            Material lockedIcon,
            String lockedName,
            List<String> lockedLore
    ) {
        public ZoneMenu {
            lockedLore = List.copyOf(lockedLore);
        }
    }

    public record AutoSellMenu(
            int size,
            String title,
            int toggleSlot,
            Material enabledIcon,
            Material disabledIcon,
            Material lockedIcon,
            String enabledName,
            String disabledName,
            String lockedName,
            List<String> toggleLore
    ) {
        public AutoSellMenu {
            toggleLore = List.copyOf(toggleLore);
        }
    }

    public record Teleport(int cooldownSeconds) {
    }

    /** Configurable reverse impulse applied by a locked rank portal. */
    public record RankBarrier(double horizontalKnockback, double verticalKnockback) {
        public RankBarrier {
            if (!Double.isFinite(horizontalKnockback)
                    || !Double.isFinite(verticalKnockback)
                    || horizontalKnockback <= 0.0
                    || verticalKnockback < 0.0) {
                throw new IllegalArgumentException("Invalid rank barrier knockback");
            }
        }
    }

    public record Regeneration(int checkIntervalTicks, int maximumBlocksPerRun, int unloadedRetrySeconds) {
    }

    public record AutoSell(int flushIntervalTicks, List<AutoSellLevel> levels) {
        public AutoSell {
            levels = List.copyOf(levels);
        }

        public int maximumLevel() {
            return levels.size();
        }

        public Optional<AutoSellLevel> level(int level) {
            if (level < 1 || level > levels.size()) {
                return Optional.empty();
            }
            return Optional.of(levels.get(level - 1));
        }
    }

    public record AutoSellLevel(int level, long costCents, BigDecimal saleMultiplier) {
    }
}
