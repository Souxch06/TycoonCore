package com.cryptomorin.xseries;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.base.Enums;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

public enum XBiome {
    WINDSWEPT_HILLS("MOUNTAINS", "EXTREME_HILLS"),
    SNOWY_PLAINS("SNOWY_TUNDRA", "ICE_FLATS", "ICE_PLAINS"),
    SPARSE_JUNGLE("JUNGLE_EDGE", "JUNGLE_EDGE"),
    STONY_SHORE("STONE_SHORE", "STONE_BEACH"),
    CHERRY_GROVE(new String[0]),
    OLD_GROWTH_PINE_TAIGA("GIANT_TREE_TAIGA", "REDWOOD_TAIGA", "MEGA_TAIGA"),
    WINDSWEPT_FOREST("WOODED_MOUNTAINS", "EXTREME_HILLS_WITH_TREES", "EXTREME_HILLS_PLUS"),
    WOODED_BADLANDS("WOODED_BADLANDS_PLATEAU", "MESA_ROCK", "MESA_PLATEAU_FOREST"),
    WINDSWEPT_GRAVELLY_HILLS("GRAVELLY_MOUNTAINS", "MUTATED_EXTREME_HILLS", "EXTREME_HILLS_MOUNTAINS"),
    OLD_GROWTH_BIRCH_FOREST("TALL_BIRCH_FOREST", "MUTATED_BIRCH_FOREST", "BIRCH_FOREST_MOUNTAINS"),
    OLD_GROWTH_SPRUCE_TAIGA("GIANT_SPRUCE_TAIGA", "MUTATED_REDWOOD_TAIGA", "MEGA_SPRUCE_TAIGA"),
    WINDSWEPT_SAVANNA("SHATTERED_SAVANNA", "MUTATED_SAVANNA", "SAVANNA_MOUNTAINS"),
    MEADOW(new String[0]),
    MANGROVE_SWAMP(new String[0]),
    DEEP_DARK(new String[0]),
    GROVE(new String[0]),
    SNOWY_SLOPES(new String[0]),
    FROZEN_PEAKS(new String[0]),
    JAGGED_PEAKS(new String[0]),
    STONY_PEAKS(new String[0]),
    CUSTOM(new String[0]),
    BADLANDS("MESA"),
    BADLANDS_PLATEAU(WOODED_BADLANDS, "MESA_CLEAR_ROCK", "MESA_PLATEAU"),
    BEACH("BEACHES"),
    BIRCH_FOREST(OLD_GROWTH_BIRCH_FOREST, "BIRCH_FOREST"),
    BIRCH_FOREST_HILLS(OLD_GROWTH_BIRCH_FOREST, "BIRCH_FOREST_HILLS"),
    COLD_OCEAN("COLD_OCEAN"),
    DARK_FOREST("ROOFED_FOREST"),
    DARK_FOREST_HILLS("MUTATED_ROOFED_FOREST", "ROOFED_FOREST_MOUNTAINS"),
    DEEP_COLD_OCEAN("COLD_DEEP_OCEAN"),
    DEEP_FROZEN_OCEAN("FROZEN_DEEP_OCEAN"),
    DEEP_LUKEWARM_OCEAN("LUKEWARM_DEEP_OCEAN"),
    DEEP_OCEAN("DEEP_OCEAN"),
    DEEP_WARM_OCEAN("WARM_DEEP_OCEAN"),
    DESERT("DESERT"),
    DESERT_HILLS("DESERT_HILLS"),
    DESERT_LAKES("MUTATED_DESERT", "DESERT_MOUNTAINS"),
    END_BARRENS(World.Environment.THE_END, "SKY_ISLAND_BARREN"),
    END_HIGHLANDS(World.Environment.THE_END, "SKY_ISLAND_HIGH"),
    END_MIDLANDS(World.Environment.THE_END, "SKY_ISLAND_MEDIUM"),
    ERODED_BADLANDS("MUTATED_MESA", "MESA_BRYCE"),
    FLOWER_FOREST("MUTATED_FOREST"),
    FOREST("FOREST"),
    FROZEN_OCEAN("FROZEN_OCEAN"),
    FROZEN_RIVER("FROZEN_RIVER"),
    GIANT_SPRUCE_TAIGA(OLD_GROWTH_SPRUCE_TAIGA, "MUTATED_REDWOOD_TAIGA", "MEGA_SPRUCE_TAIGA"),
    GIANT_SPRUCE_TAIGA_HILLS(OLD_GROWTH_SPRUCE_TAIGA, "MUTATED_REDWOOD_TAIGA_HILLS", "MEGA_SPRUCE_TAIGA_HILLS"),
    GIANT_TREE_TAIGA(OLD_GROWTH_PINE_TAIGA, "REDWOOD_TAIGA", "MEGA_TAIGA"),
    GIANT_TREE_TAIGA_HILLS(OLD_GROWTH_PINE_TAIGA, "REDWOOD_TAIGA_HILLS", "MEGA_TAIGA_HILLS"),
    ICE_SPIKES("MUTATED_ICE_FLATS", "ICE_PLAINS_SPIKES"),
    JUNGLE("JUNGLE"),
    JUNGLE_HILLS("JUNGLE_HILLS"),
    LUKEWARM_OCEAN("LUKEWARM_OCEAN"),
    MODIFIED_BADLANDS_PLATEAU(WOODED_BADLANDS, "MUTATED_MESA_CLEAR_ROCK", "MESA_PLATEAU"),
    MODIFIED_GRAVELLY_MOUNTAINS(WINDSWEPT_GRAVELLY_HILLS, "MUTATED_EXTREME_HILLS_WITH_TREES", "EXTREME_HILLS_MOUNTAINS"),
    MODIFIED_JUNGLE("MUTATED_JUNGLE", "JUNGLE_MOUNTAINS"),
    MODIFIED_JUNGLE_EDGE(SPARSE_JUNGLE, "MUTATED_JUNGLE_EDGE", "JUNGLE_EDGE_MOUNTAINS"),
    MODIFIED_WOODED_BADLANDS_PLATEAU(WOODED_BADLANDS, "MUTATED_MESA_ROCK", "MESA_PLATEAU_FOREST_MOUNTAINS"),
    MOUNTAIN_EDGE(SPARSE_JUNGLE, "SMALLER_EXTREME_HILLS"),
    MUSHROOM_FIELDS("MUSHROOM_ISLAND"),
    MUSHROOM_FIELD_SHORE(STONY_SHORE, "MUSHROOM_ISLAND_SHORE", "MUSHROOM_SHORE"),
    SOUL_SAND_VALLEY(World.Environment.NETHER, new String[0]),
    CRIMSON_FOREST(World.Environment.NETHER, new String[0]),
    WARPED_FOREST(World.Environment.NETHER, new String[0]),
    BASALT_DELTAS(World.Environment.NETHER, new String[0]),
    NETHER_WASTES(World.Environment.NETHER, "NETHER", "HELL"),
    OCEAN("OCEAN"),
    PLAINS("PLAINS"),
    RIVER("RIVER"),
    SAVANNA("SAVANNA"),
    SAVANNA_PLATEAU(WINDSWEPT_SAVANNA, "SAVANNA_ROCK", "SAVANNA_PLATEAU"),
    SHATTERED_SAVANNA_PLATEAU(WINDSWEPT_SAVANNA, "MUTATED_SAVANNA_ROCK", "SAVANNA_PLATEAU_MOUNTAINS"),
    SMALL_END_ISLANDS(World.Environment.THE_END, "SKY_ISLAND_LOW"),
    SNOWY_BEACH("COLD_BEACH"),
    SNOWY_MOUNTAINS(WINDSWEPT_HILLS, "ICE_MOUNTAINS"),
    SNOWY_TAIGA("TAIGA_COLD", "COLD_TAIGA"),
    SNOWY_TAIGA_HILLS("TAIGA_COLD_HILLS", "COLD_TAIGA_HILLS"),
    SNOWY_TAIGA_MOUNTAINS(WINDSWEPT_FOREST, "MUTATED_TAIGA_COLD", "COLD_TAIGA_MOUNTAINS"),
    SUNFLOWER_PLAINS("MUTATED_PLAINS"),
    SWAMP("SWAMPLAND"),
    SWAMP_HILLS("MUTATED_SWAMPLAND", "SWAMPLAND_MOUNTAINS"),
    TAIGA("TAIGA"),
    TAIGA_HILLS("TAIGA_HILLS"),
    TAIGA_MOUNTAINS(WINDSWEPT_FOREST, "MUTATED_TAIGA"),
    TALL_BIRCH_FOREST(OLD_GROWTH_BIRCH_FOREST, "MUTATED_BIRCH_FOREST", "BIRCH_FOREST_MOUNTAINS"),
    TALL_BIRCH_HILLS(OLD_GROWTH_BIRCH_FOREST, "MUTATED_BIRCH_FOREST_HILLS", "MESA_PLATEAU_FOREST_MOUNTAINS"),
    THE_END(World.Environment.THE_END, "SKY"),
    THE_VOID("VOID"),
    WARM_OCEAN("WARM_OCEAN"),
    WOODED_BADLANDS_PLATEAU("MESA_ROCK", "MESA_PLATEAU_FOREST"),
    WOODED_HILLS("FOREST_HILLS"),
    WOODED_MOUNTAINS("EXTREME_HILLS_WITH_TREES", "EXTREME_HILLS_PLUS"),
    BAMBOO_JUNGLE(new String[0]),
    BAMBOO_JUNGLE_HILLS(new String[0]),
    DRIPSTONE_CAVES(new String[0]),
    LUSH_CAVES(new String[0]);

    public static final XBiome[] VALUES;
    private static final boolean HORIZONTAL_SUPPORT;
    private static final boolean EXTENDED_MINIMUM;
    @Nullable
    private final Biome biome;
    @Nonnull
    private final World.Environment environment;

    private XBiome(World.Environment environment, String ... stringArray) {
        this(environment, (XBiome)null, stringArray);
    }

    private XBiome(String ... stringArray) {
        this(World.Environment.NORMAL, stringArray);
    }

    private XBiome(XBiome xBiome, String ... stringArray) {
        this(World.Environment.NORMAL, xBiome, stringArray);
    }

    private XBiome(@Nonnull World.Environment environment, XBiome xBiome, String ... biome) {
        this.environment = environment;
        Data.NAMES.put(this.name(), this);
        for (String string2 : biome) {
            Data.NAMES.put(string2, this);
        }
        Biome biome2 = (Biome)Enums.getIfPresent(Biome.class, (String)this.name()).orNull();
        if (biome2 == null) {
            if (xBiome != null) {
                biome2 = xBiome.biome;
            }
            if (biome2 == null) {
                Biome biome3;
                Biome biome4 = biome;
                int n2 = ((Biome)biome4).length;
                for (int i = 0; i < n2 && (biome2 = (Biome)Enums.getIfPresent(Biome.class, (String)(biome3 = biome4[i])).orNull()) == null; ++i) {
                }
            }
        }
        this.biome = biome2;
    }

    @Nonnull
    private static String format(@Nonnull String string) {
        int n = string.length();
        char[] cArray = new char[n];
        int n2 = 0;
        boolean bl = false;
        for (int i = 0; i < n; ++i) {
            char c = string.charAt(i);
            if (!(bl || n2 == 0 || c != '-' && c != ' ' && c != '_' || cArray[n2] == '_')) {
                bl = true;
                continue;
            }
            if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z')) continue;
            if (bl) {
                cArray[n2++] = 95;
                bl = false;
            }
            cArray[n2++] = (char)(c & 0x5F);
        }
        return new String(cArray, 0, n2);
    }

    @Nonnull
    public static Optional<XBiome> matchXBiome(@Nonnull String string) {
        if (string == null || string.isEmpty()) {
            throw new IllegalArgumentException("Cannot match XBiome of a null or empty biome name");
        }
        return Optional.ofNullable((XBiome)((Object)Data.NAMES.get(XBiome.format(string))));
    }

    @Nonnull
    public static XBiome matchXBiome(@Nonnull Biome biome) {
        Objects.requireNonNull(biome, "Cannot match XBiome of a null biome");
        return Objects.requireNonNull((XBiome)((Object)Data.NAMES.get(biome.name())), () -> "Unsupported biome: " + biome.name());
    }

    public boolean isSupported() {
        return this.biome != null;
    }

    @Nullable
    public XBiome or(@Nullable XBiome xBiome) {
        return this.isSupported() ? this : xBiome;
    }

    @Nonnull
    public World.Environment getEnvironment() {
        return this.environment;
    }

    @Nullable
    public Biome getBiome() {
        return this.biome;
    }

    @Nonnull
    public CompletableFuture<Void> setBiome(@Nonnull Chunk chunk) {
        Objects.requireNonNull(this.biome, () -> "Unsupported biome: " + this.name());
        Objects.requireNonNull(chunk, "Cannot set biome of null chunk");
        if (!chunk.isLoaded() && !chunk.load(true)) {
            throw new IllegalStateException("Could not load chunk at " + chunk.getX() + ", " + chunk.getZ());
        }
        int n = HORIZONTAL_SUPPORT ? chunk.getWorld().getMaxHeight() : 1;
        int n2 = EXTENDED_MINIMUM ? chunk.getWorld().getMinHeight() : 0;
        return CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 16; ++i) {
                for (int j = n2; j < n; j += 4) {
                    for (int k = 0; k < 16; ++k) {
                        Block block = chunk.getBlock(i, j, k);
                        if (block.getBiome() == this.biome) continue;
                        block.setBiome(this.biome);
                    }
                }
            }
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    @Nonnull
    public CompletableFuture<Void> setBiome(@Nonnull Location location, @Nonnull Location location2) {
        Objects.requireNonNull(location, "Start location cannot be null");
        Objects.requireNonNull(location2, "End location cannot be null");
        Objects.requireNonNull(this.biome, () -> "Unsupported biome: " + this.name());
        World world = location.getWorld();
        if (!world.getUID().equals(location2.getWorld().getUID())) {
            throw new IllegalArgumentException("Location worlds mismatch");
        }
        int n = HORIZONTAL_SUPPORT ? world.getMaxHeight() : 1;
        int n2 = EXTENDED_MINIMUM ? world.getMinHeight() : 0;
        return CompletableFuture.runAsync(() -> {
            for (int i = location.getBlockX(); i < location2.getBlockX(); ++i) {
                for (int j = n2; j < n; j += 4) {
                    for (int k = location.getBlockZ(); k < location2.getBlockZ(); ++k) {
                        Block block = new Location(world, (double)i, (double)j, (double)k).getBlock();
                        if (block.getBiome() == this.biome) continue;
                        block.setBiome(this.biome);
                    }
                }
            }
        }).exceptionally(throwable -> {
            throwable.printStackTrace();
            return null;
        });
    }

    static {
        VALUES = XBiome.values();
        HORIZONTAL_SUPPORT = XMaterial.supports(16);
        EXTENDED_MINIMUM = XMaterial.supports(17);
    }

    private static final class Data {
        private static final Map<String, XBiome> NAMES = new HashMap<String, XBiome>();

        private Data() {
        }
    }
}

