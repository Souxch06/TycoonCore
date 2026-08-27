package xyz.arcadiadevs.valoriatycoon.models;

import com.awaitquality.api.spigot.chat.ChatUtil;
import com.cryptomorin.xseries.XMaterial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Generated;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.holoeasy.hologram.Hologram;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;
import xyz.arcadiadevs.valoriatycoon.models.GeneratorsData;
import xyz.arcadiadevs.valoriatycoon.models.events.DropEvent;
import xyz.arcadiadevs.valoriatycoon.models.events.Event;
import xyz.arcadiadevs.valoriatycoon.models.location.SimplifiedLocation;
import xyz.arcadiadevs.valoriatycoon.tasks.EventLoop;
import xyz.arcadiadevs.valoriatycoon.utils.HologramsUtil;
import xyz.arcadiadevs.valoriatycoon.utils.PlayerUtil;
import xyz.arcadiadevs.valoriatycoon.utils.SkyblockUtil;
import xyz.arcadiadevs.valoriatycoon.utils.TimeUtil;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;

public record LocationsData(CopyOnWriteArrayList<GeneratorLocation> locations) {
    private static final Map<Block, GeneratorLocation> locationMap = new ConcurrentHashMap<Block, GeneratorLocation>();

    public LocationsData(CopyOnWriteArrayList<GeneratorLocation> copyOnWriteArrayList) {
        this.locations = copyOnWriteArrayList;
        copyOnWriteArrayList.forEach(generatorLocation -> generatorLocation.getBlockLocations().forEach(block -> {
            GeneratorLocation generatorLocation2 = locationMap.put((Block)block, (GeneratorLocation)generatorLocation);
        }));
    }

    public Integer getGeneratorsCountByPlayer(Player player) {
        return (int)this.locations.stream().filter(generatorLocation -> generatorLocation.getPlacedBy().equals(player)).mapToLong(generatorLocation -> generatorLocation.getBlockLocations().size()).sum();
    }

    public Integer getGeneratorsCountByIsland(String string) {
        return (int)this.locations.stream().filter(generatorLocation -> generatorLocation.islandId != null && generatorLocation.islandId.equals(string)).mapToLong(generatorLocation -> generatorLocation.getBlockLocations().size()).sum();
    }

    public GeneratorLocation createLocation(OfflinePlayer offlinePlayer, int n, Block block) {
        if (Config.DEVELOPER_OPTIONS.getBoolean()) {
            ValoriaTycoon.getInstance().getLogger().info("[DEBUG] Création de l'emplacement du générateur :");
            ValoriaTycoon.getInstance().getLogger().info("[DEBUG] Joueur : " + offlinePlayer.getName());
            ValoriaTycoon.getInstance().getLogger().info("[DEBUG] Palier du générateur : " + n);
            ValoriaTycoon.getInstance().getLogger().info("[DEBUG] Position : " + String.valueOf(block.getLocation()));
        }
        GeneratorLocation[] generatorLocationArray = new GeneratorLocation[]{this.getGeneratorLocation(block.getRelative(0, 1, 0)), this.getGeneratorLocation(block.getRelative(0, -1, 0)), this.getGeneratorLocation(block.getRelative(-1, 0, 0)), this.getGeneratorLocation(block.getRelative(1, 0, 0)), this.getGeneratorLocation(block.getRelative(0, 0, -1)), this.getGeneratorLocation(block.getRelative(0, 0, 1))};
        List<GeneratorLocation> list = Stream.of(generatorLocationArray).filter(Objects::nonNull).filter(generatorLocation -> generatorLocation.getGenerator() == n).filter(generatorLocation -> generatorLocation.getPlacedBy().equals(offlinePlayer)).toList();
        this.removeAll(list);
        HashSet hashSet = list.stream().flatMap(generatorLocation -> generatorLocation.getBlockLocations().stream()).collect(Collectors.toCollection(HashSet::new));
        hashSet.add(block);
        GeneratorLocation generatorLocation2 = new GeneratorLocation(offlinePlayer.getUniqueId().toString(), SkyblockUtil.getIslandId(block.getLocation()), n, new ArrayList(hashSet));
        this.addLocation(generatorLocation2);
        return generatorLocation2;
    }

    public void addLocation(GeneratorLocation generatorLocation) {
        this.locations.add(generatorLocation);
        generatorLocation.getBlockLocations().forEach(block -> {
            GeneratorLocation generatorLocation2 = locationMap.put((Block)block, generatorLocation);
        });
    }

    public void removeLocation(GeneratorLocation generatorLocation) {
        block3: {
            try {
                generatorLocation.getBlockLocations().forEach(locationMap::remove);
                if (Config.HOLOGRAMS_ENABLED.getBoolean()) {
                    HologramsUtil.removeHologram(generatorLocation.getHologram());
                }
                this.locations.remove(generatorLocation);
                generatorLocation.getBlockLocations().forEach(locationMap::remove);
            }
            catch (Exception exception) {
                if (!Config.DEVELOPER_OPTIONS.getBoolean()) break block3;
                exception.printStackTrace();
            }
        }
    }

    public void removeAll(List<GeneratorLocation> list) {
        list.forEach(this::removeLocation);
    }

    public GeneratorLocation getGeneratorLocation(Block block) {
        return locationMap.get(block);
    }

    public static class GeneratorLocation {
        private final String playerId;
        private String islandId;
        private final Integer generator;
        private final ArrayList<SimplifiedLocation> blockLocations;
        private transient String hologramId;

        public GeneratorLocation(String string2, String string3, Integer n, List<?> list) {
            block9: {
                this.playerId = string2;
                this.generator = n;
                this.islandId = string3;
                if (list.get(0) instanceof Block) {
                    this.blockLocations = list.stream().map(object -> SimplifiedLocation.fromLocation(((Block)object).getLocation())).collect(Collectors.toCollection(ArrayList::new));
                } else if (list.get(0) instanceof SimplifiedLocation) {
                    this.blockLocations = (ArrayList)list;
                } else {
                    throw new IllegalArgumentException("Type blockLocations invalide");
                }
                Material material = XMaterial.matchXMaterial(this.getGeneratorObject().blockType().getType().toString()).orElseThrow(() -> new RuntimeException("Pile d'items invalide")).parseItem().getType();
                List list2 = ValoriaTycoon.getInstance().getConfig().getMapList("generators");
                Map map2 = list2.stream().filter(map -> map.get("name").equals(this.getGeneratorObject().name())).findFirst().orElse(null);
                if (map2 == null) {
                    return;
                }
                List<String> list3 = ((List)map2.get("hologramLines")).isEmpty() ? ValoriaTycoon.getInstance().getConfig().getStringList(Config.DEFAULT_HOLOGRAM_LINES.getPath()) : (List<String>)map2.get("hologramLines");
                list3 = list3.stream().map(string -> string.replace("%name%", this.getGeneratorObject().name())).map(string -> string.replace("%tier%", String.valueOf(this.getGeneratorObject().tier()))).map(string -> string.replace("%speed%", String.valueOf(this.getGeneratorObject().speed()))).map(string -> string.replace("%spawnItem%", this.getGeneratorObject().spawnItem().getType().toString())).map(string -> string.replace("%sellPrice%", String.valueOf(this.getGeneratorObject().sellPrice()))).map(ChatUtil::translate).toList();
                if (!Config.HOLOGRAMS_ENABLED.getBoolean()) {
                    return;
                }
                try {
                    Hologram hologram = HologramsUtil.createHologram(this.getCenter(), list3, material);
                    if (hologram != null) {
                        this.hologramId = hologram.getId().toString();
                    }
                }
                catch (Exception exception) {
                    if (!Config.DEVELOPER_OPTIONS.getBoolean()) break block9;
                    exception.printStackTrace();
                }
            }
        }

        public GeneratorLocation(String string, Integer n, List<?> list) {
            this(string, null, n, list);
        }

        public void removeBlock(Block block) {
            this.blockLocations.remove(SimplifiedLocation.fromLocation(block.getLocation()));
            if (this.blockLocations.isEmpty()) {
                ValoriaTycoon.getInstance().getLocationsData().removeLocation(this);
            }
        }

        public void removeSimpleBlock(SimplifiedLocation simplifiedLocation) {
            this.blockLocations.remove(simplifiedLocation);
            if (this.blockLocations.isEmpty()) {
                ValoriaTycoon.getInstance().getLocationsData().removeLocation(this);
            }
        }

        public OfflinePlayer getPlacedBy() {
            return Bukkit.getOfflinePlayer((UUID)UUID.fromString(this.playerId));
        }

        public World getWorld() {
            return ((SimplifiedLocation)this.blockLocations.stream().findFirst().orElseThrow()).getLocation().getWorld();
        }

        public ArrayList<Block> getBlockLocations() {
            return this.blockLocations.stream().map(SimplifiedLocation::getLocation).map(location -> {
                if (location == null) {
                    return null;
                }
                return location.getBlock();
            }).collect(Collectors.toCollection(ArrayList::new));
        }

        public ArrayList<SimplifiedLocation> getSimplifiedBlockLocations() {
            return this.blockLocations;
        }

        public GeneratorsData.Generator getGeneratorObject() {
            return ValoriaTycoon.getInstance().getGeneratorsData().getGenerator(this.generator);
        }

        public GeneratorLocation getNextTier() {
            return new GeneratorLocation(this.playerId, this.generator + 1, this.blockLocations);
        }

        public void spawn() {
            long l;
            Location location = this.getCenter();
            OfflinePlayer offlinePlayer = this.getPlacedBy();
            if (Config.DISABLE_GENERATORS_WHEN_OFFLINE.getBoolean() && !offlinePlayer.isOnline()) {
                return;
            }
            if (Config.CHUNK_RADIUS_ENABLED.getBoolean() && !this.hasPlayer()) {
                return;
            }
            ArrayList<Item> arrayList = new ArrayList<Item>();
            Event event = EventLoop.getActiveEvent().event();
            if (event instanceof DropEvent) {
                DropEvent dropEvent = (DropEvent)event;
                v0 = dropEvent.getMultiplier();
            } else {
                v0 = l = 1L;
            }
            if (Config.HOLOGRAMS_ENABLED.getBoolean()) {
                int n = 0;
                while ((long)n < l * (long)this.blockLocations.size()) {
                    Item item = location.getWorld().dropItemNaturally(location.clone().add(0.5, 1.0, 0.5), this.getGeneratorObject().spawnItem());
                    arrayList.add(item);
                    ++n;
                }
            } else {
                this.blockLocations.forEach(simplifiedLocation -> IntStream.range(0, (int)l).mapToObj(n -> simplifiedLocation.getLocation().getWorld().dropItem(simplifiedLocation.getLocation().clone().add(0.5, 1.0, 0.5), this.getGeneratorObject().spawnItem())).forEach(item -> {
                    item.setVelocity(new Vector(0, 0, 0));
                    arrayList.add((Item)item);
                }));
            }
            long l2 = TimeUtil.parseTime(ValoriaTycoon.getInstance().getConfig().getString(Config.ITEM_DESPAWN_TIME.getPath()));
            Bukkit.getScheduler().runTaskLater((Plugin)ValoriaTycoon.getInstance(), () -> this.getWorld().getEntities().stream().filter(entity -> entity instanceof Item).filter(arrayList::contains).forEach(Entity::remove), l2);
        }

        public Location getCenter() {
            double d = 2.147483647E9;
            double d2 = 2.147483647E9;
            double d3 = -2.147483648E9;
            double d4 = -2.147483648E9;
            for (SimplifiedLocation simplifiedLocation : this.blockLocations) {
                Location location = simplifiedLocation.getLocation();
                int n = location.getBlockX();
                int n2 = location.getBlockZ();
                d = Math.min(d, (double)n);
                d2 = Math.min(d2, (double)n2);
                d3 = Math.max(d3, (double)n);
                d4 = Math.max(d4, (double)n2);
            }
            double d5 = (d + d3) / 2.0;
            double d6 = (d2 + d4) / 2.0;
            double d7 = this.blockLocations.stream().map(SimplifiedLocation::getLocation).map(Location::getBlockY).max(Integer::compareTo).orElse(0).intValue();
            return new Location(this.getWorld(), d5 + 0.5, d7 + 1.0, d6 + 0.5);
        }

        private boolean hasPlayer() {
            int n = PlayerUtil.getRadius(this.getPlacedBy().getPlayer());
            for (SimplifiedLocation simplifiedLocation : this.blockLocations) {
                Location location = simplifiedLocation.getLocation();
                int n2 = (int)Math.ceil((double)n / 16.0);
                for (Entity entity : location.getWorld().getNearbyEntities(location, (double)(n2 * 16), 256.0, (double)(n2 * 16))) {
                    if (!(entity instanceof Player)) continue;
                    return true;
                }
            }
            return false;
        }

        public Hologram getHologram() {
            return HologramsUtil.getHologram(this.hologramId);
        }

        public void setHologram(Hologram hologram) {
            if (hologram != null) {
                this.hologramId = hologram.getId().toString();
            }
        }

        public String toString() {
            return "GeneratorLocation{playerId='" + this.playerId + "', islandId='" + this.islandId + "', generator=" + String.valueOf(this.generator) + ", blockLocations=" + String.valueOf(this.blockLocations) + ", hologramId='" + this.hologramId + "'}";
        }

        @Generated
        public void setIslandId(String string) {
            this.islandId = string;
        }

        @Generated
        public void setHologramId(String string) {
            this.hologramId = string;
        }

        @Generated
        public String getPlayerId() {
            return this.playerId;
        }

        @Generated
        public String getIslandId() {
            return this.islandId;
        }

        @Generated
        public Integer getGenerator() {
            return this.generator;
        }

        @Generated
        public String getHologramId() {
            return this.hologramId;
        }
    }
}

