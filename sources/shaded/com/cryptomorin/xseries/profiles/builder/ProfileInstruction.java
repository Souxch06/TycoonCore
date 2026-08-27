package com.cryptomorin.xseries.profiles.builder;

import com.cryptomorin.xseries.profiles.ProfilesCore;
import com.cryptomorin.xseries.profiles.builder.ProfileFallback;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.exceptions.InvalidProfileException;
import com.cryptomorin.xseries.profiles.exceptions.ProfileChangeException;
import com.cryptomorin.xseries.profiles.exceptions.ProfileException;
import com.cryptomorin.xseries.profiles.mojang.PlayerProfileFetcherThread;
import com.cryptomorin.xseries.profiles.mojang.ProfileRequestConfiguration;
import com.cryptomorin.xseries.profiles.objects.ProfileContainer;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public final class ProfileInstruction<T>
implements Profileable {
    private final ProfileContainer<T> profileContainer;
    private Profileable profileable;
    private final List<Profileable> fallbacks = new ArrayList<Profileable>();
    private Consumer<ProfileFallback<T>> onFallback;
    private ProfileRequestConfiguration profileRequestConfiguration;
    private boolean lenient = false;

    protected ProfileInstruction(ProfileContainer<T> profileContainer) {
        this.profileContainer = profileContainer;
    }

    public T removeProfile() {
        this.profileContainer.setProfile(null);
        return this.profileContainer.getObject();
    }

    @ApiStatus.Experimental
    public ProfileInstruction<T> profileRequestConfiguration(ProfileRequestConfiguration profileRequestConfiguration) {
        this.profileRequestConfiguration = profileRequestConfiguration;
        return this;
    }

    public ProfileInstruction<T> lenient() {
        this.lenient = true;
        return this;
    }

    @Override
    @Nullable
    public GameProfile getProfile() {
        return this.profileContainer.getProfile();
    }

    @Nullable
    public String getProfileString() {
        return this.profileContainer.getProfileValue();
    }

    public ProfileInstruction<T> profile(Profileable profileable) {
        this.profileable = profileable;
        return this;
    }

    public ProfileInstruction<T> fallback(Profileable ... profileableArray) {
        this.fallbacks.addAll(Arrays.asList(profileableArray));
        return this;
    }

    public ProfileInstruction<T> onFallback(Consumer<ProfileFallback<T>> consumer) {
        this.onFallback = consumer;
        return this;
    }

    public ProfileInstruction<T> onFallback(Runnable runnable) {
        this.onFallback = profileFallback -> runnable.run();
        return this;
    }

    public T apply() {
        Objects.requireNonNull(this.profileable, "No profile was set");
        Throwable throwable = null;
        ArrayList<Profileable> arrayList = new ArrayList<Profileable>(2 + this.fallbacks.size());
        arrayList.add(this.profileable);
        arrayList.addAll(this.fallbacks);
        if (this.lenient) {
            arrayList.add(XSkull.getDefaultProfile());
        }
        boolean bl = false;
        boolean bl2 = false;
        for (Profileable object : arrayList) {
            try {
                GameProfile profileException = object.getDisposableProfile();
                if (profileException != null) {
                    this.profileContainer.setProfile(profileException);
                    bl = true;
                    break;
                }
                if (throwable == null) {
                    throwable = new ProfileChangeException("Could not set the profile for " + this.profileContainer);
                }
                throwable.addSuppressed(new InvalidProfileException("Profile doesn't have a value: " + object));
                bl2 = true;
            }
            catch (ProfileException profileException) {
                if (throwable == null) {
                    throwable = new ProfileChangeException("Could not set the profile for " + this.profileContainer);
                }
                throwable.addSuppressed(profileException);
                bl2 = true;
            }
        }
        if (throwable != null) {
            if (bl || this.lenient) {
                ProfilesCore.debug("apply() silenced exception {}", throwable);
            } else {
                throw throwable;
            }
        }
        Iterator<Object> iterator2 = this.profileContainer.getObject();
        if (bl2 && this.onFallback != null) {
            ProfileFallback profileFallback = new ProfileFallback(this, iterator2, (ProfileChangeException)throwable);
            this.onFallback.accept(profileFallback);
            iterator2 = profileFallback.getObject();
        }
        return (T)iterator2;
    }

    public CompletableFuture<T> applyAsync() {
        return CompletableFuture.supplyAsync(this::apply, PlayerProfileFetcherThread.EXECUTOR);
    }
}

