/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.comphenix.protocol.reflect.StructureModifier
 */
package org.holoeasy.ext;

import com.comphenix.protocol.reflect.StructureModifier;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=2, xi=48, d1={"\u0000\u0016\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\u001a.\u0010\u0000\u001a\u00020\u0001\"\u0004\b\u0000\u0010\u0002*\b\u0012\u0004\u0012\u0002H\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u0002H\u0002H\u0080\u0002\u00a2\u0006\u0002\u0010\u0007\u00a8\u0006\b"}, d2={"set", "", "T", "Lcom/comphenix/protocol/reflect/StructureModifier;", "index", "", "value", "(Lcom/comphenix/protocol/reflect/StructureModifier;ILjava/lang/Object;)V", "holoeasy-core"})
public final class StructureModifierExtKt {
    public static final <T> void set(@NotNull StructureModifier<T> structureModifier, int n, T t) {
        Intrinsics.checkNotNullParameter(structureModifier, "<this>");
        structureModifier.write(n, t);
    }
}

