package com.cryptomorin.xseries.profiles.mojang;

import com.cryptomorin.xseries.profiles.PlayerProfiles;
import com.cryptomorin.xseries.profiles.PlayerUUIDs;
import com.cryptomorin.xseries.profiles.ProfilesCore;
import com.cryptomorin.xseries.profiles.exceptions.MojangAPIException;
import com.cryptomorin.xseries.profiles.exceptions.UnknownPlayerException;
import com.cryptomorin.xseries.profiles.mojang.MinecraftClient;
import com.cryptomorin.xseries.profiles.mojang.MojangProfileCache;
import com.cryptomorin.xseries.profiles.mojang.PlayerProfile;
import com.cryptomorin.xseries.profiles.mojang.ProfileRequestConfiguration;
import com.cryptomorin.xseries.profiles.mojang.RateLimiter;
import com.cryptomorin.xseries.profiles.objects.ProfileInputType;
import com.cryptomorin.xseries.reflection.XReflection;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class MojangAPI {
    private static final MojangProfileCache MOJANG_PROFILE_CACHE = !ProfilesCore.NULLABILITY_RECORD_UPDATE ? new MojangProfileCache.GameProfileCache(ProfilesCore.YggdrasilMinecraftSessionService_insecureProfiles) : new MojangProfileCache.ProfileResultCache(ProfilesCore.YggdrasilMinecraftSessionService_insecureProfiles);
    private static final Cache<UUID, Optional<GameProfile>> INSECURE_PROFILES = CacheBuilder.newBuilder().expireAfterWrite(6L, TimeUnit.HOURS).build();
    private static final boolean REQUIRE_SECURE_PROFILES = false;
    private static final MinecraftClient USERNAME_TO_UUID = new MinecraftClient("GET", "https://api.mojang.com/users/profiles/minecraft/", new RateLimiter(600, Duration.ofMinutes(10L)));
    private static final MinecraftClient USERNAMES_TO_UUIDS = new MinecraftClient("POST", "https://api.minecraftservices.com/minecraft/profile/lookup/bulk/byname", new RateLimiter(600, Duration.ofMinutes(10L)));
    private static final MinecraftClient UUID_TO_PROFILE = new MinecraftClient("GET", "https://sessionserver.mojang.com/session/minecraft/profile/", new RateLimiter(200, Duration.ofMinutes(1L)));

    @Nullable
    public static UUID requestUsernameToUUID(@Nonnull String string) {
        JsonElement jsonElement = USERNAME_TO_UUID.session(null).append(string).request();
        if (jsonElement == null) {
            return null;
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonElement jsonElement2 = jsonObject.get("id");
        if (jsonElement2 == null) {
            throw new RuntimeException("No 'id' field for UUID request for '" + string + "': " + jsonObject);
        }
        return PlayerUUIDs.UUIDFromDashlessString(jsonElement2.getAsString());
    }

    private static GameProfile getCachedProfileByUsername(String string) {
        try {
            Object object = ProfilesCore.GET_PROFILE_BY_NAME.invoke(ProfilesCore.USER_CACHE, string);
            if (object instanceof Optional) {
                object = ((Optional)object).orElse(null);
            }
            GameProfile gameProfile = object == null ? PlayerProfiles.createGameProfile(PlayerUUIDs.IDENTITY_UUID, string) : PlayerProfiles.sanitizeProfile((GameProfile)object);
            ProfilesCore.debug("The cached profile for {} -> {}", string, object);
            return gameProfile;
        }
        catch (Throwable throwable) {
            ProfilesCore.LOGGER.error("Unable to get cached profile by username: {}", (Object)string, (Object)throwable);
            return null;
        }
    }

    public static Optional<GameProfile> getMojangCachedProfileFromUsername(String string) {
        try {
            return MojangAPI.getMojangCachedProfileFromUsername0(string);
        }
        catch (Throwable throwable) {
            throw XReflection.throwCheckedException(throwable);
        }
    }

    private static Optional<GameProfile> getMojangCachedProfileFromUsername0(String string) {
        Optional<GameProfile> optional;
        String string2 = string.toLowerCase(Locale.ROOT);
        Object object = ProfilesCore.UserCache_profilesByName.get(string2);
        if (object != null) {
            if (ProfilesCore.UserCacheEntry_setLastAccess != null && ProfilesCore.UserCache_getNextOperation != null) {
                long l = ProfilesCore.UserCache_getNextOperation.invoke(ProfilesCore.USER_CACHE);
                ProfilesCore.UserCacheEntry_setLastAccess.invoke(object, l);
            }
            optional = Optional.of(ProfilesCore.UserCacheEntry_getProfile.invoke(object));
        } else {
            UUID uUID = PlayerUUIDs.getRealUUIDOfPlayer(string);
            if (uUID == null) {
                return Optional.empty();
            }
            GameProfile gameProfile = PlayerProfiles.createGameProfile(PlayerUUIDs.isOnlineMode() ? uUID : PlayerUUIDs.getOfflineUUID(string), string);
            optional = Optional.of(gameProfile);
            MojangAPI.cacheProfile(gameProfile);
        }
        return optional;
    }

    public static Map<UUID, String> usernamesToUUIDs(@Nonnull Collection<String> collection, @Nullable ProfileRequestConfiguration profileRequestConfiguration) {
        Object object;
        Object object2;
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException("Usernames are null or empty");
        }
        for (String object32 : collection) {
            if (object32 != null && ProfileInputType.USERNAME.pattern.matcher(object32).matches()) continue;
            throw new IllegalArgumentException("One of the requested usernames is invalid: " + object32 + " in " + collection);
        }
        HashMap hashMap = new HashMap(collection.size());
        HashSet<String> hashSet = new HashSet<String>(collection);
        Iterator bl = hashSet.iterator();
        while (bl.hasNext()) {
            object2 = (String)bl.next();
            object = PlayerUUIDs.USERNAME_TO_ONLINE.get(object2);
            if (object == null) continue;
            bl.remove();
            hashMap.put(object, object2);
        }
        if (hashSet.isEmpty()) {
            return hashMap;
        }
        boolean bl2 = PlayerUUIDs.isOnlineMode();
        object2 = Iterables.partition(hashSet, (int)10);
        object = object2.iterator();
        while (object.hasNext()) {
            JsonArray jsonArray;
            List list = (List)object.next();
            try {
                jsonArray = USERNAMES_TO_UUIDS.session(profileRequestConfiguration).body(list).request().getAsJsonArray();
            }
            catch (IOException iOException) {
                throw new MojangAPIException("Failed to request UUIDs for username batch: " + list, iOException);
            }
            for (JsonElement jsonElement : jsonArray) {
                String string;
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                String string2 = jsonObject.get("name").getAsString();
                UUID uUID = PlayerUUIDs.UUIDFromDashlessString(jsonObject.get("id").getAsString());
                UUID uUID2 = PlayerUUIDs.getOfflineUUID(string2);
                PlayerUUIDs.USERNAME_TO_ONLINE.put(string2, uUID);
                PlayerUUIDs.ONLINE_TO_OFFLINE.put(uUID, uUID2);
                PlayerUUIDs.OFFLINE_TO_ONLINE.put(uUID2, uUID);
                if (!ProfilesCore.UserCache_profilesByName.containsKey(string2)) {
                    MojangAPI.cacheProfile(PlayerProfiles.createGameProfile(bl2 ? uUID : uUID2, string2));
                }
                if ((string = hashMap.put(uUID, string2)) == null) continue;
                throw new RuntimeException("Got duplicate usernames for UUID: " + uUID + " (" + string + " -> " + string2 + ')');
            }
        }
        return hashMap;
    }

    @Nonnull
    public static GameProfile getCachedProfileByUUID(UUID uUID) {
        uUID = PlayerUUIDs.isOnlineMode() ? uUID : PlayerUUIDs.ONLINE_TO_OFFLINE.getOrDefault(uUID, uUID);
        try {
            Object object = ProfilesCore.GET_PROFILE_BY_UUID.invoke(ProfilesCore.USER_CACHE, uUID);
            if (object instanceof Optional) {
                object = ((Optional)object).orElse(null);
            }
            ProfilesCore.debug("The cached profile for {} -> {}", uUID, object);
            return object == null ? PlayerProfiles.createNamelessGameProfile(uUID) : PlayerProfiles.sanitizeProfile((GameProfile)object);
        }
        catch (Throwable throwable) {
            ProfilesCore.LOGGER.error("Unable to get cached profile by UUID: {}", (Object)uUID, (Object)throwable);
            return PlayerProfiles.createNamelessGameProfile(uUID);
        }
    }

    private static void cacheProfile(GameProfile gameProfile) {
        try {
            ProfilesCore.CACHE_PROFILE.invoke(ProfilesCore.USER_CACHE, gameProfile);
            ProfilesCore.debug("Profile is now cached: {}", gameProfile);
        }
        catch (Throwable throwable) {
            ProfilesCore.LOGGER.error("Unable to cache profile {}", (Object)gameProfile);
            throwable.printStackTrace();
        }
    }

    @Nonnull
    public static GameProfile getOrFetchProfile(@Nonnull GameProfile gameProfile) {
        Object object;
        Iterable<JsonElement> iterable;
        JsonElement jsonElement;
        UUID uUID;
        if (gameProfile.getName().equals("XSeries")) {
            uUID = gameProfile.getId();
        } else {
            uUID = PlayerUUIDs.getRealUUIDOfPlayer(gameProfile.getName(), gameProfile.getId());
            if (uUID == null) {
                throw new UnknownPlayerException("Player with the given properties not found: " + gameProfile);
            }
        }
        Optional optional = (Optional)INSECURE_PROFILES.getIfPresent((Object)uUID);
        if (optional != null) {
            ProfilesCore.debug("Found cached profile from UUID ({}): {} -> {}", uUID, gameProfile, optional);
            if (optional.isPresent()) {
                return (GameProfile)optional.get();
            }
            throw new UnknownPlayerException("Player with the given properties not found: " + gameProfile);
        }
        Optional<GameProfile> optional2 = MOJANG_PROFILE_CACHE.get(uUID, gameProfile);
        if (optional2 != null) {
            INSECURE_PROFILES.put((Object)uUID, optional2);
            if (optional2.isPresent()) {
                return optional2.get();
            }
            throw new UnknownPlayerException("Player with the given properties not found: " + gameProfile);
        }
        try {
            jsonElement = UUID_TO_PROFILE.session(null).append(PlayerUUIDs.toUndashedUUID(uUID) + "?unsigned=" + true).request();
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
        if (jsonElement == null) {
            INSECURE_PROFILES.put((Object)uUID, Optional.empty());
            MOJANG_PROFILE_CACHE.cache(new PlayerProfile(uUID, gameProfile, null, null));
            throw new UnknownPlayerException("Player with the given properties not found: " + gameProfile);
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        UUID uUID2 = PlayerUUIDs.UUIDFromDashlessString(jsonObject.get("id").getAsString());
        String string = jsonObject.get("name").getAsString();
        GameProfile gameProfile2 = PlayerProfiles.createGameProfile(uUID2, string);
        JsonElement jsonElement2 = jsonObject.get("properties");
        if (jsonElement2 != null) {
            iterable = jsonElement2.getAsJsonArray();
            object = gameProfile2.getProperties();
            for (JsonElement jsonElement3 : iterable) {
                JsonObject jsonObject2 = jsonElement3.getAsJsonObject();
                String string2 = jsonObject2.get("name").getAsString();
                String string3 = jsonObject2.get("value").getAsString();
                JsonElement jsonElement4 = jsonObject2.get("signature");
                Property property = jsonElement4 != null ? new Property(string2, string3, jsonElement4.getAsString()) : new Property(string2, string3);
                object.put((Object)string2, (Object)property);
            }
        }
        iterable = new ArrayList();
        object = jsonObject.get("profileActions");
        if (object != null) {
            for (JsonElement jsonElement3 : ((JsonElement)object).getAsJsonArray()) {
                iterable.add((String)jsonElement3.getAsString());
            }
        }
        gameProfile2 = PlayerProfiles.sanitizeProfile(gameProfile2);
        MojangAPI.cacheProfile(gameProfile2);
        INSECURE_PROFILES.put((Object)uUID, Optional.of(gameProfile2));
        MOJANG_PROFILE_CACHE.cache(new PlayerProfile(uUID, gameProfile, gameProfile2, (List<String>)iterable));
        return gameProfile2;
    }
}

