/*
 * Decompiled with CFR 0.152.
 */
package org.holoeasy.packet;

import kotlin.Lazy;
import kotlin.LazyKt;
import kotlin.Metadata;
import kotlin.collections.ArraysKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import org.holoeasy.HoloEasy;
import org.holoeasy.packet.IPacket;
import org.holoeasy.packet.PacketType;
import org.holoeasy.packet.delete.IDeletePacket;
import org.holoeasy.packet.equipment.IEquipmentPacket;
import org.holoeasy.packet.metadata.item.IMetadataItemPacket;
import org.holoeasy.packet.metadata.text.IMetadataTextPacket;
import org.holoeasy.packet.rotate.IRotatePacket;
import org.holoeasy.packet.spawn.ISpawnPacket;
import org.holoeasy.packet.teleport.ITeleportPacket;
import org.holoeasy.packet.velocity.IVelocityPacket;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0011\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J+\u0010,\u001a\u0002H-\"\b\b\u0000\u0010-*\u00020.2\u0012\u0010/\u001a\n\u0012\u0006\b\u0001\u0012\u0002H-00\"\u0002H-H\u0002\u00a2\u0006\u0002\u00101R\u001b\u0010\u0003\u001a\u00020\u00048FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0007\u0010\b\u001a\u0004\b\u0005\u0010\u0006R\u001b\u0010\t\u001a\u00020\n8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\r\u0010\b\u001a\u0004\b\u000b\u0010\fR\u001b\u0010\u000e\u001a\u00020\u000f8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0012\u0010\b\u001a\u0004\b\u0010\u0010\u0011R\u001b\u0010\u0013\u001a\u00020\u00148FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0017\u0010\b\u001a\u0004\b\u0015\u0010\u0016R\u001b\u0010\u0018\u001a\u00020\u00198FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001c\u0010\b\u001a\u0004\b\u001a\u0010\u001bR\u001b\u0010\u001d\u001a\u00020\u001e8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b!\u0010\b\u001a\u0004\b\u001f\u0010 R\u001b\u0010\"\u001a\u00020#8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b&\u0010\b\u001a\u0004\b$\u0010%R\u001b\u0010'\u001a\u00020(8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b+\u0010\b\u001a\u0004\b)\u0010*\u00a8\u00062"}, d2={"Lorg/holoeasy/packet/PacketType;", "", "()V", "DELETE", "Lorg/holoeasy/packet/delete/IDeletePacket;", "getDELETE", "()Lorg/holoeasy/packet/delete/IDeletePacket;", "DELETE$delegate", "Lkotlin/Lazy;", "EQUIPMENT", "Lorg/holoeasy/packet/equipment/IEquipmentPacket;", "getEQUIPMENT", "()Lorg/holoeasy/packet/equipment/IEquipmentPacket;", "EQUIPMENT$delegate", "METADATA_ITEM", "Lorg/holoeasy/packet/metadata/item/IMetadataItemPacket;", "getMETADATA_ITEM", "()Lorg/holoeasy/packet/metadata/item/IMetadataItemPacket;", "METADATA_ITEM$delegate", "METADATA_TEXT", "Lorg/holoeasy/packet/metadata/text/IMetadataTextPacket;", "getMETADATA_TEXT", "()Lorg/holoeasy/packet/metadata/text/IMetadataTextPacket;", "METADATA_TEXT$delegate", "ROTATE", "Lorg/holoeasy/packet/rotate/IRotatePacket;", "getROTATE", "()Lorg/holoeasy/packet/rotate/IRotatePacket;", "ROTATE$delegate", "SPAWN", "Lorg/holoeasy/packet/spawn/ISpawnPacket;", "getSPAWN", "()Lorg/holoeasy/packet/spawn/ISpawnPacket;", "SPAWN$delegate", "TELEPORT", "Lorg/holoeasy/packet/teleport/ITeleportPacket;", "getTELEPORT", "()Lorg/holoeasy/packet/teleport/ITeleportPacket;", "TELEPORT$delegate", "VELOCITY", "Lorg/holoeasy/packet/velocity/IVelocityPacket;", "getVELOCITY", "()Lorg/holoeasy/packet/velocity/IVelocityPacket;", "VELOCITY$delegate", "getCurrImpl", "T", "Lorg/holoeasy/packet/IPacket;", "impls", "", "([Lorg/holoeasy/packet/IPacket;)Lorg/holoeasy/packet/IPacket;", "holoeasy-core"})
@SourceDebugExtension(value={"SMAP\nIPacket.kt\nKotlin\n*S Kotlin\n*F\n+ 1 IPacket.kt\norg/holoeasy/packet/PacketType\n+ 2 _Arrays.kt\nkotlin/collections/ArraysKt___ArraysKt\n*L\n1#1,101:1\n1282#2,2:102\n*S KotlinDebug\n*F\n+ 1 IPacket.kt\norg/holoeasy/packet/PacketType\n*L\n41#1:102,2\n*E\n"})
public final class PacketType {
    @NotNull
    public static final PacketType INSTANCE = new PacketType();
    @NotNull
    private static final Lazy DELETE$delegate = LazyKt.lazy(DELETE.2.INSTANCE);
    @NotNull
    private static final Lazy METADATA_TEXT$delegate = LazyKt.lazy(METADATA_TEXT.2.INSTANCE);
    @NotNull
    private static final Lazy METADATA_ITEM$delegate = LazyKt.lazy(METADATA_ITEM.2.INSTANCE);
    @NotNull
    private static final Lazy SPAWN$delegate = LazyKt.lazy(SPAWN.2.INSTANCE);
    @NotNull
    private static final Lazy TELEPORT$delegate = LazyKt.lazy(TELEPORT.2.INSTANCE);
    @NotNull
    private static final Lazy VELOCITY$delegate = LazyKt.lazy(VELOCITY.2.INSTANCE);
    @NotNull
    private static final Lazy ROTATE$delegate = LazyKt.lazy(ROTATE.2.INSTANCE);
    @NotNull
    private static final Lazy EQUIPMENT$delegate = LazyKt.lazy(EQUIPMENT.2.INSTANCE);

    private PacketType() {
    }

    private final <T extends IPacket> T getCurrImpl(T ... TArray) {
        T t;
        block3: {
            T t2;
            T[] TArray2 = TArray;
            boolean bl = false;
            int n = TArray2.length;
            for (int i = 0; i < n; ++i) {
                T t3;
                T t4 = t3 = TArray2[i];
                boolean bl2 = false;
                if (!t4.isCurrentVersion()) continue;
                t2 = t3;
                break block3;
            }
            t2 = t = null;
        }
        if (t != null) {
            return t;
        }
        if (HoloEasy.useLastSupportedVersion) {
            T t5 = ArraysKt.last(TArray);
            Intrinsics.checkNotNull(t5, "null cannot be cast to non-null type T of org.holoeasy.packet.PacketType.getCurrImpl");
            return (T)((IPacket)t5);
        }
        throw new RuntimeException("No version support for this packet\nSet HoloEasy.useLastSupportedVersion to true or\nopen an issue at https://github.com/unldenis/holoeasy");
    }

    @NotNull
    public final IDeletePacket getDELETE() {
        Lazy lazy = DELETE$delegate;
        return (IDeletePacket)lazy.getValue();
    }

    @NotNull
    public final IMetadataTextPacket getMETADATA_TEXT() {
        Lazy lazy = METADATA_TEXT$delegate;
        return (IMetadataTextPacket)lazy.getValue();
    }

    @NotNull
    public final IMetadataItemPacket getMETADATA_ITEM() {
        Lazy lazy = METADATA_ITEM$delegate;
        return (IMetadataItemPacket)lazy.getValue();
    }

    @NotNull
    public final ISpawnPacket getSPAWN() {
        Lazy lazy = SPAWN$delegate;
        return (ISpawnPacket)lazy.getValue();
    }

    @NotNull
    public final ITeleportPacket getTELEPORT() {
        Lazy lazy = TELEPORT$delegate;
        return (ITeleportPacket)lazy.getValue();
    }

    @NotNull
    public final IVelocityPacket getVELOCITY() {
        Lazy lazy = VELOCITY$delegate;
        return (IVelocityPacket)lazy.getValue();
    }

    @NotNull
    public final IRotatePacket getROTATE() {
        Lazy lazy = ROTATE$delegate;
        return (IRotatePacket)lazy.getValue();
    }

    @NotNull
    public final IEquipmentPacket getEQUIPMENT() {
        Lazy lazy = EQUIPMENT$delegate;
        return (IEquipmentPacket)lazy.getValue();
    }

    public static final /* synthetic */ IPacket access$getCurrImpl(PacketType packetType, IPacket ... iPacketArray) {
        return packetType.getCurrImpl(iPacketArray);
    }
}

