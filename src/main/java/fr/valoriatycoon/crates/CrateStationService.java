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
import org.bukkit.Sound;
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
    private final Map<CrateStationType, OpeningAnimation> openingAnimations = new EnumMap<>(
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
        openingAnimations.clear();
        animationTicks = 0L;
    }

    /** Starts a committed opening cinematic; database state is already safe at this point. */
    public void playOpening(Player player, CrateType crateType) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Crate cinematics must start on the primary thread");
        }
        CrateStationType stationType = switch (crateType) {
            case VOTE -> CrateStationType.VOTE;
            case QUEST -> CrateStationType.QUEST;
            case FARM -> CrateStationType.FARM;
            case COMMON -> CrateStationType.COMMON;
            case RARE -> CrateStationType.RARE;
            case EPIC -> CrateStationType.EPIC;
            case LEGENDARY -> CrateStationType.LEGENDARY;
            case VALORIA -> CrateStationType.VALORIA;
        };
        StationEntities station = entities.get(stationType);
        if (station == null || !station.valid()) {
            return;
        }
        openingAnimations.put(stationType, new OpeningAnimation(0));
        Location base = station.baseLocation();
        base.getWorld().playSound(base, Sound.BLOCK_CHEST_OPEN, 1.1F, 0.72F + crateType.ordinal() * 0.06F);
        base.getWorld().spawnParticle(Particle.END_ROD, base.clone().add(0.0, 0.8, 0.0),
                stationType == CrateStationType.VALORIA ? 32 : 18, 0.45, 0.45, 0.45, 0.025);
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
            StationEntities station = entities.get(CrateStationType.PETS);
            if (station != null && station.valid()) {
                openingAnimations.put(CrateStationType.PETS, new OpeningAnimation(0));
                station.baseLocation().getWorld().playSound(
                        station.baseLocation(), Sound.BLOCK_CHEST_OPEN, 0.9F, 1.35F
                );
            }
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
            display.setBrightness(new Display.Brightness(12, 15));
            if (type == CrateStationType.LEGENDARY || type == CrateStationType.VALORIA) {
                display.setGlowing(true);
                display.setGlowColorOverride(Color.fromRGB(station.effect().primaryRgb()));
            }
            ItemStack item = new ItemStack(Material.CHEST);
            ItemMeta meta = item.getItemMeta();
            visuals.apply(meta, station.itemModel());
            item.setItemMeta(meta);
            display.setItemStack(item);
            display.setViewRange(1.0F);
        });
        List<ItemDisplay> satellites = new ArrayList<>();
        for (int index = 0; index < satelliteCount(type); index++) {
            Location satelliteLocation = location.clone().add(0.0, 0.7, 0.0);
            satellites.add(location.getWorld().spawn(satelliteLocation, ItemDisplay.class, display -> {
                mark(display, type);
                display.setPersistent(false);
                display.setInvulnerable(true);
                display.setGravity(false);
                display.setSilent(true);
                display.setBillboard(Display.Billboard.FIXED);
                display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                display.setTeleportDuration(settings.effectsEnabled() ? settings.effectIntervalTicks() : 0);
                display.setBrightness(new Display.Brightness(15, 15));
                display.setGlowing(true);
                display.setGlowColorOverride(Color.fromRGB(station.effect().primaryRgb()));
                ItemStack rune = new ItemStack(Material.AMETHYST_SHARD);
                ItemMeta runeMeta = rune.getItemMeta();
                visuals.apply(runeMeta, "item/crate/rune/" + type.configKey());
                rune.setItemMeta(runeMeta);
                display.setItemStack(rune);
                display.setViewRange(1.0F);
            }));
        }
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
        return new StationEntities(model, interaction, label, List.copyOf(satellites), location.clone());
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
            OpeningAnimation opening = openingAnimations.get(type);
            Location animated = stationEntities.baseLocation().clone();
            double openingProgress = 0.0;
            if (opening == null) {
                animated.add(0.0, Math.sin(phase * 0.70) * effect.bobHeight(), 0.0);
                animated.setYaw((float) (
                        station.yaw() + Math.sin(phase * 0.42) * effect.yawSwayDegrees()
                ));
            } else {
                int elapsed = opening.elapsedTicks() + settings.effectIntervalTicks();
                openingProgress = Math.min(1.0, elapsed / 60.0);
                double lift = Math.sin(openingProgress * Math.PI) * (type == CrateStationType.VALORIA ? 0.62 : 0.42);
                double shake = Math.sin(elapsed * 1.65) * (1.0 - openingProgress) * 0.055;
                animated.add(shake, lift, -shake);
                animated.setYaw((float) (station.yaw() + elapsed * (type == CrateStationType.VALORIA ? 13.0 : 9.0)));
                spawnOpeningEffects(type, stationEntities.baseLocation(), effect, opening.elapsedTicks(), elapsed);
                if (elapsed >= 60) {
                    openingAnimations.remove(type);
                } else {
                    openingAnimations.put(type, new OpeningAnimation(elapsed));
                }
            }
            stationEntities.model().teleport(animated);
            animateSatellites(type, stationEntities, effect, phase, openingProgress);

            if (hasNearbyPlayer(animated) && opening == null) {
                spawnAmbientEffects(type, stationEntities.baseLocation(), effect, phase);
            }
        }
    }

    private void animateSatellites(
            CrateStationType type,
            StationEntities station,
            CrateStationSettings.Effect effect,
            double phase,
            double openingProgress
    ) {
        int count = station.satellites().size();
        if (count == 0) {
            return;
        }
        boolean opening = openingProgress > 0.0;
        double speed = opening ? 4.8 : 1.35;
        double radius = effect.orbitRadius() * (opening ? 1.15 - openingProgress * 0.48 : 0.72);
        double lift = opening ? Math.sin(openingProgress * Math.PI) * 0.55 : 0.0;
        for (int index = 0; index < count; index++) {
            double direction = index % 2 == 0 ? 1.0 : -1.0;
            double angle = phase * speed * direction + TWO_PI * index / count;
            Location target = station.baseLocation().clone().add(
                    Math.cos(angle) * radius,
                    0.72 + lift + Math.sin(angle * 2.0 + index) * (opening ? 0.30 : 0.18),
                    Math.sin(angle) * radius
            );
            target.setYaw((float) Math.toDegrees(-angle + Math.PI / 2.0));
            station.satellites().get(index).teleport(target);
        }
    }

    private void spawnOpeningEffects(
            CrateStationType type,
            Location base,
            CrateStationSettings.Effect effect,
            int previous,
            int elapsed
    ) {
        World world = base.getWorld();
        double progress = Math.min(1.0, elapsed / 60.0);
        int points = type == CrateStationType.VALORIA ? 20 : 14;
        for (int index = 0; index < points; index++) {
            double angle = elapsed * 0.22 + TWO_PI * index / points;
            double radius = effect.orbitRadius() * (1.25 - progress * 0.72);
            double height = 0.2 + progress * 1.45 + Math.sin(angle * 3.0) * 0.16;
            int rgb = index % 2 == 0 ? effect.primaryRgb() : effect.secondaryRgb();
            world.spawnParticle(
                    Particle.DUST,
                    base.clone().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius),
                    1, 0.0, 0.0, 0.0, 0.0,
                    new Particle.DustOptions(Color.fromRGB(rgb), effect.particleSize() + 0.22F)
            );
        }
        world.spawnParticle(
                elapsed < 40 ? Particle.ENCHANT : Particle.END_ROD,
                base.clone().add(0.0, 0.65 + progress, 0.0),
                type == CrateStationType.VALORIA ? 7 : 4,
                0.45, 0.35, 0.45, 0.02
        );
        if (previous < 20 && elapsed >= 20) {
            world.playSound(base, Sound.BLOCK_BEACON_ACTIVATE, 0.85F, 1.15F);
            world.spawnParticle(Particle.ELECTRIC_SPARK, base.clone().add(0.0, 0.8, 0.0),
                    24, 0.65, 0.45, 0.65, 0.06);
        }
        if (previous < 44 && elapsed >= 44) {
            world.playSound(base, Sound.ENTITY_PLAYER_LEVELUP, 1.1F,
                    type == CrateStationType.VALORIA ? 0.82F : 1.25F);
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, base.clone().add(0.0, 1.0, 0.0),
                    type == CrateStationType.VALORIA ? 70 : 42, 0.75, 0.8, 0.75, 0.14);
            world.spawnParticle(Particle.END_ROD, base.clone().add(0.0, 1.0, 0.0),
                    type == CrateStationType.VALORIA ? 45 : 28, 0.55, 0.65, 0.55, 0.08);
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
            // Counter-rotating inner halo gives the idle effect real depth rather than one flat ring.
            double innerAngle = -phase * 1.35 + TWO_PI * index / effect.particleCount();
            double innerRadius = effect.orbitRadius() * 0.48;
            Location inner = base.clone().add(
                    Math.cos(innerAngle) * innerRadius,
                    0.82 + Math.sin(innerAngle * 2.0) * 0.16,
                    Math.sin(innerAngle) * innerRadius
            );
            int innerRgb = index % 2 == 0 ? effect.secondaryRgb() : effect.primaryRgb();
            world.spawnParticle(
                    Particle.DUST,
                    inner,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    new Particle.DustOptions(Color.fromRGB(innerRgb), Math.max(0.35F, effect.particleSize() - 0.18F))
            );
        }

        int sparklePeriod = type == CrateStationType.VALORIA ? 20 : 40;
        if ((type == CrateStationType.VALORIA || type == CrateStationType.LEGENDARY)
                && animationTicks % sparklePeriod < settings.effectIntervalTicks()) {
            Location sparkle = base.clone().add(0.0, type == CrateStationType.VALORIA ? 1.55 : 1.35, 0.0);
            world.spawnParticle(Particle.END_ROD, sparkle, 1, 0.16, 0.12, 0.16, 0.01);
        }
    }

    private int satelliteCount(CrateStationType type) {
        return switch (type) {
            case COMMON -> 1;
            case VOTE, QUEST, FARM, RARE -> 2;
            case EPIC, PETS -> 3;
            case LEGENDARY -> 4;
            case VALORIA -> 5;
        };
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
            List<ItemDisplay> satellites,
            Location baseLocation
    ) {
        private StationEntities {
            satellites = List.copyOf(satellites);
        }

        private boolean valid() {
            return model.isValid()
                    && hitbox.isValid()
                    && label.isValid()
                    && satellites.stream().allMatch(Entity::isValid);
        }

        private void remove() {
            List<Entity> all = new ArrayList<>(List.of(model, hitbox, label));
            all.addAll(satellites);
            all.forEach(Entity::remove);
        }
    }

    private record OpeningAnimation(int elapsedTicks) {
        private OpeningAnimation {
            if (elapsedTicks < 0 || elapsedTicks > 60) {
                throw new IllegalArgumentException("Invalid crate opening animation tick");
            }
        }
    }
}
