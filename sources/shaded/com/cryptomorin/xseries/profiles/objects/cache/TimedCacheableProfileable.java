/*
 * Decompiled with CFR 0.152.
 */
package com.cryptomorin.xseries.profiles.objects.cache;

import com.cryptomorin.xseries.profiles.objects.cache.CacheableProfileable;
import java.time.Duration;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public abstract class TimedCacheableProfileable
extends CacheableProfileable {
    private long lastUpdate;

    @NotNull
    protected Duration expiresAfter() {
        return Duration.ofHours(6L);
    }

    @Override
    public final boolean hasExpired(boolean bl) {
        if (super.hasExpired(bl)) {
            return true;
        }
        if (this.cache == null && this.lastError == null) {
            return true;
        }
        Duration duration = this.expiresAfter();
        if (duration.isZero()) {
            return false;
        }
        long l = System.currentTimeMillis();
        if (this.lastUpdate == 0L) {
            if (bl) {
                this.lastUpdate = l;
            }
            return true;
        }
        long l2 = l - this.lastUpdate;
        if (l2 >= duration.toMillis()) {
            if (bl) {
                this.lastUpdate = l;
            }
            return true;
        }
        return false;
    }
}

