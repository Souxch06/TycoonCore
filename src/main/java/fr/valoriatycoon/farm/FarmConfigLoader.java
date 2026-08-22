package fr.valoriatycoon.farm;

import fr.valoriatycoon.economy.MoneyCodec;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict parser for farms.yml; invalid commercial-server configuration fails startup early. */
public final class FarmConfigLoader {
    private static final Pattern FARM_ID = Pattern.compile("[a-z0-9_-]{1,32}");

    private FarmConfigLoader() {
    }

    public static FarmSettings load(FileConfiguration config) {
        int menuSize = integer(config, "menu.size", 27, 9, 54);
        if (menuSize % 9 != 0) {
            throw new IllegalArgumentException("menu.size must be a multiple of 9");
        }
        FarmSettings.AutoSell autoSell = parseAutoSell(config);
        String autoSellMenuPath = config.isConfigurationSection("autosell-menu")
                ? "autosell-menu"
                : "menu.autosell";
        int autoSellMenuSize = integer(config, autoSellMenuPath + ".size", 27, 9, 54);
        if (autoSellMenuSize % 9 != 0) {
            throw new IllegalArgumentException(autoSellMenuPath + ".size must be a multiple of 9");
        }
        FarmSettings.AutoSellMenu autoSellMenu = new FarmSettings.AutoSellMenu(
                autoSellMenuSize,
                text(config, autoSellMenuPath + ".title", "<dark_gray>Gestion de la vente automatique</dark_gray>"),
                slot(config, autoSellMenuPath + ".slot", autoSellMenuSize),
                material(config, autoSellMenuPath + ".enabled-icon"),
                material(config, autoSellMenuPath + ".disabled-icon"),
                material(config, autoSellMenuPath + ".locked-icon"),
                text(config, autoSellMenuPath + ".enabled-name"),
                text(config, autoSellMenuPath + ".disabled-name"),
                text(config, autoSellMenuPath + ".locked-name"),
                config.getStringList(autoSellMenuPath + ".lore")
        );
        FarmSettings.Menu menu = new FarmSettings.Menu(menuSize, text(config, "menu.title"));
        int zoneMenuSize = integer(config, "zone-menu.size", 27, 9, 54);
        if (zoneMenuSize % 9 != 0) {
            throw new IllegalArgumentException("zone-menu.size must be a multiple of 9");
        }
        FarmSettings.ZoneMenu zoneMenu = new FarmSettings.ZoneMenu(
                zoneMenuSize,
                text(config, "zone-menu.title"),
                material(config, "zone-menu.locked-icon"),
                text(config, "zone-menu.locked-name"),
                config.getStringList("zone-menu.locked-lore")
        );
        FarmSettings.Teleport teleport = new FarmSettings.Teleport(
                integer(config, "teleport.cooldown-seconds", 3, 0, 3600)
        );
        FarmSettings.RankBarrier rankBarrier = new FarmSettings.RankBarrier(
                decimal(config, "rank-barrier.horizontal-knockback", 0.1, 5.0),
                decimal(config, "rank-barrier.vertical-knockback", 0.0, 2.0)
        );
        FarmSettings.Regeneration regeneration = new FarmSettings.Regeneration(
                integer(config, "regeneration.check-interval-ticks", 10, 1, 1200),
                integer(config, "regeneration.maximum-blocks-per-run", 250, 1, 10_000),
                integer(config, "regeneration.unloaded-retry-seconds", 15, 1, 3600)
        );

        ConfigurationSection farmsSection = requiredSection(config, "farms");
        Map<String, FarmDefinition> farms = new LinkedHashMap<>();
        Set<String> worldNames = new LinkedHashSet<>();
        Set<Integer> menuSlots = new LinkedHashSet<>();
        for (String rawId : farmsSection.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!FARM_ID.matcher(id).matches() || !id.equals(rawId)) {
                throw new IllegalArgumentException("Invalid farm id (lowercase required): " + rawId);
            }
            ConfigurationSection section = requiredSection(farmsSection, rawId);
            FarmDefinition definition = parseFarm(id, section, menuSize, zoneMenuSize);
            if (!worldNames.add(definition.worldName())) {
                throw new IllegalArgumentException("Duplicate farm world: " + definition.worldName());
            }
            if (definition.enabled() && !menuSlots.add(definition.menuSlot())) {
                throw new IllegalArgumentException("Duplicate enabled farm menu slot: " + definition.menuSlot());
            }
            farms.put(id, definition);
        }
        if (farms.values().stream().noneMatch(FarmDefinition::enabled)) {
            throw new IllegalArgumentException("At least one farm must be enabled");
        }
        return new FarmSettings(
                menu,
                zoneMenu,
                autoSellMenu,
                teleport,
                rankBarrier,
                regeneration,
                autoSell,
                farms
        );
    }

    private static FarmSettings.AutoSell parseAutoSell(FileConfiguration config) {
        int maximumLevel = integer(config, "autosell.max-level", 5, 5, 5);
        ConfigurationSection levelsSection = requiredSection(config, "autosell.levels");
        List<FarmSettings.AutoSellLevel> levels = new ArrayList<>(maximumLevel);
        long previousCost = -1L;
        BigDecimal previousMultiplier = BigDecimal.ZERO;
        for (int level = 1; level <= maximumLevel; level++) {
            String levelPath = Integer.toString(level);
            ConfigurationSection section = requiredSection(levelsSection, levelPath);
            long cost = positiveMoney(section, "cost", "autosell.levels." + level + ".cost");
            BigDecimal multiplier = positiveDecimal(
                    section,
                    "multiplier",
                    "autosell.levels." + level + ".multiplier"
            );
            if (cost < previousCost) {
                throw new IllegalArgumentException("Auto-sell level costs must not decrease");
            }
            if (multiplier.compareTo(previousMultiplier) < 0) {
                throw new IllegalArgumentException("Auto-sell sale multipliers must not decrease");
            }
            levels.add(new FarmSettings.AutoSellLevel(level, cost, multiplier));
            previousCost = cost;
            previousMultiplier = multiplier;
        }
        if (levelsSection.getKeys(false).size() != maximumLevel) {
            throw new IllegalArgumentException("autosell.levels must contain exactly levels 1 to " + maximumLevel);
        }
        return new FarmSettings.AutoSell(
                integer(config, "autosell.flush-interval-ticks", 20, 1, 1200),
                levels
        );
    }

    private static FarmDefinition parseFarm(
            String id,
            ConfigurationSection section,
            int menuSize,
            int zoneMenuSize
    ) {
        String path = "farms." + id + '.';
        FarmType type = enumValue(FarmType.class, text(section, "type"), path + "type");
        Set<Material> breakable = materialSet(
                section.getStringList("breakable-blocks"),
                path + "breakable-blocks",
                type != FarmType.FISHING
        );
        Duration defaultDelay = seconds(section, "regeneration.default-delay-seconds", 1, 86_400);
        Map<Material, Duration> delays = durationMap(section.getConfigurationSection("regeneration.delays"), path);
        Map<Material, Long> prices = priceMap(section.getConfigurationSection("sell-prices"), path);
        double borderSize = decimal(section, "border-size", 128.0, 59_999_968.0);
        FarmGenerationSettings generation = switch (type) {
            case MINE -> parseMine(section, path, zoneMenuSize);
            case FIELDS -> parseFields(section, path, zoneMenuSize);
            case FISHING -> parseFishing(section, path);
            case FOREST -> parseForest(section, path, zoneMenuSize);
        };
        double halfBorder = borderSize / 2.0;
        for (FarmZoneDefinition zone : generation.zones()) {
            for (Material generated : generatedBreakableBlocks(zone)) {
                if (!breakable.contains(generated)) {
                    throw new IllegalArgumentException(
                            path + "breakable-blocks is missing " + generated + " from zone " + zone.id()
                    );
                }
            }
            if (zone.minimumX() < -halfBorder
                    || zone.maximumX() >= halfBorder
                    || zone.minimumZ() < -halfBorder
                    || zone.maximumZ() >= halfBorder) {
                throw new IllegalArgumentException(path + "zone " + zone.id() + " exceeds world border");
            }
        }
        return new FarmDefinition(
                id,
                section.getBoolean("enabled", true),
                type,
                text(section, "world"),
                section.getLong("seed", 1L),
                borderSize,
                integer(section, "spawn-protection-radius", 6, 0, 128),
                (float) section.getDouble("spawn-yaw", 0.0),
                material(section, "menu.icon"),
                slot(section, "menu.slot", menuSize),
                text(section, "menu.name"),
                section.getStringList("menu.lore"),
                breakable,
                defaultDelay,
                delays,
                prices,
                generation
        );
    }

    private static Set<Material> generatedBreakableBlocks(FarmZoneDefinition zone) {
        Set<Material> materials = new LinkedHashSet<>();
        switch (zone.resource()) {
            case FarmZoneDefinition.MineResource mine -> {
                materials.add(mine.filler());
                mine.ores().forEach(rule -> materials.add(rule.material()));
            }
            case FarmZoneDefinition.FieldsResource fields -> materials.add(fields.crop());
            case FarmZoneDefinition.ForestResource forest -> {
                materials.add(forest.log());
                materials.add(forest.leaves());
            }
        }
        return materials;
    }

    private static FarmGenerationSettings.Mine parseMine(
            ConfigurationSection section,
            String path,
            int zoneMenuSize
    ) {
        ConfigurationSection generation = requiredSection(section, "generation");
        int bottomY = integer(generation, "bottom-y", -48, -64, 240);
        int quarryFloorY = integer(
                generation,
                "quarry-floor-y",
                12,
                bottomY + 16,
                260
        );
        int floorVariation = integer(generation, "floor-variation", 18, 4, 48);
        int bridgeY = integer(
                generation,
                "bridge-y",
                48,
                quarryFloorY + floorVariation + 4,
                280
        );
        int ceilingVariation = integer(generation, "ceiling-variation", 24, 4, 48);
        int ceilingY = integer(
                generation,
                "ceiling-y",
                160,
                bridgeY + ceilingVariation + 24,
                312 - ceilingVariation
        );
        int wallThickness = integer(generation, "wall-thickness", 48, 8, 128);
        int bridgeWidth = integer(generation, "bridge-width", 15, 5, 31);
        if (bridgeWidth % 2 == 0) {
            throw new IllegalArgumentException(path + "generation.bridge-width must be odd");
        }
        List<FarmZoneDefinition> zones = new ArrayList<>();
        for (ZoneBase base : parseZoneBases(section, path, zoneMenuSize)) {
            ConfigurationSection resource = requiredSection(base.section(), "generation");
            Material filler = blockMaterial(resource, "filler");
            List<FarmGenerationSettings.OreRule> ores = parseOres(
                    requiredSection(resource, "ores"),
                    bottomY,
                    bridgeY,
                    path + "zones." + base.index() + ".generation.ores"
            );
            zones.add(base.create(new FarmZoneDefinition.MineResource(filler, ores)));
        }
        return new FarmGenerationSettings.Mine(
                bottomY,
                quarryFloorY,
                floorVariation,
                bridgeY,
                ceilingY,
                ceilingVariation,
                wallThickness,
                bridgeWidth,
                zones
        );
    }

    private static FarmGenerationSettings.Fields parseFields(
            ConfigurationSection section,
            String path,
            int zoneMenuSize
    ) {
        ConfigurationSection generation = requiredSection(section, "generation");
        int bottomY = integer(generation, "bottom-y", 0, -64, 300);
        int soilY = integer(generation, "soil-y", 63, bottomY + 2, 318);
        List<FarmZoneDefinition> zones = new ArrayList<>();
        for (ZoneBase base : parseZoneBases(section, path, zoneMenuSize)) {
            ConfigurationSection resource = requiredSection(base.section(), "generation");
            Material crop = blockMaterial(resource, "crop");
            zones.add(base.create(new FarmZoneDefinition.FieldsResource(crop)));
        }
        return new FarmGenerationSettings.Fields(
                bottomY,
                soilY,
                integer(generation, "path-spacing", 256, 16, 512),
                blockMaterial(generation, "path-material"),
                zones
        );
    }

    private static FarmGenerationSettings.Fishing parseFishing(ConfigurationSection section, String path) {
        ConfigurationSection generation = requiredSection(section, "generation");
        int bottomY = integer(generation, "bottom-y", 0, -64, 300);
        int seabedY = integer(generation, "seabed-y", 48, bottomY + 1, 317);
        int waterY = integer(generation, "water-y", 62, seabedY + 2, 317);
        return new FarmGenerationSettings.Fishing(
                bottomY,
                seabedY,
                waterY,
                integer(generation, "platform-radius", 7, 2, 64),
                blockMaterial(generation, "seabed-material"),
                blockMaterial(generation, "platform-material")
        );
    }

    private static FarmGenerationSettings.Forest parseForest(
            ConfigurationSection section,
            String path,
            int zoneMenuSize
    ) {
        ConfigurationSection generation = requiredSection(section, "generation");
        int bottomY = integer(generation, "bottom-y", 0, -64, 300);
        int groundY = integer(generation, "ground-y", 63, bottomY + 2, 310);
        List<FarmZoneDefinition> zones = new ArrayList<>();
        for (ZoneBase base : parseZoneBases(section, path, zoneMenuSize)) {
            ConfigurationSection resource = requiredSection(base.section(), "generation");
            zones.add(base.create(new FarmZoneDefinition.ForestResource(
                    blockMaterial(resource, "log-material"),
                    blockMaterial(resource, "leaves-material")
            )));
        }
        int terrainVariation = integer(generation, "terrain-variation", 12, 2, 32);
        int islandDepth = integer(generation, "island-depth", 28, 8, 64);
        int treeSpacing = integer(generation, "tree-spacing", 30, 16, 64);
        int minimumTrunkHeight = integer(
                generation,
                "minimum-trunk-height",
                10,
                4,
                40
        );
        int maximumTrunkHeight = integer(
                generation,
                "maximum-trunk-height",
                28,
                minimumTrunkHeight,
                64
        );
        int pathSpacing = integer(
                generation,
                "path-spacing",
                256,
                treeSpacing * 2,
                1024
        );
        int bridgeWidth = integer(generation, "bridge-width", 13, 5, 31);
        if (bridgeWidth % 2 == 0) {
            throw new IllegalArgumentException(path + "generation.bridge-width must be odd");
        }
        if (groundY - terrainVariation - islandDepth - 4 < bottomY) {
            throw new IllegalArgumentException(path + "forest island extends below bottom-y");
        }
        if (groundY + terrainVariation + maximumTrunkHeight + 8 >= 320) {
            throw new IllegalArgumentException(path + "forest canopy exceeds world height");
        }
        return new FarmGenerationSettings.Forest(
                bottomY,
                groundY,
                terrainVariation,
                islandDepth,
                treeSpacing,
                minimumTrunkHeight,
                maximumTrunkHeight,
                pathSpacing,
                integer(generation, "platform-radius", 24, 4, 64),
                blockMaterial(generation, "platform-material"),
                bridgeWidth,
                zones
        );
    }

    private static List<FarmGenerationSettings.OreRule> parseOres(
            ConfigurationSection oreSection,
            int bottomY,
            int topY,
            String path
    ) {
        List<FarmGenerationSettings.OreRule> ores = new ArrayList<>();
        double totalChance = 0.0;
        for (String name : oreSection.getKeys(false)) {
            Material ore = requireMaterial(name, path + '.' + name);
            if (!ore.isBlock()) {
                throw new IllegalArgumentException("Ore must be a block: " + ore);
            }
            ConfigurationSection rule = requiredSection(oreSection, name);
            double chance = decimal(rule, "chance", 0.0, 1.0);
            int minimumY = integer(rule, "min-y", bottomY + 1, bottomY + 1, topY);
            int maximumY = integer(rule, "max-y", topY, minimumY, topY);
            totalChance += chance;
            ores.add(new FarmGenerationSettings.OreRule(ore, chance, minimumY, maximumY));
        }
        if (ores.isEmpty() || totalChance > 0.80) {
            throw new IllegalArgumentException(path + " must contain ores totaling at most 0.80");
        }
        return ores;
    }

    private static List<ZoneBase> parseZoneBases(
            ConfigurationSection farm,
            String path,
            int zoneMenuSize
    ) {
        ConfigurationSection configured = requiredSection(farm, "zones");
        if (configured.getKeys(false).size() != 4) {
            throw new IllegalArgumentException(path + "zones must contain exactly zones 1 to 4");
        }
        List<ZoneBase> zones = new ArrayList<>(4);
        Set<Integer> menuSlots = new LinkedHashSet<>();
        Set<String> ids = new LinkedHashSet<>();
        int previousRank = -1;
        for (int index = 1; index <= 4; index++) {
            ConfigurationSection section = requiredSection(configured, Integer.toString(index));
            String id = text(section, "id").toLowerCase(Locale.ROOT);
            if (!FARM_ID.matcher(id).matches() || !ids.add(id)) {
                throw new IllegalArgumentException(path + "zones." + index + " has an invalid/duplicate id");
            }
            int requiredRank = integer(section, "required-rank", 0, 0, 10);
            if (index == 1 && requiredRank != 0 || requiredRank < previousRank) {
                throw new IllegalArgumentException(path + "zone ranks must start at 0 and not decrease");
            }
            int menuSlot = slot(section, "menu.slot", zoneMenuSize);
            if (!menuSlots.add(menuSlot)) {
                throw new IllegalArgumentException(path + "zones contains a duplicate menu slot");
            }
            zones.add(new ZoneBase(
                    index,
                    id,
                    requiredRank,
                    integer(section, "center-x", 0, -29_000_000, 29_000_000),
                    integer(section, "center-z", 0, -29_000_000, 29_000_000),
                    integer(section, "size", 192, 16, 8192),
                    enumValue(FarmZoneDefinition.Shape.class, text(section, "shape"), path + "shape"),
                    menuSlot,
                    material(section, "menu.icon"),
                    text(section, "menu.name"),
                    section.getStringList("menu.lore"),
                    section
            ));
            previousRank = requiredRank;
        }
        for (int left = 0; left < zones.size(); left++) {
            for (int right = left + 1; right < zones.size(); right++) {
                if (overlaps(zones.get(left), zones.get(right))) {
                    throw new IllegalArgumentException(path + "zones overlap: "
                            + zones.get(left).id() + " / " + zones.get(right).id());
                }
            }
        }
        return zones;
    }

    private static boolean overlaps(ZoneBase left, ZoneBase right) {
        int leftMinX = left.centerX() - left.size() / 2;
        int leftMaxX = leftMinX + left.size() - 1;
        int leftMinZ = left.centerZ() - left.size() / 2;
        int leftMaxZ = leftMinZ + left.size() - 1;
        int rightMinX = right.centerX() - right.size() / 2;
        int rightMaxX = rightMinX + right.size() - 1;
        int rightMinZ = right.centerZ() - right.size() / 2;
        int rightMaxZ = rightMinZ + right.size() - 1;
        return leftMinX <= rightMaxX && leftMaxX >= rightMinX
                && leftMinZ <= rightMaxZ && leftMaxZ >= rightMinZ;
    }

    private static Map<Material, Duration> durationMap(ConfigurationSection section, String path) {
        Map<Material, Duration> result = new LinkedHashMap<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            Material material = requireMaterial(key, path + "regeneration.delays." + key);
            long value = section.getLong(key, -1L);
            if (value < 1 || value > 86_400) {
                throw new IllegalArgumentException("Invalid regeneration delay for " + material);
            }
            result.put(material, Duration.ofSeconds(value));
        }
        return result;
    }

    private static Map<Material, Long> priceMap(ConfigurationSection section, String path) {
        Map<Material, Long> result = new LinkedHashMap<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            Material material = requireMaterial(key, path + "sell-prices." + key);
            String raw = section.getString(key);
            try {
                long cents = MoneyCodec.toCents(new BigDecimal(raw));
                if (cents <= 0) {
                    throw new IllegalArgumentException("Sell price must be positive for " + material);
                }
                result.put(material, cents);
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid sell price for " + material + " at " + path, exception);
            }
        }
        return result;
    }

    private static long positiveMoney(ConfigurationSection section, String key, String path) {
        String raw = section.getString(key);
        try {
            long cents = MoneyCodec.toCents(new BigDecimal(raw));
            if (cents <= 0) {
                throw new IllegalArgumentException(path + " must be positive");
            }
            return cents;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(path + " must be an exact positive monetary amount", exception);
        }
    }

    private static BigDecimal positiveDecimal(ConfigurationSection section, String key, String path) {
        String raw = section.getString(key);
        try {
            BigDecimal value = new BigDecimal(raw);
            if (value.signum() <= 0 || value.scale() > 4) {
                throw new IllegalArgumentException(path + " must be positive with at most four decimals");
            }
            return value.stripTrailingZeros();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(path + " must be a positive decimal", exception);
        }
    }

    private static Set<Material> materialSet(List<String> names, String path, boolean requireNonEmpty) {
        Set<Material> result = new LinkedHashSet<>();
        for (String name : names) {
            result.add(requireMaterial(name, path));
        }
        if (requireNonEmpty && result.isEmpty()) {
            throw new IllegalArgumentException(path + " cannot be empty");
        }
        return result;
    }

    private static Duration seconds(ConfigurationSection section, String path, long minimum, long maximum) {
        long value = section.getLong(path, -1L);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(path + " must be between " + minimum + " and " + maximum);
        }
        return Duration.ofSeconds(value);
    }

    private static int slot(ConfigurationSection section, String path, int inventorySize) {
        return integer(section, path, 0, 0, inventorySize - 1);
    }

    private static int integer(ConfigurationSection section, String path, int fallback, int minimum, int maximum) {
        int value = section.getInt(path, fallback);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(path + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private static double decimal(ConfigurationSection section, String path, double minimum, double maximum) {
        double value = section.getDouble(path, Double.NaN);
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(path + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private static String text(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(path + " cannot be blank");
        }
        return value.trim();
    }

    private static String text(ConfigurationSection section, String path, String fallback) {
        String value = section.getString(path, fallback);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(path + " cannot be blank");
        }
        return value.trim();
    }

    private static Material material(ConfigurationSection section, String path) {
        return requireMaterial(text(section, path), path);
    }

    private static Material blockMaterial(ConfigurationSection section, String path) {
        Material material = material(section, path);
        if (!material.isBlock()) {
            throw new IllegalArgumentException(path + " must be a block material");
        }
        return material;
    }

    private static Material requireMaterial(String input, String path) {
        Material material = Material.matchMaterial(input);
        if (material == null) {
            throw new IllegalArgumentException("Unknown material at " + path + ": " + input);
        }
        return material;
    }

    private static ConfigurationSection requiredSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing configuration section: " + path);
        }
        return section;
    }

    private record ZoneBase(
            int index,
            String id,
            int requiredRank,
            int centerX,
            int centerZ,
            int size,
            FarmZoneDefinition.Shape shape,
            int menuSlot,
            Material menuIcon,
            String menuName,
            List<String> menuLore,
            ConfigurationSection section
    ) {
        private FarmZoneDefinition create(FarmZoneDefinition.Resource resource) {
            return new FarmZoneDefinition(
                    index,
                    id,
                    requiredRank,
                    centerX,
                    centerZ,
                    size,
                    shape,
                    menuSlot,
                    menuIcon,
                    menuName,
                    menuLore,
                    resource
            );
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String path) {
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown value at " + path + ": " + value, exception);
        }
    }
}
