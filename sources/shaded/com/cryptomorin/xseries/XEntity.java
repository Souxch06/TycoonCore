package com.cryptomorin.xseries;

import com.cryptomorin.xseries.XItemStack;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.google.common.base.Enums;
import com.google.common.base.Strings;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.TreeSpecies;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Cat;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Fox;
import org.bukkit.entity.GlowSquid;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Mob;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Spellcaster;
import org.bukkit.entity.Strider;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.TropicalFish;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.Lootable;

public final class XEntity {
    public static final Set<EntityType> UNDEAD;

    private XEntity() {
    }

    public static boolean isUndead(@Nullable EntityType entityType) {
        return entityType != null && UNDEAD.contains(entityType);
    }

    @Nullable
    public static Entity spawn(@Nonnull Location location, @Nonnull ConfigurationSection configurationSection) {
        Objects.requireNonNull(location, "Cannot spawn entity at a null location.");
        Objects.requireNonNull(configurationSection, "Cannot spawn entity from a null configuration section");
        String string = configurationSection.getString("type");
        if (string == null) {
            return null;
        }
        EntityType entityType = (EntityType)Enums.getIfPresent(EntityType.class, (String)string.toUpperCase(Locale.ENGLISH)).or((Object)EntityType.ZOMBIE);
        return XEntity.edit(location.getWorld().spawnEntity(location, entityType), configurationSection);
    }

    @Nonnull
    public static Entity edit(@Nonnull Entity entity, @Nonnull ConfigurationSection configurationSection) {
        BossBar bossBar;
        Lootable lootable;
        int n;
        Objects.requireNonNull(entity, "Cannot edit properties of a null entity");
        Objects.requireNonNull(configurationSection, "Cannot edit an entity from a null configuration section");
        String string = configurationSection.getString("name");
        if (string != null) {
            entity.setCustomName(ChatColor.translateAlternateColorCodes((char)'&', (String)string));
            entity.setCustomNameVisible(true);
        }
        if (configurationSection.isSet("glowing")) {
            entity.setGlowing(configurationSection.getBoolean("glowing"));
        }
        if (configurationSection.isSet("gravity")) {
            entity.setGravity(configurationSection.getBoolean("gravity"));
        }
        if (configurationSection.isSet("silent")) {
            entity.setSilent(configurationSection.getBoolean("silent"));
        }
        entity.setFireTicks(configurationSection.getInt("fire-ticks"));
        entity.setFallDistance((float)configurationSection.getInt("fall-distance"));
        if (configurationSection.isSet("invulnerable")) {
            entity.setInvulnerable(configurationSection.getBoolean("invulnerable"));
        }
        if ((n = configurationSection.getInt("ticks-lived")) > 0) {
            entity.setTicksLived(n);
        }
        if (configurationSection.isSet("portal-cooldown")) {
            entity.setPortalCooldown(configurationSection.getInt("portal-cooldown", -1));
        }
        if (XMaterial.supports(13)) {
            if (entity instanceof Lootable) {
                lootable = (Lootable)entity;
                long l = configurationSection.getLong("seed");
                if (l != 0L) {
                    lootable.setSeed(l);
                }
            }
            if (entity instanceof Boss) {
                lootable = (Boss)entity;
                ConfigurationSection configurationSection2 = configurationSection.getConfigurationSection("bossbar");
                if (configurationSection2 != null) {
                    bossBar = lootable.getBossBar();
                    XEntity.editBossBar(bossBar, configurationSection2);
                }
            }
        }
        if (entity instanceof Vehicle && entity instanceof Boat) {
            lootable = (Boat)entity;
            String string2 = configurationSection.getString("tree-species");
            if (string2 != null && (bossBar = Enums.getIfPresent(TreeSpecies.class, (String)string2)).isPresent()) {
                lootable.setWoodType((TreeSpecies)bossBar.get());
            }
        }
        if (entity instanceof LivingEntity) {
            ConfigurationSection configurationSection3;
            ConfigurationSection configurationSection4;
            Object object4;
            Object object2;
            int n2;
            lootable = (LivingEntity)entity;
            if (configurationSection.isSet("health")) {
                double d = configurationSection.getDouble("health");
                lootable.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(d);
                lootable.setHealth(d);
            }
            if (XMaterial.supports(14)) {
                lootable.setAbsorptionAmount((double)configurationSection.getInt("absorption"));
            }
            if (configurationSection.isSet("AI")) {
                lootable.setAI(configurationSection.getBoolean("AI"));
            }
            if (configurationSection.isSet("can-pickup-items")) {
                lootable.setCanPickupItems(configurationSection.getBoolean("can-pickup-items"));
            }
            if (configurationSection.isSet("collidable")) {
                lootable.setCollidable(configurationSection.getBoolean("collidable"));
            }
            if (configurationSection.isSet("gliding")) {
                lootable.setGliding(configurationSection.getBoolean("gliding"));
            }
            if (configurationSection.isSet("remove-when-far-away")) {
                lootable.setRemoveWhenFarAway(configurationSection.getBoolean("remove-when-far-away"));
            }
            if (XMaterial.supports(13) && configurationSection.isSet("swimming")) {
                lootable.setSwimming(configurationSection.getBoolean("swimming"));
            }
            if (configurationSection.isSet("max-air")) {
                lootable.setMaximumAir(configurationSection.getInt("max-air"));
            }
            if (configurationSection.isSet("no-damage-ticks")) {
                lootable.setNoDamageTicks(configurationSection.getInt("no-damage-ticks"));
            }
            if (configurationSection.isSet("remaining-air")) {
                lootable.setRemainingAir(configurationSection.getInt("remaining-air"));
            }
            XPotion.addEffects((LivingEntity)lootable, configurationSection.getStringList("effects"));
            ConfigurationSection configurationSection5 = configurationSection.getConfigurationSection("equipment");
            if (configurationSection5 != null) {
                ConfigurationSection configurationSection6;
                Object object3;
                bossBar = lootable.getEquipment();
                n2 = entity instanceof Mob;
                object2 = configurationSection5.getConfigurationSection("helmet");
                if (object2 != null) {
                    bossBar.setHelmet(XItemStack.deserialize(object2.getConfigurationSection("item")));
                    if (n2 != 0) {
                        bossBar.setHelmetDropChance((float)object2.getInt("drop-chance"));
                    }
                }
                if ((object3 = configurationSection5.getConfigurationSection("chestplate")) != null) {
                    bossBar.setChestplate(XItemStack.deserialize(object3.getConfigurationSection("item")));
                    if (n2 != 0) {
                        bossBar.setChestplateDropChance((float)object3.getInt("drop-chance"));
                    }
                }
                if ((object4 = configurationSection5.getConfigurationSection("leggings")) != null) {
                    bossBar.setLeggings(XItemStack.deserialize(object4.getConfigurationSection("item")));
                    if (n2 != 0) {
                        bossBar.setLeggingsDropChance((float)object4.getInt("drop-chance"));
                    }
                }
                if ((configurationSection4 = configurationSection5.getConfigurationSection("boots")) != null) {
                    bossBar.setBoots(XItemStack.deserialize(configurationSection4.getConfigurationSection("item")));
                    if (n2 != 0) {
                        bossBar.setBootsDropChance((float)configurationSection4.getInt("drop-chance"));
                    }
                }
                if ((configurationSection6 = configurationSection5.getConfigurationSection("main-hand")) != null) {
                    bossBar.setItemInMainHand(XItemStack.deserialize(configurationSection6.getConfigurationSection("item")));
                    if (n2 != 0) {
                        bossBar.setItemInMainHandDropChance((float)configurationSection6.getInt("drop-chance"));
                    }
                }
                if ((configurationSection3 = configurationSection5.getConfigurationSection("off-hand")) != null) {
                    bossBar.setItemInOffHand(XItemStack.deserialize(configurationSection3.getConfigurationSection("item")));
                    if (n2 != 0) {
                        bossBar.setItemInOffHandDropChance((float)configurationSection3.getInt("drop-chance"));
                    }
                }
            }
            if (lootable instanceof Ageable) {
                bossBar = (Ageable)lootable;
                if (configurationSection.isSet("breed")) {
                    bossBar.setBreed(configurationSection.getBoolean("breed"));
                }
                if (configurationSection.isSet("baby")) {
                    if (configurationSection.getBoolean("baby")) {
                        bossBar.setBaby();
                    } else {
                        bossBar.setAdult();
                    }
                }
                if ((n2 = configurationSection.getInt("age", 0)) > 0) {
                    bossBar.setAge(n2);
                }
                if (configurationSection.isSet("age-lock")) {
                    bossBar.setAgeLock(configurationSection.getBoolean("age-lock"));
                }
                if (lootable instanceof Animals) {
                    object2 = (Animals)lootable;
                    int n3 = configurationSection.getInt("love-mode");
                    if (n3 != 0) {
                        object2.setLoveModeTicks(n3);
                    }
                    if (lootable instanceof Tameable) {
                        object4 = (Tameable)lootable;
                        object4.setTamed(configurationSection.getBoolean("tamed"));
                    }
                }
            }
            if (lootable instanceof Sittable) {
                bossBar = (Sittable)lootable;
                bossBar.setSitting(configurationSection.getBoolean("sitting"));
            }
            if (lootable instanceof Spellcaster) {
                bossBar = (Spellcaster)lootable;
                String string3 = configurationSection.getString("spell");
                if (string3 != null) {
                    bossBar.setSpell((Spellcaster.Spell)Enums.getIfPresent(Spellcaster.Spell.class, (String)string3).or((Object)Spellcaster.Spell.NONE));
                }
            }
            if (lootable instanceof AbstractHorse) {
                ConfigurationSection configurationSection7;
                bossBar = (AbstractHorse)lootable;
                if (configurationSection.isSet("domestication")) {
                    bossBar.setDomestication(configurationSection.getInt("domestication"));
                }
                if (configurationSection.isSet("jump-strength")) {
                    bossBar.setJumpStrength(configurationSection.getDouble("jump-strength"));
                }
                if (configurationSection.isSet("max-domestication")) {
                    bossBar.setMaxDomestication(configurationSection.getInt("max-domestication"));
                }
                if ((configurationSection7 = configurationSection.getConfigurationSection("items")) != null) {
                    object2 = bossBar.getInventory();
                    for (Object object4 : configurationSection7.getKeys(false)) {
                        configurationSection4 = configurationSection7.getConfigurationSection((String)object4);
                        int n4 = configurationSection4.getInt("slot", -1);
                        if (n4 == -1 || (configurationSection3 = XItemStack.deserialize(configurationSection4)) == null) continue;
                        object2.setItem(n4, (ItemStack)configurationSection3);
                    }
                }
                if (lootable instanceof ChestedHorse) {
                    object2 = (ChestedHorse)lootable;
                    boolean bl = configurationSection.getBoolean("has-chest");
                    if (bl) {
                        object2.setCarryingChest(true);
                    }
                }
            }
            if (lootable instanceof Enderman) {
                ItemStack itemStack;
                bossBar = (Enderman)lootable;
                String string4 = configurationSection.getString("carrying");
                if (string4 != null && ((Optional)(object2 = XMaterial.matchXMaterial(string4))).isPresent() && (itemStack = ((XMaterial)((Object)((Optional)object2).get())).parseItem()) != null) {
                    bossBar.setCarriedMaterial(itemStack.getData());
                }
            } else if (lootable instanceof Sheep) {
                bossBar = (Sheep)lootable;
                boolean bl = configurationSection.getBoolean("sheared");
                if (bl) {
                    bossBar.setSheared(true);
                }
            } else if (lootable instanceof Rabbit) {
                bossBar = (Rabbit)lootable;
                bossBar.setRabbitType((Rabbit.Type)Enums.getIfPresent(Rabbit.Type.class, (String)configurationSection.getString("color")).or((Object)Rabbit.Type.WHITE));
            } else if (lootable instanceof Bat) {
                bossBar = (Bat)lootable;
                if (!configurationSection.getBoolean("awake")) {
                    bossBar.setAwake(false);
                }
            } else if (lootable instanceof Wolf) {
                bossBar = (Wolf)lootable;
                bossBar.setAngry(configurationSection.getBoolean("angry"));
                bossBar.setCollarColor((DyeColor)Enums.getIfPresent(DyeColor.class, (String)configurationSection.getString("color")).or((Object)DyeColor.GREEN));
            } else if (lootable instanceof Creeper) {
                bossBar = (Creeper)lootable;
                bossBar.setExplosionRadius(configurationSection.getInt("explosion-radius"));
                bossBar.setMaxFuseTicks(configurationSection.getInt("max-fuse-ticks"));
                bossBar.setPowered(configurationSection.getBoolean("powered"));
            } else if (XMaterial.supports(10) && XMaterial.supports(11)) {
                if (lootable instanceof Llama) {
                    com.google.common.base.Optional optional;
                    bossBar = (Llama)lootable;
                    if (configurationSection.isSet("strength")) {
                        bossBar.setStrength(configurationSection.getInt("strength"));
                    }
                    if ((optional = Enums.getIfPresent(Llama.Color.class, (String)configurationSection.getString("color"))).isPresent()) {
                        bossBar.setColor((Llama.Color)optional.get());
                    }
                } else if (XMaterial.supports(12)) {
                    if (lootable instanceof Parrot) {
                        bossBar = (Parrot)lootable;
                        bossBar.setVariant((Parrot.Variant)Enums.getIfPresent(Parrot.Variant.class, (String)configurationSection.getString("color")).or((Object)Parrot.Variant.RED));
                    }
                    if (XMaterial.supports(13)) {
                        XEntity.thirteen(entity, configurationSection);
                    }
                    if (XMaterial.supports(14)) {
                        XEntity.fourteen(entity, configurationSection);
                    }
                    if (XMaterial.supports(15)) {
                        if (lootable instanceof Bee) {
                            bossBar = (Bee)lootable;
                            bossBar.setAnger(configurationSection.getInt("anger") * 20);
                            bossBar.setHasNectar(configurationSection.getBoolean("nectar"));
                            bossBar.setHasStung(configurationSection.getBoolean("stung"));
                            bossBar.setCannotEnterHiveTicks(configurationSection.getInt("disallow-hive") * 20);
                        }
                        if (XMaterial.supports(16)) {
                            XEntity.sixteen(entity, configurationSection);
                        }
                        if (XMaterial.supports(17)) {
                            XEntity.seventeen(entity, configurationSection);
                        }
                    }
                }
            }
        } else if (entity instanceof EnderSignal) {
            lootable = (EnderSignal)entity;
            lootable.setDespawnTimer(configurationSection.getInt("despawn-timer"));
            lootable.setDropItem(configurationSection.getBoolean("drop-item"));
        } else if (entity instanceof ExperienceOrb) {
            lootable = (ExperienceOrb)entity;
            lootable.setExperience(configurationSection.getInt("exp"));
        } else if (entity instanceof Explosive) {
            lootable = (Explosive)entity;
            lootable.setIsIncendiary(configurationSection.getBoolean("incendiary"));
        } else if (entity instanceof EnderCrystal) {
            lootable = (EnderCrystal)entity;
            lootable.setShowingBottom(configurationSection.getBoolean("show-bottom"));
        }
        return entity;
    }

    private static void fourteen(Entity entity, ConfigurationSection configurationSection) {
        if (entity instanceof Raider) {
            Raider raider = (Raider)entity;
            if (configurationSection.isSet("can-join-raid")) {
                raider.setCanJoinRaid(configurationSection.getBoolean("can-join-raid"));
            }
            if (configurationSection.isSet("is-patrol-leader")) {
                raider.setCanJoinRaid(configurationSection.getBoolean("is-patrol-leader"));
            }
        } else if (entity instanceof Cat) {
            Cat cat = (Cat)entity;
            cat.setCatType((Cat.Type)Enums.getIfPresent(Cat.Type.class, (String)configurationSection.getString("variant")).or((Object)Cat.Type.TABBY));
            cat.setCollarColor((DyeColor)Enums.getIfPresent(DyeColor.class, (String)configurationSection.getString("color")).or((Object)DyeColor.GREEN));
        } else if (entity instanceof Fox) {
            Fox fox = (Fox)entity;
            fox.setCrouching(configurationSection.getBoolean("crouching"));
            fox.setSleeping(configurationSection.getBoolean("sleeping"));
            fox.setFoxType((Fox.Type)Enums.getIfPresent(Fox.Type.class, (String)configurationSection.getString("color")).or((Object)Fox.Type.RED));
        } else if (entity instanceof Panda) {
            Panda panda = (Panda)entity;
            panda.setHiddenGene((Panda.Gene)Enums.getIfPresent(Panda.Gene.class, (String)configurationSection.getString("hidden-gene")).or((Object)Panda.Gene.PLAYFUL));
            panda.setMainGene((Panda.Gene)Enums.getIfPresent(Panda.Gene.class, (String)configurationSection.getString("main-gene")).or((Object)Panda.Gene.NORMAL));
        } else if (entity instanceof MushroomCow) {
            MushroomCow mushroomCow = (MushroomCow)entity;
            mushroomCow.setVariant((MushroomCow.Variant)Enums.getIfPresent(MushroomCow.Variant.class, (String)configurationSection.getString("color")).or((Object)MushroomCow.Variant.RED));
        }
    }

    private static void thirteen(Entity entity, ConfigurationSection configurationSection) {
        if (entity instanceof Husk) {
            Husk husk = (Husk)entity;
            husk.setConversionTime(configurationSection.getInt("conversion-time"));
        } else if (entity instanceof Vex) {
            Vex vex = (Vex)entity;
            vex.setCharging(configurationSection.getBoolean("charging"));
        } else if (entity instanceof PufferFish) {
            PufferFish pufferFish = (PufferFish)entity;
            pufferFish.setPuffState(configurationSection.getInt("puff-state"));
        } else if (entity instanceof TropicalFish) {
            TropicalFish tropicalFish = (TropicalFish)entity;
            tropicalFish.setBodyColor((DyeColor)Enums.getIfPresent(DyeColor.class, (String)configurationSection.getString("color")).or((Object)DyeColor.WHITE));
            tropicalFish.setPattern((TropicalFish.Pattern)Enums.getIfPresent(TropicalFish.Pattern.class, (String)configurationSection.getString("pattern")).or((Object)TropicalFish.Pattern.BETTY));
            tropicalFish.setPatternColor((DyeColor)Enums.getIfPresent(DyeColor.class, (String)configurationSection.getString("pattern-color")).or((Object)DyeColor.WHITE));
        } else if (entity instanceof EnderDragon) {
            EnderDragon enderDragon = (EnderDragon)entity;
            enderDragon.setPhase((EnderDragon.Phase)Enums.getIfPresent(EnderDragon.Phase.class, (String)configurationSection.getString("phase")).or((Object)EnderDragon.Phase.ROAR_BEFORE_ATTACK));
        } else if (entity instanceof Phantom) {
            Phantom phantom = (Phantom)entity;
            phantom.setSize(configurationSection.getInt("size"));
        }
    }

    private static void sixteen(Entity entity, ConfigurationSection configurationSection) {
        if (entity instanceof Hoglin) {
            Hoglin hoglin = (Hoglin)entity;
            hoglin.setConversionTime(configurationSection.getInt("conversation") * 20);
            hoglin.setImmuneToZombification(configurationSection.getBoolean("zombification-immunity"));
            hoglin.setIsAbleToBeHunted(configurationSection.getBoolean("can-be-hunted"));
        } else if (entity instanceof Piglin) {
            Piglin piglin = (Piglin)entity;
            piglin.setConversionTime(configurationSection.getInt("conversation") * 20);
            piglin.setImmuneToZombification(configurationSection.getBoolean("zombification-immunity"));
        } else if (entity instanceof Strider) {
            Strider strider = (Strider)entity;
            strider.setShivering(configurationSection.getBoolean("shivering"));
        }
    }

    private static boolean seventeen(Entity entity, ConfigurationSection configurationSection) {
        if (entity instanceof Axolotl) {
            com.google.common.base.Optional optional;
            Axolotl axolotl = (Axolotl)entity;
            String string = configurationSection.getString("variant");
            if (Strings.isNullOrEmpty((String)string) && (optional = Enums.getIfPresent(Axolotl.Variant.class, (String)string)).isPresent()) {
                axolotl.setVariant((Axolotl.Variant)optional.get());
            }
            if (configurationSection.isSet("playing-dead")) {
                axolotl.setPlayingDead(configurationSection.getBoolean("playing-dead"));
            }
            return true;
        }
        if (entity instanceof Goat) {
            Goat goat = (Goat)entity;
            if (configurationSection.isSet("screaming")) {
                goat.setScreaming(configurationSection.getBoolean("screaming"));
            }
            return true;
        }
        if (entity instanceof GlowSquid) {
            GlowSquid glowSquid = (GlowSquid)entity;
            if (configurationSection.isSet("dark-ticks-remaining")) {
                glowSquid.setDarkTicksRemaining(configurationSection.getInt("dark-ticks-remaining"));
            }
            return true;
        }
        return false;
    }

    public static void editBossBar(BossBar bossBar, ConfigurationSection configurationSection) {
        List list;
        String string;
        String string2;
        String string3 = configurationSection.getString("title");
        if (string3 != null) {
            bossBar.setTitle(ChatColor.translateAlternateColorCodes((char)'&', (String)string3));
        }
        if ((string2 = configurationSection.getString("color")) != null && (string = Enums.getIfPresent(BarColor.class, (String)string2.toUpperCase(Locale.ENGLISH))).isPresent()) {
            bossBar.setColor((BarColor)string.get());
        }
        if ((string = configurationSection.getString("style")) != null && (list = Enums.getIfPresent(BarStyle.class, (String)string.toUpperCase(Locale.ENGLISH))).isPresent()) {
            bossBar.setStyle((BarStyle)list.get());
        }
        if (!(list = configurationSection.getStringList("flags")).isEmpty()) {
            EnumSet<BarFlag> enumSet = EnumSet.noneOf(BarFlag.class);
            for (String string4 : list) {
                BarFlag barFlag = (BarFlag)Enums.getIfPresent(BarFlag.class, (String)string4.toUpperCase(Locale.ENGLISH)).orNull();
                if (barFlag == null) continue;
                enumSet.add(barFlag);
            }
            for (BarFlag barFlag : BarFlag.values()) {
                if (enumSet.contains(barFlag)) {
                    bossBar.addFlag(barFlag);
                    continue;
                }
                bossBar.removeFlag(barFlag);
            }
        }
    }

    static {
        EnumSet<EntityType[]> enumSet = EnumSet.of(EntityType.SKELETON, new EntityType[]{EntityType.ZOMBIE, EntityType.GIANT, EntityType.ZOMBIE_VILLAGER, EntityType.WITHER, EntityType.WITHER_SKELETON, EntityType.ZOMBIE_HORSE});
        if (XMaterial.supports(10)) {
            enumSet.add((EntityType[])EntityType.HUSK);
            enumSet.add((EntityType[])EntityType.STRAY);
            if (XMaterial.supports(11)) {
                enumSet.add((EntityType[])EntityType.SKELETON_HORSE);
                if (XMaterial.supports(13)) {
                    enumSet.add((EntityType[])EntityType.DROWNED);
                    enumSet.add((EntityType[])EntityType.PHANTOM);
                    if (XMaterial.supports(16)) {
                        enumSet.add((EntityType[])EntityType.ZOGLIN);
                        enumSet.add((EntityType[])EntityType.PIGLIN);
                        enumSet.add((EntityType[])EntityType.ZOMBIFIED_PIGLIN);
                    }
                }
            }
        }
        if (!XMaterial.supports(16)) {
            enumSet.add((EntityType[])EntityType.valueOf((String)"PIG_ZOMBIE"));
        }
        UNDEAD = Collections.unmodifiableSet(enumSet);
    }
}

