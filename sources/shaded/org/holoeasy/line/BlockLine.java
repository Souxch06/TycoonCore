package org.holoeasy.line;

import java.util.Collection;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.holoeasy.animation.Animations;
import org.holoeasy.ext.PacketContainerExtKt;
import org.holoeasy.line.ILine;
import org.holoeasy.line.Line;
import org.holoeasy.packet.PacketType;
import org.holoeasy.reactive.MutableState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000X\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0017\b\u0016\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0002\u00a2\u0006\u0002\u0010\u0006B\u001b\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0010\u0010'\u001a\u00020(2\u0006\u0010)\u001a\u00020*H\u0016J\u0010\u0010+\u001a\u00020(2\u0006\u0010\u0016\u001a\u00020\u0013H\u0016J\u0010\u0010,\u001a\u00020(2\u0006\u0010)\u001a\u00020*H\u0016J\u0010\u0010-\u001a\u00020(2\u0006\u0010)\u001a\u00020*H\u0016J\u0010\u0010.\u001a\u00020(2\u0006\u0010)\u001a\u00020*H\u0016R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\u00020\u000b8VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\f\u0010\rR\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0012\u001a\u0004\u0018\u00010\u00138VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0014\u0010\u0015R$\u0010\u0005\u001a\u00020\u00022\u0006\u0010\u0016\u001a\u00020\u00028V@VX\u0096\u000e\u00a2\u0006\f\u001a\u0004\b\u0017\u0010\u0018\"\u0004\b\u0019\u0010\u001aR\u0014\u0010\u0003\u001a\u00020\u00048VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u001b\u0010\u001cR\u001a\u0010\u001d\u001a\u00020\u001eX\u0096\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001f\u0010 \"\u0004\b!\u0010\"R\u0014\u0010#\u001a\u00020$8VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b%\u0010&\u00a8\u0006/"}, d2={"Lorg/holoeasy/line/BlockLine;", "Lorg/holoeasy/line/ILine;", "Lorg/bukkit/inventory/ItemStack;", "plugin", "Lorg/bukkit/plugin/Plugin;", "obj", "(Lorg/bukkit/plugin/Plugin;Lorg/bukkit/inventory/ItemStack;)V", "Lorg/holoeasy/reactive/MutableState;", "(Lorg/bukkit/plugin/Plugin;Lorg/holoeasy/reactive/MutableState;)V", "_mutableStateOf", "entityId", "", "getEntityId", "()I", "firstRender", "", "line", "Lorg/holoeasy/line/Line;", "location", "Lorg/bukkit/Location;", "getLocation", "()Lorg/bukkit/Location;", "value", "getObj", "()Lorg/bukkit/inventory/ItemStack;", "setObj", "(Lorg/bukkit/inventory/ItemStack;)V", "getPlugin", "()Lorg/bukkit/plugin/Plugin;", "pvt", "Lorg/holoeasy/line/ILine$PrivateConfig;", "getPvt", "()Lorg/holoeasy/line/ILine$PrivateConfig;", "setPvt", "(Lorg/holoeasy/line/ILine$PrivateConfig;)V", "type", "Lorg/holoeasy/line/ILine$Type;", "getType", "()Lorg/holoeasy/line/ILine$Type;", "hide", "", "player", "Lorg/bukkit/entity/Player;", "setLocation", "show", "teleport", "update", "holoeasy-core"})
public final class BlockLine
implements ILine<ItemStack> {
    @NotNull
    private final Line line;
    @NotNull
    private final MutableState<ItemStack> _mutableStateOf;
    private boolean firstRender;
    @NotNull
    private ILine.PrivateConfig pvt;

    public BlockLine(@NotNull Plugin plugin, @NotNull MutableState<ItemStack> mutableState) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        Intrinsics.checkNotNullParameter(mutableState, "obj");
        this.line = new Line(plugin, EntityType.ARMOR_STAND, null, 4, null);
        this._mutableStateOf = mutableState;
        this.firstRender = true;
        this.pvt = new ILine.PrivateConfig(this);
    }

    public BlockLine(@NotNull Plugin plugin, @NotNull ItemStack itemStack) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        Intrinsics.checkNotNullParameter(itemStack, "obj");
        this(plugin, new MutableState<ItemStack>(itemStack));
    }

    @Override
    @NotNull
    public Plugin getPlugin() {
        return this.line.getPlugin();
    }

    @Override
    @NotNull
    public ILine.Type getType() {
        return ILine.Type.BLOCK_LINE;
    }

    @Override
    public int getEntityId() {
        return this.line.getEntityID();
    }

    @Override
    @Nullable
    public Location getLocation() {
        return this.line.getLocation();
    }

    @Override
    @NotNull
    public ItemStack getObj() {
        return this._mutableStateOf.get();
    }

    @Override
    public void setObj(@NotNull ItemStack itemStack) {
        Intrinsics.checkNotNullParameter(itemStack, "value");
        this._mutableStateOf.set(itemStack);
    }

    @Override
    @NotNull
    public ILine.PrivateConfig getPvt() {
        return this.pvt;
    }

    @Override
    public void setPvt(@NotNull ILine.PrivateConfig privateConfig) {
        Intrinsics.checkNotNullParameter(privateConfig, "<set-?>");
        this.pvt = privateConfig;
    }

    @Override
    public void setLocation(@NotNull Location location) {
        Intrinsics.checkNotNullParameter(location, "value");
        this.line.setLocation(location);
    }

    @Override
    public void hide(@NotNull Player player) {
        Intrinsics.checkNotNullParameter(player, "player");
        this.line.destroy(player);
    }

    @Override
    public void teleport(@NotNull Player player) {
        Intrinsics.checkNotNullParameter(player, "player");
        this.line.teleport(player);
    }

    @Override
    public void show(@NotNull Player player) {
        Intrinsics.checkNotNullParameter(player, "player");
        this.line.spawn(player);
        PacketContainerExtKt.send(PacketType.INSTANCE.getMETADATA_TEXT().metadata(this.getEntityId(), null, true), player);
        this.update(player);
        if (this.firstRender) {
            this.firstRender = false;
            this._mutableStateOf.addObserver(this.getPvt());
        }
    }

    @Override
    public void update(@NotNull Player player) {
        Intrinsics.checkNotNullParameter(player, "player");
        PacketContainerExtKt.send(PacketType.INSTANCE.getEQUIPMENT().equip(this.getEntityId(), this._mutableStateOf.get()), player);
    }

    @Override
    public void update(@NotNull Collection<? extends Player> collection) {
        ILine.DefaultImpls.update(this, collection);
    }

    @Override
    public void setAnimation(@NotNull Animations animations) {
        ILine.DefaultImpls.setAnimation(this, animations);
    }

    @Override
    public void cancelAnimation() {
        ILine.DefaultImpls.cancelAnimation(this);
    }
}

