package com.cryptomorin.xseries.profiles.objects;

import com.cryptomorin.xseries.profiles.PlayerProfiles;
import com.cryptomorin.xseries.profiles.PlayerUUIDs;
import com.cryptomorin.xseries.profiles.exceptions.InvalidProfileException;
import com.cryptomorin.xseries.profiles.exceptions.UnknownPlayerException;
import com.cryptomorin.xseries.profiles.mojang.MojangAPI;
import com.cryptomorin.xseries.profiles.mojang.PlayerProfileFetcherThread;
import com.cryptomorin.xseries.profiles.mojang.ProfileRequestConfiguration;
import com.cryptomorin.xseries.profiles.objects.ProfileContainer;
import com.cryptomorin.xseries.profiles.objects.ProfileInputType;
import com.cryptomorin.xseries.profiles.objects.cache.TimedCacheableProfileable;
import com.cryptomorin.xseries.profiles.objects.transformer.ProfileTransformer;
import com.cryptomorin.xseries.profiles.objects.transformer.TransformableProfile;
import com.cryptomorin.xseries.reflection.XReflection;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public interface Profileable {
    @Nullable
    @ApiStatus.Internal
    public @Unmodifiable GameProfile getProfile();

    @Nullable
    @ApiStatus.Internal
    default public GameProfile getDisposableProfile() {
        GameProfile profile = this.getProfile();
        return profile == null ? null : PlayerProfiles.clone(profile);
    }

    default public Profileable transform(ProfileTransformer ... transformers) {
        return new TransformableProfile(this, Arrays.asList(transformers));
    }

    @Nullable
    default public String getProfileValue() {
        GameProfile profile = this.getProfile();
        return PlayerProfiles.getTextureProperty(profile).map(PlayerProfiles::getPropertyValue).orElse(null);
    }

    @Nonnull
    @ApiStatus.Experimental
    public static <C extends Collection<Profileable>> CompletableFuture<C> prepare(@Nonnull C profileables) {
        return Profileable.prepare(profileables, null, null);
    }

    @Nonnull
    @ApiStatus.Experimental
    public static <C extends Collection<Profileable>> CompletableFuture<C> prepare(@Nonnull C profileables, @Nullable ProfileRequestConfiguration config, @Nullable Function<Throwable, Boolean> errorHandler) {
        CompletableFuture initial = CompletableFuture.completedFuture(new HashMap());
        ArrayList<String> usernameRequests = new ArrayList<String>();
        if (!PlayerUUIDs.isOnlineMode()) {
            for (Profileable profileable : profileables) {
                String username = null;
                if (profileable instanceof UsernameProfileable) {
                    username = ((UsernameProfileable)profileable).username;
                } else if (profileable instanceof PlayerProfileable) {
                    username = ((PlayerProfileable)profileable).username;
                } else if (profileable instanceof StringProfileable && ((StringProfileable)profileable).determineType().type == ProfileInputType.USERNAME) {
                    username = ((StringProfileable)profileable).string;
                }
                if (username == null) continue;
                usernameRequests.add(username);
            }
            if (!usernameRequests.isEmpty()) {
                initial = CompletableFuture.supplyAsync(() -> MojangAPI.usernamesToUUIDs(usernameRequests, config), PlayerProfileFetcherThread.EXECUTOR);
            }
        }
        return XReflection.stacktrace(((CompletableFuture)initial.thenCompose(a -> {
            ArrayList<CompletableFuture<GameProfile>> requests = new ArrayList<CompletableFuture<GameProfile>>(profileables.size());
            for (Profileable profileable : profileables) {
                CompletionStage<Object> async = CompletableFuture.supplyAsync(profileable::getProfile, PlayerProfileFetcherThread.EXECUTOR);
                if (errorHandler != null) {
                    async = XReflection.stacktrace(async).exceptionally(ex -> {
                        boolean rethrow = (Boolean)errorHandler.apply(ex);
                        if (rethrow) {
                            throw XReflection.throwCheckedException(ex);
                        }
                        return null;
                    });
                }
                requests.add((CompletableFuture<GameProfile>)async);
            }
            return CompletableFuture.allOf(requests.toArray(new CompletableFuture[0]));
        })).thenApply(a -> profileables));
    }

    public static Profileable username(String username) {
        return new UsernameProfileable(username);
    }

    public static Profileable of(UUID uuid) {
        return new UUIDProfileable(uuid);
    }

    public static Profileable of(GameProfile profile) {
        return new GameProfileProfileable(profile);
    }

    public static Profileable of(OfflinePlayer offlinePlayer) {
        return new PlayerProfileable(offlinePlayer);
    }

    public static Profileable of(BlockState blockState) {
        return new ProfileContainer.BlockStateProfileContainer((Skull)blockState);
    }

    public static Profileable of(Block block) {
        return new ProfileContainer.BlockProfileContainer(block);
    }

    public static Profileable of(ItemStack item) {
        return new ProfileContainer.ItemStackProfileContainer(item);
    }

    public static Profileable of(ItemMeta meta) {
        return new ProfileContainer.ItemMetaProfileContainer((SkullMeta)meta);
    }

    public static Profileable detect(String input) {
        Objects.requireNonNull(input);
        return new StringProfileable(input, null);
    }

    public static Profileable of(ProfileInputType type, String input) {
        Objects.requireNonNull(type, () -> "Cannot profile from a null input type: " + input);
        Objects.requireNonNull(input, () -> "Cannot profile from a null input: " + (Object)((Object)type));
        return new StringProfileable(input, type);
    }

    public static final class UsernameProfileable
    extends TimedCacheableProfileable {
        private final String username;
        private Boolean valid;

        public UsernameProfileable(String string) {
            this.username = Objects.requireNonNull(string);
        }

        @Override
        protected GameProfile getProfile0() {
            if (this.valid == null) {
                this.valid = ProfileInputType.USERNAME.pattern.matcher(this.username).matches();
            }
            if (!this.valid.booleanValue()) {
                throw new InvalidProfileException("Invalid username: '" + this.username + '\'');
            }
            Optional<GameProfile> optional = MojangAPI.getMojangCachedProfileFromUsername(this.username);
            if (!optional.isPresent()) {
                throw new UnknownPlayerException("Cannot find player named '" + this.username + '\'');
            }
            GameProfile gameProfile = optional.get();
            if (PlayerProfiles.hasTextures(gameProfile)) {
                return gameProfile;
            }
            return MojangAPI.getOrFetchProfile(gameProfile);
        }
    }

    public static final class PlayerProfileable
    extends TimedCacheableProfileable {
        @Nullable
        private final String username;
        @Nonnull
        private final UUID id;

        public PlayerProfileable(OfflinePlayer offlinePlayer) {
            Objects.requireNonNull(offlinePlayer);
            this.username = offlinePlayer.getName();
            this.id = offlinePlayer.getUniqueId();
        }

        @Override
        protected GameProfile getProfile0() {
            if (Strings.isNullOrEmpty((String)this.username)) {
                return new UUIDProfileable(this.id).getProfile();
            }
            return new UsernameProfileable(this.username).getProfile();
        }
    }

    public static final class StringProfileable
    extends TimedCacheableProfileable {
        private final String string;
        @Nullable
        private ProfileInputType type;

        public StringProfileable(String string, @Nullable ProfileInputType profileInputType) {
            this.string = Objects.requireNonNull(string);
            this.type = profileInputType;
        }

        private StringProfileable determineType() {
            if (this.type == null) {
                this.type = ProfileInputType.typeOf(this.string);
            }
            return this;
        }

        @Override
        protected GameProfile getProfile0() {
            this.determineType();
            if (this.type == null) {
                throw new InvalidProfileException("Unknown skull string value: " + this.string);
            }
            return this.type.getProfile(this.string);
        }
    }

    public static final class UUIDProfileable
    extends TimedCacheableProfileable {
        private final UUID id;

        public UUIDProfileable(UUID uUID) {
            this.id = Objects.requireNonNull(uUID, "UUID cannot be null");
        }

        @Override
        protected GameProfile getProfile0() {
            GameProfile gameProfile = MojangAPI.getCachedProfileByUUID(this.id);
            if (PlayerProfiles.hasTextures(gameProfile)) {
                return gameProfile;
            }
            return MojangAPI.getOrFetchProfile(gameProfile);
        }
    }

    public static final class GameProfileProfileable
    extends TimedCacheableProfileable {
        private final GameProfile profile;

        public GameProfileProfileable(GameProfile gameProfile) {
            this.profile = Objects.requireNonNull(gameProfile);
        }

        @Override
        protected GameProfile getProfile0() {
            if (PlayerProfiles.hasTextures(this.profile)) {
                return this.profile;
            }
            return (PlayerUUIDs.isOnlineMode() ? new UUIDProfileable(this.profile.getId()) : new UsernameProfileable(this.profile.getName())).getProfile();
        }
    }
}

