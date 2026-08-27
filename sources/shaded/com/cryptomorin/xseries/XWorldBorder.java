/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.WorldBorder
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerChangedWorldEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.util.Vector
 */
package com.cryptomorin.xseries;

import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.jvm.MethodMemberHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftConnection;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftMapping;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftPackage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class XWorldBorder
implements Cloneable {
    private static final MethodHandle WORLD_HANDLE;
    private static final MethodHandle WORLDBORDER;
    private static final MethodHandle WORLDBORDER_WORLD;
    private static final MethodHandle CENTER;
    private static final MethodHandle WARNING_DISTANCE;
    private static final MethodHandle WARNING_TIME;
    private static final MethodHandle SIZE;
    private static final MethodHandle TRANSITION;
    private static final MethodHandle PACKET_WARNING_DISTANCE;
    private static final MethodHandle PACKET_WARNING_DELAY;
    private static final MethodHandle PACKET_LERP_SIZE;
    private static final MethodHandle PACKET_INIT;
    private static final MethodHandle PACKET_CENTER;
    private static final MethodHandle PACKET_SIZE;
    private static final Object INITIALIZE;
    public static final double MAX_SIZE = 5.9999968E7;
    public static final double MAX_CENTER_COORDINATE = 2.9999984E7;
    private static final boolean SUPPORTS_SEPARATE_PACKETS;
    private static final Map<UUID, XWorldBorder> WORLD_BORDERS;
    private Object handle;
    public int absoluteMaxSize = 29999984;
    private double damagePerBlock = 0.2;
    private double damageSafeZone = 5.0;
    private double size = 100.0;
    private double sizeLerpTarget = 0.0;
    private BorderBounds borderBounds;
    private Duration warningTime = Duration.ofSeconds(15L);
    private Duration sizeLerpTime = Duration.ZERO;
    private int warningBlocks = 5;
    private World world;
    private double centerX;
    private double centerZ;
    private final Set<Component> updateRequired = EnumSet.noneOf(Component.class);
    private UUID player;
    private boolean init = true;

    private XWorldBorder() {
    }

    public static XWorldBorder getOrCreate(Player player) {
        XWorldBorder xWorldBorder = XWorldBorder.get(player);
        if (xWorldBorder != null) {
            return xWorldBorder;
        }
        return XWorldBorder.of(player.getLocation()).setPlayer(player);
    }

    public static XWorldBorder get(Player player) {
        return WORLD_BORDERS.get(player.getUniqueId());
    }

    public XWorldBorder clone() {
        XWorldBorder xWorldBorder = new XWorldBorder();
        xWorldBorder.world = this.world;
        xWorldBorder.centerX = this.centerX;
        xWorldBorder.centerZ = this.centerZ;
        xWorldBorder.size = this.size;
        xWorldBorder.sizeLerpTime = this.sizeLerpTime;
        xWorldBorder.damagePerBlock = this.damagePerBlock;
        xWorldBorder.damageSafeZone = this.damageSafeZone;
        xWorldBorder.warningTime = this.warningTime;
        xWorldBorder.warningBlocks = this.warningBlocks;
        xWorldBorder.handle = xWorldBorder.createHandle();
        xWorldBorder.player = this.player;
        return xWorldBorder;
    }

    public static XWorldBorder from(WorldBorder worldBorder) {
        XWorldBorder xWorldBorder = new XWorldBorder();
        xWorldBorder.world = worldBorder.getCenter().getWorld();
        xWorldBorder.centerX = worldBorder.getCenter().getX();
        xWorldBorder.centerZ = worldBorder.getCenter().getZ();
        xWorldBorder.size = worldBorder.getSize();
        xWorldBorder.sizeLerpTime = Duration.ZERO;
        xWorldBorder.damagePerBlock = worldBorder.getDamageAmount();
        xWorldBorder.damageSafeZone = worldBorder.getDamageBuffer();
        xWorldBorder.warningTime = Duration.ofSeconds(worldBorder.getWarningTime());
        xWorldBorder.warningBlocks = worldBorder.getWarningDistance();
        xWorldBorder.handle = xWorldBorder.createHandle();
        return xWorldBorder;
    }

    @Nullable
    public UUID getPlayerId() {
        return this.player;
    }

    @Nullable
    public Player getPlayer() {
        return Bukkit.getPlayer((UUID)Objects.requireNonNull(this.player, "No player provided"));
    }

    public static XWorldBorder of(Location location) {
        XWorldBorder xWorldBorder = new XWorldBorder();
        xWorldBorder.world = Objects.requireNonNull(location.getWorld());
        xWorldBorder.centerX = location.getX();
        xWorldBorder.centerZ = location.getZ();
        xWorldBorder.handle = xWorldBorder.createHandle();
        xWorldBorder.update(Component.CENTER);
        return xWorldBorder;
    }

    public XWorldBorder setDamageAmount(double d) {
        this.damagePerBlock = d;
        return this;
    }

    public double getDamageAmount() {
        return this.damagePerBlock;
    }

    public XWorldBorder setDamageBuffer(double d) {
        this.damageSafeZone = d;
        return this;
    }

    public double getDamageBuffer() {
        return this.damageSafeZone;
    }

    public XWorldBorder setWarningTime(Duration duration) {
        if (this.warningTime == duration) {
            return this;
        }
        this.warningTime = duration;
        this.update(Component.WARNING_DELAY);
        return this;
    }

    public Duration getWarningTime() {
        return this.warningTime;
    }

    public XWorldBorder setWarningDistance(int n) {
        if (this.warningBlocks == n) {
            return this;
        }
        this.warningBlocks = n;
        this.update(Component.WARNING_DISTANCE);
        return this;
    }

    public double getSizeLerpTarget() {
        return this.sizeLerpTarget;
    }

    public XWorldBorder setSizeLerpTarget(double d) {
        if (this.sizeLerpTarget == d) {
            return this;
        }
        this.sizeLerpTarget = d;
        this.update(Component.SIZE_LERP);
        return this;
    }

    public int getWarningDistance() {
        return this.warningBlocks;
    }

    public XWorldBorder setCenter(double d, double d2) {
        if (this.centerX == d && this.centerZ == d2) {
            return this;
        }
        this.centerX = d;
        this.centerZ = d2;
        this.updateBorderBounds();
        this.update(Component.CENTER);
        return this;
    }

    public Vector getCenter() {
        return new Vector(this.centerX, 0.0, this.centerZ);
    }

    public XWorldBorder setSize(double d, @Nonnull Duration duration) {
        if (this.size == d && this.sizeLerpTime == duration) {
            return this;
        }
        this.size = d;
        this.sizeLerpTime = duration;
        this.updateBorderBounds();
        this.update(Component.SIZE);
        if (Duration.ZERO != duration) {
            this.update(Component.SIZE_LERP);
        }
        return this;
    }

    private void updateBorderBounds() {
        this.borderBounds = this.size <= 0.0 ? null : new BorderBounds();
    }

    private void update(Component component) {
        if (SUPPORTS_SEPARATE_PACKETS) {
            this.updateRequired.add(component);
        }
    }

    public boolean isWithinBorder(Vector vector) {
        if (this.borderBounds == null) {
            return false;
        }
        return vector.getX() + 1.0 > this.borderBounds.minX && vector.getX() < this.borderBounds.maxX && vector.getZ() + 1.0 > this.borderBounds.minZ && vector.getZ() < this.borderBounds.maxZ;
    }

    public double getDistanceToBorder(Vector vector) {
        if (this.borderBounds == null) {
            return this.getCenter().distanceSquared(vector);
        }
        double d = vector.getX();
        double d2 = vector.getZ();
        double d3 = d2 - this.borderBounds.minZ;
        double d4 = this.borderBounds.maxZ - d2;
        double d5 = d - this.borderBounds.minX;
        double d6 = this.borderBounds.maxX - d;
        double d7 = Math.min(d5, d6);
        d7 = Math.min(d7, d3);
        return Math.min(d7, d4);
    }

    public void remove() {
        WORLD_BORDERS.remove(this.player);
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        WorldBorder worldBorder = player.getWorld().getWorldBorder();
        XWorldBorder.from(worldBorder).setPlayer(player).send(true);
    }

    public static void remove(Player player) {
        XWorldBorder xWorldBorder = XWorldBorder.get(player);
        if (xWorldBorder == null) {
            return;
        }
        xWorldBorder.remove();
    }

    public XWorldBorder setPlayer(Player player) {
        if (player.getUniqueId().equals(this.player)) {
            return this;
        }
        WORLD_BORDERS.remove(this.player, this);
        WORLD_BORDERS.put(player.getUniqueId(), this);
        this.player = player.getUniqueId();
        this.init = true;
        return this;
    }

    public XWorldBorder send() {
        return this.send(false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public XWorldBorder send(boolean bl) {
        Player player = this.getPlayer();
        if (player == null) {
            return this;
        }
        boolean bl2 = bl || this.init;
        this.init = false;
        try {
            if (SUPPORTS_SEPARATE_PACKETS && !bl2) {
                Object[] objectArray = new Object[this.updateRequired.size()];
                int n = 0;
                for (Component component : this.updateRequired) {
                    component.setHandle(this);
                    objectArray[n++] = component.createPacket(this);
                }
                MinecraftConnection.sendPacket(player, objectArray);
            } else {
                for (Component component : this.updateRequired) {
                    component.setHandle(this);
                }
                Object object = XReflection.supports(17) ? PACKET_INIT.invoke(this.handle) : PACKET_INIT.invoke(this.handle, INITIALIZE);
                MinecraftConnection.sendPacket(player, object);
            }
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        finally {
            this.updateRequired.clear();
        }
        return this;
    }

    private Object createHandle() {
        Objects.requireNonNull(this.world, "No world specified");
        try {
            Object object = WORLDBORDER.invoke();
            Object object2 = WORLD_HANDLE.invoke(this.world);
            WORLDBORDER_WORLD.invoke(object, object2);
            return object;
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    static {
        boolean bl;
        Object object;
        MinecraftClassHandle minecraftClassHandle;
        MinecraftClassHandle minecraftClassHandle2;
        MinecraftClassHandle minecraftClassHandle3;
        MethodHandle methodHandle;
        MethodHandle methodHandle2;
        MethodHandle methodHandle3;
        MethodHandle methodHandle4;
        MethodHandle methodHandle5;
        MethodHandle methodHandle6;
        Object var1_1;
        block7: {
            WORLD_BORDERS = new HashMap<UUID, XWorldBorder>();
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            var1_1 = null;
            Object var2_2 = null;
            Object var3_3 = null;
            Object var4_4 = null;
            Object var5_5 = null;
            Object var6_6 = null;
            Object var7_7 = null;
            Object var8_8 = null;
            Object var9_9 = null;
            methodHandle6 = null;
            methodHandle5 = null;
            methodHandle4 = null;
            methodHandle3 = null;
            methodHandle2 = null;
            methodHandle = null;
            minecraftClassHandle3 = XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.level.border").named("WorldBorder");
            minecraftClassHandle2 = XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "server.level").map(MinecraftMapping.MOJANG, "ServerLevel").map(MinecraftMapping.SPIGOT, "WorldServer");
            minecraftClassHandle = XReflection.ofMinecraft().inPackage(MinecraftPackage.CB).named("CraftWorld");
            try {
                if (XReflection.supports(17)) break block7;
                try {
                    object = Class.forName("EnumWorldBorderAction");
                }
                catch (ClassNotFoundException classNotFoundException) {
                    object = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS).named("PacketPlayOutWorldBorder$EnumWorldBorderAction").unreflect();
                }
                methodHandle6 = lookup.findConstructor((Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS).named("PacketPlayOutWorldBorder").unreflect(), MethodType.methodType(Void.TYPE, minecraftClassHandle3.reflect(), new Class[]{object}));
                for (Object t : ((Class)object).getEnumConstants()) {
                    if (!t.toString().equals("INITIALIZE")) continue;
                    var1_1 = t;
                    break;
                }
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        try {
            object = string -> {
                try {
                    return XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.protocol.game").named((String)string).constructor(minecraftClassHandle3).reflect();
                }
                catch (ReflectiveOperationException reflectiveOperationException) {
                    throw new RuntimeException(reflectiveOperationException);
                }
            };
            methodHandle5 = (MethodHandle)object.apply("ClientboundSetBorderWarningDistancePacket");
            methodHandle4 = (MethodHandle)object.apply("ClientboundSetBorderWarningDelayPacket");
            methodHandle3 = (MethodHandle)object.apply("ClientboundSetBorderLerpSizePacket");
            methodHandle6 = (MethodHandle)object.apply("ClientboundInitializeBorderPacket");
            methodHandle2 = (MethodHandle)object.apply("ClientboundSetBorderCenterPacket");
            methodHandle = (MethodHandle)object.apply("ClientboundSetBorderSizePacket");
            bl = true;
        }
        catch (Throwable throwable) {
            bl = false;
        }
        PACKET_INIT = methodHandle6;
        PACKET_SIZE = methodHandle;
        PACKET_CENTER = methodHandle2;
        PACKET_LERP_SIZE = methodHandle3;
        PACKET_WARNING_DELAY = methodHandle4;
        PACKET_WARNING_DISTANCE = methodHandle5;
        SUPPORTS_SEPARATE_PACKETS = bl;
        WORLD_HANDLE = (MethodHandle)minecraftClassHandle.method().named("getHandle").returns(minecraftClassHandle2).unreflect();
        INITIALIZE = var1_1;
        WORLDBORDER = (MethodHandle)minecraftClassHandle3.constructor().unreflect();
        WORLDBORDER_WORLD = (MethodHandle)minecraftClassHandle3.field().setter().named("world").returns(minecraftClassHandle2).unreflect();
        CENTER = (MethodHandle)((MethodMemberHandle)minecraftClassHandle3.method().named(XReflection.v(18, "c").orElse("setCenter")).returns((Class)Void.TYPE)).parameters(Double.TYPE, Double.TYPE).unreflect();
        SIZE = (MethodHandle)((MethodMemberHandle)minecraftClassHandle3.method().named(XReflection.v(18, "a").orElse("setSize")).returns((Class)Void.TYPE)).parameters(Double.TYPE).unreflect();
        WARNING_TIME = (MethodHandle)((MethodMemberHandle)minecraftClassHandle3.method().named(XReflection.v(18, "b").orElse("setWarningTime")).returns((Class)Void.TYPE)).parameters(Integer.TYPE).unreflect();
        WARNING_DISTANCE = (MethodHandle)((MethodMemberHandle)minecraftClassHandle3.method().named(XReflection.v(20, "c").v(18, "b").orElse("setWarningDistance")).returns((Class)Void.TYPE)).parameters(Integer.TYPE).unreflect();
        TRANSITION = (MethodHandle)((MethodMemberHandle)minecraftClassHandle3.method().named(XReflection.v(18, "a").orElse("transitionSizeBetween")).returns((Class)Void.TYPE)).parameters(Double.TYPE, Double.TYPE, Long.TYPE).unreflect();
    }

    private static enum Component {
        SIZE{

            @Override
            protected void setHandle(XWorldBorder xWorldBorder) {
                SIZE.invoke(xWorldBorder.handle, xWorldBorder.size);
            }

            @Override
            protected Object createPacket(XWorldBorder xWorldBorder) {
                return PACKET_SIZE.invoke(xWorldBorder.handle);
            }
        }
        ,
        SIZE_LERP{

            @Override
            protected void setHandle(XWorldBorder xWorldBorder) {
                TRANSITION.invoke(xWorldBorder.handle, xWorldBorder.sizeLerpTarget, xWorldBorder.size, xWorldBorder.sizeLerpTime.toMillis());
            }

            @Override
            protected Object createPacket(XWorldBorder xWorldBorder) {
                return PACKET_LERP_SIZE.invoke(xWorldBorder.handle);
            }
        }
        ,
        WARNING_DISTANCE{

            @Override
            protected void setHandle(XWorldBorder xWorldBorder) {
                WARNING_DISTANCE.invoke(xWorldBorder.handle, xWorldBorder.warningBlocks);
            }

            @Override
            protected Object createPacket(XWorldBorder xWorldBorder) {
                return PACKET_WARNING_DISTANCE.invoke(xWorldBorder.handle);
            }
        }
        ,
        WARNING_DELAY{

            @Override
            protected void setHandle(XWorldBorder xWorldBorder) {
                WARNING_TIME.invoke(xWorldBorder.handle, xWorldBorder.warningBlocks);
            }

            @Override
            protected Object createPacket(XWorldBorder xWorldBorder) {
                return PACKET_WARNING_DELAY.invoke(xWorldBorder.handle);
            }
        }
        ,
        CENTER{

            @Override
            protected void setHandle(XWorldBorder xWorldBorder) {
                CENTER.invoke(xWorldBorder.handle, xWorldBorder.centerX, xWorldBorder.centerZ);
            }

            @Override
            protected Object createPacket(XWorldBorder xWorldBorder) {
                return PACKET_CENTER.invoke(xWorldBorder.handle);
            }
        };


        protected abstract void setHandle(XWorldBorder var1);

        protected abstract Object createPacket(XWorldBorder var1);
    }

    private final class BorderBounds {
        public final double minX;
        public final double minZ;
        public final double maxX;
        public final double maxZ;

        private double clamp(double d, double d2, double d3) {
            return d < d2 ? d2 : Math.min(d, d3);
        }

        public BorderBounds() {
            this.minX = this.clamp(XWorldBorder.this.centerX - XWorldBorder.this.size / 2.0, -XWorldBorder.this.absoluteMaxSize, XWorldBorder.this.absoluteMaxSize);
            this.minZ = this.clamp(XWorldBorder.this.centerZ - XWorldBorder.this.size / 2.0, -XWorldBorder.this.absoluteMaxSize, XWorldBorder.this.absoluteMaxSize);
            this.maxX = this.clamp(XWorldBorder.this.centerX + XWorldBorder.this.size / 2.0, -XWorldBorder.this.absoluteMaxSize, XWorldBorder.this.absoluteMaxSize);
            this.maxZ = this.clamp(XWorldBorder.this.centerZ + XWorldBorder.this.size / 2.0, -XWorldBorder.this.absoluteMaxSize, XWorldBorder.this.absoluteMaxSize);
        }
    }

    public static final class Events
    implements Listener {
        @EventHandler
        public void onJoin(PlayerMoveEvent playerMoveEvent) {
            XWorldBorder xWorldBorder = XWorldBorder.get(playerMoveEvent.getPlayer());
            if (xWorldBorder == null) {
                return;
            }
            Player player = playerMoveEvent.getPlayer();
            Vector vector = player.getLocation().toVector();
            if (xWorldBorder.isWithinBorder(vector)) {
                return;
            }
            double d = xWorldBorder.getDistanceToBorder(vector);
            if (d < xWorldBorder.damageSafeZone) {
                return;
            }
            player.damage(xWorldBorder.damagePerBlock * d);
        }

        @EventHandler
        public void onJoin(PlayerJoinEvent playerJoinEvent) {
            XWorldBorder xWorldBorder = XWorldBorder.get(playerJoinEvent.getPlayer());
            if (xWorldBorder == null) {
                return;
            }
            xWorldBorder.send(true);
        }

        @EventHandler(ignoreCancelled=true, priority=EventPriority.MONITOR)
        public void onWorldChange(PlayerChangedWorldEvent playerChangedWorldEvent) {
            XWorldBorder xWorldBorder = XWorldBorder.get(playerChangedWorldEvent.getPlayer());
            if (xWorldBorder == null) {
                return;
            }
            xWorldBorder.send(true);
        }
    }
}

