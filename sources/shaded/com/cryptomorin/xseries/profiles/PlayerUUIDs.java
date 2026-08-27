package com.cryptomorin.xseries.profiles;

import com.cryptomorin.xseries.profiles.ProfilesCore;
import com.cryptomorin.xseries.profiles.mojang.MojangAPI;
import com.google.common.base.Strings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class PlayerUUIDs {
    public static final UUID IDENTITY_UUID = new UUID(0L, 0L);
    private static final Pattern UUID_NO_DASHES = Pattern.compile("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})");
    public static final Map<UUID, UUID> OFFLINE_TO_ONLINE = new HashMap<UUID, UUID>();
    public static final Map<UUID, UUID> ONLINE_TO_OFFLINE = new HashMap<UUID, UUID>();
    public static final Map<String, UUID> USERNAME_TO_ONLINE = new HashMap<String, UUID>();

    public static UUID UUIDFromDashlessString(String string) {
        Matcher matcher = UUID_NO_DASHES.matcher(string);
        try {
            return UUID.fromString(matcher.replaceFirst("$1-$2-$3-$4-$5"));
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException("Cannot convert from dashless UUID: " + string, illegalArgumentException);
        }
    }

    public static String toUndashedUUID(UUID uUID) {
        return uUID.toString().replace("-", "");
    }

    @Nonnull
    public static UUID getOfflineUUID(@Nonnull String string) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + string).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isOnlineMode() {
        return Bukkit.getOnlineMode();
    }

    @Nullable
    public static UUID getRealUUIDOfPlayer(@Nonnull String string) {
        boolean bl;
        if (Strings.isNullOrEmpty((String)string)) {
            throw new IllegalArgumentException("Username is null or empty: " + string);
        }
        UUID uUID = PlayerUUIDs.getOfflineUUID(string);
        UUID uUID2 = USERNAME_TO_ONLINE.get(string);
        boolean bl2 = bl = uUID2 != null;
        if (uUID2 == null) {
            try {
                uUID2 = MojangAPI.requestUsernameToUUID(string);
                if (uUID2 == null) {
                    ProfilesCore.debug("Caching null for {} ({}) because it doesn't exist.", string, uUID);
                    uUID2 = IDENTITY_UUID;
                } else {
                    ONLINE_TO_OFFLINE.put(uUID2, uUID);
                }
                OFFLINE_TO_ONLINE.put(uUID, uUID2);
                USERNAME_TO_ONLINE.put(string, uUID2);
            }
            catch (IOException iOException) {
                throw new RuntimeException(iOException);
            }
        }
        if (uUID2 == IDENTITY_UUID) {
            ProfilesCore.debug("Providing null UUID for {} because it doesn't exist.", string);
            uUID2 = null;
        } else {
            ProfilesCore.debug((bl ? "Cached " : "") + "Real UUID for {} ({}) is {}", string, uUID, uUID2);
        }
        return uUID2;
    }

    @Nullable
    public static UUID getRealUUIDOfPlayer(@Nonnull String string, @Nonnull UUID uUID) {
        boolean bl;
        Objects.requireNonNull(uUID);
        if (Strings.isNullOrEmpty((String)string)) {
            throw new IllegalArgumentException("Username is null or empty: " + string);
        }
        if (PlayerUUIDs.isOnlineMode()) {
            return uUID;
        }
        UUID uUID2 = OFFLINE_TO_ONLINE.get(uUID);
        boolean bl2 = bl = uUID2 != null;
        if (uUID2 == null) {
            try {
                uUID2 = MojangAPI.requestUsernameToUUID(string);
                if (uUID2 == null) {
                    ProfilesCore.debug("Caching null for {} ({}) because it doesn't exist.", string, uUID);
                    uUID2 = IDENTITY_UUID;
                } else {
                    ONLINE_TO_OFFLINE.put(uUID2, uUID);
                }
                OFFLINE_TO_ONLINE.put(uUID, uUID2);
                USERNAME_TO_ONLINE.put(string, uUID2);
            }
            catch (IOException iOException) {
                throw new RuntimeException(iOException);
            }
        }
        if (uUID2 == IDENTITY_UUID) {
            ProfilesCore.debug("Providing null UUID for {} ({}) because it doesn't exist.", string, uUID);
            uUID2 = null;
        } else {
            ProfilesCore.debug((bl ? "Cached " : "") + "Real UUID for {} ({}) is {}", string, uUID, uUID2);
        }
        UUID uUID3 = PlayerUUIDs.getOfflineUUID(string);
        if (!uUID.equals(uUID3) && !uUID.equals(uUID2)) {
            throw new RuntimeException("The provided UUID (" + uUID + ") for '" + string + "' doesn't match the offline UUID (" + uUID3 + ") or the real UUID (" + uUID2 + ')');
        }
        return uUID2;
    }
}

