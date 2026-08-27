/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  javax.annotation.Nonnull
 */
package com.cryptomorin.xseries.profiles.objects;

import com.cryptomorin.xseries.profiles.PlayerProfiles;
import com.cryptomorin.xseries.profiles.exceptions.InvalidProfileException;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.mojang.authlib.GameProfile;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public enum ProfileInputType {
    TEXTURE_HASH(Pattern.compile("[0-9a-z]{55,70}")){

        @Override
        public GameProfile getProfile(String string) {
            String string2 = PlayerProfiles.encodeBase64("{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + string + "\"}}}");
            return PlayerProfiles.profileFromHashAndBase64(string, string2);
        }
    }
    ,
    TEXTURE_URL(Pattern.compile("(?:https?://)?(?:textures\\.)?minecraft\\.net/texture/(?<hash>" + ProfileInputType.TEXTURE_HASH.pattern + ')', 2)){

        @Override
        public GameProfile getProfile(String string) {
            String string2 = ProfileInputType.extractTextureHash(string);
            return TEXTURE_HASH.getProfile(string2);
        }
    }
    ,
    BASE64(Pattern.compile("[-A-Za-z0-9+/]{100,}={0,3}")){

        @Override
        public GameProfile getProfile(String string) {
            String string2 = PlayerProfiles.decodeBase64(string);
            if (string2 == null) {
                throw new InvalidProfileException("Not a base64 string: " + string);
            }
            String string3 = ProfileInputType.extractTextureHash(string2);
            if (string3 == null) {
                throw new InvalidProfileException("Can't extract texture hash from base64: " + string2);
            }
            return PlayerProfiles.profileFromHashAndBase64(string3, string);
        }
    }
    ,
    UUID(Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")){

        @Override
        public GameProfile getProfile(String string) {
            return Profileable.of(java.util.UUID.fromString(string)).getProfile();
        }
    }
    ,
    USERNAME(Pattern.compile("[A-Za-z0-9_]{1,16}")){

        @Override
        public GameProfile getProfile(String string) {
            return Profileable.username(string).getProfile();
        }
    };

    @ApiStatus.Internal
    public final Pattern pattern;
    private static final ProfileInputType[] VALUES;

    private ProfileInputType(Pattern pattern) {
        this.pattern = pattern;
    }

    public abstract GameProfile getProfile(String var1);

    @Nullable
    public static ProfileInputType typeOf(@Nonnull String string) {
        Objects.requireNonNull(string, "Identifier cannot be null");
        return Arrays.stream(VALUES).filter(profileInputType -> profileInputType.pattern.matcher(string).matches()).findFirst().orElse(null);
    }

    @Nullable
    private static String extractTextureHash(String string) {
        Matcher matcher = ProfileInputType.TEXTURE_HASH.pattern.matcher(string);
        return matcher.find() ? matcher.group() : null;
    }

    static {
        VALUES = ProfileInputType.values();
    }
}

