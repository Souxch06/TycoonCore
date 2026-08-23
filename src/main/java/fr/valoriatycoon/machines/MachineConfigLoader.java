package fr.valoriatycoon.machines;

import fr.valoriatycoon.economy.MoneyCodec;
import fr.valoriatycoon.tools.ToolType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Strict parser for resource generators and their money-only upgrade curves. */
public final class MachineConfigLoader {
    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{1,32}");
    private MachineConfigLoader() {}

    public static MachineSettings load(FileConfiguration config) {
        int shopSize = integer(config, "shop.size", 27, 9, 54);
        int controlSize = integer(config, "control.size", 27, 9, 54);
        if (shopSize % 9 != 0 || controlSize % 9 != 0) {
            throw new IllegalArgumentException("Machine inventory sizes must be multiples of 9");
        }
        MachineSettings.Control control = new MachineSettings.Control(
                controlSize,
                text(config, "control.title"),
                slot(config, "control.status-slot", controlSize),
                slot(config, "control.output-slot", controlSize),
                slot(config, "control.collect-slot", controlSize),
                slot(config, "control.autosell-slot", controlSize),
                slot(config, "control.speed-upgrade-slot", controlSize),
                slot(config, "control.sell-upgrade-slot", controlSize)
        );
        Set<Integer> slots = Set.of(
                control.statusSlot(), control.outputSlot(), control.collectSlot(),
                control.autoSellSlot(), control.speedUpgradeSlot(), control.sellUpgradeSlot()
        );
        if (slots.size() != 6) throw new IllegalArgumentException("Control slots must be unique");

        MachineSettings.Upgrades upgrades = new MachineSettings.Upgrades(
                new MachineSettings.Speed(
                        integer(config, "upgrades.speed.max-level", 10, 1, 100),
                        decimal(config, "upgrades.speed.reduction-per-level"),
                        decimal(config, "upgrades.speed.minimum-interval-multiplier"),
                        money(config, "upgrades.speed.base-cost"),
                        money(config, "upgrades.speed.cost-per-level")
                ),
                new MachineSettings.SellPrice(
                        integer(config, "upgrades.sell-price.max-level", 10, 1, 100),
                        decimal(config, "upgrades.sell-price.bonus-per-level"),
                        money(config, "upgrades.sell-price.base-cost"),
                        money(config, "upgrades.sell-price.cost-per-level")
                )
        );
        if (upgrades.speed().reductionPerLevel().signum() < 0
                || upgrades.speed().minimumIntervalMultiplier().signum() <= 0
                || upgrades.speed().minimumIntervalMultiplier().compareTo(BigDecimal.ONE) > 0
                || upgrades.sellPrice().bonusPerLevel().signum() < 0) {
            throw new IllegalArgumentException("Invalid machine upgrade multipliers");
        }

        ConfigurationSection section = required(config, "machines");
        Map<String, MachineDefinition> definitions = new LinkedHashMap<>();
        Set<Integer> shopSlots = new HashSet<>();
        for (String id : section.getKeys(false)) {
            if (!ID.matcher(id).matches()) throw new IllegalArgumentException("Invalid machine id: " + id);
            ConfigurationSection machine = required(section, id);
            int shopSlot = slot(machine, "shop-slot", shopSize);
            if (!shopSlots.add(shopSlot)) throw new IllegalArgumentException("Duplicate shop slot: " + shopSlot);
            definitions.put(id, new MachineDefinition(
                    id,
                    blockMaterial(machine, "block"),
                    material(machine, "icon"),
                    shopSlot,
                    text(machine, "name"),
                    machine.getStringList("lore"),
                    money(machine, "price.money"),
                    enumValue(ToolType.class, text(machine, "price.coin-type"), id + ".coin-type"),
                    nonNegativeLong(machine, "price.coins"),
                    Duration.ofSeconds(integer(machine, "production.interval-seconds", 10, 1, 86_400)),
                    material(machine, "production.output-material"),
                    integer(machine, "production.output-amount", 1, 1, 64_000),
                    money(machine, "production.sell-price"),
                    positiveLong(machine, "production.storage-capacity")
            ));
        }
        if (definitions.isEmpty()) throw new IllegalArgumentException("At least one generator is required");
        return new MachineSettings(
                new MachineSettings.Shop(shopSize, text(config, "shop.title")),
                control,
                upgrades,
                integer(config, "limits.max-machines-per-island", 50, 1, 10_000),
                integer(config, "engine.maximum-cycles-per-tick", 100, 1, 10_000),
                definitions
        );
    }

    private static BigDecimal decimal(ConfigurationSection section, String path) {
        try { return new BigDecimal(section.getString(path)).stripTrailingZeros(); }
        catch (RuntimeException exception) { throw new IllegalArgumentException("Invalid decimal " + path, exception); }
    }

    private static long money(ConfigurationSection section, String path) {
        try {
            long value = MoneyCodec.toCents(new BigDecimal(section.getString(path)));
            if (value < 0) throw new IllegalArgumentException("Negative money");
            return value;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid money at " + path, exception);
        }
    }

    private static long positiveLong(ConfigurationSection s, String p) {
        long v = s.getLong(p, -1); if (v <= 0) throw new IllegalArgumentException(p); return v;
    }
    private static long nonNegativeLong(ConfigurationSection s, String p) {
        long v = s.getLong(p, -1); if (v < 0) throw new IllegalArgumentException(p); return v;
    }
    private static int integer(ConfigurationSection s, String p, int f, int min, int max) {
        int v = s.getInt(p, f); if (v < min || v > max) throw new IllegalArgumentException(p); return v;
    }
    private static int slot(ConfigurationSection s, String p, int size) { return integer(s, p, 0, 0, size - 1); }
    private static String text(ConfigurationSection s, String p) {
        String v = s.getString(p); if (v == null || v.isBlank()) throw new IllegalArgumentException(p); return v.trim();
    }
    private static Material material(ConfigurationSection s, String p) {
        Material m = Material.matchMaterial(text(s, p).toUpperCase(Locale.ROOT));
        if (m == null) throw new IllegalArgumentException(p); return m;
    }
    private static Material blockMaterial(ConfigurationSection s, String p) {
        Material m = material(s, p); if (!m.isBlock()) throw new IllegalArgumentException(p); return m;
    }
    private static ConfigurationSection required(ConfigurationSection s, String p) {
        ConfigurationSection v = s.getConfigurationSection(p); if (v == null) throw new IllegalArgumentException(p); return v;
    }
    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String path) {
        try { return Enum.valueOf(type, value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException(path, e); }
    }
}
