package com.cryptomorin.xseries.particles;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Particle;

public enum XParticle {
    POOF("EXPLOSION_NORMAL"),
    EXPLOSION("EXPLOSION_LARGE"),
    EXPLOSION_EMITTER("EXPLOSION_HUGE"),
    FIREWORK("FIREWORKS_SPARK"),
    BUBBLE("WATER_BUBBLE"),
    SPLASH("WATER_SPLASH"),
    FISHING("WATER_WAKE"),
    UNDERWATER("SUSPENDED"),
    CRIT(new String[0]),
    ENCHANTED_HIT("CRIT_MAGIC"),
    SMOKE("SMOKE_NORMAL"),
    LARGE_SMOKE("SMOKE_LARGE"),
    EFFECT("SPELL"),
    INSTANT_EFFECT("SPELL_INSTANT"),
    ENTITY_EFFECT("SPELL_MOB", "SPELL_MOB_AMBIENT"),
    WITCH("SPELL_WITCH"),
    DRIPPING_WATER("DRIP_WATER"),
    DRIPPING_LAVA("DRIP_LAVA"),
    ANGRY_VILLAGER("VILLAGER_ANGRY"),
    HAPPY_VILLAGER("VILLAGER_HAPPY"),
    MYCELIUM("TOWN_AURA"),
    NOTE(new String[0]),
    PORTAL(new String[0]),
    ENCHANT("ENCHANTMENT_TABLE"),
    FLAME(new String[0]),
    LAVA(new String[0]),
    CLOUD(new String[0]),
    DUST("REDSTONE"),
    ITEM_SNOWBALL("SNOWBALL", "SNOW_SHOVEL"),
    ITEM_SLIME("SLIME"),
    HEART(new String[0]),
    ITEM("ITEM_CRACK"),
    BLOCK("BLOCK_CRACK", "BLOCK_DUST"),
    RAIN("WATER_DROP"),
    ELDER_GUARDIAN("MOB_APPEARANCE"),
    DRAGON_BREATH(new String[0]),
    END_ROD(new String[0]),
    DAMAGE_INDICATOR(new String[0]),
    SWEEP_ATTACK(new String[0]),
    FALLING_DUST(new String[0]),
    TOTEM_OF_UNDYING("TOTEM"),
    SPIT(new String[0]),
    SQUID_INK(new String[0]),
    BUBBLE_POP(new String[0]),
    CURRENT_DOWN(new String[0]),
    BUBBLE_COLUMN_UP(new String[0]),
    NAUTILUS(new String[0]),
    DOLPHIN(new String[0]),
    SNEEZE(new String[0]),
    CAMPFIRE_COSY_SMOKE(new String[0]),
    CAMPFIRE_SIGNAL_SMOKE(new String[0]),
    COMPOSTER(new String[0]),
    FLASH(new String[0]),
    FALLING_LAVA(new String[0]),
    LANDING_LAVA(new String[0]),
    FALLING_WATER(new String[0]),
    DRIPPING_HONEY(new String[0]),
    FALLING_HONEY(new String[0]),
    LANDING_HONEY(new String[0]),
    FALLING_NECTAR(new String[0]),
    SOUL_FIRE_FLAME(new String[0]),
    ASH(new String[0]),
    CRIMSON_SPORE(new String[0]),
    WARPED_SPORE(new String[0]),
    SOUL(new String[0]),
    DRIPPING_OBSIDIAN_TEAR(new String[0]),
    FALLING_OBSIDIAN_TEAR(new String[0]),
    LANDING_OBSIDIAN_TEAR(new String[0]),
    REVERSE_PORTAL(new String[0]),
    WHITE_ASH(new String[0]),
    DUST_COLOR_TRANSITION(new String[0]),
    VIBRATION(new String[0]),
    FALLING_SPORE_BLOSSOM(new String[0]),
    SPORE_BLOSSOM_AIR(new String[0]),
    SMALL_FLAME(new String[0]),
    SNOWFLAKE(new String[0]),
    DRIPPING_DRIPSTONE_LAVA(new String[0]),
    FALLING_DRIPSTONE_LAVA(new String[0]),
    DRIPPING_DRIPSTONE_WATER(new String[0]),
    FALLING_DRIPSTONE_WATER(new String[0]),
    GLOW_SQUID_INK(new String[0]),
    GLOW(new String[0]),
    WAX_ON(new String[0]),
    WAX_OFF(new String[0]),
    ELECTRIC_SPARK(new String[0]),
    SCRAPE(new String[0]),
    SONIC_BOOM(new String[0]),
    SCULK_SOUL(new String[0]),
    SCULK_CHARGE(new String[0]),
    SCULK_CHARGE_POP(new String[0]),
    SHRIEK(new String[0]),
    CHERRY_LEAVES(new String[0]),
    EGG_CRACK(new String[0]),
    DUST_PLUME(new String[0]),
    WHITE_SMOKE(new String[0]),
    GUST(new String[0]),
    SMALL_GUST(new String[0]),
    GUST_EMITTER_LARGE(new String[0]),
    GUST_EMITTER_SMALL(new String[0]),
    TRIAL_SPAWNER_DETECTION(new String[0]),
    TRIAL_SPAWNER_DETECTION_OMINOUS(new String[0]),
    VAULT_CONNECTION(new String[0]),
    INFESTED(new String[0]),
    ITEM_COBWEB(new String[0]),
    DUST_PILLAR(new String[0]),
    OMINOUS_SPAWNING(new String[0]),
    RAID_OMEN(new String[0]),
    TRIAL_OMEN(new String[0]),
    BLOCK_MARKER("BARRIER", "LIGHT");

    private final Particle particle;

    private XParticle(String ... stringArray) {
        Particle particle = XParticle.tryGetParticle(this.name());
        Data.NAME_MAPPING.put(this.name(), this);
        for (String string2 : stringArray) {
            if (particle == null) {
                particle = XParticle.tryGetParticle(string2);
            }
            Data.NAME_MAPPING.put(string2, this);
        }
        this.particle = particle;
        if (this.particle != null) {
            Data.BUKKIT_MAPPING.put(this.particle, this);
        }
    }

    public Particle get() {
        return this.particle;
    }

    public boolean isSupported() {
        return this.particle != null;
    }

    public XParticle or(XParticle xParticle) {
        return this.isSupported() ? this : xParticle;
    }

    public static XParticle of(Particle particle) {
        Objects.requireNonNull(particle, "Cannot match null particle");
        XParticle xParticle = (XParticle)((Object)Data.BUKKIT_MAPPING.get(particle));
        if (xParticle != null) {
            return xParticle;
        }
        throw new UnsupportedOperationException("Unknown particle: " + particle);
    }

    public static Optional<XParticle> of(String string) {
        Objects.requireNonNull(string, "Cannot match null particle");
        return Optional.ofNullable((XParticle)((Object)Data.NAME_MAPPING.get(string)));
    }

    private static Particle tryGetParticle(String string) {
        try {
            return Particle.valueOf((String)string);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    private static final class Data {
        private static final Map<String, XParticle> NAME_MAPPING = new HashMap<String, XParticle>();
        private static final Map<Particle, XParticle> BUKKIT_MAPPING = new EnumMap<Particle, XParticle>(Particle.class);

        private Data() {
        }
    }
}

