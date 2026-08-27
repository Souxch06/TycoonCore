/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.properties.Property
 */
package com.cryptomorin.xseries.profiles.objects.transformer;

import com.cryptomorin.xseries.profiles.PlayerProfiles;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public interface ProfileTransformer {
    @NotNull
    public GameProfile transform(@NotNull GameProfile var1);

    @ApiStatus.Internal
    public boolean canBeCached();

    public static ProfileTransformer stackable() {
        return RemoveMetadata.INSTANCE;
    }

    public static ProfileTransformer nonStackable() {
        return MakeNotStackable.INSTANCE;
    }

    public static ProfileTransformer removeMetadata() {
        return RemoveMetadata.INSTANCE;
    }

    public static final class RemoveMetadata
    implements ProfileTransformer {
        private static final RemoveMetadata INSTANCE = new RemoveMetadata();

        @Override
        public GameProfile transform(GameProfile gameProfile) {
            PlayerProfiles.removeTimestamp(gameProfile);
            gameProfile.getProperties().asMap().remove("XSeries");
            return gameProfile;
        }

        @Override
        public boolean canBeCached() {
            return true;
        }
    }

    public static final class MakeNotStackable
    implements ProfileTransformer {
        private static final MakeNotStackable INSTANCE = new MakeNotStackable();
        private static final String PROPERTY_NAME = "XSeriesSeed";
        private static final AtomicLong NEXT_ID = new AtomicLong();

        @Override
        public GameProfile transform(GameProfile gameProfile) {
            String string = System.currentTimeMillis() + "-" + NEXT_ID.getAndIncrement();
            gameProfile.getProperties().put((Object)PROPERTY_NAME, (Object)new Property(PROPERTY_NAME, string));
            return gameProfile;
        }

        @Override
        public boolean canBeCached() {
            return false;
        }
    }
}

