package org.holoeasy.packet.equipment;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import java.util.ArrayList;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.ClosedRange;
import kotlin.ranges.RangesKt;
import org.bukkit.inventory.ItemStack;
import org.holoeasy.ext.StructureModifierExtKt;
import org.holoeasy.packet.PacketBuilderKt;
import org.holoeasy.packet.equipment.IEquipmentPacket;
import org.holoeasy.util.VersionEnum;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000.\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0016R\"\u0010\u0003\u001a\u0010\u0012\f\b\u0001\u0012\b\u0012\u0004\u0012\u00020\u00060\u00050\u00048VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\b\u00a8\u0006\u000f"}, d2={"Lorg/holoeasy/packet/equipment/EquipmentPacketC;", "Lorg/holoeasy/packet/equipment/IEquipmentPacket;", "()V", "versionSupport", "", "Lkotlin/ranges/ClosedRange;", "Lorg/holoeasy/util/VersionEnum;", "getVersionSupport", "()[Lkotlin/ranges/ClosedRange;", "equip", "Lcom/comphenix/protocol/events/PacketContainer;", "entityId", "", "helmet", "Lorg/bukkit/inventory/ItemStack;", "holoeasy-core"})
public final class EquipmentPacketC
implements IEquipmentPacket {
    @NotNull
    public static final EquipmentPacketC INSTANCE = new EquipmentPacketC();

    private EquipmentPacketC() {
    }

    @Override
    @NotNull
    public ClosedRange<VersionEnum>[] getVersionSupport() {
        ClosedRange[] closedRangeArray = new ClosedRange[]{RangesKt.rangeTo((Comparable)VersionEnum.V1_13, (Comparable)VersionEnum.LATEST)};
        return closedRangeArray;
    }

    @Override
    @NotNull
    public PacketContainer equip(int n, @NotNull ItemStack itemStack) {
        Intrinsics.checkNotNullParameter(itemStack, "helmet");
        PacketType packetType = PacketType.Play.Server.ENTITY_EQUIPMENT;
        Intrinsics.checkNotNullExpressionValue(packetType, "ENTITY_EQUIPMENT");
        return PacketBuilderKt.packet(packetType, (Function1<? super PacketContainer, Unit>)new Function1<PacketContainer, Unit>(n, itemStack){
            final /* synthetic */ int $entityId;
            final /* synthetic */ ItemStack $helmet;
            {
                this.$entityId = n;
                this.$helmet = itemStack;
                super(1);
            }

            public final void invoke(@NotNull PacketContainer packetContainer) {
                Intrinsics.checkNotNullParameter(packetContainer, "$this$packet");
                StructureModifier structureModifier = packetContainer.getIntegers();
                Intrinsics.checkNotNullExpressionValue(structureModifier, "getIntegers(...)");
                StructureModifierExtKt.set(structureModifier, 0, this.$entityId);
                ArrayList<Pair> arrayList = new ArrayList<Pair>();
                arrayList.add(new Pair((Object)EnumWrappers.ItemSlot.HEAD, (Object)this.$helmet));
                StructureModifier structureModifier2 = packetContainer.getSlotStackPairLists();
                Intrinsics.checkNotNullExpressionValue(structureModifier2, "getSlotStackPairLists(...)");
                StructureModifierExtKt.set(structureModifier2, 0, arrayList);
            }
        });
    }

    @Override
    public boolean isCurrentVersion() {
        return IEquipmentPacket.DefaultImpls.isCurrentVersion(this);
    }
}

