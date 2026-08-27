package com.cryptomorin.xseries;

import com.google.common.base.Enums;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public enum XEnchantment {
    AQUA_AFFINITY("WATER_WORKER", "WATER_WORKER", "AQUA_AFFINITY", "WATER_MINE"),
    BANE_OF_ARTHROPODS("DAMAGE_ARTHROPODS", "ARDMG", "BANE_OF_ARTHROPOD", "ARTHROPOD"),
    BINDING_CURSE("BINDING_CURSE", "BIND_CURSE", "BINDING", "BIND"),
    BLAST_PROTECTION("PROTECTION_EXPLOSIONS", "BLAST_PROTECT", "EXPLOSIONS_PROTECTION", "EXPLOSION_PROTECTION", "BLAST_PROTECTION"),
    BREACH(new String[0]),
    CHANNELING("CHANNELLING", "CHANELLING", "CHANELING", "CHANNEL"),
    DENSITY(new String[0]),
    DEPTH_STRIDER("DEPTH", "STRIDER"),
    EFFICIENCY("DIG_SPEED", "MINE_SPEED", "CUT_SPEED"),
    FEATHER_FALLING("PROTECTION_FALL", "FALL_PROT", "FEATHER_FALL", "FALL_PROTECTION", "FEATHER_FALLING"),
    FIRE_ASPECT("FIRE", "MELEE_FIRE", "MELEE_FLAME"),
    FIRE_PROTECTION("PROTECTION_FIRE", "FIRE_PROT", "FIRE_PROTECT", "FIRE_PROTECTION", "FLAME_PROTECTION", "FLAME_PROTECT", "FLAME_PROT"),
    FLAME("ARROW_FIRE", "FLAME_ARROW", "FIRE_ARROW"),
    FORTUNE("LOOT_BONUS_BLOCKS", "BLOCKS_LOOT_BONUS", "FORT"),
    FROST_WALKER("FROST", "WALKER"),
    IMPALING("IMPALE", "OCEAN_DAMAGE", "OCEAN_DMG"),
    INFINITY("ARROW_INFINITE", "INF_ARROWS", "INFINITE_ARROWS", "INFINITE", "UNLIMITED", "UNLIMITED_ARROWS"),
    KNOCKBACK("K_BACK"),
    LOOTING("LOOT_BONUS_MOBS", "MOB_LOOT", "MOBS_LOOT_BONUS"),
    LOYALTY("LOYAL", "RETURN"),
    LUCK_OF_THE_SEA("LUCK", "LUCK_OF_SEA", "LUCK_OF_SEAS", "ROD_LUCK"),
    LURE("ROD_LURE"),
    MENDING(new String[0]),
    MULTISHOT("TRIPLE_SHOT"),
    PIERCING(new String[0]),
    POWER("ARROW_DAMAGE", "ARROW_POWER"),
    PROJECTILE_PROTECTION("PROTECTION_PROJECTILE", "PROJECTILE_PROTECTION", "PROJ_PROT"),
    PROTECTION("PROTECTION_ENVIRONMENTAL", "PROTECT"),
    PUNCH("ARROW_KNOCKBACK", "ARROWKB", "ARROW_PUNCH"),
    QUICK_CHARGE("QUICKCHARGE", "QUICK_DRAW", "FAST_CHARGE", "FAST_DRAW"),
    RESPIRATION("OXYGEN", "BREATH", "BREATHING"),
    RIPTIDE("RIP", "TIDE", "LAUNCH"),
    SHARPNESS("DAMAGE_ALL", "ALL_DAMAGE", "ALL_DMG", "SHARP"),
    SILK_TOUCH("SOFT_TOUCH"),
    SMITE("DAMAGE_UNDEAD", "UNDEAD_DAMAGE"),
    SOUL_SPEED("SPEED_SOUL", "SOUL_RUNNER"),
    SWEEPING_EDGE("SWEEPING", "SWEEPING_EDGE", "SWEEP_EDGE"),
    SWIFT_SNEAK("SNEAK_SWIFT"),
    THORNS("HIGHCRIT", "THORN", "HIGHERCRIT"),
    UNBREAKING("DURABILITY", "DURA"),
    VANISHING_CURSE("VANISHING_CURSE", "VANISH_CURSE", "VANISHING", "VANISH"),
    WIND_BURST(new String[0]);

    public static final XEnchantment[] VALUES;
    public static final Set<EntityType> EFFECTIVE_SMITE_ENTITIES;
    public static final Set<EntityType> EFFECTIVE_BANE_OF_ARTHROPODS_ENTITIES;
    @Nullable
    private final Enchantment enchantment;

    private XEnchantment(String ... stringArray) {
        Enchantment enchantment = XEnchantment.getBukkitEnchant(this.name());
        Data.NAMES.put(this.name(), this);
        for (String string2 : stringArray) {
            Data.NAMES.put(string2, this);
            if (enchantment != null) continue;
            enchantment = XEnchantment.getBukkitEnchant(string2);
        }
        this.enchantment = enchantment;
    }

    private static Enchantment getBukkitEnchant(String string) {
        if (Data.IS_SUPER_FLAT) {
            return (Enchantment)Registry.ENCHANTMENT.get(NamespacedKey.minecraft((String)string.toLowerCase(Locale.ENGLISH)));
        }
        if (Data.ISFLAT) {
            return Enchantment.getByKey((NamespacedKey)NamespacedKey.minecraft((String)string.toLowerCase(Locale.ENGLISH)));
        }
        return Enchantment.getByName((String)string);
    }

    public static boolean isSmiteEffectiveAgainst(@Nullable EntityType entityType) {
        return entityType != null && EFFECTIVE_SMITE_ENTITIES.contains(entityType);
    }

    public static boolean isArthropodsEffectiveAgainst(@Nullable EntityType entityType) {
        return entityType != null && EFFECTIVE_BANE_OF_ARTHROPODS_ENTITIES.contains(entityType);
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
    public static Optional<XEnchantment> matchXEnchantment(@Nonnull String string) {
        if (string == null || string.isEmpty()) {
            throw new IllegalArgumentException("Enchantment name cannot be null or empty");
        }
        return Optional.ofNullable((XEnchantment)((Object)Data.NAMES.get(XEnchantment.format(string))));
    }

    @Nonnull
    public static XEnchantment matchXEnchantment(@Nonnull Enchantment enchantment) {
        Objects.requireNonNull(enchantment, "Cannot parse XEnchantment of a null enchantment");
        return Objects.requireNonNull((XEnchantment)((Object)Data.NAMES.get(enchantment.getName())), () -> "Unsupported enchantment: " + enchantment.getName());
    }

    @Nonnull
    public ItemStack getBook(int n) {
        ItemStack itemStack = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta)itemStack.getItemMeta();
        enchantmentStorageMeta.addStoredEnchant(this.enchantment, n, true);
        itemStack.setItemMeta((ItemMeta)enchantmentStorageMeta);
        return itemStack;
    }

    @Nullable
    public Enchantment getEnchant() {
        return this.enchantment;
    }

    public boolean isSupported() {
        return this.enchantment != null;
    }

    @Nullable
    public XEnchantment or(@Nullable XEnchantment xEnchantment) {
        return this.isSupported() ? this : xEnchantment;
    }

    @Nonnull
    public String toString() {
        return Arrays.stream(this.name().split("_")).map(string -> string.charAt(0) + string.substring(1).toLowerCase()).collect(Collectors.joining(" "));
    }

    static {
        VALUES = XEnchantment.values();
        EntityType entityType = (EntityType)Enums.getIfPresent(EntityType.class, (String)"BEE").orNull();
        EntityType entityType2 = (EntityType)Enums.getIfPresent(EntityType.class, (String)"PHANTOM").orNull();
        EntityType entityType3 = (EntityType)Enums.getIfPresent(EntityType.class, (String)"DROWNED").orNull();
        EntityType entityType4 = (EntityType)Enums.getIfPresent(EntityType.class, (String)"WITHER_SKELETON").orNull();
        EntityType entityType5 = (EntityType)Enums.getIfPresent(EntityType.class, (String)"SKELETON_HORSE").orNull();
        EntityType entityType6 = (EntityType)Enums.getIfPresent(EntityType.class, (String)"STRAY").orNull();
        EntityType entityType7 = (EntityType)Enums.getIfPresent(EntityType.class, (String)"HUSK").orNull();
        EnumSet<EntityType> enumSet = EnumSet.of(EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.SILVERFISH, EntityType.ENDERMITE);
        if (entityType != null) {
            enumSet.add(entityType);
        }
        EFFECTIVE_BANE_OF_ARTHROPODS_ENTITIES = Collections.unmodifiableSet(enumSet);
        EnumSet<EntityType> enumSet2 = EnumSet.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITHER);
        if (entityType2 != null) {
            enumSet2.add(entityType2);
        }
        if (entityType3 != null) {
            enumSet2.add(entityType3);
        }
        if (entityType4 != null) {
            enumSet2.add(entityType4);
        }
        if (entityType5 != null) {
            enumSet2.add(entityType5);
        }
        if (entityType6 != null) {
            enumSet2.add(entityType6);
        }
        if (entityType7 != null) {
            enumSet2.add(entityType7);
        }
        EFFECTIVE_SMITE_ENTITIES = Collections.unmodifiableSet(enumSet2);
    }

    private static final class Data {
        private static final boolean ISFLAT;
        private static final boolean IS_SUPER_FLAT;
        private static final Map<String, XEnchantment> NAMES;

        private Data() {
        }

        static {
            boolean bl;
            boolean bl2;
            NAMES = new HashMap<String, XEnchantment>();
            try {
                Class<?> clazz = Class.forName("org.bukkit.NamespacedKey");
                Class<?> clazz2 = Class.forName("org.bukkit.enchantments.Enchantment");
                clazz2.getDeclaredMethod("getByKey", clazz);
                bl2 = true;
            }
            catch (ClassNotFoundException | NoSuchMethodException reflectiveOperationException) {
                bl2 = false;
            }
            try {
                Class.forName("org.bukkit.Registry");
                bl = true;
            }
            catch (ClassNotFoundException classNotFoundException) {
                bl = false;
            }
            ISFLAT = bl2;
            IS_SUPER_FLAT = bl;
        }
    }
}

