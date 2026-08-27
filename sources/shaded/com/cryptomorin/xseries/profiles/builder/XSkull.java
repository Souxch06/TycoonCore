/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Multimap
 *  com.mojang.authlib.GameProfile
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Skull
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 */
package com.cryptomorin.xseries.profiles.builder;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.PlayerProfiles;
import com.cryptomorin.xseries.profiles.builder.ProfileInstruction;
import com.cryptomorin.xseries.profiles.objects.ProfileContainer;
import com.cryptomorin.xseries.profiles.objects.ProfileInputType;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class XSkull {
    private static final GameProfile DEFAULT_PROFILE = PlayerProfiles.signXSeries(ProfileInputType.BASE64.getProfile("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzEwNTkxZTY5MDllNmEyODFiMzcxODM2ZTQ2MmQ2N2EyYzc4ZmEwOTUyZTkxMGYzMmI0MWEyNmM0OGMxNzU3YyJ9fX0="));

    public static ProfileInstruction<ItemStack> createItem() {
        return XSkull.of(XMaterial.PLAYER_HEAD.parseItem());
    }

    public static ProfileInstruction<ItemStack> of(ItemStack itemStack) {
        return new ProfileInstruction<ItemStack>(new ProfileContainer.ItemStackProfileContainer(itemStack));
    }

    public static ProfileInstruction<ItemMeta> of(ItemMeta itemMeta) {
        return new ProfileInstruction<ItemMeta>(new ProfileContainer.ItemMetaProfileContainer((SkullMeta)itemMeta));
    }

    public static ProfileInstruction<Block> of(Block block) {
        return new ProfileInstruction<Block>(new ProfileContainer.BlockProfileContainer(block));
    }

    public static ProfileInstruction<Skull> of(BlockState blockState) {
        return new ProfileInstruction<Skull>(new ProfileContainer.BlockStateProfileContainer((Skull)blockState));
    }

    protected static Profileable getDefaultProfile() {
        GameProfile gameProfile = PlayerProfiles.createGameProfile(DEFAULT_PROFILE.getId(), DEFAULT_PROFILE.getName());
        gameProfile.getProperties().putAll((Multimap)DEFAULT_PROFILE.getProperties());
        return Profileable.of(gameProfile);
    }
}

