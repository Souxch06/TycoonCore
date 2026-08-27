/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Enums
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 */
package com.cryptomorin.xseries;

import com.google.common.base.Enums;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public enum XEntityType {
    ALLAY(new String[0]),
    AREA_EFFECT_CLOUD(new String[0]),
    ARMADILLO(new String[0]),
    ARMOR_STAND(new String[0]),
    ARROW(new String[0]),
    AXOLOTL(new String[0]),
    BAT(new String[0]),
    BEE(new String[0]),
    BLAZE(new String[0]),
    BLOCK_DISPLAY(new String[0]),
    BOAT(new String[0]),
    BOGGED(new String[0]),
    BREEZE(new String[0]),
    BREEZE_WIND_CHARGE(new String[0]),
    CAMEL(new String[0]),
    CAT(new String[0]),
    CAVE_SPIDER(new String[0]),
    CHEST_BOAT(new String[0]),
    CHEST_MINECART("MINECART_CHEST"),
    CHICKEN(new String[0]),
    COD(new String[0]),
    COMMAND_BLOCK_MINECART("MINECART_COMMAND"),
    COW(new String[0]),
    CREEPER(new String[0]),
    DOLPHIN(new String[0]),
    DONKEY(new String[0]),
    DRAGON_FIREBALL(new String[0]),
    DROWNED(new String[0]),
    EGG(new String[0]),
    ELDER_GUARDIAN(new String[0]),
    ENDERMAN(new String[0]),
    ENDERMITE(new String[0]),
    ENDER_DRAGON(new String[0]),
    ENDER_PEARL(new String[0]),
    END_CRYSTAL("ENDER_CRYSTAL"),
    EVOKER(new String[0]),
    EVOKER_FANGS(new String[0]),
    EXPERIENCE_BOTTLE("THROWN_EXP_BOTTLE"),
    EXPERIENCE_ORB(new String[0]),
    EYE_OF_ENDER("ENDER_SIGNAL"),
    FALLING_BLOCK(new String[0]),
    FIREBALL(new String[0]),
    FIREWORK_ROCKET("FIREWORK"),
    FISHING_BOBBER("FISHING_HOOK"),
    FOX(new String[0]),
    FROG(new String[0]),
    FURNACE_MINECART(new String[0]),
    GHAST(new String[0]),
    GIANT(new String[0]),
    GLOW_ITEM_FRAME(new String[0]),
    GLOW_SQUID(new String[0]),
    GOAT(new String[0]),
    GUARDIAN(new String[0]),
    HOGLIN(new String[0]),
    HOPPER_MINECART("MINECART_HOPPER"),
    HORSE(new String[0]),
    HUSK(new String[0]),
    ILLUSIONER(new String[0]),
    INTERACTION(new String[0]),
    IRON_GOLEM(new String[0]),
    ITEM("DROPPED_ITEM"),
    ITEM_DISPLAY(new String[0]),
    ITEM_FRAME(new String[0]),
    LEASH_KNOT("LEASH_HITCH"),
    LIGHTNING_BOLT("LIGHTNING"),
    LLAMA(new String[0]),
    LLAMA_SPIT(new String[0]),
    MAGMA_CUBE(new String[0]),
    MARKER(new String[0]),
    MINECART(new String[0]),
    MOOSHROOM("MUSHROOM_COW"),
    MULE(new String[0]),
    OCELOT(new String[0]),
    OMINOUS_ITEM_SPAWNER(new String[0]),
    PAINTING(new String[0]),
    PANDA(new String[0]),
    PARROT(new String[0]),
    PHANTOM(new String[0]),
    PIG(new String[0]),
    PIGLIN(new String[0]),
    PIGLIN_BRUTE(new String[0]),
    PILLAGER(new String[0]),
    PLAYER(new String[0]),
    POLAR_BEAR(new String[0]),
    POTION("SPLASH_POTION"),
    PUFFERFISH(new String[0]),
    RABBIT(new String[0]),
    RAVAGER(new String[0]),
    SALMON(new String[0]),
    SHEEP(new String[0]),
    SHULKER(new String[0]),
    SHULKER_BULLET(new String[0]),
    SILVERFISH(new String[0]),
    SKELETON(new String[0]),
    SKELETON_HORSE(new String[0]),
    SLIME(new String[0]),
    SMALL_FIREBALL(new String[0]),
    SNIFFER(new String[0]),
    SNOWBALL(new String[0]),
    SNOW_GOLEM("SNOWMAN"),
    SPAWNER_MINECART("MINECART_MOB_SPAWNER"),
    SPECTRAL_ARROW(new String[0]),
    SPIDER(new String[0]),
    SQUID(new String[0]),
    STRAY(new String[0]),
    STRIDER(new String[0]),
    TADPOLE(new String[0]),
    TEXT_DISPLAY(new String[0]),
    TNT("PRIMED_TNT"),
    TNT_MINECART("MINECART_TNT"),
    TRADER_LLAMA(new String[0]),
    TRIDENT(new String[0]),
    TROPICAL_FISH(new String[0]),
    TURTLE(new String[0]),
    UNKNOWN(new String[0]),
    VEX(new String[0]),
    VILLAGER(new String[0]),
    VINDICATOR(new String[0]),
    WANDERING_TRADER(new String[0]),
    WARDEN(new String[0]),
    WIND_CHARGE(new String[0]),
    WITCH(new String[0]),
    WITHER(new String[0]),
    WITHER_SKELETON(new String[0]),
    WITHER_SKULL(new String[0]),
    WOLF(new String[0]),
    ZOGLIN(new String[0]),
    ZOMBIE(new String[0]),
    ZOMBIE_HORSE(new String[0]),
    ZOMBIE_VILLAGER(new String[0]),
    ZOMBIFIED_PIGLIN(new String[0]);

    private final EntityType entityType;

    private XEntityType(String ... stringArray) {
        EntityType entityType = (EntityType)Enums.getIfPresent(EntityType.class, (String)this.name()).orNull();
        Data.NAME_MAPPING.put(this.name(), this);
        for (String string2 : stringArray) {
            if (entityType == null) {
                entityType = XEntityType.tryGetEntityType(string2);
            }
            Data.NAME_MAPPING.put(string2, this);
        }
        this.entityType = entityType;
        if (entityType != null) {
            Data.BUKKIT_MAPPING.put(entityType, this);
        }
    }

    private static EntityType tryGetEntityType(String string) {
        try {
            return EntityType.valueOf((String)string);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    public boolean isSupported() {
        return this.entityType != null;
    }

    public XEntityType or(XEntityType xEntityType) {
        return this.isSupported() ? this : xEntityType;
    }

    public static XEntityType of(Entity entity) {
        Objects.requireNonNull(entity, "Cannot match entity type from null entity");
        return XEntityType.of(entity.getType());
    }

    public static XEntityType of(EntityType entityType) {
        Objects.requireNonNull(entityType, "Cannot match null entity type");
        XEntityType xEntityType = (XEntityType)((Object)Data.BUKKIT_MAPPING.get(entityType));
        if (xEntityType != null) {
            return xEntityType;
        }
        throw new UnsupportedOperationException("Unknown entity type: " + entityType);
    }

    public static Optional<XEntityType> of(String string) {
        Objects.requireNonNull(string, "Cannot match null entity type");
        return Optional.ofNullable((XEntityType)((Object)Data.NAME_MAPPING.get(string)));
    }

    public EntityType get() {
        return this.entityType;
    }

    public static final class Data {
        private static final Map<String, XEntityType> NAME_MAPPING = new HashMap<String, XEntityType>();
        private static final Map<EntityType, XEntityType> BUKKIT_MAPPING = new EnumMap<EntityType, XEntityType>(EntityType.class);
    }
}

