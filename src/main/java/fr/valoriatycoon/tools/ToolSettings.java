package fr.valoriatycoon.tools;

import fr.valoriatycoon.progression.ExperienceCurve;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;

/** Immutable tools.yml snapshot. */
public record ToolSettings(
        Menu menu,
        PurchaseMenu purchaseMenu,
        MultiTool multiTool,
        AbilitySettings abilities,
        Progression progression,
        Map<ToolType, ToolDefinition> tools,
        Map<ToolCapability, ToolCapabilityDefinition> capabilities
) {
    public ToolSettings {
        EnumMap<ToolType, ToolDefinition> toolCopy = new EnumMap<>(ToolType.class);
        toolCopy.putAll(tools);
        tools = Collections.unmodifiableMap(toolCopy);
        EnumMap<ToolCapability, ToolCapabilityDefinition> capabilityCopy = new EnumMap<>(ToolCapability.class);
        capabilityCopy.putAll(capabilities);
        capabilities = Collections.unmodifiableMap(capabilityCopy);
    }

    public ToolDefinition tool(ToolType type) {
        ToolDefinition definition = tools.get(type);
        if (definition == null) {
            throw new IllegalArgumentException("Missing tool definition for " + type);
        }
        return definition;
    }

    public ToolCapabilityDefinition capability(ToolCapability capability) {
        ToolCapabilityDefinition definition = capabilities.get(capability);
        if (definition == null) {
            throw new IllegalArgumentException("Missing capability definition for " + capability);
        }
        return definition;
    }

    public List<ToolCapabilityDefinition> capabilities(ToolType type) {
        return capabilities.values().stream()
                .filter(definition -> definition.appliesTo(type))
                .toList();
    }

    public record Menu(
            int size,
            String title,
            int infoSlot,
            String infoName,
            List<String> infoLore,
            int autoSellSlot,
            Material autoSellIcon,
            Material autoSellMaximumIcon,
            String autoSellName,
            String autoSellMaximumName,
            List<String> autoSellLore,
            List<String> autoSellMaximumLore
    ) {
        public Menu {
            infoLore = List.copyOf(infoLore);
            autoSellLore = List.copyOf(autoSellLore);
            autoSellMaximumLore = List.copyOf(autoSellMaximumLore);
        }
    }

    public record PurchaseMenu(
            int size,
            String title,
            int moneySlot,
            Material moneyIcon,
            String moneyName,
            List<String> moneyLore,
            int coinSlot,
            String coinName,
            List<String> coinLore
    ) {
        public PurchaseMenu {
            moneyLore = List.copyOf(moneyLore);
            coinLore = List.copyOf(coinLore);
        }
    }

    /** Rank-driven physical material and visual progression for the four multi-tool forms. */
    public record MultiTool(
            boolean enabled,
            double fishingRayDistance,
            boolean notifySwitch,
            Map<Integer, ToolTier> rankTiers
    ) {
        public MultiTool {
            rankTiers = Collections.unmodifiableMap(new LinkedHashMap<>(rankTiers));
            if (rankTiers.size() != 11) {
                throw new IllegalArgumentException("Multi-tool needs one tier for every rank from 0 to 10");
            }
            for (int rank = 0; rank <= 10; rank++) {
                if (!rankTiers.containsKey(rank) || rankTiers.get(rank) == null) {
                    throw new IllegalArgumentException("Missing multi-tool tier for rank " + rank);
                }
            }
        }

        /** Returns the physical material tier assigned to a persisted medieval rank. */
        public ToolTier tierForRank(int rank) {
            ToolTier tier = rankTiers.get(rank);
            if (tier == null) {
                throw new IllegalArgumentException("Unsupported multi-tool rank: " + rank);
            }
            return tier;
        }

        /** Builds the former static material progression for source compatibility. */
        public MultiTool(boolean enabled, double fishingRayDistance, boolean notifySwitch) {
            this(enabled, fishingRayDistance, notifySwitch, defaultRankTiers());
        }

        private static Map<Integer, ToolTier> defaultRankTiers() {
            return Map.ofEntries(
                    Map.entry(0, ToolTier.WOODEN),
                    Map.entry(1, ToolTier.WOODEN),
                    Map.entry(2, ToolTier.STONE),
                    Map.entry(3, ToolTier.STONE),
                    Map.entry(4, ToolTier.IRON),
                    Map.entry(5, ToolTier.IRON),
                    Map.entry(6, ToolTier.GOLDEN),
                    Map.entry(7, ToolTier.GOLDEN),
                    Map.entry(8, ToolTier.DIAMOND),
                    Map.entry(9, ToolTier.DIAMOND),
                    Map.entry(10, ToolTier.NETHERITE)
            );
        }
    }

    public record AbilitySettings(
            int speedDurationTicks,
            int speedAmplifier,
            int speedCooldownSeconds,
            int maximumTimberBlocks,
            BigDecimal efficiencyHardCap,
            int gemCoinBonus,
            BigDecimal goldenAppleRelativeChance,
            BigDecimal notchAppleRelativeChance,
            int ufoDisplayTicks
    ) {
    }

    /** Configurable experience curve with additional difficulty milestones. */
    public record Progression(
            int maximumToolLevel,
            long baseExperience,
            BigDecimal experienceMultiplier,
            List<ExperienceTier> difficultyTiers,
            int flushIntervalTicks
    ) implements ExperienceCurve {
        public Progression {
            if (maximumToolLevel < 1 || baseExperience < 1L || flushIntervalTicks < 1) {
                throw new IllegalArgumentException("Tool progression values must be positive");
            }
            if (experienceMultiplier == null
                    || experienceMultiplier.compareTo(BigDecimal.ONE) < 0) {
                throw new IllegalArgumentException(
                        "Tool experience multiplier must be greater than or equal to 1"
                );
            }
            difficultyTiers = List.copyOf(difficultyTiers);
            if (difficultyTiers.isEmpty()
                    || difficultyTiers.getFirst().minimumLevel() != 1
                    || difficultyTiers.getFirst().multiplier().compareTo(BigDecimal.ONE) != 0) {
                throw new IllegalArgumentException(
                        "Tool difficulty tiers must start at level 1 with multiplier 1"
                );
            }
            int previousLevel = 0;
            BigDecimal previousMultiplier = BigDecimal.ZERO;
            for (ExperienceTier tier : difficultyTiers) {
                if (tier.minimumLevel() <= previousLevel
                        || tier.minimumLevel() > maximumToolLevel) {
                    throw new IllegalArgumentException(
                            "Tool difficulty tier levels must be ordered and within the level cap"
                    );
                }
                if (tier.multiplier().compareTo(previousMultiplier) < 0) {
                    throw new IllegalArgumentException(
                            "Tool difficulty tier multipliers must not decrease"
                    );
                }
                previousLevel = tier.minimumLevel();
                previousMultiplier = tier.multiplier();
            }
        }

        /** Creates the former single-tier curve for API compatibility. */
        public Progression(
                int maximumToolLevel,
                long baseExperience,
                BigDecimal experienceMultiplier,
                int flushIntervalTicks
        ) {
            this(
                    maximumToolLevel,
                    baseExperience,
                    experienceMultiplier,
                    List.of(new ExperienceTier(1, BigDecimal.ONE)),
                    flushIntervalTicks
            );
        }

        @Override
        public int maximumLevel() {
            return maximumToolLevel;
        }

        /** Returns the configured difficulty multiplier active at a tool level. */
        @Override
        public BigDecimal difficultyMultiplier(int level) {
            if (level < 1) {
                throw new IllegalArgumentException("Tool level must be positive");
            }
            ExperienceTier active = difficultyTiers.getFirst();
            for (ExperienceTier tier : difficultyTiers) {
                if (tier.minimumLevel() > level) {
                    break;
                }
                active = tier;
            }
            return active.multiplier();
        }

        /** One additional difficulty multiplier starting at {@code minimumLevel}. */
        public record ExperienceTier(int minimumLevel, BigDecimal multiplier) {
            public ExperienceTier {
                if (minimumLevel < 1) {
                    throw new IllegalArgumentException("Tier minimum level must be positive");
                }
                if (multiplier == null || multiplier.signum() <= 0) {
                    throw new IllegalArgumentException("Tier multiplier must be positive");
                }
            }
        }
    }
}
