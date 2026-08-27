/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  lombok.Generated
 *  org.bukkit.configuration.file.FileConfiguration
 */
package xyz.arcadiadevs.valoriatycoon.utils.config;

import com.awaitquality.api.spigot.chat.ChatUtil;
import java.util.ArrayList;
import java.util.Map;
import lombok.Generated;
import org.bukkit.configuration.file.FileConfiguration;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;

public enum Config {
    ITEM_DESPAWN_TIME("item-despawn-time", "5m"),
    CAN_DROPS_BE_PLACED("can-items-be-placed", false),
    CAN_DROPS_BE_USED_IN_CRAFTING("can-items-be-used-in-crafting", false),
    CAN_DROPS_BE_USED_IN_SMELTING("can-items-be-used-in-smelting", false),
    CAN_DROPS_BE_USED_IN_ENCHANTING("can-items-be-used-in-enchanting", false),
    DISABLE_GENERATORS_WHEN_OFFLINE("disable-generators-when-offline", true),
    DISABLED_WORLDS("disabled-worlds", new ArrayList<E>()),
    INSTANT_PICKUP("instant-pickup", false),
    ON_JOIN_ENABLED("on-join.enabled", true),
    ON_JOIN_GENERATOR_TIER("on-join.generator-tier", 1),
    ON_JOIN_GENERATOR_AMOUNT("on-join.generator-amount", 3),
    GENERATOR_UPGRADE_SNEAK("guis.upgrade-gui.sneak-required", true),
    GENERATOR_UPGRADE_ACTION("guis.upgrade-gui.action", "RIGHT_CLICK_BLOCK"),
    SELL_WAND_ACTION_SNEAK("wands.sell-wand.sneak-required", true),
    SELL_WAND_ACTION("wands.sell-wand.action", "RIGHT_CLICK_BLOCK"),
    SELL_WAND_UNLIMITED_USES_PREFIX("wands.sell-wand.unlimited-uses-prefix", "\u221e"),
    SELL_COMMAND_ENABLED("sell-command.enabled", true),
    SELL_COMMAND_ALLIASES("sell-command.aliases", new ArrayList<E>()),
    LIMIT_PER_ISLAND_ENABLED("limits.per-island.enabled", false),
    LIMIT_PER_ISLAND_GENS_PER_LEVEL("limits.per-island.gens-per-level", new ArrayList<E>()),
    LIMIT_PER_PLAYER_ENABLED("limits.per-player.enabled", false),
    LIMIT_PER_PLAYER_USE_PERMISSIONS("limits.per-player.use-permissions", false),
    LIMIT_PER_PLAYER_USE_COMMANDS("limits.per-player.use-commands", true),
    LIMIT_PER_PLAYER_DEFAULT_LIMIT("limits.per-player.default-limit", 20),
    LIMIT_PER_PLAYER_UNLIMITED_PLACEHOLDER("limits.per-player.unlimited-placeholder", "illimité"),
    CHUNK_RADIUS_ENABLED("radius.enabled", true),
    CHUNK_RADIUS_USE_PERMISSIONS("radius.use-permissions", true),
    CHUNK_RADIUS_DEFAULT_RADIUS("radius.default-radius", 1),
    MULTIPLIER_USE_PERMISSIONS("multiplier.use-permissions", true),
    MULTIPLIER_DEFAULT_MULTIPLIER("multiplier.default-multiplier", 1),
    GUIS_GENERATORS_GUI_ENABLED("guis.generators-gui.enabled", true),
    GUIS_GENERATORS_GUI_TITLE("guis.generators-gui.title", "Générateurs"),
    GUIS_GENERATORS_GUI_ROWS("guis.generators-gui.rows", 6),
    GUIS_GENERATORS_GUI_BORDER_ENABLED("guis.generators-gui.border.enabled", true),
    GUIS_GENERATORS_GUI_BORDER_MATERIAL("guis.generators-gui.border.material", "WHITE_STAINED_GLASS_PANE"),
    GUIS_GENERATORS_GUI_BORDER_NAME("guis.generators-gui.border.name", " "),
    GUIS_GENERATORS_GUI_NEXT_PAGE_MATERIAL("guis.generators-gui.material.next-page", "ARROW"),
    GUIS_GENERATORS_GUI_PREVIOUS_PAGE_MATERIAL("guis.generators-gui.material.previous-page", "ARROW"),
    GUIS_GENERATORS_GUI_CLOSE_BUTTON_MATERIAL("guis.generators-gui.material.close-button", "BARRIER"),
    GUIS_UPGRADE_GUI_ENABLED("guis.upgrade-gui.enabled", true),
    GUIS_UPGRADE_GUI_UPGRADE_ONE_FIRST_LINE("guis.upgrade-gui.upgradeOne.first-line", "&e\u300b &nCliquez pour améliorer le générateur !&e \u300a"),
    GUIS_UPGRADE_GUI_UPGRADE_ONE_LORE("guis.upgrade-gui.upgradeOne.lore", new ArrayList<E>()),
    GUIS_UPGRADE_GUI_UPGRADE_ALL_FIRST_LINE("guis.upgrade-gui.upgradeAll.first-line", "&e\u300b &nCliquez pour améliorer tous les générateurs !&e \u300a"),
    GUIS_UPGRADE_GUI_UPGRADE_ALL_LORE("guis.upgrade-gui.upgradeAll.lore", new ArrayList<E>()),
    GUIS_UPGRADE_GUI_TITLE("guis.upgrade-gui.title", "Améliorer le générateur"),
    GUIS_UPGRADE_GUI_ROWS("guis.upgrade-gui.rows", 3),
    GUIS_SELL_GUI_ENABLED("guis.sell-gui.enabled", true),
    GUIS_SELL_GUI_TITLE("guis.sell-gui.title", "Vendre les items"),
    GUIS_SELL_GUI_ROWS("guis.sell-gui.rows", 3),
    HOLOGRAMS_ENABLED("holograms.enabled", false),
    HOLOGRAMS_VIEW_DISTANCE("holograms.view-distance", 2000),
    EVENTS_TIME_BETWEEN_EVENTS("events.time-between-events", "1h"),
    EVENTS_EVENT_DURATION("events.event-duration", "2m"),
    EVENTS_BROADCAST_ENABLED("events.broadcast.enabled", true),
    EVENTS_DROP_EVENT_ENABLED("events.drop-event.enabled", true),
    EVENTS_DROP_EVENT_NAME("events.drop-event.name", "Événement de drops"),
    EVENTS_DROP_EVENT_MULTIPLIER("events.drop-event.multiplier", 2),
    EVENTS_SELL_EVENT_ENABLED("events.sell-event.enabled", true),
    EVENTS_SELL_EVENT_NAME("events.sell-event.name", "Événement de vente"),
    EVENTS_SELL_EVENT_MULTIPLIER("events.sell-event.multiplier", 2),
    EVENTS_SPEED_EVENT_ENABLED("events.speed-event.enabled", true),
    EVENTS_SPEED_EVENT_NAME("events.speed-event.name", "Événement de vitesse"),
    EVENTS_SPEED_EVENT_MULTIPLIER("events.speed-event.multiplier", 2),
    PARTICLES_ENABLED("particles.enabled", true),
    PARTICLES_TYPE("particles.type", "FIREWORKS_SPARK"),
    PARTICLES_SOUND("particles.sound", "ENTITY_FIREWORK_ROCKET_BLAST"),
    DEFAULT_LORE("default-lore", new ArrayList<E>()),
    DEFAULT_ITEM_SPAWN_LORE("default-item-spawn-lore", new ArrayList<E>()),
    DEFAULT_HOLOGRAM_LINES("default-hologram-lines", new ArrayList<E>()),
    DEVELOPER_OPTIONS("developer-options.enabled", false),
    GENERATORS("generators", new ArrayList<E>());

    private final String path;
    private final Object defaultValue;

    private Config(String string2, Object object) {
        this.path = string2;
        this.defaultValue = object;
    }

    public Object get(boolean bl) {
        FileConfiguration fileConfiguration = ValoriaTycoon.getInstance().getConfig();
        if (!fileConfiguration.contains(this.path)) {
            fileConfiguration.set(this.path, this.defaultValue);
            ValoriaTycoon.getInstance().saveConfig();
        }
        Object object = fileConfiguration.get(this.path);
        if (bl && !(object instanceof String)) {
            throw new IllegalArgumentException("Impossible de formater une valeur qui n'est pas une chaîne !");
        }
        return bl ? ChatUtil.translate(object.toString()) : object;
    }

    public Object get() {
        return this.get(false);
    }

    public boolean getBoolean() {
        return (Boolean)this.get();
    }

    public int getInt() {
        return (Integer)this.get();
    }

    public double getDouble() {
        return (Double)this.get();
    }

    public String getString() {
        return (String)this.get();
    }

    public String getStringFormatted() {
        return (String)this.getFormatted();
    }

    public ArrayList<String> getStringList() {
        return (ArrayList)this.get();
    }

    public ArrayList<Map<?, ?>> getMapList() {
        return (ArrayList)this.get();
    }

    public ArrayList<Integer> getIntegerList() {
        return (ArrayList)this.get();
    }

    public ArrayList<Double> getDoubleList() {
        return (ArrayList)this.get();
    }

    public ArrayList<Boolean> getBooleanList() {
        return (ArrayList)this.get();
    }

    public Object getFormatted() {
        return this.get(true);
    }

    @Generated
    public String getPath() {
        return this.path;
    }

    @Generated
    public Object getDefaultValue() {
        return this.defaultValue;
    }
}

