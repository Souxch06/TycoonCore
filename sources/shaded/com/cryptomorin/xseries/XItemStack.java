/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Enums
 *  com.google.common.base.Optional
 *  com.google.common.base.Strings
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 *  org.bukkit.Bukkit
 *  org.bukkit.Color
 *  org.bukkit.DyeColor
 *  org.bukkit.FireworkEffect
 *  org.bukkit.FireworkEffect$Type
 *  org.bukkit.Location
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.World
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.attribute.AttributeModifier
 *  org.bukkit.attribute.AttributeModifier$Operation
 *  org.bukkit.block.Banner
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.CreatureSpawner
 *  org.bukkit.block.ShulkerBox
 *  org.bukkit.block.banner.Pattern
 *  org.bukkit.block.banner.PatternType
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.MemoryConfiguration
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Axolotl$Variant
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.TropicalFish$Pattern
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ArmorMeta
 *  org.bukkit.inventory.meta.AxolotlBucketMeta
 *  org.bukkit.inventory.meta.BannerMeta
 *  org.bukkit.inventory.meta.BlockStateMeta
 *  org.bukkit.inventory.meta.BookMeta
 *  org.bukkit.inventory.meta.BookMeta$Generation
 *  org.bukkit.inventory.meta.CompassMeta
 *  org.bukkit.inventory.meta.CrossbowMeta
 *  org.bukkit.inventory.meta.Damageable
 *  org.bukkit.inventory.meta.EnchantmentStorageMeta
 *  org.bukkit.inventory.meta.FireworkMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.LeatherArmorMeta
 *  org.bukkit.inventory.meta.MapMeta
 *  org.bukkit.inventory.meta.PotionMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.inventory.meta.SpawnEggMeta
 *  org.bukkit.inventory.meta.SuspiciousStewMeta
 *  org.bukkit.inventory.meta.TropicalFishBucketMeta
 *  org.bukkit.inventory.meta.trim.ArmorTrim
 *  org.bukkit.inventory.meta.trim.TrimMaterial
 *  org.bukkit.inventory.meta.trim.TrimPattern
 *  org.bukkit.map.MapView
 *  org.bukkit.map.MapView$Scale
 *  org.bukkit.material.MaterialData
 *  org.bukkit.material.SpawnEgg
 *  org.bukkit.potion.PotionEffect
 */
package com.cryptomorin.xseries;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XPotion;
import com.cryptomorin.xseries.XTag;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.google.common.base.Enums;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Banner;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.AxolotlBucketMeta;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.inventory.meta.TropicalFishBucketMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.map.MapView;
import org.bukkit.material.MaterialData;
import org.bukkit.material.SpawnEgg;
import org.bukkit.potion.PotionEffect;

public final class XItemStack {
    public static final ItemFlag[] ITEM_FLAGS = ItemFlag.values();
    public static final boolean SUPPORTS_CUSTOM_MODEL_DATA;
    private static final XMaterial DEFAULT_MATERIAL;
    private static final boolean SUPPORTS_POTION_COLOR;

    private XItemStack() {
    }

    private static BlockState safeBlockState(BlockStateMeta blockStateMeta) {
        try {
            return blockStateMeta.getBlockState();
        }
        catch (IllegalStateException illegalStateException) {
            if (illegalStateException.getMessage().toLowerCase(Locale.ENGLISH).contains("missing blockstate")) {
                return null;
            }
            throw illegalStateException;
        }
        catch (ClassCastException classCastException) {
            return null;
        }
    }

    public static void serialize(@Nonnull ItemStack itemStack, @Nonnull ConfigurationSection configurationSection) {
        XItemStack.serialize(itemStack, configurationSection, Function.identity());
    }

    public static void serialize(@Nonnull ItemStack itemStack, @Nonnull ConfigurationSection configurationSection, @Nonnull Function<String, String> function) {
        AttributeModifier attributeModifier;
        Object object2;
        Object object3;
        Object object4;
        ItemMeta itemMeta;
        Objects.requireNonNull(itemStack, "Cannot serialize a null item");
        Objects.requireNonNull(configurationSection, "Cannot serialize item from a null configuration section.");
        configurationSection.set("material", (Object)XMaterial.matchXMaterial(itemStack).name());
        if (itemStack.getAmount() > 1) {
            configurationSection.set("amount", (Object)itemStack.getAmount());
        }
        if ((itemMeta = itemStack.getItemMeta()) == null) {
            return;
        }
        if (XMaterial.supports(13)) {
            if (itemMeta instanceof Damageable && (object4 = (Damageable)itemMeta).hasDamage()) {
                configurationSection.set("damage", (Object)object4.getDamage());
            }
        } else {
            configurationSection.set("damage", (Object)itemStack.getDurability());
        }
        if (itemMeta.hasDisplayName()) {
            configurationSection.set("name", (Object)function.apply(itemMeta.getDisplayName()));
        }
        if (itemMeta.hasLore()) {
            configurationSection.set("lore", itemMeta.getLore().stream().map(function).collect(Collectors.toList()));
        }
        if (XMaterial.supports(14) && itemMeta.hasCustomModelData()) {
            configurationSection.set("custom-model-data", (Object)itemMeta.getCustomModelData());
        }
        if (XMaterial.supports(11) && itemMeta.isUnbreakable()) {
            configurationSection.set("unbreakable", (Object)true);
        }
        object4 = itemMeta.getEnchants().entrySet().iterator();
        while (object4.hasNext()) {
            object3 = (Map.Entry)object4.next();
            String object22 = "enchants." + XEnchantment.matchXEnchantment((Enchantment)object3.getKey()).name();
            configurationSection.set(object22, object3.getValue());
        }
        if (!itemMeta.getItemFlags().isEmpty()) {
            object4 = itemMeta.getItemFlags();
            object3 = new ArrayList(object4.size());
            Iterator iterator2 = object4.iterator();
            while (iterator2.hasNext()) {
                object2 = (ItemFlag)iterator2.next();
                object3.add(object2.name());
            }
            configurationSection.set("flags", object3);
        }
        if (XMaterial.supports(13) && (object4 = itemMeta.getAttributeModifiers()) != null) {
            object3 = object4.entries().iterator();
            while (object3.hasNext()) {
                Map.Entry entry = (Map.Entry)object3.next();
                object2 = "attributes." + ((Attribute)entry.getKey()).name() + '.';
                attributeModifier = (AttributeModifier)entry.getValue();
                configurationSection.set((String)object2 + "id", (Object)attributeModifier.getUniqueId().toString());
                configurationSection.set((String)object2 + "name", (Object)attributeModifier.getName());
                configurationSection.set((String)object2 + "amount", (Object)attributeModifier.getAmount());
                configurationSection.set((String)object2 + "operation", (Object)attributeModifier.getOperation().name());
                if (attributeModifier.getSlot() == null) continue;
                configurationSection.set((String)object2 + "slot", (Object)attributeModifier.getSlot().name());
            }
        }
        if (itemMeta instanceof BlockStateMeta) {
            object4 = XItemStack.safeBlockState((BlockStateMeta)itemMeta);
            if (XMaterial.supports(11) && object4 instanceof ShulkerBox) {
                object3 = (ShulkerBox)object4;
                ConfigurationSection configurationSection2 = configurationSection.createSection("contents");
                int n = 0;
                for (AttributeModifier attributeModifier2 : object3.getInventory().getContents()) {
                    if (attributeModifier2 != null) {
                        XItemStack.serialize((ItemStack)attributeModifier2, configurationSection2.createSection(Integer.toString(n)), function);
                    }
                    ++n;
                }
            } else if (object4 instanceof CreatureSpawner && (object3 = (CreatureSpawner)object4).getSpawnedType() != null) {
                configurationSection.set("spawner", (Object)object3.getSpawnedType().name());
            }
        } else if (itemMeta instanceof EnchantmentStorageMeta) {
            object4 = (EnchantmentStorageMeta)itemMeta;
            for (Map.Entry entry : object4.getStoredEnchants().entrySet()) {
                object2 = "stored-enchants." + XEnchantment.matchXEnchantment((Enchantment)entry.getKey()).name();
                configurationSection.set((String)object2, entry.getValue());
            }
        } else if (itemMeta instanceof SkullMeta) {
            object4 = XSkull.of(itemMeta).getProfileString();
            if (object4 != null) {
                configurationSection.set("skull", object4);
            }
        } else if (itemMeta instanceof BannerMeta) {
            object4 = (BannerMeta)itemMeta;
            object3 = configurationSection.createSection("patterns");
            for (Object object2 : object4.getPatterns()) {
                object3.set(object2.getPattern().name(), (Object)object2.getColor().name());
            }
        } else if (itemMeta instanceof LeatherArmorMeta) {
            object4 = (LeatherArmorMeta)itemMeta;
            object3 = object4.getColor();
            configurationSection.set("color", (Object)(object3.getRed() + ", " + object3.getGreen() + ", " + object3.getBlue()));
        } else if (itemMeta instanceof PotionMeta) {
            if (XMaterial.supports(9)) {
                object4 = (PotionMeta)itemMeta;
                object3 = object4.getCustomEffects();
                ArrayList<String> arrayList = new ArrayList<String>(object3.size());
                object2 = object3.iterator();
                while (object2.hasNext()) {
                    attributeModifier = (PotionEffect)object2.next();
                    arrayList.add(attributeModifier.getType().getName() + ", " + attributeModifier.getDuration() + ", " + attributeModifier.getAmplifier());
                }
                if (!arrayList.isEmpty()) {
                    configurationSection.set("effects", arrayList);
                }
                object2 = object4.getBasePotionType();
                configurationSection.set("base-type", (Object)object2.name());
                configurationSection.set("effects", object4.getCustomEffects().stream().map(potionEffect -> {
                    NamespacedKey namespacedKey = potionEffect.getType().getKey();
                    String string = namespacedKey.getNamespace() + ':' + namespacedKey.getKey();
                    return string + ", " + potionEffect.getDuration() + ", " + potionEffect.getAmplifier();
                }));
                if (SUPPORTS_POTION_COLOR && object4.hasColor()) {
                    configurationSection.set("color", (Object)object4.getColor().asRGB());
                }
            }
        } else if (itemMeta instanceof FireworkMeta) {
            object4 = (FireworkMeta)itemMeta;
            configurationSection.set("power", (Object)object4.getPower());
            int n = 0;
            for (Object object2 : object4.getEffects()) {
                configurationSection.set("firework." + n + ".type", (Object)object2.getType().name());
                attributeModifier = configurationSection.getConfigurationSection("firework." + n);
                attributeModifier.set("flicker", (Object)object2.hasFlicker());
                attributeModifier.set("trail", (Object)object2.hasTrail());
                List list = object2.getColors();
                List list2 = object2.getFadeColors();
                ArrayList<String> arrayList = new ArrayList<String>(list.size());
                ArrayList<String> arrayList2 = new ArrayList<String>(list2.size());
                ConfigurationSection configurationSection3 = attributeModifier.createSection("colors");
                for (Color color : list) {
                    arrayList.add(color.getRed() + ", " + color.getGreen() + ", " + color.getBlue());
                }
                configurationSection3.set("base", arrayList);
                for (Color color : list2) {
                    arrayList2.add(color.getRed() + ", " + color.getGreen() + ", " + color.getBlue());
                }
                configurationSection3.set("fade", arrayList2);
                ++n;
            }
        } else if (itemMeta instanceof BookMeta) {
            object4 = (BookMeta)itemMeta;
            if (object4.getTitle() != null || object4.getAuthor() != null || object4.getGeneration() != null || !object4.getPages().isEmpty()) {
                BookMeta.Generation generation;
                object3 = configurationSection.createSection("book");
                if (object4.getTitle() != null) {
                    object3.set("title", (Object)object4.getTitle());
                }
                if (object4.getAuthor() != null) {
                    object3.set("author", (Object)object4.getAuthor());
                }
                if (XMaterial.supports(9) && (generation = object4.getGeneration()) != null) {
                    object3.set("generation", (Object)object4.getGeneration().toString());
                }
                if (!object4.getPages().isEmpty()) {
                    object3.set("pages", (Object)object4.getPages());
                }
            }
        } else if (itemMeta instanceof MapMeta) {
            object4 = (MapMeta)itemMeta;
            object3 = configurationSection.createSection("map");
            object3.set("scaling", (Object)object4.isScaling());
            if (XMaterial.supports(11)) {
                if (object4.hasLocationName()) {
                    object3.set("location", (Object)object4.getLocationName());
                }
                if (object4.hasColor()) {
                    Color color = object4.getColor();
                    object3.set("color", (Object)(color.getRed() + ", " + color.getGreen() + ", " + color.getBlue()));
                }
            }
            if (XMaterial.supports(14) && object4.hasMapView()) {
                MapView mapView = object4.getMapView();
                object2 = object3.createSection("view");
                object2.set("scale", (Object)mapView.getScale().toString());
                object2.set("world", (Object)mapView.getWorld().getName());
                attributeModifier = object2.createSection("center");
                attributeModifier.set("x", (Object)mapView.getCenterX());
                attributeModifier.set("z", (Object)mapView.getCenterZ());
                object2.set("locked", (Object)mapView.isLocked());
                object2.set("tracking-position", (Object)mapView.isTrackingPosition());
                object2.set("unlimited-tracking", (Object)mapView.isUnlimitedTracking());
            }
        } else {
            if (XMaterial.supports(20) && itemMeta instanceof ArmorMeta && (object4 = (ArmorMeta)itemMeta).hasTrim()) {
                object3 = object4.getTrim();
                ConfigurationSection configurationSection4 = configurationSection.createSection("trim");
                configurationSection4.set("material", (Object)(object3.getMaterial().getKey().getNamespace() + ':' + object3.getMaterial().getKey().getKey()));
                configurationSection4.set("pattern", (Object)(object3.getPattern().getKey().getNamespace() + ':' + object3.getPattern().getKey().getKey()));
            }
            if (XMaterial.supports(17) && itemMeta instanceof AxolotlBucketMeta && (object4 = (AxolotlBucketMeta)itemMeta).hasVariant()) {
                configurationSection.set("color", (Object)object4.getVariant().toString());
            }
            if (XMaterial.supports(16) && itemMeta instanceof CompassMeta) {
                object4 = (CompassMeta)itemMeta;
                object3 = configurationSection.createSection("lodestone");
                object3.set("tracked", (Object)object4.isLodestoneTracked());
                if (object4.hasLodestone()) {
                    Location location = object4.getLodestone();
                    object3.set("location.world", (Object)location.getWorld().getName());
                    object3.set("location.x", (Object)location.getX());
                    object3.set("location.y", (Object)location.getY());
                    object3.set("location.z", (Object)location.getZ());
                }
            }
            if (XMaterial.supports(14)) {
                if (itemMeta instanceof CrossbowMeta) {
                    object4 = (CrossbowMeta)itemMeta;
                    int n = 0;
                    for (Object object2 : object4.getChargedProjectiles()) {
                        XItemStack.serialize((ItemStack)object2, configurationSection.getConfigurationSection("projectiles." + n), function);
                        ++n;
                    }
                } else if (itemMeta instanceof TropicalFishBucketMeta) {
                    object4 = (TropicalFishBucketMeta)itemMeta;
                    configurationSection.set("pattern", (Object)object4.getPattern().name());
                    configurationSection.set("color", (Object)object4.getBodyColor().name());
                    configurationSection.set("pattern-color", (Object)object4.getPatternColor().name());
                } else if (itemMeta instanceof SuspiciousStewMeta) {
                    object4 = (SuspiciousStewMeta)itemMeta;
                    object3 = object4.getCustomEffects();
                    ArrayList<String> arrayList = new ArrayList<String>(object3.size());
                    object2 = object3.iterator();
                    while (object2.hasNext()) {
                        attributeModifier = (PotionEffect)object2.next();
                        arrayList.add(attributeModifier.getType().getName() + ", " + attributeModifier.getDuration() + ", " + attributeModifier.getAmplifier());
                    }
                    configurationSection.set("effects", arrayList);
                }
            }
            if (!XMaterial.supports(13)) {
                if (XMaterial.supports(11)) {
                    if (itemMeta instanceof SpawnEggMeta) {
                        object4 = (SpawnEggMeta)itemMeta;
                        configurationSection.set("creature", (Object)object4.getSpawnedType().getName());
                    }
                } else {
                    object4 = itemStack.getData();
                    if (object4 instanceof SpawnEgg) {
                        SpawnEgg spawnEgg = (SpawnEgg)object4;
                        configurationSection.set("creature", (Object)spawnEgg.getSpawnedType().getName());
                    }
                }
            }
        }
    }

    public static Map<String, Object> serialize(@Nonnull ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "Cannot serialize a null item");
        MemoryConfiguration memoryConfiguration = new MemoryConfiguration();
        XItemStack.serialize(itemStack, (ConfigurationSection)memoryConfiguration);
        return XItemStack.configSectionToMap((ConfigurationSection)memoryConfiguration);
    }

    @Nonnull
    public static ItemStack deserialize(@Nonnull ConfigurationSection configurationSection) {
        return XItemStack.edit(DEFAULT_MATERIAL.parseItem(), configurationSection, Function.identity(), null);
    }

    @Nonnull
    public static ItemStack deserialize(@Nonnull Map<String, Object> map) {
        Objects.requireNonNull(map, "serializedItem cannot be null.");
        return XItemStack.deserialize(XItemStack.mapToConfigSection(map));
    }

    @Nonnull
    public static ItemStack deserialize(@Nonnull ConfigurationSection configurationSection, @Nonnull Function<String, String> function) {
        return XItemStack.deserialize(configurationSection, function, null);
    }

    @Nonnull
    public static ItemStack deserialize(@Nonnull ConfigurationSection configurationSection, @Nonnull Function<String, String> function, @Nullable Consumer<Exception> consumer) {
        return XItemStack.edit(DEFAULT_MATERIAL.parseItem(), configurationSection, function, consumer);
    }

    @Nonnull
    public static ItemStack deserialize(@Nonnull Map<String, Object> map, @Nonnull Function<String, String> function) {
        Objects.requireNonNull(map, "serializedItem cannot be null.");
        Objects.requireNonNull(function, "translator cannot be null.");
        return XItemStack.deserialize(XItemStack.mapToConfigSection(map), function);
    }

    private static int toInt(String string, int n) {
        try {
            return Integer.parseInt(string);
        }
        catch (NumberFormatException numberFormatException) {
            return n;
        }
    }

    private static List<String> split(@Nonnull String string, char c) {
        ArrayList<String> arrayList = new ArrayList<String>(5);
        boolean bl = false;
        boolean bl2 = false;
        int n = string.length();
        int n2 = 0;
        for (int i = 0; i < n; ++i) {
            if (string.charAt(i) == c) {
                if (bl) {
                    arrayList.add(string.substring(n2, i));
                    bl = false;
                    bl2 = true;
                }
                n2 = i + 1;
                continue;
            }
            bl2 = false;
            bl = true;
        }
        if (bl || bl2) {
            arrayList.add(string.substring(n2, n));
        }
        return arrayList;
    }

    private static List<String> splitNewLine(String string) {
        int n = string.length();
        ArrayList<String> arrayList = new ArrayList<String>();
        int n2 = 0;
        int n3 = 0;
        boolean bl = false;
        boolean bl2 = false;
        while (n2 < n) {
            if (string.charAt(n2) == '\n') {
                if (bl) {
                    arrayList.add(string.substring(n3, n2));
                    bl = false;
                    bl2 = true;
                }
                n3 = ++n2;
                continue;
            }
            bl2 = false;
            bl = true;
            ++n2;
        }
        if (bl || bl2) {
            arrayList.add(string.substring(n3, n2));
        }
        return arrayList;
    }

    /*
     * WARNING - void declaration
     */
    @Nonnull
    public static ItemStack edit(@Nonnull ItemStack itemStack, @Nonnull ConfigurationSection configurationSection, @Nonnull Function<String, String> function, @Nullable Consumer<Exception> consumer) {
        List list;
        ConfigurationSection configurationSection2;
        int n;
        Object object;
        Object object322;
        Object object422;
        Object object5;
        int n2;
        Object object6;
        Object object7;
        Objects.requireNonNull(itemStack, "Cannot operate on null ItemStack, considering using an AIR ItemStack instead");
        Objects.requireNonNull(configurationSection, "Cannot deserialize item to a null configuration section.");
        Objects.requireNonNull(function, "Translator function cannot be null");
        String string = configurationSection.getString("material");
        if (!Strings.isNullOrEmpty((String)string)) {
            Optional<XMaterial> optional = XMaterial.matchXMaterial(string);
            if (optional.isPresent()) {
                object7 = optional.get();
            } else {
                object6 = new UnknownMaterialCondition(string);
                if (consumer == null) {
                    throw object6;
                }
                consumer.accept((Exception)object6);
                if (((MaterialCondition)object6).hasSolution()) {
                    object7 = ((UnknownMaterialCondition)object6).solution;
                } else {
                    throw object6;
                }
            }
            if (!object7.isSupported()) {
                object6 = new UnAcceptableMaterialCondition((XMaterial)((Object)object7), UnAcceptableMaterialCondition.Reason.UNSUPPORTED);
                if (consumer == null) {
                    throw object6;
                }
                consumer.accept((Exception)object6);
                if (((MaterialCondition)object6).hasSolution()) {
                    object7 = ((UnAcceptableMaterialCondition)object6).solution;
                } else {
                    throw object6;
                }
            }
            if (XTag.INVENTORY_NOT_DISPLAYABLE.isTagged((XMaterial)((Object)object7))) {
                object6 = new UnAcceptableMaterialCondition((XMaterial)((Object)object7), UnAcceptableMaterialCondition.Reason.NOT_DISPLAYABLE);
                if (consumer == null) {
                    throw object6;
                }
                consumer.accept((Exception)object6);
                if (((MaterialCondition)object6).hasSolution()) {
                    object7 = ((UnAcceptableMaterialCondition)object6).solution;
                } else {
                    throw object6;
                }
            }
            object7.setType(itemStack);
        }
        if ((n2 = configurationSection.getInt("amount")) > 1) {
            itemStack.setAmount(n2);
        }
        object7 = (object6 = itemStack.getItemMeta()) == null ? Bukkit.getItemFactory().getItemMeta(XMaterial.STONE.parseMaterial()) : object6;
        if (XMaterial.supports(13)) {
            int n3;
            if (object7 instanceof Damageable && (n3 = configurationSection.getInt("damage")) > 0) {
                ((Damageable)object7).setDamage(n3);
            }
        } else {
            int n4 = configurationSection.getInt("damage");
            if (n4 > 0) {
                itemStack.setDurability((short)n4);
            }
        }
        if (object7 instanceof SkullMeta) {
            String string2 = configurationSection.getString("skull");
            if (string2 != null) {
                if (string2.isEmpty()) {
                    XSkull.of((ItemMeta)object7).profile(Profileable.detect(string2)).removeProfile();
                } else {
                    XSkull.of((ItemMeta)object7).profile(Profileable.detect(string2)).lenient().apply();
                }
            }
        } else if (object7 instanceof BannerMeta) {
            BannerMeta bannerMeta = (BannerMeta)object7;
            object5 = configurationSection.getConfigurationSection("patterns");
            if (object5 != null) {
                object422 = object5.getKeys(false).iterator();
                while (object422.hasNext()) {
                    String string3 = (String)object422.next();
                    object322 = (PatternType)Enums.getIfPresent(PatternType.class, (String)string3).orNull();
                    if (object322 == null) {
                        object322 = (PatternType)Enums.getIfPresent(PatternType.class, (String)string3.toUpperCase(Locale.ENGLISH)).or((Object)PatternType.BASE);
                    }
                    DyeColor dyeColor = (DyeColor)Enums.getIfPresent(DyeColor.class, (String)object5.getString(string3).toUpperCase(Locale.ENGLISH)).or((Object)DyeColor.WHITE);
                    bannerMeta.addPattern(new Pattern(dyeColor, (PatternType)object322));
                }
            }
        } else if (object7 instanceof LeatherArmorMeta) {
            LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta)object7;
            object5 = configurationSection.getString("color");
            if (object5 != null) {
                leatherArmorMeta.setColor(XItemStack.parseColor((String)object5));
            }
        } else if (object7 instanceof PotionMeta) {
            if (XMaterial.supports(9)) {
                PotionMeta potionMeta = (PotionMeta)object7;
                for (Object object422 : configurationSection.getStringList("effects")) {
                    XPotion.Effect effect = XPotion.parseEffect((String)object422);
                    if (!effect.hasChance()) continue;
                    potionMeta.addCustomEffect(effect.getEffect(), true);
                }
                object5 = configurationSection.getString("base-type");
                if (!Strings.isNullOrEmpty((String)object5)) {
                    XPotion.matchXPotion((String)object5).ifPresent(xPotion -> potionMeta.setBasePotionType(xPotion.getPotionType()));
                }
                if (SUPPORTS_POTION_COLOR && configurationSection.contains("color")) {
                    potionMeta.setColor(Color.fromRGB((int)configurationSection.getInt("color")));
                }
            }
        } else if (object7 instanceof BlockStateMeta) {
            String string4;
            BlockStateMeta blockStateMeta = (BlockStateMeta)object7;
            object5 = XItemStack.safeBlockState(blockStateMeta);
            if (object5 instanceof CreatureSpawner) {
                object422 = (CreatureSpawner)object5;
                string4 = configurationSection.getString("spawner");
                if (!Strings.isNullOrEmpty((String)string4)) {
                    object422.setSpawnedType((EntityType)Enums.getIfPresent(EntityType.class, (String)string4.toUpperCase(Locale.ENGLISH)).orNull());
                    object422.update(true);
                    blockStateMeta.setBlockState((BlockState)object422);
                }
            } else if (XMaterial.supports(11) && object5 instanceof ShulkerBox) {
                object422 = configurationSection.getConfigurationSection("contents");
                if (object422 != null) {
                    string4 = (ShulkerBox)object5;
                    for (String string5 : object422.getKeys(false)) {
                        ItemStack object22 = XItemStack.deserialize(object422.getConfigurationSection(string5));
                        int n3 = XItemStack.toInt(string5, 0);
                        string4.getInventory().setItem(n3, object22);
                    }
                    string4.update(true);
                    blockStateMeta.setBlockState((BlockState)string4);
                }
            } else if (object5 instanceof Banner) {
                object422 = (Banner)object5;
                string4 = configurationSection.getConfigurationSection("patterns");
                if (!XMaterial.supports(14)) {
                    object422.setBaseColor(DyeColor.WHITE);
                }
                if (string4 != null) {
                    for (String string5 : string4.getKeys(false)) {
                        void var13_61;
                        PatternType patternType = (PatternType)Enums.getIfPresent(PatternType.class, (String)string5).orNull();
                        if (patternType == null) {
                            PatternType patternType2 = (PatternType)Enums.getIfPresent(PatternType.class, (String)string5.toUpperCase(Locale.ENGLISH)).or((Object)PatternType.BASE);
                        }
                        DyeColor dyeColor = (DyeColor)Enums.getIfPresent(DyeColor.class, (String)string4.getString(string5).toUpperCase(Locale.ENGLISH)).or((Object)DyeColor.WHITE);
                        object422.addPattern(new Pattern(dyeColor, (PatternType)var13_61));
                    }
                    object422.update(true);
                    blockStateMeta.setBlockState((BlockState)object422);
                }
            }
        } else if (object7 instanceof FireworkMeta) {
            FireworkMeta fireworkMeta = (FireworkMeta)object7;
            fireworkMeta.setPower(configurationSection.getInt("power"));
            object5 = configurationSection.getConfigurationSection("firework");
            if (object5 != null) {
                object422 = FireworkEffect.builder();
                for (Object object322 : object5.getKeys(false)) {
                    ConfigurationSection configurationSection3 = configurationSection.getConfigurationSection("firework." + (String)object322);
                    object422.flicker(configurationSection3.getBoolean("flicker"));
                    object422.trail(configurationSection3.getBoolean("trail"));
                    object422.with((FireworkEffect.Type)Enums.getIfPresent(FireworkEffect.Type.class, (String)configurationSection3.getString("type").toUpperCase(Locale.ENGLISH)).or((Object)FireworkEffect.Type.STAR));
                    ConfigurationSection configurationSection4 = configurationSection3.getConfigurationSection("colors");
                    if (configurationSection4 != null) {
                        List list2 = configurationSection4.getStringList("base");
                        object = new ArrayList(list2.size());
                        for (Object object2 : list2) {
                            object.add(XItemStack.parseColor((String)object2));
                        }
                        object422.withColor((Iterable)object);
                        list2 = configurationSection4.getStringList("fade");
                        object = new ArrayList(list2.size());
                        for (Object object2 : list2) {
                            object.add(XItemStack.parseColor((String)object2));
                        }
                        object422.withFade((Iterable)object);
                    }
                    fireworkMeta.addEffect(object422.build());
                }
            }
        } else if (object7 instanceof BookMeta) {
            BookMeta bookMeta = (BookMeta)object7;
            object5 = configurationSection.getConfigurationSection("book");
            if (object5 != null) {
                bookMeta.setTitle(object5.getString("title"));
                bookMeta.setAuthor(object5.getString("author"));
                bookMeta.setPages(object5.getStringList("pages"));
                if (XMaterial.supports(9) && (object422 = object5.getString("generation")) != null) {
                    BookMeta.Generation generation = (BookMeta.Generation)Enums.getIfPresent(BookMeta.Generation.class, (String)object422).orNull();
                    bookMeta.setGeneration(generation);
                }
            }
        } else if (object7 instanceof MapMeta) {
            MapMeta mapMeta = (MapMeta)object7;
            object5 = configurationSection.getConfigurationSection("map");
            if (object5 != null) {
                World world;
                mapMeta.setScaling(object5.getBoolean("scaling"));
                if (XMaterial.supports(11)) {
                    if (object5.isSet("location")) {
                        mapMeta.setLocationName(object5.getString("location"));
                    }
                    if (object5.isSet("color")) {
                        object422 = XItemStack.parseColor(object5.getString("color"));
                        mapMeta.setColor((Color)object422);
                    }
                }
                if (XMaterial.supports(14) && (object422 = object5.getConfigurationSection("view")) != null && (world = Bukkit.getWorld((String)object422.getString("world"))) != null) {
                    object322 = Bukkit.createMap((World)world);
                    object322.setWorld(world);
                    object322.setScale((MapView.Scale)Enums.getIfPresent(MapView.Scale.class, (String)object422.getString("scale")).or((Object)MapView.Scale.NORMAL));
                    object322.setLocked(object422.getBoolean("locked"));
                    object322.setTrackingPosition(object422.getBoolean("tracking-position"));
                    object322.setUnlimitedTracking(object422.getBoolean("unlimited-tracking"));
                    ConfigurationSection configurationSection4 = object422.getConfigurationSection("center");
                    if (configurationSection4 != null) {
                        object322.setCenterX(configurationSection4.getInt("x"));
                        object322.setCenterZ(configurationSection4.getInt("z"));
                    }
                    mapMeta.setMapView((MapView)object322);
                }
            }
        } else {
            if (XMaterial.supports(20) && object7 instanceof ArmorMeta) {
                ArmorMeta armorMeta = (ArmorMeta)object7;
                if (configurationSection.isSet("trim")) {
                    object5 = configurationSection.getConfigurationSection("trim");
                    object422 = (TrimMaterial)Registry.TRIM_MATERIAL.get(NamespacedKey.fromString((String)object5.getString("material")));
                    TrimPattern trimPattern = (TrimPattern)Registry.TRIM_PATTERN.get(NamespacedKey.fromString((String)object5.getString("pattern")));
                    armorMeta.setTrim(new ArmorTrim((TrimMaterial)object422, trimPattern));
                }
            }
            if (XMaterial.supports(17) && object7 instanceof AxolotlBucketMeta) {
                AxolotlBucketMeta axolotlBucketMeta = (AxolotlBucketMeta)object7;
                object5 = configurationSection.getString("color");
                if (object5 != null) {
                    object422 = (Axolotl.Variant)Enums.getIfPresent(Axolotl.Variant.class, (String)((String)object5).toUpperCase(Locale.ENGLISH)).or((Object)Axolotl.Variant.BLUE);
                    axolotlBucketMeta.setVariant((Axolotl.Variant)object422);
                }
            }
            if (XMaterial.supports(16) && object7 instanceof CompassMeta) {
                CompassMeta compassMeta = (CompassMeta)object7;
                compassMeta.setLodestoneTracked(configurationSection.getBoolean("tracked"));
                object5 = configurationSection.getConfigurationSection("lodestone");
                if (object5 != null) {
                    object422 = Bukkit.getWorld((String)object5.getString("world"));
                    double d = object5.getDouble("x");
                    double d2 = object5.getDouble("y");
                    double d3 = object5.getDouble("z");
                    compassMeta.setLodestone(new Location((World)object422, d, d2, d3));
                }
            }
            if (XMaterial.supports(15) && object7 instanceof SuspiciousStewMeta) {
                SuspiciousStewMeta suspiciousStewMeta = (SuspiciousStewMeta)object7;
                object5 = configurationSection.getStringList("effects").iterator();
                while (object5.hasNext()) {
                    object422 = (String)object5.next();
                    XPotion.Effect effect = XPotion.parseEffect((String)object422);
                    if (!effect.hasChance()) continue;
                    suspiciousStewMeta.addCustomEffect(effect.getEffect(), true);
                }
            }
            if (XMaterial.supports(14)) {
                if (object7 instanceof CrossbowMeta) {
                    CrossbowMeta crossbowMeta = (CrossbowMeta)object7;
                    object5 = configurationSection.getConfigurationSection("projectiles");
                    if (object5 != null) {
                        object422 = object5.getKeys(false).iterator();
                        while (object422.hasNext()) {
                            String string6 = (String)object422.next();
                            object322 = XItemStack.deserialize(configurationSection.getConfigurationSection("projectiles." + string6));
                            crossbowMeta.addChargedProjectile((ItemStack)object322);
                        }
                    }
                } else if (object7 instanceof TropicalFishBucketMeta) {
                    TropicalFishBucketMeta tropicalFishBucketMeta = (TropicalFishBucketMeta)object7;
                    object5 = (DyeColor)Enums.getIfPresent(DyeColor.class, (String)configurationSection.getString("color")).or((Object)DyeColor.WHITE);
                    object422 = (DyeColor)Enums.getIfPresent(DyeColor.class, (String)configurationSection.getString("pattern-color")).or((Object)DyeColor.WHITE);
                    TropicalFish.Pattern pattern = (TropicalFish.Pattern)Enums.getIfPresent(TropicalFish.Pattern.class, (String)configurationSection.getString("pattern")).or((Object)TropicalFish.Pattern.BETTY);
                    tropicalFishBucketMeta.setBodyColor((DyeColor)object5);
                    tropicalFishBucketMeta.setPatternColor((DyeColor)object422);
                    tropicalFishBucketMeta.setPattern(pattern);
                }
            }
            if (!XMaterial.supports(13)) {
                if (XMaterial.supports(11)) {
                    String string7;
                    if (object7 instanceof SpawnEggMeta && !Strings.isNullOrEmpty((String)(string7 = configurationSection.getString("creature")))) {
                        object5 = (SpawnEggMeta)object7;
                        object422 = Enums.getIfPresent(EntityType.class, (String)string7.toUpperCase(Locale.ENGLISH));
                        if (object422.isPresent()) {
                            object5.setSpawnedType((EntityType)object422.get());
                        }
                    }
                } else {
                    MaterialData materialData = itemStack.getData();
                    if (materialData instanceof SpawnEgg && !Strings.isNullOrEmpty((String)(object5 = configurationSection.getString("creature")))) {
                        object422 = (SpawnEgg)materialData;
                        com.google.common.base.Optional optional = Enums.getIfPresent(EntityType.class, (String)((String)object5).toUpperCase(Locale.ENGLISH));
                        if (optional.isPresent()) {
                            object422.setSpawnedType((EntityType)optional.get());
                        }
                        itemStack.setData(materialData);
                    }
                }
            }
        }
        String string8 = configurationSection.getString("name");
        if (!Strings.isNullOrEmpty((String)string8)) {
            object5 = function.apply(string8);
            object7.setDisplayName((String)object5);
        } else if (string8 != null && string8.isEmpty()) {
            object7.setDisplayName(" ");
        }
        if (XMaterial.supports(11) && configurationSection.isSet("unbreakable")) {
            object7.setUnbreakable(configurationSection.getBoolean("unbreakable"));
        }
        if (XMaterial.supports(14) && (n = configurationSection.getInt("custom-model-data")) != 0) {
            object7.setCustomModelData(n);
        }
        if (configurationSection.isSet("lore")) {
            Object object9;
            ArrayList<String> arrayList;
            object422 = configurationSection.getStringList("lore");
            if (!object422.isEmpty()) {
                arrayList = new ArrayList<String>(object422.size());
                object9 = object422.iterator();
                while (object9.hasNext()) {
                    object322 = (String)object9.next();
                    if (((String)object322).isEmpty()) {
                        arrayList.add(" ");
                        continue;
                    }
                    for (String string2 : XItemStack.splitNewLine((String)object322)) {
                        if (string2.isEmpty()) {
                            arrayList.add(" ");
                            continue;
                        }
                        arrayList.add(function.apply(string2));
                    }
                }
            } else {
                object9 = configurationSection.getString("lore");
                arrayList = new ArrayList(10);
                if (!Strings.isNullOrEmpty((String)object9)) {
                    for (String string9 : XItemStack.splitNewLine((String)object9)) {
                        if (string9.isEmpty()) {
                            arrayList.add(" ");
                            continue;
                        }
                        arrayList.add(function.apply(string9));
                    }
                }
            }
            object7.setLore(arrayList);
        }
        if ((configurationSection2 = configurationSection.getConfigurationSection("enchants")) != null) {
            object422 = configurationSection2.getKeys(false).iterator();
            while (object422.hasNext()) {
                String string10 = (String)object422.next();
                object322 = XEnchantment.matchXEnchantment(string10);
                ((Optional)object322).ifPresent(arg_0 -> XItemStack.lambda$edit$2((ItemMeta)object7, configurationSection2, string10, arg_0));
            }
        } else if (configurationSection.getBoolean("glow")) {
            object7.addEnchant(XEnchantment.UNBREAKING.getEnchant(), 1, false);
            object7.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        }
        if ((object422 = configurationSection.getConfigurationSection("stored-enchants")) != null) {
            for (Object object322 : object422.getKeys(false)) {
                Optional<XEnchantment> optional = XEnchantment.matchXEnchantment((String)object322);
                EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta)object7;
                optional.ifPresent(arg_0 -> XItemStack.lambda$edit$3(enchantmentStorageMeta, (ConfigurationSection)object422, (String)object322, arg_0));
            }
        }
        if (!(list = configurationSection.getStringList("flags")).isEmpty()) {
            object322 = list.iterator();
            while (object322.hasNext()) {
                String string11 = (String)object322.next();
                if ((string11 = string11.toUpperCase(Locale.ENGLISH)).equals("ALL")) {
                    object7.addItemFlags(ITEM_FLAGS);
                    break;
                }
                ItemFlag itemFlag = (ItemFlag)Enums.getIfPresent(ItemFlag.class, (String)string11).orNull();
                if (itemFlag == null) continue;
                object7.addItemFlags(new ItemFlag[]{itemFlag});
            }
        } else {
            object322 = configurationSection.getString("flags");
            if (!Strings.isNullOrEmpty((String)object322) && ((String)object322).equalsIgnoreCase("ALL")) {
                object7.addItemFlags(ITEM_FLAGS);
            }
        }
        if (XMaterial.supports(13) && (object322 = configurationSection.getConfigurationSection("attributes")) != null) {
            for (String string3 : object322.getKeys(false)) {
                Object object2;
                Attribute attribute = (Attribute)Enums.getIfPresent(Attribute.class, (String)string3.toUpperCase(Locale.ENGLISH)).orNull();
                if (attribute == null || (object = object322.getConfigurationSection(string3)) == null) continue;
                String string4 = object.getString("id");
                object2 = string4 != null ? UUID.fromString(string4) : UUID.randomUUID();
                EquipmentSlot equipmentSlot = object.getString("slot") != null ? (EquipmentSlot)Enums.getIfPresent(EquipmentSlot.class, (String)object.getString("slot")).or((Object)EquipmentSlot.HAND) : null;
                AttributeModifier attributeModifier = new AttributeModifier((UUID)object2, object.getString("name"), object.getDouble("amount"), (AttributeModifier.Operation)Enums.getIfPresent(AttributeModifier.Operation.class, (String)object.getString("operation")).or((Object)AttributeModifier.Operation.ADD_NUMBER), equipmentSlot);
                object7.addAttributeModifier(attribute, attributeModifier);
            }
        }
        itemStack.setItemMeta((ItemMeta)object7);
        return itemStack;
    }

    @Nonnull
    private static ConfigurationSection mapToConfigSection(@Nonnull Map<?, ?> map) {
        MemoryConfiguration memoryConfiguration = new MemoryConfiguration();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String string = entry.getKey().toString();
            Object object = entry.getValue();
            if (object == null) continue;
            if (object instanceof Map) {
                object = XItemStack.mapToConfigSection((Map)object);
            }
            memoryConfiguration.set(string, object);
        }
        return memoryConfiguration;
    }

    @Nonnull
    private static Map<String, Object> configSectionToMap(@Nonnull ConfigurationSection configurationSection) {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
        for (String string : configurationSection.getKeys(false)) {
            Map<String, Object> map = configurationSection.get(string);
            if (map == null) continue;
            if (map instanceof ConfigurationSection) {
                map = XItemStack.configSectionToMap((ConfigurationSection)map);
            }
            linkedHashMap.put(string, map);
        }
        return linkedHashMap;
    }

    @Nonnull
    public static Color parseColor(@Nullable String string) {
        if (Strings.isNullOrEmpty((String)string)) {
            return Color.BLACK;
        }
        List<String> list = XItemStack.split(string.replace(" ", ""), ',');
        if (list.size() == 3) {
            return Color.fromRGB((int)XItemStack.toInt(list.get(0), 0), (int)XItemStack.toInt(list.get(1), 0), (int)XItemStack.toInt(list.get(2), 0));
        }
        try {
            return Color.fromRGB((int)Integer.parseInt(string));
        }
        catch (NumberFormatException numberFormatException) {
            if (string.startsWith("#")) {
                string = string.substring(1);
            }
            try {
                return Color.fromRGB((int)Integer.parseInt(string, 16));
            }
            catch (NumberFormatException numberFormatException2) {
                return Color.WHITE;
            }
        }
    }

    @Nonnull
    public static List<ItemStack> giveOrDrop(@Nonnull Player player, ItemStack ... itemStackArray) {
        return XItemStack.giveOrDrop(player, false, itemStackArray);
    }

    @Nonnull
    public static List<ItemStack> giveOrDrop(@Nonnull Player player, boolean bl, ItemStack ... itemStackArray) {
        if (itemStackArray == null || itemStackArray.length == 0) {
            return new ArrayList<ItemStack>();
        }
        List<ItemStack> list = XItemStack.addItems((Inventory)player.getInventory(), bl, itemStackArray);
        World world = player.getWorld();
        Location location = player.getLocation();
        for (ItemStack itemStack : list) {
            world.dropItemNaturally(location, itemStack);
        }
        return list;
    }

    public static List<ItemStack> addItems(@Nonnull Inventory inventory, boolean bl, ItemStack ... itemStackArray) {
        return XItemStack.addItems(inventory, bl, null, itemStackArray);
    }

    @Nonnull
    public static List<ItemStack> addItems(@Nonnull Inventory inventory, boolean bl, @Nullable Predicate<Integer> predicate, ItemStack ... itemStackArray) {
        Objects.requireNonNull(inventory, "Cannot add items to null inventory");
        Objects.requireNonNull(itemStackArray, "Cannot add null items to inventory");
        ArrayList<ItemStack> arrayList = new ArrayList<ItemStack>(itemStackArray.length);
        int n = inventory.getStorageContents().length;
        int n2 = 0;
        block0: for (ItemStack itemStack : itemStackArray) {
            int n3 = 0;
            int n4 = bl ? itemStack.getMaxStackSize() : inventory.getMaxStackSize();
            while (true) {
                int n5;
                int n6 = n5 = n3 >= n ? -1 : XItemStack.firstPartial(inventory, itemStack, n3, predicate);
                if (n5 == -1) {
                    if (n2 != -1) {
                        n2 = XItemStack.firstEmpty(inventory, n2, predicate);
                    }
                    if (n2 == -1) {
                        arrayList.add(itemStack);
                        continue block0;
                    }
                    n3 = Integer.MAX_VALUE;
                    int n7 = itemStack.getAmount();
                    if (n7 <= n4) {
                        inventory.setItem(n2, itemStack);
                        continue block0;
                    }
                    ItemStack itemStack2 = itemStack.clone();
                    itemStack2.setAmount(n4);
                    inventory.setItem(n2, itemStack2);
                    itemStack.setAmount(n7 - n4);
                    if (++n2 != n) continue;
                    n2 = -1;
                    continue;
                }
                ItemStack itemStack3 = inventory.getItem(n5);
                int n8 = itemStack.getAmount() + itemStack3.getAmount();
                if (n8 <= n4) {
                    itemStack3.setAmount(n8);
                    inventory.setItem(n5, itemStack3);
                    continue block0;
                }
                itemStack3.setAmount(n4);
                inventory.setItem(n5, itemStack3);
                itemStack.setAmount(n8 - n4);
                n3 = n5 + 1;
            }
        }
        return arrayList;
    }

    public static int firstPartial(@Nonnull Inventory inventory, @Nullable ItemStack itemStack, int n) {
        return XItemStack.firstPartial(inventory, itemStack, n, null);
    }

    public static int firstPartial(@Nonnull Inventory inventory, @Nullable ItemStack itemStack, int n, @Nullable Predicate<Integer> predicate) {
        if (itemStack != null) {
            ItemStack[] itemStackArray = inventory.getStorageContents();
            int n2 = itemStackArray.length;
            if (n < 0 || n >= n2) {
                throw new IndexOutOfBoundsException("Begin Index: " + n + ", Inventory storage content size: " + n2);
            }
            while (n < n2) {
                ItemStack itemStack2;
                if ((predicate == null || predicate.test(n)) && (itemStack2 = itemStackArray[n]) != null && itemStack2.getAmount() < itemStack2.getMaxStackSize() && itemStack2.isSimilar(itemStack)) {
                    return n;
                }
                ++n;
            }
        }
        return -1;
    }

    public static List<ItemStack> stack(@Nonnull Collection<ItemStack> collection) {
        return XItemStack.stack(collection, ItemStack::isSimilar);
    }

    @Nonnull
    public static List<ItemStack> stack(@Nonnull Collection<ItemStack> collection, @Nonnull BiPredicate<ItemStack, ItemStack> biPredicate) {
        Objects.requireNonNull(collection, "Cannot stack null items");
        Objects.requireNonNull(biPredicate, "Similarity check cannot be null");
        ArrayList<ItemStack> arrayList = new ArrayList<ItemStack>(collection.size());
        for (ItemStack itemStack : collection) {
            if (itemStack == null) continue;
            boolean bl = true;
            for (ItemStack itemStack2 : arrayList) {
                if (!biPredicate.test(itemStack, itemStack2)) continue;
                itemStack2.setAmount(itemStack2.getAmount() + itemStack.getAmount());
                bl = false;
                break;
            }
            if (!bl) continue;
            arrayList.add(itemStack.clone());
        }
        return arrayList;
    }

    public static int firstEmpty(@Nonnull Inventory inventory, int n) {
        return XItemStack.firstEmpty(inventory, n, null);
    }

    public static int firstEmpty(@Nonnull Inventory inventory, int n, @Nullable Predicate<Integer> predicate) {
        ItemStack[] itemStackArray = inventory.getStorageContents();
        int n2 = itemStackArray.length;
        if (n < 0 || n >= n2) {
            throw new IndexOutOfBoundsException("Begin Index: " + n + ", Inventory storage content size: " + n2);
        }
        while (n < n2) {
            if ((predicate == null || predicate.test(n)) && itemStackArray[n] == null) {
                return n;
            }
            ++n;
        }
        return -1;
    }

    public static int firstPartialOrEmpty(@Nonnull Inventory inventory, @Nullable ItemStack itemStack, int n) {
        if (itemStack != null) {
            ItemStack[] itemStackArray = inventory.getStorageContents();
            int n2 = itemStackArray.length;
            if (n < 0 || n >= n2) {
                throw new IndexOutOfBoundsException("Begin Index: " + n + ", Size: " + n2);
            }
            while (n < n2) {
                ItemStack itemStack2 = itemStackArray[n];
                if (itemStack2 == null || itemStack2.getAmount() < itemStack2.getMaxStackSize() && itemStack2.isSimilar(itemStack)) {
                    return n;
                }
                ++n;
            }
        }
        return -1;
    }

    private static /* synthetic */ void lambda$edit$3(EnchantmentStorageMeta enchantmentStorageMeta, ConfigurationSection configurationSection, String string, XEnchantment xEnchantment) {
        enchantmentStorageMeta.addStoredEnchant(xEnchantment.getEnchant(), configurationSection.getInt(string), true);
    }

    private static /* synthetic */ void lambda$edit$2(ItemMeta itemMeta, ConfigurationSection configurationSection, String string, XEnchantment xEnchantment) {
        itemMeta.addEnchant(xEnchantment.getEnchant(), configurationSection.getInt(string), true);
    }

    static {
        DEFAULT_MATERIAL = XMaterial.BARRIER;
        boolean bl = false;
        try {
            Class.forName("org.bukkit.inventory.meta.PotionMeta").getMethod("setColor", Color.class);
            bl = true;
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        SUPPORTS_POTION_COLOR = bl;
        bl = false;
        try {
            ItemMeta.class.getMethod("hasCustomModelData", new Class[0]);
            bl = true;
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        SUPPORTS_CUSTOM_MODEL_DATA = bl;
    }

    public static final class UnknownMaterialCondition
    extends MaterialCondition {
        private final String material;

        public UnknownMaterialCondition(String string) {
            super("Unknown material: " + string);
            this.material = string;
        }

        public String getMaterial() {
            return this.material;
        }
    }

    public static final class UnAcceptableMaterialCondition
    extends MaterialCondition {
        private final XMaterial material;
        private final Reason reason;

        public UnAcceptableMaterialCondition(XMaterial xMaterial, Reason reason) {
            super("Unacceptable material: " + xMaterial.name() + " (" + reason.name() + ')');
            this.material = xMaterial;
            this.reason = reason;
        }

        public Reason getReason() {
            return this.reason;
        }

        public XMaterial getMaterial() {
            return this.material;
        }

        public static enum Reason {
            UNSUPPORTED,
            NOT_DISPLAYABLE;

        }
    }

    public static class MaterialCondition
    extends RuntimeException {
        protected XMaterial solution;

        public MaterialCondition(String string) {
            super(string);
        }

        public void setSolution(XMaterial xMaterial) {
            this.solution = xMaterial;
        }

        public boolean hasSolution() {
            return this.solution != null;
        }
    }
}

