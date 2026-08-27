/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 */
package com.cryptomorin.xseries.profiles.mojang;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class PlayerProfile {
    public final UUID realUUID;
    public final GameProfile requestedGameProfile;
    public final GameProfile fetchedGameProfile;
    public final List<String> profileActions;

    PlayerProfile(UUID uUID, GameProfile gameProfile, GameProfile gameProfile2, List<String> list) {
        this.realUUID = uUID;
        this.requestedGameProfile = gameProfile;
        this.fetchedGameProfile = gameProfile2;
        this.profileActions = list;
    }

    boolean exists() {
        return this.fetchedGameProfile != null;
    }
}

