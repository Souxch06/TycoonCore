/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.plugin.Plugin
 */
package org.holoeasy.pool;

import java.util.UUID;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.holoeasy.action.ClickAction;
import org.holoeasy.builder.interfaces.HologramRegisterGroup;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.line.ClickEvent;
import org.holoeasy.line.ILine;
import org.holoeasy.line.ITextLine;
import org.holoeasy.line.TextLine;
import org.holoeasy.pool.HologramPool;
import org.holoeasy.pool.IHologramPool;
import org.holoeasy.util.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000F\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u00012\u00020\u0002B'\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\u0006\u0012\b\u0010\b\u001a\u0004\u0018\u00010\t\u00a2\u0006\u0002\u0010\nJ\u0010\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017H\u0016J\u0010\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u001bH\u0007J\u0012\u0010\u001c\u001a\u0004\u0018\u00010\u00152\u0006\u0010\u0016\u001a\u00020\u0017H\u0016J\u0010\u0010\u001d\u001a\u00020\u00192\u0006\u0010\u001e\u001a\u00020\u0015H\u0016R\u0013\u0010\b\u001a\u0004\u0018\u00010\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u000eR\u0014\u0010\u0010\u001a\u00020\u00118VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0012\u0010\u0013R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001f"}, d2={"Lorg/holoeasy/pool/InteractiveHologramPool;", "Lorg/bukkit/event/Listener;", "Lorg/holoeasy/pool/IHologramPool;", "pool", "Lorg/holoeasy/pool/HologramPool;", "minHitDistance", "", "maxHitDistance", "clickAction", "Lorg/holoeasy/action/ClickAction;", "(Lorg/holoeasy/pool/HologramPool;FFLorg/holoeasy/action/ClickAction;)V", "getClickAction", "()Lorg/holoeasy/action/ClickAction;", "getMaxHitDistance", "()F", "getMinHitDistance", "plugin", "Lorg/bukkit/plugin/Plugin;", "getPlugin", "()Lorg/bukkit/plugin/Plugin;", "get", "Lorg/holoeasy/hologram/Hologram;", "id", "Ljava/util/UUID;", "handleInteract", "", "e", "Lorg/bukkit/event/player/PlayerInteractEvent;", "remove", "takeCareOf", "value", "holoeasy-core"})
@SourceDebugExtension(value={"SMAP\nInteractiveHologramPool.kt\nKotlin\n*S Kotlin\n*F\n+ 1 InteractiveHologramPool.kt\norg/holoeasy/pool/InteractiveHologramPool\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,112:1\n1#2:113\n*E\n"})
public final class InteractiveHologramPool
implements Listener,
IHologramPool {
    @NotNull
    private final HologramPool pool;
    @Nullable
    private final ClickAction clickAction;
    private final float minHitDistance;
    private final float maxHitDistance;

    public InteractiveHologramPool(@NotNull HologramPool hologramPool, float f, float f2, @Nullable ClickAction clickAction) {
        Intrinsics.checkNotNullParameter(hologramPool, "pool");
        this.pool = hologramPool;
        this.clickAction = clickAction;
        if (!(f > 0.0f)) {
            boolean bl = false;
            String string = "minHitDistance must be positive";
            throw new IllegalArgumentException(string.toString());
        }
        if (!(f2 < 120.0f)) {
            boolean bl = false;
            String string = "maxHitDistance cannot be greater than 120";
            throw new IllegalArgumentException(string.toString());
        }
        this.minHitDistance = f;
        this.maxHitDistance = f2;
        Bukkit.getPluginManager().registerEvents((Listener)this, this.getPlugin());
    }

    @Nullable
    public final ClickAction getClickAction() {
        return this.clickAction;
    }

    @Override
    @NotNull
    public Plugin getPlugin() {
        return this.pool.getPlugin();
    }

    @Override
    @NotNull
    public Hologram get(@NotNull UUID uUID) {
        Intrinsics.checkNotNullParameter(uUID, "id");
        return this.pool.get(uUID);
    }

    @Override
    public void takeCareOf(@NotNull Hologram hologram) {
        Intrinsics.checkNotNullParameter(hologram, "value");
        this.pool.takeCareOf(hologram);
    }

    @Override
    @Nullable
    public Hologram remove(@NotNull UUID uUID) {
        Intrinsics.checkNotNullParameter(uUID, "id");
        return this.pool.remove(uUID);
    }

    public final float getMinHitDistance() {
        return this.minHitDistance;
    }

    public final float getMaxHitDistance() {
        return this.maxHitDistance;
    }

    @EventHandler
    public final void handleInteract(@NotNull PlayerInteractEvent playerInteractEvent) {
        Intrinsics.checkNotNullParameter(playerInteractEvent, "e");
        Bukkit.getScheduler().runTaskAsynchronously(this.getPlugin(), () -> InteractiveHologramPool.handleInteract$lambda$2(playerInteractEvent, this));
    }

    @Override
    public void registerHolograms(@NotNull HologramRegisterGroup hologramRegisterGroup) {
        IHologramPool.DefaultImpls.registerHolograms(this, hologramRegisterGroup);
    }

    private static final void handleInteract$lambda$2(PlayerInteractEvent playerInteractEvent, InteractiveHologramPool interactiveHologramPool) {
        Intrinsics.checkNotNullParameter(playerInteractEvent, "$e");
        Intrinsics.checkNotNullParameter(interactiveHologramPool, "this$0");
        Player player = playerInteractEvent.getPlayer();
        Intrinsics.checkNotNullExpressionValue(player, "getPlayer(...)");
        Player player2 = player;
        if (interactiveHologramPool.clickAction == null) {
            if (playerInteractEvent.getAction() != Action.LEFT_CLICK_AIR && playerInteractEvent.getAction() != Action.RIGHT_CLICK_AIR) {
                return;
            }
        } else {
            switch (WhenMappings.$EnumSwitchMapping$0[interactiveHologramPool.clickAction.ordinal()]) {
                case 1: {
                    if (playerInteractEvent.getAction() == Action.LEFT_CLICK_AIR) break;
                    return;
                }
                case 2: {
                    if (playerInteractEvent.getAction() == Action.RIGHT_CLICK_AIR) break;
                    return;
                }
            }
        }
        block7: for (Hologram hologram : interactiveHologramPool.pool.getHolograms().values()) {
            if (!hologram.isShownFor(player2)) continue;
            block8: for (ILine<?> iLine : hologram.getLines()) {
                switch (WhenMappings.$EnumSwitchMapping$1[iLine.getType().ordinal()]) {
                    case 1: {
                        TextLine textLine;
                        Intrinsics.checkNotNull(iLine, "null cannot be cast to non-null type org.holoeasy.line.ITextLine");
                        ITextLine iTextLine = (ITextLine)iLine;
                        if (!iTextLine.getClickable() || (textLine = iTextLine.getTextLine()).getHitbox() == null) continue block8;
                        AABB aABB = textLine.getHitbox();
                        Intrinsics.checkNotNull(aABB);
                        Location location = player2.getEyeLocation();
                        Intrinsics.checkNotNullExpressionValue(location, "getEyeLocation(...)");
                        AABB.Vec3D vec3D = aABB.intersectsRay(new AABB.Ray3D(location), interactiveHologramPool.minHitDistance, interactiveHologramPool.maxHitDistance);
                        if (vec3D == null) continue block8;
                        ClickEvent clickEvent = textLine.getClickEvent();
                        if (clickEvent != null) {
                            clickEvent.onClick(player2);
                        }
                        break block7;
                    }
                    default: {
                        continue block8;
                    }
                }
            }
        }
    }

    @Metadata(mv={1, 9, 0}, k=3, xi=48)
    public final class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;
        public static final /* synthetic */ int[] $EnumSwitchMapping$1;

        static {
            int[] nArray = new int[ClickAction.values().length];
            try {
                nArray[ClickAction.LEFT_CLICK.ordinal()] = 1;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[ClickAction.RIGHT_CLICK.ordinal()] = 2;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            $EnumSwitchMapping$0 = nArray;
            nArray = new int[ILine.Type.values().length];
            try {
                nArray[ILine.Type.TEXT_LINE.ordinal()] = 1;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[ILine.Type.EXTERNAL.ordinal()] = 2;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[ILine.Type.CLICKABLE_TEXT_LINE.ordinal()] = 3;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[ILine.Type.ITEM_LINE.ordinal()] = 4;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[ILine.Type.BLOCK_LINE.ordinal()] = 5;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            $EnumSwitchMapping$1 = nArray;
        }
    }
}

