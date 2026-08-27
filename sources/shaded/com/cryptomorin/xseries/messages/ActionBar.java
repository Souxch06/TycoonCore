package com.cryptomorin.xseries.messages;

import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftConnection;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftPackage;
import com.google.common.base.Strings;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class ActionBar {
    private static final boolean USE_SPIGOT_API = XReflection.supports(12);
    private static final MethodHandle CHAT_COMPONENT_TEXT;
    private static final MethodHandle PACKET_PLAY_OUT_CHAT;
    private static final Object CHAT_MESSAGE_TYPE;
    private static final char TIME_SPECIFIER_START = '^';
    private static final char TIME_SPECIFIER_END = '|';

    private ActionBar() {
    }

    public static void sendActionBar(@Nonnull Plugin plugin, @Nonnull Player player, @Nullable String string) {
        int n;
        if (!Strings.isNullOrEmpty((String)string) && string.charAt(0) == '^' && (n = string.indexOf(124)) != -1) {
            int n2 = 0;
            try {
                n2 = Integer.parseInt(string.substring(1, n)) * 20;
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
            if (n2 >= 0) {
                ActionBar.sendActionBar(plugin, player, string.substring(n + 1), n2);
            }
        }
        ActionBar.sendActionBar(player, string);
    }

    public static void sendActionBar(@Nonnull Player player, @Nullable String string) {
        Objects.requireNonNull(player, "Cannot send action bar to null player");
        Objects.requireNonNull(string, "Cannot send null actionbar message");
        if (USE_SPIGOT_API) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText((String)string));
            return;
        }
        try {
            Object object = CHAT_COMPONENT_TEXT.invoke("{\"text\":\"" + string.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
            Object object2 = PACKET_PLAY_OUT_CHAT.invoke(object, CHAT_MESSAGE_TYPE);
            MinecraftConnection.sendPacket(player, object2);
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static void sendActionBar(@Nonnull Plugin plugin, final @Nonnull Player player, final @Nullable String string, final long l) {
        if (l < 1L) {
            return;
        }
        Objects.requireNonNull(plugin, "Cannot send consistent actionbar with null plugin");
        Objects.requireNonNull(player, "Cannot send actionbar to null player");
        Objects.requireNonNull(string, "Cannot send null actionbar message");
        new BukkitRunnable(){
            long repeater;
            {
                this.repeater = l;
            }

            public void run() {
                ActionBar.sendActionBar(player, string);
                this.repeater -= 40L;
                if (this.repeater - 40L < -20L) {
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 40L);
    }

    public static void sendPlayersActionBar(@Nullable String string) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ActionBar.sendActionBar(player, string);
        }
    }

    public static void clearActionBar(@Nonnull Player player) {
        ActionBar.sendActionBar(player, " ");
    }

    public static void clearPlayersActionBar() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ActionBar.clearActionBar(player);
        }
    }

    public static void sendActionBarWhile(@Nonnull Plugin plugin, final @Nonnull Player player, final @Nullable String string, final @Nonnull Callable<Boolean> callable) {
        new BukkitRunnable(){

            public void run() {
                try {
                    if (!((Boolean)callable.call()).booleanValue()) {
                        this.cancel();
                        return;
                    }
                }
                catch (Exception exception) {
                    exception.printStackTrace();
                }
                ActionBar.sendActionBar(player, string);
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 40L);
    }

    public static void sendActionBarWhile(@Nonnull Plugin plugin, final @Nonnull Player player, final @Nullable Callable<String> callable, final @Nonnull Callable<Boolean> callable2) {
        new BukkitRunnable(){

            public void run() {
                try {
                    if (!((Boolean)callable2.call()).booleanValue()) {
                        this.cancel();
                        return;
                    }
                    ActionBar.sendActionBar(player, (String)callable.call());
                }
                catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 40L);
    }

    static {
        MethodHandle methodHandle = null;
        MethodHandle methodHandle2 = null;
        Byte by = null;
        if (!USE_SPIGOT_API) {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class clazz = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.protocol.game").named("PacketPlayOutChat").unreflect();
            Class clazz2 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.chat").named("IChatBaseComponent").unreflect();
            Class clazz3 = (Class)XReflection.ofMinecraft().inPackage(MinecraftPackage.NMS, "network.chat").named("IChatBaseComponent$ChatSerializer").unreflect();
            try {
                methodHandle2 = lookup.findStatic(clazz3, "a", MethodType.methodType(clazz2, String.class));
                Class<?> clazz4 = Class.forName(XReflection.NMS_PACKAGE + XReflection.v(17, "network.chat").orElse("") + "ChatMessageType");
                MethodType methodType = MethodType.methodType(Void.TYPE, clazz2, clazz4);
                for (Object obj : clazz4.getEnumConstants()) {
                    String string = obj.toString();
                    if (!string.equals("GAME_INFO") && !string.equalsIgnoreCase("ACTION_BAR")) continue;
                    by = obj;
                    break;
                }
                methodHandle = lookup.findConstructor(clazz, methodType);
            }
            catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException reflectiveOperationException) {
                try {
                    by = 2;
                    methodHandle = lookup.findConstructor(clazz, MethodType.methodType(Void.TYPE, clazz2, Byte.TYPE));
                }
                catch (IllegalAccessException | NoSuchMethodException reflectiveOperationException2) {
                    reflectiveOperationException2.printStackTrace();
                }
            }
        }
        CHAT_MESSAGE_TYPE = by;
        CHAT_COMPONENT_TEXT = methodHandle2;
        PACKET_PLAY_OUT_CHAT = methodHandle;
    }
}

