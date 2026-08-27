package com.cryptomorin.xseries.reflection.minecraft;

import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.jvm.MethodMemberHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftMapping;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftPackage;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MinecraftConnection {
    public static final MinecraftClassHandle ServerPlayer = XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "server.level").map(MinecraftMapping.MOJANG, "ServerPlayer").map(MinecraftMapping.SPIGOT, "EntityPlayer");
    public static final MinecraftClassHandle CraftPlayer = XReflection.ofMinecraft().inPackage(MinecraftPackage.CB, "entity").named("CraftPlayer");
    public static final MinecraftClassHandle ServerPlayerConnection = XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "server.network").map(MinecraftMapping.MOJANG, "ServerPlayerConnection").map(MinecraftMapping.SPIGOT, "PlayerConnection");
    public static final MinecraftClassHandle ServerGamePacketListenerImpl = XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "server.network").map(MinecraftMapping.MOJANG, "ServerGamePacketListenerImpl").map(MinecraftMapping.SPIGOT, "PlayerConnection");
    public static final MinecraftClassHandle Packet = XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.protocol").map(MinecraftMapping.SPIGOT, "Packet");
    private static final MethodHandle PLAYER_CONNECTION = (MethodHandle)ServerPlayer.field().getter().returns(ServerGamePacketListenerImpl).map(MinecraftMapping.MOJANG, "connection").map(MinecraftMapping.OBFUSCATED, XReflection.v(20, "c").v(17, "b").orElse("playerConnection")).unreflect();
    private static final MethodHandle GET_HANDLE = (MethodHandle)CraftPlayer.method().named("getHandle").returns(ServerPlayer).unreflect();
    private static final MethodHandle SEND_PACKET = (MethodHandle)((MethodMemberHandle)ServerPlayerConnection.method().returns((Class)Void.TYPE)).parameters(Packet).map(MinecraftMapping.MOJANG, "send").map(MinecraftMapping.OBFUSCATED, XReflection.v(20, 2, "b").v(18, "a").orElse("sendPacket")).unreflect();

    @NotNull
    public static Object getHandle(@NotNull Player player) {
        Objects.requireNonNull(player, "Cannot get handle of null player");
        try {
            return GET_HANDLE.invoke(player);
        }
        catch (Throwable throwable) {
            throw XReflection.throwCheckedException(throwable);
        }
    }

    @Nullable
    public static Object getConnection(@NotNull Player player) {
        Objects.requireNonNull(player, "Cannot get connection of null player");
        try {
            Object object = GET_HANDLE.invoke(player);
            return PLAYER_CONNECTION.invoke(object);
        }
        catch (Throwable throwable) {
            throw XReflection.throwCheckedException(throwable);
        }
    }

    @NotNull
    public static void sendPacket(@NotNull Player player, Object ... objectArray) {
        Objects.requireNonNull(player, () -> "Can't send packet to null player: " + Arrays.toString(objectArray));
        Objects.requireNonNull(objectArray, () -> "Can't send null packets to player: " + player);
        try {
            Object object = GET_HANDLE.invoke(player);
            Object object2 = PLAYER_CONNECTION.invoke(object);
            if (object2 != null) {
                for (Object object3 : objectArray) {
                    Objects.requireNonNull(object3, "Null packet detected between packets array");
                    SEND_PACKET.invoke(object2, object3);
                }
            }
        }
        catch (Throwable throwable) {
            throw new RuntimeException("Failed to send packet to " + player + ": " + Arrays.toString(objectArray), throwable);
        }
    }
}

