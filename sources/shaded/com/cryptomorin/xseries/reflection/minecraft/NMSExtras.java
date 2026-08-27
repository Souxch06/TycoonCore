package com.cryptomorin.xseries.reflection.minecraft;

import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.jvm.MethodMemberHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftConnection;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftMapping;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftPackage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class NMSExtras {
    public static final Class<?> EntityLiving = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.entity").map(MinecraftMapping.MOJANG, "LivingEntity").map(MinecraftMapping.SPIGOT, "EntityLiving").unreflect();
    private static final MethodHandle GET_ENTITY_HANDLE;
    public static final MethodHandle EXP_PACKET;
    public static final MethodHandle ENTITY_PACKET;
    public static final MethodHandle WORLD_HANDLE;
    public static final MethodHandle ENTITY_HANDLE;
    public static final MethodHandle LIGHTNING_ENTITY;
    public static final MethodHandle VEC3D;
    public static final MethodHandle GET_DATA_WATCHER;
    public static final MethodHandle DATA_WATCHER_GET_ITEM;
    public static final MethodHandle DATA_WATCHER_SET_ITEM;
    public static final MethodHandle PACKET_PLAY_OUT_OPEN_SIGN_EDITOR;
    public static final MethodHandle PACKET_PLAY_OUT_BLOCK_CHANGE;
    public static final MethodHandle ANIMATION_PACKET;
    public static final MethodHandle ANIMATION_TYPE;
    public static final MethodHandle ANIMATION_ENTITY_ID;
    public static final MethodHandle PLAY_OUT_MULTI_BLOCK_CHANGE_PACKET;
    public static final MethodHandle MULTI_BLOCK_CHANGE_INFO;
    public static final MethodHandle CHUNK_WRAPPER_SET;
    public static final MethodHandle CHUNK_WRAPPER;
    public static final MethodHandle SHORTS_OR_INFO;
    public static final MethodHandle SET_BlockState;
    public static final MethodHandle BLOCK_POSITION;
    public static final MethodHandle PLAY_BLOCK_ACTION;
    public static final MethodHandle GET_BUKKIT_ENTITY;
    public static final MethodHandle GET_BLOCK_TYPE;
    public static final MethodHandle GET_BLOCK;
    public static final MethodHandle GET_IBlockState;
    public static final MethodHandle SANITIZE_LINES;
    public static final MethodHandle TILE_ENTITY_SIGN;
    public static final MethodHandle TILE_ENTITY_SIGN__GET_UPDATE_PACKET;
    public static final MethodHandle TILE_ENTITY_SIGN__SET_LINE;
    public static final MethodHandle SIGN_TEXT;
    public static final Class<?> BlockState;
    public static final Class<?> MULTI_BLOCK_CHANGE_INFO_CLASS;

    private NMSExtras() {
    }

    public static void setExp(Player player, float f, int n, int n2) {
        try {
            Object object = EXP_PACKET.invoke(f, n, n2);
            MinecraftConnection.sendPacket(player, object);
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static void lightning(Player player, Location location, boolean bl) {
        NMSExtras.lightning(Collections.singletonList(player), location, bl);
    }

    public static void lightning(Collection<Player> collection, Location location, boolean bl) {
        try {
            Object object = WORLD_HANDLE.invoke(location.getWorld());
            if (!XReflection.supports(16)) {
                Object object2 = LIGHTNING_ENTITY.invoke(object, location.getX(), location.getY(), location.getZ(), false, false);
                Object object3 = ENTITY_PACKET.invoke(object2);
                for (Player player : collection) {
                    if (bl) {
                        XSound.ENTITY_LIGHTNING_BOLT_THUNDER.record().soundPlayer().forPlayers(player).play();
                    }
                    MinecraftConnection.sendPacket(player, object3);
                }
            } else {
                Class clazz = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.entity").map(MinecraftMapping.MOJANG, "EntityType").map(MinecraftMapping.SPIGOT, "EntityTypes").unreflect();
                Object object4 = clazz.getField(XReflection.supports(17) ? "U" : "LIGHTNING_BOLT").get(clazz);
                Object object5 = LIGHTNING_ENTITY.invoke(object4, object);
                Object object6 = object5.getClass().getMethod("getId", new Class[0]).invoke(object5, new Object[0]);
                Object object7 = object5.getClass().getMethod("getUniqueID", new Class[0]).invoke(object5, new Object[0]);
                Object object8 = VEC3D.invoke(0.0, 0.0, 0.0);
                Object object9 = ENTITY_PACKET.invoke(object6, object7, location.getX(), location.getY(), location.getZ(), 0.0f, 0.0f, object4, 0, object8);
                for (Player player : collection) {
                    if (bl) {
                        XSound.ENTITY_LIGHTNING_BOLT_THUNDER.record().soundPlayer().forPlayers(player).play();
                    }
                    MinecraftConnection.sendPacket(player, object9);
                }
            }
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static Object getData(Object object, Object object2) {
        try {
            return DATA_WATCHER_GET_ITEM.invoke(object, object2);
        }
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    @Nullable
    public static Object getEntityHandle(Entity entity) {
        Objects.requireNonNull(entity, "Cannot get handle of null entity");
        try {
            return GET_ENTITY_HANDLE.invoke(entity);
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    public static Object getDataWatcher(Object object) {
        try {
            return GET_DATA_WATCHER.invoke(object);
        }
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static Object setData(Object object, Object object2, Object object3) {
        try {
            return DATA_WATCHER_SET_ITEM.invoke(object, object2, object3);
        }
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static Object getStaticFieldIgnored(Class<?> clazz, String string) {
        return NMSExtras.getStaticField(clazz, string, true);
    }

    public static Object getStaticField(Class<?> clazz, String string, boolean bl) {
        try {
            Field field = clazz.getDeclaredField(string);
            field.setAccessible(true);
            return field.get(null);
        }
        catch (Throwable throwable) {
            if (!bl) {
                throw new RuntimeException(throwable);
            }
            return null;
        }
    }

    public static void spinEntity(LivingEntity livingEntity, boolean bl) {
        if (!EntityPose.SPIN_ATTACK.isSupported()) {
            throw new UnsupportedOperationException("Spin attacks are not supported in " + XReflection.getVersionInformation());
        }
        NMSExtras.setLivingEntityFlag((Entity)livingEntity, LivingEntityFlags.SPIN_ATTACK.getBit(), bl);
    }

    public static void setLivingEntityFlag(Entity entity, int n, boolean bl) {
        Object object = NMSExtras.getEntityHandle(entity);
        Object object2 = NMSExtras.getDataWatcher(object);
        Object object3 = DataWatcherItemType.DATA_LIVING_ENTITY_FLAGS.getId();
        byte by = (Byte)NMSExtras.getData(object2, object3);
        int n2 = bl ? by | n : by & ~n;
        NMSExtras.setData(object2, object3, (byte)n2);
    }

    public static boolean hasLivingEntityFlag(Entity entity, int n) {
        Object object = NMSExtras.getEntityHandle(entity);
        Object object2 = NMSExtras.getDataWatcher(object);
        byte by = (Byte)NMSExtras.getData(object2, DataWatcherItemType.DATA_LIVING_ENTITY_FLAGS.getId());
        return (by & n) != 0;
    }

    public boolean isAutoSpinAttack(LivingEntity livingEntity) {
        return NMSExtras.hasLivingEntityFlag((Entity)livingEntity, LivingEntityFlags.SPIN_ATTACK.getBit());
    }

    public static void animation(Collection<? extends Player> collection, LivingEntity livingEntity, Animation animation) {
        try {
            Object object;
            if (XReflection.supports(17)) {
                object = ANIMATION_PACKET.invoke(ENTITY_HANDLE.invoke(livingEntity), animation.ordinal());
            } else {
                object = ANIMATION_PACKET.invoke();
                ANIMATION_TYPE.invoke(object, animation.ordinal());
                ANIMATION_ENTITY_ID.invoke(object, livingEntity.getEntityId());
            }
            for (Player player : collection) {
                MinecraftConnection.sendPacket(player, object);
            }
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static void chest(Block block, boolean bl) {
        Location location = block.getLocation();
        try {
            Object object = WORLD_HANDLE.invoke(location.getWorld());
            Object object2 = XReflection.v(19, () -> {
                try {
                    return BLOCK_POSITION.invoke(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                }
                catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }).orElse(() -> {
                try {
                    return BLOCK_POSITION.invoke(location.getX(), location.getY(), location.getZ());
                }
                catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            });
            Object object3 = GET_BLOCK.invoke(GET_BLOCK_TYPE.invoke(object, object2));
            PLAY_BLOCK_ACTION.invoke(object, object2, object3, 1, bl ? 1 : 0);
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Deprecated
    protected static void sendBlockChange(Player player, Chunk chunk, Map<WorldlessBlockWrapper, Object> map) {
        try {
            Object object = PLAY_OUT_MULTI_BLOCK_CHANGE_PACKET.invoke();
            if (XReflection.supports(16)) {
                Object object2 = CHUNK_WRAPPER.invoke(chunk.getX(), chunk.getZ());
                CHUNK_WRAPPER_SET.invoke(object2);
                Object object3 = Array.newInstance(BlockState, map.size());
                Object object4 = Array.newInstance(Short.TYPE, map.size());
                int n = 0;
                for (Map.Entry<WorldlessBlockWrapper, Object> entry : map.entrySet()) {
                    Block block = entry.getKey().block;
                    int n2 = block.getX() & 0xF;
                    int n3 = block.getY() & 0xF;
                    int n4 = block.getZ() & 0xF;
                    ++n;
                }
                SHORTS_OR_INFO.invoke(object, object4);
                SET_BlockState.invoke(object, object3);
            } else {
                Object object5 = CHUNK_WRAPPER.invoke(chunk.getX(), chunk.getZ());
                CHUNK_WRAPPER_SET.invoke(object5);
                Object object6 = Array.newInstance(MULTI_BLOCK_CHANGE_INFO_CLASS, map.size());
                int n = 0;
                for (Map.Entry<WorldlessBlockWrapper, Object> entry : map.entrySet()) {
                    Block block = entry.getKey().block;
                    int n5 = block.getX() & 0xF;
                    int n6 = block.getZ() & 0xF;
                    ++n;
                }
                SHORTS_OR_INFO.invoke(object, object6);
            }
            MinecraftConnection.sendPacket(player, object);
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static void openSign(Player player, DyeColor dyeColor, String[] stringArray, boolean bl) {
        try {
            Object object;
            Object object2;
            Location location = player.getLocation();
            Object object3 = BLOCK_POSITION.invoke(location.getBlockX(), 1, location.getBlockY());
            Object object4 = GET_IBlockState.invoke(Material.OAK_SIGN, (byte)0);
            Object object5 = PACKET_PLAY_OUT_BLOCK_CHANGE.invoke(object3, object4);
            Object object6 = SANITIZE_LINES.invoke(stringArray);
            Object object7 = TILE_ENTITY_SIGN.invoke(object3, object4);
            if (XReflection.supports(20)) {
                object2 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.item").map(MinecraftMapping.MOJANG, "DyeColor").map(MinecraftMapping.SPIGOT, "EnumColor").unreflect();
                object = null;
                for (Field field : ((Class)object2).getFields()) {
                    Object object8 = field.get(null);
                    String string = (String)((Class)object2).getDeclaredMethod("b", new Class[0]).invoke(object8, new Object[0]);
                    if (!dyeColor.name().equalsIgnoreCase(string)) continue;
                    object = object8;
                    break;
                }
                Object object9 = SIGN_TEXT.invoke(object6, object6, object, bl);
                TILE_ENTITY_SIGN__SET_LINE.invoke(object9, true);
            } else {
                for (int i = 0; i < stringArray.length; ++i) {
                    object = Array.get(object6, i);
                    TILE_ENTITY_SIGN__SET_LINE.invoke(object7, i, object, object);
                }
            }
            object2 = TILE_ENTITY_SIGN__GET_UPDATE_PACKET.invoke(object7);
            object = XReflection.v(20, PACKET_PLAY_OUT_OPEN_SIGN_EDITOR.invoke(object3, true)).orElse(PACKET_PLAY_OUT_OPEN_SIGN_EDITOR.invoke(object3));
            MinecraftConnection.sendPacket(player, object5, object2, object);
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    static {
        BlockState = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.level.block.state").map(MinecraftMapping.MOJANG, "BlockState").map(MinecraftMapping.SPIGOT, "IBlockData").unreflect();
        MULTI_BLOCK_CHANGE_INFO_CLASS = null;
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle methodHandle = null;
        MethodHandle methodHandle2 = null;
        MethodHandle methodHandle3 = null;
        MethodHandle methodHandle4 = null;
        MethodHandle methodHandle5 = null;
        MethodHandle methodHandle6 = null;
        MethodHandle methodHandle7 = null;
        MethodHandle methodHandle8 = null;
        MethodHandle methodHandle9 = null;
        MethodHandle methodHandle10 = null;
        MethodHandle methodHandle11 = null;
        MethodHandle methodHandle12 = null;
        MethodHandle methodHandle13 = null;
        MethodHandle methodHandle14 = null;
        MethodHandle methodHandle15 = null;
        MethodHandle methodHandle16 = null;
        MethodHandle methodHandle17 = null;
        MethodHandle methodHandle18 = null;
        MethodHandle methodHandle19 = null;
        MethodHandle methodHandle20 = null;
        MethodHandle methodHandle21 = null;
        MethodHandle methodHandle22 = null;
        Object var23_23 = null;
        Object var24_24 = null;
        MethodHandle methodHandle23 = null;
        Object var26_26 = null;
        Object var27_27 = null;
        Object var28_28 = null;
        MethodHandle methodHandle24 = null;
        MethodHandle methodHandle25 = null;
        MethodHandle methodHandle26 = null;
        MethodHandle methodHandle27 = null;
        try {
            AnnotatedElement annotatedElement;
            Cloneable cloneable;
            Class clazz = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.CB, "entity").named("CraftEntity").unreflect();
            Class clazz2 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.entity").map(MinecraftMapping.MOJANG, "EntityType").map(MinecraftMapping.SPIGOT, "EntityTypes").unreflect();
            Class clazz3 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.entity").named("Entity").unreflect();
            Class clazz4 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.CB, "entity").named("CraftEntity").unreflect();
            Class clazz5 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.phys").map(MinecraftMapping.MOJANG, "Vec3").map(MinecraftMapping.SPIGOT, "Vec3D").unreflect();
            Class clazz6 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.level").map(MinecraftMapping.MOJANG, "Level").map(MinecraftMapping.SPIGOT, "World").unreflect();
            Class clazz7 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.protocol.game").map(MinecraftMapping.MOJANG, "ClientboundOpenSignEditorPacket").map(MinecraftMapping.SPIGOT, "PacketPlayOutOpenSignEditor").unreflect();
            Class clazz8 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.protocol.game").map(MinecraftMapping.MOJANG, "ClientboundBlockUpdatePacket").map(MinecraftMapping.SPIGOT, "PacketPlayOutBlockChange").unreflect();
            Class clazz9 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.CB, "util").named("CraftMagicNumbers").unreflect();
            Class clazz10 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.CB, "block").named("CraftSign").unreflect();
            Class clazz11 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.chat").map(MinecraftMapping.MOJANG, "Component").map(MinecraftMapping.SPIGOT, "IChatBaseComponent").unreflect();
            Class clazz12 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.level.block.entity").map(MinecraftMapping.MOJANG, "SignBlockEntity").map(MinecraftMapping.SPIGOT, "TileEntitySign").unreflect();
            Class clazz13 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.protocol.game").map(MinecraftMapping.MOJANG, "ClientboundBlockEntityDataPacket").map(MinecraftMapping.SPIGOT, "PacketPlayOutTileEntityData").unreflect();
            Class clazz14 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.syncher").map(MinecraftMapping.MOJANG, "SynchedEntityData").map(MinecraftMapping.SPIGOT, "DataWatcher").unreflect();
            Class clazz15 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.syncher").map(MinecraftMapping.MOJANG, "SynchedEntityData$DataItem").map(MinecraftMapping.SPIGOT, "DataWatcher$Item").unreflect();
            Class clazz16 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.syncher").map(MinecraftMapping.MOJANG, "EntityDataAccessor").map(MinecraftMapping.SPIGOT, "DataWatcherObject").unreflect();
            methodHandle27 = lookup.findVirtual(clazz, "getHandle", MethodType.methodType(clazz3));
            methodHandle24 = (MethodHandle)((MethodMemberHandle)XReflection.of(clazz3).method().returns(clazz14)).map(MinecraftMapping.MOJANG, "getEntityData").map(MinecraftMapping.SPIGOT, XReflection.v(21, "ar").v(20, 5, "ap").v(20, 4, "an").v(20, 2, "al").v(19, "aj").v(18, "ai").orElse("getDataWatcher")).unreflect();
            methodHandle25 = (MethodHandle)((MethodMemberHandle)XReflection.of(clazz14).method().returns((Class)Object.class)).parameters(clazz16).map(MinecraftMapping.MOJANG, "get").map(MinecraftMapping.SPIGOT, XReflection.v(20, 5, "a").v(20, "b").v(18, "a").orElse("get")).unreflect();
            methodHandle26 = (MethodHandle)((MethodMemberHandle)XReflection.of(clazz14).method().returns((Class)Void.TYPE)).parameters(clazz16, Object.class).map(MinecraftMapping.MOJANG, "set").map(MinecraftMapping.SPIGOT, XReflection.v(20, 5, "a").v(18, "b").orElse("set")).unreflect();
            methodHandle12 = lookup.findVirtual(clazz3, "getBukkitEntity", MethodType.methodType(clazz4));
            methodHandle4 = lookup.findVirtual(clazz4, "getHandle", MethodType.methodType(clazz3));
            methodHandle = lookup.findConstructor((Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.protocol.game").map(MinecraftMapping.MOJANG, "ClientboundSetExperiencePacket").map(MinecraftMapping.SPIGOT, "PacketPlayOutExperience").unreflect(), MethodType.methodType(Void.TYPE, Float.TYPE, Integer.TYPE, Integer.TYPE));
            if (!XReflection.supports(16)) {
                methodHandle2 = (MethodHandle)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS).named("PacketPlayOutSpawnEntityWeather").constructor().parameters(clazz3).unreflect();
            } else {
                methodHandle6 = lookup.findConstructor(clazz5, MethodType.methodType(Void.TYPE, Double.TYPE, Double.TYPE, Double.TYPE));
                cloneable = new ArrayList<Class>(Arrays.asList(Integer.TYPE, UUID.class, Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, clazz2, Integer.TYPE, clazz5));
                if (XReflection.supports(19)) {
                    cloneable.add(Double.TYPE);
                }
                methodHandle2 = lookup.findConstructor((Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.protocol.game").map(MinecraftMapping.MOJANG, "ClientboundAddEntityPacket").map(MinecraftMapping.SPIGOT, "PacketPlayOutSpawnEntity").unreflect(), MethodType.methodType(Void.TYPE, cloneable));
            }
            methodHandle3 = lookup.findVirtual((Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.CB).named("CraftWorld").unreflect(), "getHandle", MethodType.methodType((Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "server.level").map(MinecraftMapping.MOJANG, "ServerLevel").map(MinecraftMapping.SPIGOT, "WorldServer").unreflect()));
            cloneable = XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.entity").map(MinecraftMapping.MOJANG, "LightningBolt").map(MinecraftMapping.SPIGOT, "EntityLightning");
            methodHandle5 = !XReflection.supports(16) ? lookup.findConstructor((Class)cloneable.unreflect(), MethodType.methodType(Void.TYPE, clazz6, Double.TYPE, Double.TYPE, Double.TYPE, Boolean.TYPE, Boolean.TYPE)) : lookup.findConstructor((Class)cloneable.unreflect(), MethodType.methodType(Void.TYPE, clazz2, clazz6));
            Class clazz17 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.protocol.game").map(MinecraftMapping.MOJANG, "ClientboundSectionBlocksUpdatePacket").map(MinecraftMapping.SPIGOT, "PacketPlayOutMultiBlockChange").unreflect();
            Class clazz18 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.level").map(MinecraftMapping.MOJANG, "ChunkPos").map(MinecraftMapping.SPIGOT, "ChunkCoordIntPair").unreflect();
            try {
                if (!XReflection.supports(16)) {
                    methodHandle23 = lookup.findConstructor(clazz18, MethodType.methodType(Void.TYPE, Integer.TYPE, Integer.TYPE));
                }
            }
            catch (IllegalAccessException | NoSuchMethodException reflectiveOperationException) {
                reflectiveOperationException.printStackTrace();
            }
            Class clazz19 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.protocol.game").map(MinecraftMapping.MOJANG, "ClientboundAnimatePacket").map(MinecraftMapping.SPIGOT, "PacketPlayOutAnimation").unreflect();
            methodHandle9 = lookup.findConstructor(clazz19, XReflection.supports(17) ? MethodType.methodType(Void.TYPE, clazz3, Integer.TYPE) : MethodType.methodType(Void.TYPE));
            if (!XReflection.supports(17)) {
                annotatedElement = clazz19.getDeclaredField("a");
                ((Field)annotatedElement).setAccessible(true);
                methodHandle11 = lookup.unreflectSetter((Field)annotatedElement);
                annotatedElement = clazz19.getDeclaredField("b");
                ((Field)annotatedElement).setAccessible(true);
                methodHandle10 = lookup.unreflectSetter((Field)annotatedElement);
            }
            annotatedElement = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "core").map(MinecraftMapping.MOJANG, "BlockPos").map(MinecraftMapping.SPIGOT, "BlockPosition").unreflect();
            Class clazz20 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.level.block").named("Block").unreflect();
            methodHandle13 = lookup.findConstructor((Class<?>)annotatedElement, XReflection.v(19, MethodType.methodType(Void.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE)).orElse(MethodType.methodType(Void.TYPE, Double.TYPE, Double.TYPE, Double.TYPE)));
            methodHandle15 = (MethodHandle)((MethodMemberHandle)XReflection.of(clazz6).method().returns((Class)BlockState)).parameters(new Class[]{annotatedElement}).map(MinecraftMapping.MOJANG, "getBlockState").map(MinecraftMapping.SPIGOT, XReflection.v(18, "a_").orElse("getType")).unreflect();
            methodHandle16 = XReflection.supports(21) ? (MethodHandle)((MethodMemberHandle)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.level.block.state").map(MinecraftMapping.MOJANG, "BlockBehaviour").map(MinecraftMapping.SPIGOT, "BlockBase").inner(XReflection.ofMinecraft().map(MinecraftMapping.MOJANG, "BlockStateBase").map(MinecraftMapping.SPIGOT, "BlockData")).method().returns(clazz20)).map(MinecraftMapping.MOJANG, "getBlock").map(MinecraftMapping.SPIGOT, "b").unreflect() : (MethodHandle)((MethodMemberHandle)XReflection.of(BlockState).method().returns(clazz20)).map(MinecraftMapping.MOJANG, "getBlock").map(MinecraftMapping.SPIGOT, XReflection.v(18, "b").orElse("getBlock")).unreflect();
            methodHandle14 = (MethodHandle)((MethodMemberHandle)XReflection.of(clazz6).method().returns((Class)Void.TYPE)).parameters(new Class[]{annotatedElement, clazz20, Integer.TYPE, Integer.TYPE}).map(MinecraftMapping.MOJANG, "blockEvent").map(MinecraftMapping.SPIGOT, XReflection.v(18, "a").orElse("playBlockAction")).unreflect();
            methodHandle7 = lookup.findConstructor(clazz7, XReflection.v(20, MethodType.methodType(Void.TYPE, annotatedElement, new Class[]{Boolean.TYPE})).orElse(MethodType.methodType(Void.TYPE, annotatedElement)));
            if (XReflection.supports(17)) {
                methodHandle8 = lookup.findConstructor(clazz8, MethodType.methodType(Void.TYPE, annotatedElement, new Class[]{BlockState}));
                methodHandle17 = lookup.findStatic(clazz9, "getBlock", MethodType.methodType(BlockState, Material.class, Byte.TYPE));
                methodHandle18 = lookup.findStatic(clazz10, XReflection.v(17, "sanitizeLines").orElse("SANITIZE_LINES"), MethodType.methodType(XReflection.toArrayClass(clazz11), String[].class));
                methodHandle19 = lookup.findConstructor(clazz12, MethodType.methodType(Void.TYPE, annotatedElement, new Class[]{BlockState}));
                methodHandle20 = (MethodHandle)((MethodMemberHandle)XReflection.of(clazz12).method().returns(clazz13)).map(MinecraftMapping.MOJANG, "getUpdatePacket").map(MinecraftMapping.SPIGOT, XReflection.v(20, 5, "l").v(20, 4, "m").v(20, "j").v(19, "f").v(18, "c").orElse("getUpdatePacket")).unreflect();
                if (XReflection.supports(20)) {
                    Class clazz21 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.level.block.entity").named("SignText").unreflect();
                    if (!XReflection.supports(20, 6)) {
                        methodHandle21 = lookup.findVirtual(clazz12, "a", MethodType.methodType(Boolean.TYPE, clazz21, Boolean.TYPE));
                    }
                    Class clazz22 = (Class)XReflection.of(clazz11).asArray().unreflect();
                    Class clazz23 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.item").map(MinecraftMapping.MOJANG, "DyeColor").map(MinecraftMapping.SPIGOT, "EnumColor").unreflect();
                    methodHandle22 = lookup.findConstructor(clazz21, MethodType.methodType(Void.TYPE, clazz22, clazz22, clazz23, Boolean.TYPE));
                } else {
                    methodHandle21 = lookup.findVirtual(clazz12, "a", MethodType.methodType(Void.TYPE, Integer.TYPE, clazz11, clazz11));
                }
            }
        }
        catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException reflectiveOperationException) {
            reflectiveOperationException.printStackTrace();
        }
        GET_ENTITY_HANDLE = methodHandle27;
        GET_DATA_WATCHER = methodHandle24;
        DATA_WATCHER_GET_ITEM = methodHandle25;
        DATA_WATCHER_SET_ITEM = methodHandle26;
        EXP_PACKET = methodHandle;
        ENTITY_PACKET = methodHandle2;
        WORLD_HANDLE = methodHandle3;
        ENTITY_HANDLE = methodHandle4;
        LIGHTNING_ENTITY = methodHandle5;
        VEC3D = methodHandle6;
        PACKET_PLAY_OUT_OPEN_SIGN_EDITOR = methodHandle7;
        PACKET_PLAY_OUT_BLOCK_CHANGE = methodHandle8;
        ANIMATION_PACKET = methodHandle9;
        ANIMATION_TYPE = methodHandle10;
        ANIMATION_ENTITY_ID = methodHandle11;
        BLOCK_POSITION = methodHandle13;
        PLAY_BLOCK_ACTION = methodHandle14;
        GET_BLOCK_TYPE = methodHandle15;
        GET_BLOCK = methodHandle16;
        GET_IBlockState = methodHandle17;
        SANITIZE_LINES = methodHandle18;
        TILE_ENTITY_SIGN = methodHandle19;
        TILE_ENTITY_SIGN__GET_UPDATE_PACKET = methodHandle20;
        TILE_ENTITY_SIGN__SET_LINE = methodHandle21;
        GET_BUKKIT_ENTITY = methodHandle12;
        PLAY_OUT_MULTI_BLOCK_CHANGE_PACKET = var23_23;
        MULTI_BLOCK_CHANGE_INFO = var24_24;
        CHUNK_WRAPPER = methodHandle23;
        CHUNK_WRAPPER_SET = var26_26;
        SHORTS_OR_INFO = var27_27;
        SET_BlockState = var28_28;
        SIGN_TEXT = methodHandle22;
    }

    public static enum EntityPose {
        STANDING("a"),
        FALL_FLYING("b"),
        SLEEPING("c"),
        SWIMMING("d"),
        SPIN_ATTACK("e"),
        CROUCHING("f"),
        LONG_JUMPING("g"),
        DYING("h"),
        CROAKING("i"),
        USING_TONGUE("j"),
        SITTING("k"),
        ROARING("l"),
        SNIFFING("m"),
        EMERGING("n"),
        DIGGING("o");

        public final Object enumValue;
        private final boolean supported;

        private EntityPose(String string2) {
            boolean bl = true;
            Object object = null;
            try {
                Object object2 = XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "world.entity").map(MinecraftMapping.MOJANG, "Pose").map(MinecraftMapping.SPIGOT, "EntityPose").reflect();
                object = ((Class)object2).getDeclaredField(XReflection.v(17, string2).orElse(this.name())).get(null);
            }
            catch (Throwable throwable) {
                bl = false;
            }
            this.supported = bl;
            this.enumValue = object;
        }

        public boolean isSupported() {
            return this.supported;
        }

        public Object getEnumValue() {
            return this.enumValue;
        }
    }

    public static enum LivingEntityFlags {
        SPIN_ATTACK(4);

        private final byte bit;

        private LivingEntityFlags(int n2) {
            this.bit = (byte)n2;
        }

        public byte getBit() {
            return this.bit;
        }
    }

    public static enum DataWatcherItemType {
        DATA_LIVING_ENTITY_FLAGS(NMSExtras.getStaticFieldIgnored(EntityLiving, "t"));

        private final Object id;
        private final boolean supported;

        private DataWatcherItemType(Object object) {
            boolean bl = true;
            Object object2 = null;
            try {
                object2 = object;
            }
            catch (Throwable throwable) {
                bl = false;
            }
            this.supported = bl;
            this.id = object2;
        }

        public boolean isSupported() {
            return this.supported;
        }

        public Object getId() {
            return this.id;
        }
    }

    public static enum Animation {
        SWING_MAIN_ARM,
        HURT,
        LEAVE_BED,
        SWING_OFF_HAND,
        CRITICAL_EFFECT,
        MAGIC_CRITICAL_EFFECT;

    }

    public static class WorldlessBlockWrapper {
        public final Block block;

        public WorldlessBlockWrapper(Block block) {
            this.block = block;
        }

        public int hashCode() {
            return (this.block.getY() + this.block.getZ() * 31) * 31 + this.block.getX();
        }

        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Block)) {
                return false;
            }
            Block block = (Block)object;
            return this.block.getX() == block.getX() && this.block.getY() == block.getY() && this.block.getZ() == block.getZ();
        }
    }
}

