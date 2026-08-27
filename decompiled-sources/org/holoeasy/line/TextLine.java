/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.comphenix.protocol.events.PacketContainer
 *  org.bukkit.Location
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package org.holoeasy.line;

import com.comphenix.protocol.events.PacketContainer;
import java.util.Arrays;
import java.util.Collection;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.holoeasy.animation.Animations;
import org.holoeasy.ext.PacketContainerExtKt;
import org.holoeasy.line.ClickEvent;
import org.holoeasy.line.ILine;
import org.holoeasy.line.ITextLine;
import org.holoeasy.line.Line;
import org.holoeasy.packet.PacketType;
import org.holoeasy.packet.metadata.text.IMetadataTextPacket;
import org.holoeasy.reactive.MutableState;
import org.holoeasy.util.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={1, 9, 0}, k=1, xi=48, d1={"\u0000h\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0011\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\u0018\u00002\u00020\u0001B/\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u000e\b\u0002\u0010\u0006\u001a\b\u0012\u0002\b\u0003\u0018\u00010\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\u0010\u0010:\u001a\u00020;2\u0006\u0010<\u001a\u00020=H\u0016J\u0010\u0010>\u001a\u00020;2\u0006\u0010\u000e\u001a\u00020\u000fH\u0016J\u0010\u0010?\u001a\u00020\u00052\u0006\u0010<\u001a\u00020=H\u0016J\u0010\u0010@\u001a\u00020;2\u0006\u0010A\u001a\u00020$H\u0016J\u0010\u0010B\u001a\u00020;2\u0006\u0010<\u001a\u00020=H\u0016J\u0010\u0010C\u001a\u00020;2\u0006\u0010<\u001a\u00020=H\u0016J\u0010\u0010D\u001a\u00020;2\u0006\u0010<\u001a\u00020=H\u0016R\u001c\u0010\u0006\u001a\b\u0012\u0002\b\u0003\u0018\u00010\u0007X\u0096\u0004\u00a2\u0006\n\n\u0002\u0010\r\u001a\u0004\b\u000b\u0010\fR\u001c\u0010\u000e\u001a\u0004\u0018\u00010\u000fX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0010\u0010\u0011\"\u0004\b\u0012\u0010\u0013R\u0014\u0010\b\u001a\u00020\tX\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0014\u0010\u0016\u001a\u00020\u00178VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0018\u0010\u0019R\u000e\u0010\u001a\u001a\u00020\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\"\u0010\u001d\u001a\u0004\u0018\u00010\u001c2\b\u0010\u001b\u001a\u0004\u0018\u00010\u001c@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001fR\u000e\u0010 \u001a\u00020\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010!\u001a\u00020\"X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010#\u001a\u0004\u0018\u00010$8VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b%\u0010&R\u001a\u0010\u0004\u001a\u00020\u0005X\u0096\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b'\u0010(\"\u0004\b)\u0010*R\u0014\u0010\u0002\u001a\u00020\u00038VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b+\u0010,R\u001a\u0010-\u001a\u00020.X\u0096\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b/\u00100\"\u0004\b1\u00102R\u0014\u00103\u001a\u00020\u00008VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b4\u00105R\u0014\u00106\u001a\u0002078VX\u0096\u0004\u00a2\u0006\u0006\u001a\u0004\b8\u00109\u00a8\u0006E"}, d2={"Lorg/holoeasy/line/TextLine;", "Lorg/holoeasy/line/ITextLine;", "plugin", "Lorg/bukkit/plugin/Plugin;", "obj", "", "args", "", "clickable", "", "(Lorg/bukkit/plugin/Plugin;Ljava/lang/String;[Ljava/lang/Object;Z)V", "getArgs", "()[Ljava/lang/Object;", "[Ljava/lang/Object;", "clickEvent", "Lorg/holoeasy/line/ClickEvent;", "getClickEvent", "()Lorg/holoeasy/line/ClickEvent;", "setClickEvent", "(Lorg/holoeasy/line/ClickEvent;)V", "getClickable", "()Z", "entityId", "", "getEntityId", "()I", "firstRender", "<set-?>", "Lorg/holoeasy/util/AABB;", "hitbox", "getHitbox", "()Lorg/holoeasy/util/AABB;", "isEmpty", "line", "Lorg/holoeasy/line/Line;", "location", "Lorg/bukkit/Location;", "getLocation", "()Lorg/bukkit/Location;", "getObj", "()Ljava/lang/String;", "setObj", "(Ljava/lang/String;)V", "getPlugin", "()Lorg/bukkit/plugin/Plugin;", "pvt", "Lorg/holoeasy/line/ILine$PrivateConfig;", "getPvt", "()Lorg/holoeasy/line/ILine$PrivateConfig;", "setPvt", "(Lorg/holoeasy/line/ILine$PrivateConfig;)V", "textLine", "getTextLine", "()Lorg/holoeasy/line/TextLine;", "type", "Lorg/holoeasy/line/ILine$Type;", "getType", "()Lorg/holoeasy/line/ILine$Type;", "hide", "", "player", "Lorg/bukkit/entity/Player;", "onClick", "parse", "setLocation", "value", "show", "teleport", "update", "holoeasy-core"})
public final class TextLine
implements ITextLine {
    @Nullable
    private final Object[] args;
    private final boolean clickable;
    @NotNull
    private final Line line;
    @NotNull
    private String obj;
    @Nullable
    private ClickEvent clickEvent;
    private boolean firstRender;
    @Nullable
    private AABB hitbox;
    private boolean isEmpty;
    @NotNull
    private ILine.PrivateConfig pvt;

    public TextLine(@NotNull Plugin plugin, @NotNull String string, @Nullable Object[] objectArray, boolean bl) {
        Intrinsics.checkNotNullParameter(plugin, "plugin");
        Intrinsics.checkNotNullParameter(string, "obj");
        this.args = objectArray;
        this.clickable = bl;
        this.line = new Line(plugin, EntityType.ARMOR_STAND, null, 4, null);
        this.obj = "";
        this.firstRender = true;
        if (this.getArgs() == null) {
            this.setObj(string);
        } else {
            this.setObj(StringsKt.replace$default(string, "{}", "%s", false, 4, null));
        }
        this.pvt = new ILine.PrivateConfig(this);
    }

    public /* synthetic */ TextLine(Plugin plugin, String string, Object[] objectArray, boolean bl, int n, DefaultConstructorMarker defaultConstructorMarker) {
        if ((n & 4) != 0) {
            objectArray = null;
        }
        if ((n & 8) != 0) {
            bl = false;
        }
        this(plugin, string, objectArray, bl);
    }

    @Override
    @Nullable
    public Object[] getArgs() {
        return this.args;
    }

    @Override
    public boolean getClickable() {
        return this.clickable;
    }

    @Override
    @NotNull
    public String getObj() {
        return this.obj;
    }

    @Override
    public void setObj(@NotNull String string) {
        Intrinsics.checkNotNullParameter(string, "<set-?>");
        this.obj = string;
    }

    @Nullable
    public final ClickEvent getClickEvent() {
        return this.clickEvent;
    }

    public final void setClickEvent(@Nullable ClickEvent clickEvent) {
        this.clickEvent = clickEvent;
    }

    @Nullable
    public final AABB getHitbox() {
        return this.hitbox;
    }

    @Override
    @NotNull
    public TextLine getTextLine() {
        return this;
    }

    @Override
    @NotNull
    public String parse(@NotNull Player player) {
        Object[] objectArray;
        Intrinsics.checkNotNullParameter(player, "player");
        if (this.getArgs() == null) {
            return this.getObj();
        }
        Object[] objectArray2 = new Object[this.getArgs().length];
        int n = this.getArgs().length;
        for (int i = 0; i < n; ++i) {
            objectArray = this.getArgs()[i];
            if (objectArray instanceof MutableState) {
                objectArray2[i] = ((MutableState)objectArray).get();
                if (!this.firstRender) continue;
                this.firstRender = false;
                ((MutableState)objectArray).addObserver(this.getPvt());
                continue;
            }
            objectArray2[i] = objectArray;
        }
        String string = this.getObj();
        objectArray = Arrays.copyOf(objectArray2, objectArray2.length);
        String string2 = String.format(string, Arrays.copyOf(objectArray, objectArray.length));
        Intrinsics.checkNotNullExpressionValue(string2, "format(...)");
        return string2;
    }

    @Override
    public void onClick(@NotNull ClickEvent clickEvent) {
        Intrinsics.checkNotNullParameter(clickEvent, "clickEvent");
        this.clickEvent = clickEvent;
    }

    @Override
    @NotNull
    public Plugin getPlugin() {
        return this.line.getPlugin();
    }

    @Override
    @NotNull
    public ILine.Type getType() {
        return ILine.Type.TEXT_LINE;
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
        if (this.getClickable()) {
            AABB aABB;
            double d = this.getObj().length();
            double d2 = 0.105;
            double d3 = d2 * (d / 2.0);
            AABB aABB2 = aABB = new AABB(new AABB.Vec3D(-d3, -0.04, -d3), new AABB.Vec3D(d3, 0.04, d3));
            TextLine textLine = this;
            boolean bl = false;
            Location location2 = location.clone().add(0.0, 2.35, 0.0);
            Intrinsics.checkNotNullExpressionValue(location2, "add(...)");
            aABB2.translate(AABB.Vec3D.Companion.fromLocation(location2));
            textLine.hitbox = aABB;
        }
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
        boolean bl = this.isEmpty = ((CharSequence)this.getObj()).length() == 0;
        if (!this.isEmpty) {
            this.line.spawn(player);
            PacketContainer packetContainer = IMetadataTextPacket.DefaultImpls.metadata$default(PacketType.INSTANCE.getMETADATA_TEXT(), this.getEntityId(), this.parse(player), false, 4, null);
            PacketContainerExtKt.send(packetContainer, player);
        }
    }

    @Override
    public void update(@NotNull Player player) {
        Intrinsics.checkNotNullParameter(player, "player");
        int n = (this.isEmpty ? 1 : 0) | (((CharSequence)this.getObj()).length() == 0 ? 1 : 0) << 1;
        switch (n) {
            case 3: {
                break;
            }
            case 2: {
                this.line.destroy(player);
                this.isEmpty = true;
                break;
            }
            case 1: {
                this.line.spawn(player);
                this.isEmpty = false;
                PacketContainerExtKt.send(IMetadataTextPacket.DefaultImpls.metadata$default(PacketType.INSTANCE.getMETADATA_TEXT(), this.getEntityId(), this.parse(player), false, 4, null), player);
                break;
            }
            case 0: {
                PacketContainerExtKt.send(PacketType.INSTANCE.getMETADATA_TEXT().metadata(this.getEntityId(), this.parse(player), false), player);
            }
        }
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

