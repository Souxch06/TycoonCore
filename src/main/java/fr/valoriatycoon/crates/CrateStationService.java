package fr.valoriatycoon.crates;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.gui.PetPanel;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import fr.valoriatycoon.spawn.SpawnWorldService;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Spawns and animates the nine modeled market crates without loading chunks. */
public final class CrateStationService implements Listener {
    private static final double TWO_PI = Math.PI * 2.0;

    private final JavaPlugin plugin;
    private final CrateStationSettings settings;
    private final CrateRewardService rewards;
    private final PetPanel pets;
    private final SpawnWorldService spawnWorld;
    private final ItemVisualService visuals;
    private final MessageService messages;
    private final Logger logger;
    private final NamespacedKey markerKey;
    private final NamespacedKey typeKey;
    private final Map<CrateStationType, StationEntities> entities = new EnumMap<>(
            CrateStationType.class
    );
    private BukkitTask refreshTask;
    private BukkitTask effectTask;
    private long animationTicks;

    public CrateStationService(
            JavaPlugin plugin,
            CrateStationSettings settings,
            CrateRewardService rewards,
            PetPanel pets,
            SpawnWorldService spawnWorld,
            ItemVisualService visuals,
            MessageService messages,
            Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.rewards = Objects.requireNonNull(rewards, "rewards");
        this.pets = Objects.requireNonNull(pets, "pets");
        this.spawnWorld = Objects.requireNonNull(spawnWorld, "spawnWorld");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.markerKey = new NamespacedKey(plugin, "crate_station");
        this.typeKey = new NamespacedKey(plugin, "crate_station_type");
    }

    public void start() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Physical crate stations must start on the primary thread");
        }
        if (!settings.enabled() || refreshTask != null) {
            return;
        }
        World world = Bukkit.getWorld(settings.worldName());
        if (world == null) {
            logger.warning("Physical crate world is unavailable: " + settings.worldName());
            return;
        }
        removeStale(world);
        ensureStations();
        refreshTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::ensureStations,
                settings.refreshIntervalTicks(),
                settings.refreshIntervalTicks()
        );
        if (settings.effectsEnabled()) {
            effectTask = Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    this::animateStations,
                    settings.effectIntervalTicks(),
                    settings.effectIntervalTicks()
            );
        }
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        if (effectTask != null) {
            effectTask.cancel();
            effectTask = null;
        }
        entities.values().forEach(StationEntities::remove);
        entities.clear();
        animationTicks = 0L;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        CrateStationType type = stationType(event.getRightClicked());
        if (type == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission("tycoon.crates")) {
            messages.send(player, "errors.no-permission");
            return;
        }
        if (type.pets()) {
            pets.open(player);
            return;
        }
        rewards.open(player, type.crateType());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (stationType(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }

    private void ensureStations() {
        World world = Bukkit.getWorld(settings.worldName());
        if (world == null) {
            return;
        }
        for (CrateStationType type : CrateStationType.values()) {
            Location location = location(type, world);
            if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                continue;
            }
            StationEntities existing = entities.get(type);
            if (existing == null || !existing.valid()) {
                if (existing != null) {
                    existing.remove();
                }
                entities.put(type, spawn(type, location));
            }
        }
    }

    private StationEntities spawn(CrateStationType type, Location location) {
        CrateStationSettings.Station station = settings.station(type);
        ItemDisplay model = location.getWorld().spawn(location, ItemDisplay.class, display -> {
            mark(display, type);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setGravity(false);
            display.setSilent(true);
            display.setBillboard(Display.Billboard.FIXED);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setTeleportDuration(settings.effectsEnabled() ? settings.effectIntervalTicks() : 0);
            ItemStack item = new ItemStack(Material.CHEST);
            ItemMeta meta = item.getItemMeta();
            visuals.apply(meta, station.itemModel());
            item.setItemMeta(meta);
            display.setItemStack(item);
            display.setViewRange(1.0F);
        });
        Location interactionLocation = location.clone().add(0.0, -0.75, 0.0);
        Interaction interaction = location.getWorld().spawn(
                interactionLocation,
                Interaction.class,
                hitbox -> {
                    mark(hitbox, type);
                    hitbox.setPersistent(false);
                    hitbox.setInvulnerable(true);
                    hitbox.setGravity(false);
                    hitbox.setInteractionWidth(settings.interactionWidth());
                    hitbox.setInteractionHeight(settings.interactionHeight());
                    hitbox.setResponsive(true);
                }
        );
        Location labelLocation = location.clone().add(0.0, settings.labelOffsetY(), 0.0);
        TextDisplay label = location.getWorld().spawn(labelLocation, TextDisplay.class, display -> {
            mark(display, type);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setGravity(false);
            display.setSilent(true);
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setDefaultBackground(true);
            display.setLineWidth(180);
            display.setViewRange(1.0F);
            display.text(label(type));
        });
        return new StationEntities(model, interaction, label, location.clone());
    }

    private void animateStations() {
        animationTicks += settings.effectIntervalTicks();
        for (Map.Entry<CrateStationType, StationEntities> entry : entities.entrySet()) {
            CrateStationType type = entry.getKey();
            StationEntities stationEntities = entry.getValue();
            if (!stationEntities.valid()) {
                continue;
            }
            CrateStationSettings.Station station = settings.station(type);
            CrateStationSettings.Effect effect = station.effect();
            double phase = animationTicks * effect.orbitSpeed() + type.ordinal() * 0.73;
            Location animated = stationEntities.baseLocation().clone();
            animated.add(0.0, Math.sin(phase * 0.70) * effect.bobHeight(), 0.0);
            animated.setYaw((float) (station.yaw() + Math.sin(phase * 0.42) * effect.yawSwayDegrees()));
            stationEntities.model().teleport(animated);

            if (hasNearbyPlayer(animated)) {
                spawnAmbientEffects(type, stationEntities.baseLocation(), effect, phase);
            }
        }
    }

    private void spawnAmbientEffects(
            CrateStationType type,
            Location base,
            CrateStationSettings.Effect effect,
            double phase
    ) {
        World world = base.getWorld();
        for (int index = 0; index < effect.particleCount(); index++) {
            double angle = phase + TWO_PI * index / effect.particleCount();
            double radius = effect.orbitRadius() * radiusScale(type, index);
            double y = base.getY() + 0.62 + verticalOffset(type, angle, index);
            Location particleLocation = new Location(
                    world,
                    base.getX() + Math.cos(angle) * radius,
                    y,
                    base.getZ() + Math.sin(angle) * radius
            );
            int rgb = index % 2 == 0 ? effect.primaryRgb() : effect.secondaryRgb();
            Particle.DustOptions dust = new Particle.DustOptions(
                    Color.fromRGB(rgb),
                    effect.particleSize()
            );
            world.spawnParticle(
                    Particle.DUST,
                    particleLocation,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    dust
            );
        }

        int sparklePeriod = type == CrateStationType.VALORIA ? 20 : 40;
        if ((type == CrateStationType.VALORIA || type == CrateStationType.LEGENDARY)
                && animationTicks % sparklePeriod < settings.effectIntervalTicks()) {
            Location sparkle = base.clone().add(0.0, type == CrateStationType.VALORIA ? 1.55 : 1.35, 0.0);
            world.spawnParticle(Particle.END_ROD, sparkle, 1, 0.16, 0.12, 0.16, 0.01);
        }
    }

    private double radiusScale(CrateStationType type, int index) {
        return switch (type) {
            case VALORIA -> index % 2 == 0 ? 1.0 : 0.60;
            case LEGENDARY -> index % 2 == 0 ? 1.0 : 0.76;
            case PETS -> index % 2 == 0 ? 0.86 : 1.0;
            default -> 1.0;
        };
    }

    private double verticalOffset(CrateStationType type, double angle, int index) {
        return switch (type) {
            case VOTE -> -0.18 + ((animationTicks * 0.012 + index * 0.25) % 1.0) * 0.72;
            case QUEST -> Math.sin(angle * 2.0) * 0.27;
            case FARM -> -0.18 + Math.abs(Math.sin(angle)) * 0.32;
            case COMMON -> Math.sin(angle) * 0.12;
            case RARE -> Math.sin(angle * 2.0) * 0.30;
            case EPIC -> -0.12 + Math.abs(Math.sin(angle * 1.5)) * 0.42;
            case LEGENDARY -> index % 2 == 0 ? 0.36 : -0.04;
            case VALORIA -> (index % 2 == 0 ? 0.34 : -0.10) + Math.sin(angle * 2.0) * 0.16;
            case PETS -> Math.sin(angle * 2.0) * 0.22;
        };
    }

    private boolean hasNearbyPlayer(Location location) {
        double maximumDistanceSquared = settings.effectViewDistance() * settings.effectViewDistance();
        for (Player player : location.getWorld().getPlayers()) {
            if (player.isOnline() && player.getLocation().distanceSquared(location) <= maximumDistanceSquared) {
                return true;
            }
        }
        return false;
    }

    private net.kyori.adventure.text.Component label(CrateStationType type) {
        String color = switch (type) {
            case VOTE -> "#45E7EF";
            case QUEST -> "aqua";
            case FARM -> "#F2C94C";
            case COMMON -> "green";
            case RARE -> "blue";
            case EPIC -> "#FF7818";
            case LEGENDARY -> "#FFD32D";
            case VALORIA -> "#FF2E4B";
            case PETS -> "#F669B3";
        };
        return messages.render("<" + color + "><bold>" + type.displayName() + "</bold></" + color + ">");
    }

    private void mark(Entity entity, CrateStationType type) {
        entity.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        entity.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.name());
    }

    private CrateStationType stationType(Entity entity) {
        if (entity == null
                || !entity.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)) {
            return null;
        }
        String stored = entity.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (stored == null) {
            return null;
        }
        try {
            return CrateStationType.valueOf(stored);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Location location(CrateStationType type, World world) {
        Location anchor = spawnWorld.spawn();
        if (!anchor.getWorld().getUID().equals(world.getUID())) {
            anchor = world.getSpawnLocation();
        }
        CrateStationSettings.Station station = settings.station(type);
        Location result = anchor.add(station.offsetX(), station.offsetY(), station.offsetZ());
        result.setYaw(station.yaw());
        result.setPitch(0.0F);
        return result;
    }

    private void removeStale(World world) {
        for (CrateStationType type : CrateStationType.values()) {
            Location location = location(type, world);
            if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                continue;
            }
            world.getNearbyEntities(location, 3.0, 4.0, 3.0).stream()
                    .filter(entity -> entity.getPersistentDataContainer().has(
                            markerKey,
                            PersistentDataType.BYTE
                    ))
                    .forEach(Entity::remove);
        }
    }

    private record StationEntities(
            ItemDisplay model,
            Interaction hitbox,
            TextDisplay label,
            Location baseLocation
    ) {
        private boolean valid() {
            return model.isValid() && hitbox.isValid() && label.isValid();
        }

        private void remove() {
            List<Entity> all = new ArrayList<>(List.of(model, hitbox, label));
            all.forEach(Entity::remove);
        }
    }
}
