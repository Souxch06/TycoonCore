/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 */
package com.cryptomorin.xseries.profiles.objects.transformer;

import com.cryptomorin.xseries.profiles.PlayerProfiles;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.cryptomorin.xseries.profiles.objects.transformer.ProfileTransformer;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public final class TransformableProfile
implements Profileable {
    private final Profileable profileable;
    private final TransformationSequence transformers;

    public TransformableProfile(Profileable profileable, List<ProfileTransformer> list) {
        this.profileable = profileable;
        this.transformers = new TransformationSequence(list);
    }

    @Override
    public Profileable transform(ProfileTransformer ... profileTransformerArray) {
        ArrayList<ProfileTransformer> arrayList = new ArrayList<ProfileTransformer>(this.transformers.transformers.length + profileTransformerArray.length);
        arrayList.addAll(Arrays.stream(this.transformers.transformers).map(transformedProfileCache -> ((TransformationSequence.TransformedProfileCache)transformedProfileCache).transformer).collect(Collectors.toList()));
        arrayList.addAll(Arrays.asList(profileTransformerArray));
        return new TransformableProfile(this.profileable, arrayList);
    }

    @Override
    public GameProfile getProfile() {
        this.transformers.profile = this.profileable.getProfile();
        if (this.transformers.profile == null) {
            return null;
        }
        for (TransformationSequence.TransformedProfileCache transformedProfileCache : this.transformers.transformers) {
            transformedProfileCache.transform();
        }
        return this.transformers.profile;
    }

    private static final class TransformationSequence {
        @Nullable
        private GameProfile profile;
        private boolean expired;
        private boolean markRestAsCopy;
        private final TransformedProfileCache[] transformers;

        private TransformationSequence(List<ProfileTransformer> list) {
            this.transformers = (TransformedProfileCache[])list.stream().map(profileTransformer -> new TransformedProfileCache((ProfileTransformer)profileTransformer)).toArray(TransformedProfileCache[]::new);
        }

        private final class TransformedProfileCache {
            @Nullable
            private final ProfileTransformer transformer;
            @Nullable
            private GameProfile cacheProfile;

            private TransformedProfileCache(ProfileTransformer profileTransformer) {
                this.transformer = profileTransformer;
            }

            private void transform() {
                if (this.cacheProfile != null && this.transformer.canBeCached()) {
                    if (!TransformationSequence.this.expired) {
                        TransformationSequence.this.profile = this.cacheProfile;
                        return;
                    }
                } else {
                    TransformationSequence.this.expired = true;
                }
                this.cacheProfile = this.transformer.transform(TransformationSequence.this.markRestAsCopy ? TransformationSequence.this.profile : PlayerProfiles.clone(TransformationSequence.this.profile));
                TransformationSequence.this.profile = this.cacheProfile;
                if (!this.transformer.canBeCached()) {
                    TransformationSequence.this.markRestAsCopy = true;
                }
            }
        }
    }
}

