/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  javax.annotation.Nonnull
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Skull
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 */
package com.cryptomorin.xseries.profiles.objects;

import com.cryptomorin.xseries.profiles.ProfilesCore;
import com.cryptomorin.xseries.profiles.exceptions.InvalidProfileContainerException;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.mojang.authlib.GameProfile;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public abstract class ProfileContainer<T>
implements Profileable {
    @Nonnull
    public abstract void setProfile(@Nullable GameProfile var1);

    public abstract T getObject();

    public final String toString() {
        return this.getClass().getSimpleName() + '[' + this.getObject() + ']';
    }

    public static final class BlockStateProfileContainer
    extends ProfileContainer<Skull> {
        private final Skull state;

        public BlockStateProfileContainer(Skull skull) {
            this.state = Objects.requireNonNull(skull);
        }

        @Override
        public void setProfile(GameProfile gameProfile) {
            try {
                ProfilesCore.CRAFT_SKULL_PROFILE_SETTER.invoke(this.state, gameProfile);
            }
            catch (Throwable throwable) {
                throw new RuntimeException("Unable to set profile " + gameProfile + " to " + this.state, throwable);
            }
        }

        @Override
        public Skull getObject() {
            return this.state;
        }

        @Override
        public GameProfile getProfile() {
            try {
                return ProfilesCore.CRAFT_SKULL_PROFILE_GETTER.invoke(this.state);
            }
            catch (Throwable throwable) {
                throw new RuntimeException("Unable to get profile fr om blockstate: " + this.state, throwable);
            }
        }
    }

    public static final class BlockProfileContainer
    extends ProfileContainer<Block> {
        private final Block block;

        public BlockProfileContainer(Block block) {
            this.block = Objects.requireNonNull(block);
        }

        private Skull getBlockState() {
            BlockState blockState = this.block.getState();
            if (!(blockState instanceof Skull)) {
                throw new InvalidProfileContainerException("Block can't contain texture: " + this.block);
            }
            return (Skull)blockState;
        }

        @Override
        public void setProfile(GameProfile gameProfile) {
            Skull skull = this.getBlockState();
            new BlockStateProfileContainer(skull).setProfile(gameProfile);
            skull.update(true);
        }

        @Override
        public Block getObject() {
            return this.block;
        }

        @Override
        public GameProfile getProfile() {
            return new BlockStateProfileContainer(this.getBlockState()).getProfile();
        }
    }

    public static final class ItemMetaProfileContainer
    extends ProfileContainer<ItemMeta> {
        private final ItemMeta meta;

        public ItemMetaProfileContainer(SkullMeta skullMeta) {
            this.meta = (ItemMeta)Objects.requireNonNull(skullMeta);
        }

        @Override
        public void setProfile(GameProfile gameProfile) {
            try {
                ProfilesCore.CRAFT_META_SKULL_PROFILE_SETTER.invoke(this.meta, gameProfile);
            }
            catch (Throwable throwable) {
                throw new RuntimeException("Unable to set profile " + gameProfile + " to " + this.meta, throwable);
            }
        }

        @Override
        public ItemMeta getObject() {
            return this.meta;
        }

        @Override
        public GameProfile getProfile() {
            try {
                return ProfilesCore.CRAFT_META_SKULL_PROFILE_GETTER.invoke((SkullMeta)this.meta);
            }
            catch (Throwable throwable) {
                throw new RuntimeException("Failed to get profile from item meta: " + this.meta, throwable);
            }
        }
    }

    public static final class ItemStackProfileContainer
    extends ProfileContainer<ItemStack> {
        private final ItemStack itemStack;

        public ItemStackProfileContainer(ItemStack itemStack) {
            this.itemStack = Objects.requireNonNull(itemStack);
        }

        private ItemMetaProfileContainer getMetaContainer(ItemMeta itemMeta) {
            if (!(itemMeta instanceof SkullMeta)) {
                throw new InvalidProfileContainerException("Item can't contain texture: " + this.itemStack);
            }
            return new ItemMetaProfileContainer((SkullMeta)itemMeta);
        }

        @Override
        public void setProfile(GameProfile gameProfile) {
            ItemMeta itemMeta = this.itemStack.getItemMeta();
            this.getMetaContainer(itemMeta).setProfile(gameProfile);
            this.itemStack.setItemMeta(itemMeta);
        }

        @Override
        public ItemStack getObject() {
            return this.itemStack;
        }

        @Override
        public GameProfile getProfile() {
            return this.getMetaContainer(this.itemStack.getItemMeta()).getProfile();
        }
    }
}

