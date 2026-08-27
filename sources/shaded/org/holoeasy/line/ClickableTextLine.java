package org.holoeasy.line;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
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
import org.holoeasy.animation.Animations;
import org.holoeasy.line.ClickEvent;
import org.holoeasy.line.ILine;
import org.holoeasy.line.ITextLine;
import org.holoeasy.line.TextLine;
import org.holoeasy.util.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000\u0086\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0010\u0011\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010#\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\u0018\u00002\u00020\u00012\u00020\u0002B\u001d\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\bJ\u0010\u00105\u001a\u0002062\u0006\u00107\u001a\u000208H\u0007J\u0010\u00109\u001a\u0002062\u0006\u0010:\u001a\u00020;H\u0016J\u0010\u0010<\u001a\u0002062\u0006\u0010=\u001a\u00020>H\u0016J\u0010\u0010?\u001a\u00020\u001c2\u0006\u0010:\u001a\u00020;H\u0016J\u0010\u0010@\u001a\u0002062\u0006\u0010\u001b\u001a\u00020\u0018H\u0016J\u0010\u0010A\u001a\u0002062\u0006\u0010:\u001a\u00020;H\u0016J\u0010\u0010B\u001a\u0002062\u0006\u0010:\u001a\u00020;H\u0016J\u0010\u0010C\u001a\u0002062\u0006\u0010:\u001a\u00020;H\u0016J\b\u0010D\u001a\u000206H\u0002R\u001a\u0010\t\u001a\b\u0012\u0002\b\u0003\u0018\u00010\n8VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u000b\u0010\fR\u0014\u0010\r\u001a\u00020\u000e8VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u000f\u0010\u0010R\u0014\u0010\u0011\u001a\u00020\u00128VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0013\u0010\u0014R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0017\u001a\u0004\u0018\u00010\u00188VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0019\u0010\u001aR\u000e\u0010\u0007\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R$\u0010\u001d\u001a\u00020\u001c2\u0006\u0010\u001b\u001a\u00020\u001c8V@VX\u0096\u000e\u00a2\u0006\f\u001a\u0004\b\u001e\u0010\u001f\"\u0004\b \u0010!R\u0014\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u00120#X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010$\u001a\u00020%8VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b&\u0010'R\u001a\u0010(\u001a\u00020)X\u0096\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b*\u0010+\"\u0004\b,\u0010-R\u0014\u0010.\u001a\u00020\u00048VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b/\u00100R\u0014\u00101\u001a\u0002028VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b3\u00104\u00a8\u0006E"}, d2={"Lorg/holoeasy/line/ClickableTextLine;", "Lorg/bukkit/event/Listener;", "Lorg/holoeasy/line/ITextLine;", "line", "Lorg/holoeasy/line/TextLine;", "minHitDistance", "", "maxHitDistance", "(Lorg/holoeasy/line/TextLine;FF)V", "args", "", "getArgs", "()[Ljava/lang/Object;", "clickable", "", "getClickable", "()Z", "entityId", "", "getEntityId", "()I", "hitbox", "Lorg/holoeasy/util/AABB;", "location", "Lorg/bukkit/Location;", "getLocation", "()Lorg/bukkit/Location;", "value", "", "obj", "getObj", "()Ljava/lang/String;", "setObj", "(Ljava/lang/String;)V", "playersClickable", "", "plugin", "Lorg/bukkit/plugin/Plugin;", "getPlugin", "()Lorg/bukkit/plugin/Plugin;", "pvt", "Lorg/holoeasy/line/ILine$PrivateConfig;", "getPvt", "()Lorg/holoeasy/line/ILine$PrivateConfig;", "setPvt", "(Lorg/holoeasy/line/ILine$PrivateConfig;)V", "textLine", "getTextLine", "()Lorg/holoeasy/line/TextLine;", "type", "Lorg/holoeasy/line/ILine$Type;", "getType", "()Lorg/holoeasy/line/ILine$Type;", "handleInteract", "", "e", "Lorg/bukkit/event/player/PlayerInteractEvent;", "hide", "player", "Lorg/bukkit/entity/Player;", "onClick", "clickEvent", "Lorg/holoeasy/line/ClickEvent;", "parse", "setLocation", "show", "teleport", "update", "updateHitBox", "holoeasy-core"})
@SourceDebugExtension(value={"SMAP\nClickableTextLine.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ClickableTextLine.kt\norg/holoeasy/line/ClickableTextLine\n+ 2 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,128:1\n1#2:129\n*E\n"})
public final class ClickableTextLine
implements Listener,
ITextLine {
    @NotNull
    private final TextLine line;
    private final float minHitDistance;
    private final float maxHitDistance;
    @Nullable
    private AABB hitbox;
    @NotNull
    private final Set<Integer> playersClickable;
    @NotNull
    private ILine.PrivateConfig pvt;

    public ClickableTextLine(@NotNull TextLine textLine, float f, float f2) {
        Intrinsics.checkNotNullParameter(textLine, "line");
        this.line = textLine;
        this.playersClickable = new LinkedHashSet();
        if (!(!(f < 0.0f))) {
            boolean bl = false;
            String string = "minHitDistance must be positive";
            throw new IllegalArgumentException(string.toString());
        }
        if (!(!(f2 > 120.0f))) {
            boolean bl = false;
            String string = "maxHitDistance cannot be greater than 120";
            throw new IllegalArgumentException(string.toString());
        }
        this.minHitDistance = f;
        this.maxHitDistance = f2;
        if (this.line.getLocation() != null) {
            this.updateHitBox();
        }
        Bukkit.getPluginManager().registerEvents((Listener)this, this.line.getPlugin());
        this.pvt = new ILine.PrivateConfig(this);
    }

    @Override
    public boolean getClickable() {
        return false;
    }

    @Override
    @NotNull
    public TextLine getTextLine() {
        return this.line;
    }

    @Override
    @Nullable
    public Object[] getArgs() {
        return this.getTextLine().getArgs();
    }

    @Override
    @NotNull
    public String parse(@NotNull Player player) {
        Intrinsics.checkNotNullParameter(player, "player");
        return this.line.parse(player);
    }

    @Override
    public void onClick(@NotNull ClickEvent clickEvent) {
        Intrinsics.checkNotNullParameter(clickEvent, "clickEvent");
        this.line.onClick(clickEvent);
    }

    @Override
    @NotNull
    public Plugin getPlugin() {
        return this.line.getPlugin();
    }

    @Override
    @NotNull
    public ILine.Type getType() {
        return ILine.Type.CLICKABLE_TEXT_LINE;
    }

    @Override
    public int getEntityId() {
        return this.line.getEntityId();
    }

    @Override
    @Nullable
    public Location getLocation() {
        return this.line.getLocation();
    }

    @Override
    @NotNull
    public String getObj() {
        return this.line.getObj();
    }

    @Override
    public void setObj(@NotNull String string) {
        Intrinsics.checkNotNullParameter(string, "value");
        this.line.setObj(string);
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
        this.updateHitBox();
    }

    @Override
    public void hide(@NotNull Player player) {
        Intrinsics.checkNotNullParameter(player, "player");
        this.line.hide(player);
        this.playersClickable.remove(player.getEntityId());
    }

    @Override
    public void teleport(@NotNull Player player) {
        Intrinsics.checkNotNullParameter(player, "player");
        this.line.teleport(player);
    }

    @Override
    public void show(@NotNull Player player) {
        Intrinsics.checkNotNullParameter(player, "player");
        this.line.show(player);
        this.playersClickable.add(player.getEntityId());
    }

    @Override
    public void update(@NotNull Player player) {
        Intrinsics.checkNotNullParameter(player, "player");
        this.line.update(player);
    }

    @EventHandler
    public final void handleInteract(@NotNull PlayerInteractEvent playerInteractEvent) {
        block4: {
            Intrinsics.checkNotNullParameter(playerInteractEvent, "e");
            Player player = playerInteractEvent.getPlayer();
            Intrinsics.checkNotNullExpressionValue(player, "getPlayer(...)");
            Player player2 = player;
            if (playerInteractEvent.getAction() != Action.LEFT_CLICK_AIR) {
                return;
            }
            if (this.hitbox == null) {
                return;
            }
            if (!this.playersClickable.contains(player2.getEntityId())) {
                return;
            }
            AABB aABB = this.hitbox;
            Intrinsics.checkNotNull(aABB);
            Location location = player2.getEyeLocation();
            Intrinsics.checkNotNullExpressionValue(location, "getEyeLocation(...)");
            AABB.Vec3D vec3D = aABB.intersectsRay(new AABB.Ray3D(location), this.minHitDistance, this.maxHitDistance);
            if (vec3D == null) {
                return;
            }
            AABB.Vec3D vec3D2 = vec3D;
            ClickEvent clickEvent = this.line.getClickEvent();
            if (clickEvent == null) break block4;
            clickEvent.onClick(player2);
        }
    }

    private final void updateHitBox() {
        double d = this.getObj().length();
        double d2 = 0.105;
        double d3 = d2 * (d / 2.0);
        AABB aABB = this.hitbox = new AABB(new AABB.Vec3D(-d3, -0.04, -d3), new AABB.Vec3D(d3, 0.04, d3));
        Intrinsics.checkNotNull(aABB);
        Location location = this.getLocation();
        Intrinsics.checkNotNull(location);
        Location location2 = location.clone().add(0.0, 2.35, 0.0);
        Intrinsics.checkNotNullExpressionValue(location2, "add(...)");
        aABB.translate(AABB.Vec3D.Companion.fromLocation(location2));
    }

    @Override
    public void update(@NotNull Collection<? extends Player> collection) {
        ITextLine.DefaultImpls.update(this, collection);
    }

    @Override
    public void setAnimation(@NotNull Animations animations) {
        ITextLine.DefaultImpls.setAnimation(this, animations);
    }

    @Override
    public void cancelAnimation() {
        ITextLine.DefaultImpls.cancelAnimation(this);
    }
}

