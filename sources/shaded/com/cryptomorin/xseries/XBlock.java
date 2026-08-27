/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 *  org.bukkit.DyeColor
 *  org.bukkit.Material
 *  org.bukkit.SkullType
 *  org.bukkit.TreeSpecies
 *  org.bukkit.block.Banner
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Skull
 *  org.bukkit.block.data.Ageable
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.Directional
 *  org.bukkit.block.data.Levelled
 *  org.bukkit.block.data.Lightable
 *  org.bukkit.block.data.Openable
 *  org.bukkit.block.data.Powerable
 *  org.bukkit.block.data.Rotatable
 *  org.bukkit.block.data.type.Cake
 *  org.bukkit.block.data.type.EndPortalFrame
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.material.Cake
 *  org.bukkit.material.Colorable
 *  org.bukkit.material.Directional
 *  org.bukkit.material.MaterialData
 *  org.bukkit.material.Openable
 *  org.bukkit.material.Wool
 */
package com.cryptomorin.xseries;

import com.cryptomorin.xseries.XMaterial;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.TreeSpecies;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Cake;
import org.bukkit.material.Colorable;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;

public final class XBlock {
    public static final Set<XMaterial> CROPS = Collections.unmodifiableSet(EnumSet.of(XMaterial.CARROT, new XMaterial[]{XMaterial.CARROTS, XMaterial.POTATO, XMaterial.POTATOES, XMaterial.NETHER_WART, XMaterial.PUMPKIN_SEEDS, XMaterial.WHEAT_SEEDS, XMaterial.WHEAT, XMaterial.MELON_SEEDS, XMaterial.BEETROOT_SEEDS, XMaterial.BEETROOTS, XMaterial.SUGAR_CANE, XMaterial.BAMBOO_SAPLING, XMaterial.BAMBOO, XMaterial.CHORUS_PLANT, XMaterial.KELP, XMaterial.KELP_PLANT, XMaterial.SEA_PICKLE, XMaterial.BROWN_MUSHROOM, XMaterial.RED_MUSHROOM, XMaterial.MELON_STEM, XMaterial.PUMPKIN_STEM, XMaterial.COCOA, XMaterial.COCOA_BEANS}));
    public static final Set<XMaterial> DANGEROUS = Collections.unmodifiableSet(EnumSet.of(XMaterial.MAGMA_BLOCK, XMaterial.LAVA, XMaterial.CAMPFIRE, XMaterial.FIRE, XMaterial.SOUL_FIRE));
    public static final byte CAKE_SLICES = 6;
    private static final boolean ISFLAT = XMaterial.supports(13);
    private static final Map<XMaterial, XMaterial> ITEM_TO_BLOCK = new EnumMap<XMaterial, XMaterial>(XMaterial.class);

    private XBlock() {
    }

    public static boolean isLit(Block block) {
        if (ISFLAT) {
            if (!(block.getBlockData() instanceof Lightable)) {
                return false;
            }
            Lightable lightable = (Lightable)block.getBlockData();
            return lightable.isLit();
        }
        return XBlock.isMaterial(block, BlockMaterial.REDSTONE_LAMP_ON, BlockMaterial.REDSTONE_TORCH_ON, BlockMaterial.BURNING_FURNACE);
    }

    public static boolean isContainer(@Nullable Block block) {
        return block != null && block.getState() instanceof InventoryHolder;
    }

    public static void setLit(Block block, boolean bl) {
        if (ISFLAT) {
            if (!(block.getBlockData() instanceof Lightable)) {
                return;
            }
            BlockData blockData = block.getBlockData();
            Lightable lightable = (Lightable)blockData;
            lightable.setLit(bl);
            block.setBlockData(blockData, false);
            return;
        }
        String string = block.getType().name();
        if (string.endsWith("FURNACE")) {
            block.setType(BlockMaterial.BURNING_FURNACE.material);
        } else if (string.startsWith("REDSTONE_LAMP")) {
            block.setType(BlockMaterial.REDSTONE_LAMP_ON.material);
        } else {
            block.setType(BlockMaterial.REDSTONE_TORCH_ON.material);
        }
    }

    public static boolean isCrop(XMaterial xMaterial) {
        return CROPS.contains((Object)xMaterial);
    }

    public static boolean isDangerous(XMaterial xMaterial) {
        return DANGEROUS.contains((Object)xMaterial);
    }

    public static DyeColor getColor(Block block) {
        if (ISFLAT) {
            if (!(block.getBlockData() instanceof Colorable)) {
                return null;
            }
            Colorable colorable = (Colorable)block.getBlockData();
            return colorable.getColor();
        }
        BlockState blockState = block.getState();
        MaterialData materialData = blockState.getData();
        if (materialData instanceof Wool) {
            Wool wool = (Wool)materialData;
            return wool.getColor();
        }
        return null;
    }

    public static boolean isCake(@Nullable Material material) {
        return material == Material.CAKE || material == BlockMaterial.CAKE_BLOCK.material;
    }

    public static boolean isWheat(@Nullable Material material) {
        return material == Material.WHEAT || material == BlockMaterial.CROPS.material;
    }

    public static boolean isSugarCane(@Nullable Material material) {
        return material == Material.SUGAR_CANE || material == BlockMaterial.SUGAR_CANE_BLOCK.material;
    }

    public static boolean isBeetroot(@Nullable Material material) {
        return material == Material.BEETROOT || material == Material.BEETROOTS || material == BlockMaterial.BEETROOT_BLOCK.material;
    }

    public static boolean isNetherWart(@Nullable Material material) {
        return material == Material.NETHER_WART || material == BlockMaterial.NETHER_WARTS.material;
    }

    public static boolean isCarrot(@Nullable Material material) {
        return material == Material.CARROT || material == Material.CARROTS;
    }

    public static boolean isMelon(@Nullable Material material) {
        return material == Material.MELON || material == Material.MELON_SLICE || material == BlockMaterial.MELON_BLOCK.material;
    }

    public static boolean isPotato(@Nullable Material material) {
        return material == Material.POTATO || material == Material.POTATOES;
    }

    public static BlockFace getDirection(Block block) {
        if (ISFLAT) {
            if (!(block.getBlockData() instanceof org.bukkit.block.data.Directional)) {
                return BlockFace.SELF;
            }
            org.bukkit.block.data.Directional directional = (org.bukkit.block.data.Directional)block.getBlockData();
            return directional.getFacing();
        }
        BlockState blockState = block.getState();
        MaterialData materialData = blockState.getData();
        if (materialData instanceof Directional) {
            return ((Directional)materialData).getFacing();
        }
        return BlockFace.SELF;
    }

    public static boolean setDirection(Block block, BlockFace blockFace) {
        if (ISFLAT) {
            if (!(block.getBlockData() instanceof org.bukkit.block.data.Directional)) {
                return false;
            }
            BlockData blockData = block.getBlockData();
            org.bukkit.block.data.Directional directional = (org.bukkit.block.data.Directional)blockData;
            directional.setFacing(blockFace);
            block.setBlockData(blockData, false);
            return true;
        }
        BlockState blockState = block.getState();
        MaterialData materialData = blockState.getData();
        if (materialData instanceof Directional) {
            if (XMaterial.matchXMaterial(block.getType()) == XMaterial.LADDER) {
                blockFace = blockFace.getOppositeFace();
            }
            ((Directional)materialData).setFacingDirection(blockFace);
            blockState.update(true);
            return true;
        }
        return false;
    }

    public static boolean setType(@Nonnull Block block, @Nullable XMaterial xMaterial, boolean bl) {
        LegacyMaterial.Handling handling;
        Object object;
        Material material;
        XMaterial xMaterial2;
        Objects.requireNonNull(block, "Cannot set type of null block");
        if (xMaterial == null) {
            xMaterial = XMaterial.AIR;
        }
        if ((xMaterial2 = ITEM_TO_BLOCK.get((Object)xMaterial)) != null) {
            xMaterial = xMaterial2;
        }
        if ((material = xMaterial.parseMaterial()) == null) {
            return false;
        }
        String string = material.name();
        SkullType skullType = XBlock.getSkullType(xMaterial);
        if (!ISFLAT && (string.equals("SKULL_ITEM") || skullType != null)) {
            material = Material.valueOf((String)"SKULL");
        }
        block.setType(material, bl);
        if (ISFLAT) {
            return false;
        }
        if (string.endsWith("_ITEM")) {
            object = string.substring(0, string.length() - "_ITEM".length());
            handling = Objects.requireNonNull(Material.getMaterial((String)object), () -> XBlock.lambda$setType$0(string, (String)object));
            block.setType((Material)handling, bl);
        } else if (string.contains("CAKE")) {
            object = Material.getMaterial((String)"CAKE_BLOCK");
            block.setType((Material)object, bl);
        }
        object = LegacyMaterial.getMaterial(string);
        if (object == LegacyMaterial.BANNER) {
            block.setType(LegacyMaterial.STANDING_BANNER.material, bl);
        }
        handling = object == null ? null : ((LegacyMaterial)((Object)object)).handling;
        BlockState blockState = block.getState();
        boolean bl2 = false;
        if (handling == LegacyMaterial.Handling.COLORABLE) {
            if (blockState instanceof Banner) {
                int n;
                Banner banner = (Banner)blockState;
                String string2 = xMaterial.name();
                String string3 = string2.substring(0, n = string2.indexOf(95));
                if (string3.equals("LIGHT")) {
                    string3 = string2.substring(0, "LIGHT_".length() + 4);
                }
                banner.setBaseColor(DyeColor.valueOf((String)string3));
            } else {
                blockState.setRawData(xMaterial.getData());
            }
            bl2 = true;
        } else if (handling == LegacyMaterial.Handling.WOOD_SPECIES) {
            TreeSpecies treeSpecies;
            String string4;
            String string5 = xMaterial.name();
            int n = string5.indexOf(95);
            if (n < 0) {
                return false;
            }
            switch (string4 = string5.substring(0, n)) {
                case "OAK": {
                    treeSpecies = TreeSpecies.GENERIC;
                    break;
                }
                case "DARK": {
                    treeSpecies = TreeSpecies.DARK_OAK;
                    break;
                }
                case "SPRUCE": {
                    treeSpecies = TreeSpecies.REDWOOD;
                    break;
                }
                default: {
                    try {
                        treeSpecies = TreeSpecies.valueOf((String)string4);
                        break;
                    }
                    catch (IllegalArgumentException illegalArgumentException) {
                        throw new AssertionError((Object)("Unknown material " + object + " for wood species"));
                    }
                }
            }
            boolean bl3 = false;
            switch (((Enum)object).ordinal()) {
                case 9: 
                case 11: {
                    blockState.setRawData(treeSpecies.getData());
                    bl2 = true;
                    break;
                }
                case 12: 
                case 14: {
                    bl3 = true;
                }
                case 13: 
                case 15: {
                    switch (treeSpecies) {
                        case GENERIC: 
                        case REDWOOD: 
                        case BIRCH: 
                        case JUNGLE: {
                            if (!bl3) {
                                throw new AssertionError((Object)("Invalid tree species " + treeSpecies + " for block type" + object + ", use block type 2 instead"));
                            }
                            break;
                        }
                        case ACACIA: 
                        case DARK_OAK: {
                            if (bl3) {
                                throw new AssertionError((Object)("Invalid tree species " + treeSpecies + " for block type 2 " + object + ", use block type instead"));
                            }
                            break;
                        }
                    }
                    blockState.setRawData((byte)(blockState.getRawData() & 0xC | treeSpecies.getData() & 3));
                    bl2 = true;
                    break;
                }
                case 10: 
                case 16: {
                    blockState.setRawData((byte)(blockState.getRawData() & 8 | treeSpecies.getData()));
                    bl2 = true;
                    break;
                }
                default: {
                    throw new AssertionError((Object)("Unknown block type " + object + " for tree species: " + treeSpecies));
                }
            }
        } else if (xMaterial.getData() != 0) {
            if (skullType != null) {
                boolean bl4 = xMaterial.name().contains("WALL");
                blockState.setRawData((byte)(!bl4 ? 1 : 0));
            } else {
                blockState.setRawData(xMaterial.getData());
            }
            bl2 = true;
        }
        if (skullType != null) {
            Skull skull = (Skull)blockState;
            skull.setSkullType(skullType);
            bl2 = true;
        }
        if (bl2) {
            blockState.update(true, bl);
        }
        return bl2;
    }

    public static SkullType getSkullType(XMaterial xMaterial) {
        switch (xMaterial) {
            case PLAYER_HEAD: 
            case PLAYER_WALL_HEAD: {
                return SkullType.PLAYER;
            }
            case DRAGON_HEAD: 
            case DRAGON_WALL_HEAD: {
                return SkullType.DRAGON;
            }
            case ZOMBIE_HEAD: 
            case ZOMBIE_WALL_HEAD: {
                return SkullType.ZOMBIE;
            }
            case CREEPER_HEAD: 
            case CREEPER_WALL_HEAD: {
                return SkullType.CREEPER;
            }
            case SKELETON_SKULL: 
            case SKELETON_WALL_SKULL: {
                return SkullType.SKELETON;
            }
            case WITHER_SKELETON_SKULL: 
            case WITHER_SKELETON_WALL_SKULL: {
                return SkullType.WITHER;
            }
            case PIGLIN_HEAD: 
            case PIGLIN_WALL_HEAD: {
                return SkullType.PIGLIN;
            }
        }
        return null;
    }

    public static boolean setType(@Nonnull Block block, @Nullable XMaterial xMaterial) {
        return XBlock.setType(block, xMaterial, true);
    }

    public static int getAge(Block block) {
        if (ISFLAT) {
            if (!(block.getBlockData() instanceof Ageable)) {
                return 0;
            }
            Ageable ageable = (Ageable)block.getBlockData();
            return ageable.getAge();
        }
        BlockState blockState = block.getState();
        MaterialData materialData = blockState.getData();
        return materialData.getData();
    }

    public static void setAge(Block block, int n) {
        MaterialData materialData;
        BlockState blockState;
        if (ISFLAT) {
            if (!(block.getBlockData() instanceof Ageable)) {
                return;
            }
            blockState = block.getBlockData();
            materialData = (Ageable)blockState;
            materialData.setAge(n);
            block.setBlockData((BlockData)blockState, false);
        }
        blockState = block.getState();
        materialData = blockState.getData();
        materialData.setData((byte)n);
        blockState.update(true);
    }

    public static boolean setColor(Block block, DyeColor dyeColor) {
        if (ISFLAT) {
            String string = block.getType().name();
            int n = string.indexOf(95);
            if (n == -1) {
                return false;
            }
            String string2 = string.substring(n);
            Material material = Material.getMaterial((String)(dyeColor.name() + '_' + string2));
            if (material == null) {
                return false;
            }
            block.setType(material);
            return true;
        }
        BlockState blockState = block.getState();
        blockState.setRawData(dyeColor.getWoolData());
        blockState.update(true);
        return false;
    }

    public static boolean setFluidLevel(Block block, int n) {
        if (ISFLAT) {
            if (!(block.getBlockData() instanceof Levelled)) {
                return false;
            }
            BlockData blockData = block.getBlockData();
            Levelled levelled = (Levelled)blockData;
            levelled.setLevel(n);
            block.setBlockData(blockData, false);
            return true;
        }
        BlockState blockState = block.getState();
        MaterialData materialData = blockState.getData();
        materialData.setData((byte)n);
        blockState.update(true);
        return false;
    }

    public static int getFluidLevel(Block block) {
        if (ISFLAT) {
            if (!(block.getBlockData() instanceof Levelled)) {
                return -1;
            }
            Levelled levelled = (Levelled)block.getBlockData();
            return levelled.getLevel();
        }
        BlockState blockState = block.getState();
        MaterialData materialData = blockState.getData();
        return materialData.getData();
    }

    public static boolean isWaterStationary(Block block) {
        return ISFLAT ? XBlock.getFluidLevel(block) < 7 : block.getType() == BlockMaterial.STATIONARY_WATER.material;
    }

    public static boolean isWater(Material material) {
        return material == Material.WATER || material == BlockMaterial.STATIONARY_WATER.material;
    }

    public static boolean isLava(Material material) {
        return material == Material.LAVA || material == BlockMaterial.STATIONARY_LAVA.material;
    }

    @Deprecated
    public static boolean isOneOf(Block block, Collection<String> collection) {
        if (collection == null || collection.isEmpty()) {
            return false;
        }
        String string = block.getType().name();
        XMaterial xMaterial = XMaterial.matchXMaterial(block.getType());
        for (String string2 : collection) {
            Optional<XMaterial> optional;
            String string3 = string2.toUpperCase(Locale.ENGLISH);
            if (!(string3.startsWith("CONTAINS:") ? string.contains(string2 = XMaterial.format(string3.substring(9))) : (string3.startsWith("REGEX:") ? string.matches(string2 = string2.substring(6)) : (optional = XMaterial.matchXMaterial(string2)).isPresent() && XBlock.isSimilar(block, optional.get())))) continue;
            return true;
        }
        return false;
    }

    public static void setCakeSlices(Block block, int n) {
        if (!XBlock.isCake(block.getType())) {
            throw new IllegalArgumentException("Block is not a cake: " + block.getType());
        }
        if (ISFLAT) {
            BlockData blockData = block.getBlockData();
            org.bukkit.block.data.type.Cake cake = (org.bukkit.block.data.type.Cake)blockData;
            int n2 = cake.getMaximumBites() - (cake.getBites() + n);
            if (n2 > 0) {
                cake.setBites(n2);
                block.setBlockData(blockData);
            } else {
                block.breakNaturally();
            }
            return;
        }
        BlockState blockState = block.getState();
        Cake cake = (Cake)blockState.getData();
        if (n > 0) {
            cake.setSlicesRemaining(n);
            blockState.update(true);
        } else {
            block.breakNaturally();
        }
    }

    public static int addCakeSlices(Block block, int n) {
        if (!XBlock.isCake(block.getType())) {
            throw new IllegalArgumentException("Block is not a cake: " + block.getType());
        }
        if (ISFLAT) {
            BlockData blockData = block.getBlockData();
            org.bukkit.block.data.type.Cake cake = (org.bukkit.block.data.type.Cake)blockData;
            int n2 = cake.getBites() - n;
            int n3 = cake.getMaximumBites() - n2;
            if (n3 > 0) {
                cake.setBites(n2);
                block.setBlockData(blockData);
                return n3;
            }
            block.breakNaturally();
            return 0;
        }
        BlockState blockState = block.getState();
        Cake cake = (Cake)blockState.getData();
        int n4 = cake.getSlicesRemaining() + n;
        if (n4 > 0) {
            cake.setSlicesRemaining(n4);
            blockState.update(true);
            return n4;
        }
        block.breakNaturally();
        return 0;
    }

    public static void setEnderPearlOnFrame(Block block, boolean bl) {
        BlockState blockState = block.getState();
        if (ISFLAT) {
            BlockData blockData = blockState.getBlockData();
            EndPortalFrame endPortalFrame = (EndPortalFrame)blockData;
            endPortalFrame.setEye(bl);
            blockState.setBlockData(blockData);
        } else {
            blockState.setRawData((byte)(bl ? 4 : 0));
        }
        blockState.update(true);
    }

    public static boolean isSimilar(Block block, XMaterial xMaterial) {
        Material material = block.getType();
        if (xMaterial == XMaterial.matchXMaterial(material)) {
            return true;
        }
        switch (xMaterial) {
            case CAKE: {
                return XBlock.isCake(material);
            }
            case NETHER_WART: {
                return XBlock.isNetherWart(material);
            }
            case MELON: 
            case MELON_SLICE: {
                return XBlock.isMelon(material);
            }
            case CARROT: 
            case CARROTS: {
                return XBlock.isCarrot(material);
            }
            case POTATO: 
            case POTATOES: {
                return XBlock.isPotato(material);
            }
            case WHEAT: 
            case WHEAT_SEEDS: {
                return XBlock.isWheat(material);
            }
            case BEETROOT: 
            case BEETROOT_SEEDS: 
            case BEETROOTS: {
                return XBlock.isBeetroot(material);
            }
            case SUGAR_CANE: {
                return XBlock.isSugarCane(material);
            }
            case WATER: {
                return XBlock.isWater(material);
            }
            case LAVA: {
                return XBlock.isLava(material);
            }
            case AIR: 
            case CAVE_AIR: 
            case VOID_AIR: {
                return XBlock.isAir(material);
            }
        }
        return false;
    }

    public static boolean isAir(@Nullable Material material) {
        if (ISFLAT) {
            switch (material) {
                case AIR: 
                case CAVE_AIR: 
                case VOID_AIR: {
                    return true;
                }
            }
            return false;
        }
        return material == Material.AIR;
    }

    public static boolean isPowered(Block block) {
        if (ISFLAT) {
            if (!(block.getBlockData() instanceof Powerable)) {
                return false;
            }
            Powerable powerable = (Powerable)block.getBlockData();
            return powerable.isPowered();
        }
        String string = block.getType().name();
        if (string.startsWith("REDSTONE_COMPARATOR")) {
            return block.getType() == BlockMaterial.REDSTONE_COMPARATOR_ON.material;
        }
        return false;
    }

    public static void setPowered(Block block, boolean bl) {
        if (ISFLAT) {
            if (!(block.getBlockData() instanceof Powerable)) {
                return;
            }
            BlockData blockData = block.getBlockData();
            Powerable powerable = (Powerable)blockData;
            powerable.setPowered(bl);
            block.setBlockData(blockData, false);
            return;
        }
        String string = block.getType().name();
        if (string.startsWith("REDSTONE_COMPARATOR")) {
            block.setType(BlockMaterial.REDSTONE_COMPARATOR_ON.material);
        }
    }

    public static boolean isOpen(Block block) {
        if (ISFLAT) {
            if (!(block.getBlockData() instanceof Openable)) {
                return false;
            }
            Openable openable = (Openable)block.getBlockData();
            return openable.isOpen();
        }
        BlockState blockState = block.getState();
        if (!(blockState instanceof org.bukkit.material.Openable)) {
            return false;
        }
        org.bukkit.material.Openable openable = (org.bukkit.material.Openable)blockState.getData();
        return openable.isOpen();
    }

    public static void setOpened(Block block, boolean bl) {
        if (ISFLAT) {
            if (!(block.getBlockData() instanceof Openable)) {
                return;
            }
            BlockData blockData = block.getBlockData();
            Openable openable = (Openable)blockData;
            openable.setOpen(bl);
            block.setBlockData(blockData, false);
            return;
        }
        BlockState blockState = block.getState();
        if (!(blockState instanceof org.bukkit.material.Openable)) {
            return;
        }
        org.bukkit.material.Openable openable = (org.bukkit.material.Openable)blockState.getData();
        openable.setOpen(bl);
        blockState.setData((MaterialData)openable);
        blockState.update(true, true);
    }

    public static BlockFace getRotation(Block block) {
        if (ISFLAT) {
            BlockData blockData = block.getBlockData();
            if (blockData instanceof Rotatable) {
                return ((Rotatable)blockData).getRotation();
            }
            if (blockData instanceof org.bukkit.block.data.Directional) {
                return ((org.bukkit.block.data.Directional)blockData).getFacing();
            }
        } else {
            BlockState blockState = block.getState();
            if (blockState instanceof Skull) {
                return ((Skull)blockState).getRotation();
            }
            MaterialData materialData = blockState.getData();
            if (materialData instanceof Directional) {
                return ((Directional)materialData).getFacing();
            }
        }
        return null;
    }

    public static void setRotation(Block block, BlockFace blockFace) {
        if (ISFLAT) {
            BlockData blockData = block.getBlockData();
            if (blockData instanceof Rotatable) {
                ((Rotatable)blockData).setRotation(blockFace);
            } else if (blockData instanceof org.bukkit.block.data.Directional) {
                ((org.bukkit.block.data.Directional)blockData).setFacing(blockFace);
            }
            block.setBlockData(blockData, false);
        } else {
            BlockState blockState = block.getState();
            if (blockState instanceof Skull) {
                ((Skull)blockState).setRotation(blockFace);
            } else {
                MaterialData materialData = blockState.getData();
                if (!(materialData instanceof Directional)) {
                    return;
                }
                Directional directional = (Directional)materialData;
                directional.setFacingDirection(blockFace);
            }
            blockState.update(true, true);
        }
    }

    private static boolean isMaterial(Block block, BlockMaterial ... blockMaterialArray) {
        Material material = block.getType();
        for (BlockMaterial blockMaterial : blockMaterialArray) {
            if (material != blockMaterial.material) continue;
            return true;
        }
        return false;
    }

    private static /* synthetic */ String lambda$setType$0(String string, String string2) {
        return "Could not find block material for item '" + string + "' as '" + string2 + '\'';
    }

    static {
        ITEM_TO_BLOCK.put(XMaterial.MELON_SLICE, XMaterial.MELON_STEM);
        ITEM_TO_BLOCK.put(XMaterial.MELON_SEEDS, XMaterial.MELON_STEM);
        ITEM_TO_BLOCK.put(XMaterial.CARROT_ON_A_STICK, XMaterial.CARROTS);
        ITEM_TO_BLOCK.put(XMaterial.GOLDEN_CARROT, XMaterial.CARROTS);
        ITEM_TO_BLOCK.put(XMaterial.CARROT, XMaterial.CARROTS);
        ITEM_TO_BLOCK.put(XMaterial.POTATO, XMaterial.POTATOES);
        ITEM_TO_BLOCK.put(XMaterial.BAKED_POTATO, XMaterial.POTATOES);
        ITEM_TO_BLOCK.put(XMaterial.POISONOUS_POTATO, XMaterial.POTATOES);
        ITEM_TO_BLOCK.put(XMaterial.PUMPKIN_SEEDS, XMaterial.PUMPKIN_STEM);
        ITEM_TO_BLOCK.put(XMaterial.PUMPKIN_PIE, XMaterial.PUMPKIN);
    }

    public static enum BlockMaterial {
        CAKE_BLOCK,
        CROPS,
        SUGAR_CANE_BLOCK,
        BEETROOT_BLOCK,
        NETHER_WARTS,
        MELON_BLOCK,
        BURNING_FURNACE,
        STATIONARY_WATER,
        STATIONARY_LAVA,
        REDSTONE_LAMP_ON,
        REDSTONE_LAMP_OFF,
        REDSTONE_TORCH_ON,
        REDSTONE_TORCH_OFF,
        REDSTONE_COMPARATOR_ON,
        REDSTONE_COMPARATOR_OFF;

        @Nullable
        private final Material material = Material.getMaterial((String)this.name());
    }

    private static enum LegacyMaterial {
        STANDING_BANNER(Handling.COLORABLE),
        WALL_BANNER(Handling.COLORABLE),
        BANNER(Handling.COLORABLE),
        CARPET(Handling.COLORABLE),
        WOOL(Handling.COLORABLE),
        STAINED_CLAY(Handling.COLORABLE),
        STAINED_GLASS(Handling.COLORABLE),
        STAINED_GLASS_PANE(Handling.COLORABLE),
        THIN_GLASS(Handling.COLORABLE),
        WOOD(Handling.WOOD_SPECIES),
        WOOD_STEP(Handling.WOOD_SPECIES),
        WOOD_DOUBLE_STEP(Handling.WOOD_SPECIES),
        LEAVES(Handling.WOOD_SPECIES),
        LEAVES_2(Handling.WOOD_SPECIES),
        LOG(Handling.WOOD_SPECIES),
        LOG_2(Handling.WOOD_SPECIES),
        SAPLING(Handling.WOOD_SPECIES);

        private static final Map<String, LegacyMaterial> LOOKUP;
        private final Material material = Material.getMaterial((String)this.name());
        private final Handling handling;

        private LegacyMaterial(Handling handling) {
            this.handling = handling;
        }

        private static LegacyMaterial getMaterial(String string) {
            return LOOKUP.get(string);
        }

        static {
            LOOKUP = new HashMap<String, LegacyMaterial>();
            for (LegacyMaterial legacyMaterial : LegacyMaterial.values()) {
                LOOKUP.put(legacyMaterial.name(), legacyMaterial);
            }
        }

        private static enum Handling {
            COLORABLE,
            WOOD_SPECIES;

        }
    }
}

