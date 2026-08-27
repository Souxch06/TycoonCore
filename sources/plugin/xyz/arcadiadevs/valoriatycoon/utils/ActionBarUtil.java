package xyz.arcadiadevs.valoriatycoon.utils;

import com.awaitquality.api.spigot.chat.ChatUtil;
import com.cryptomorin.xseries.messages.ActionBar;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import xyz.arcadiadevs.valoriatycoon.utils.ServerVersion;

public class ActionBarUtil {
    private static boolean works = true;
    private static final String version = ServerVersion.getServerVersionString();
    private static final boolean useOldMethods = version.equalsIgnoreCase("v1_8_R1") || version.startsWith("1_7_");

    public static void sendActionBar(Player player, String string) {
        if (!player.isOnline()) {
            return;
        }
        ActionBar.sendActionBar(player, ChatUtil.translate(string));
    }

    private static void sendActionBarPost112(Player player, String string) {
        if (!player.isOnline()) {
            return;
        }
        try {
            Object obj;
            Class<?> clazz = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Object obj2 = clazz.cast(player);
            Class<?> clazz2 = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            Class<?> clazz3 = Class.forName("net.minecraft.server." + version + ".Packet");
            Class<?> clazz4 = Class.forName("net.minecraft.server." + version + ".ChatComponentText");
            Class<?> clazz5 = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> clazz6 = Class.forName("net.minecraft.server." + version + ".ChatMessageType");
            ?[] objArray = clazz6.getEnumConstants();
            Object var10_11 = null;
            ?[] objArray2 = objArray;
            int n = objArray.length;
            int n2 = 0;
            while (n2 < n) {
                obj = objArray2[n2];
                if (obj.toString().equals("GAME_INFO")) {
                    var10_11 = obj;
                }
                n2 = (byte)(n2 + 1);
            }
            obj = clazz4.getConstructor(String.class).newInstance(string);
            Object obj3 = clazz2.getConstructor(clazz5, clazz6).newInstance(obj, var10_11);
            Method method = clazz.getDeclaredMethod("getHandle", new Class[0]);
            Object object = method.invoke(obj2, new Object[0]);
            Field field = object.getClass().getDeclaredField("playerConnection");
            Object object2 = field.get(object);
            Method method2 = object2.getClass().getDeclaredMethod("sendPacket", clazz3);
            method2.invoke(object2, obj3);
        }
        catch (Exception exception) {
            exception.printStackTrace();
            works = false;
        }
    }

    private static void sendActionBarPre112(Player player, String string) {
        if (!player.isOnline()) {
            return;
        }
        try {
            Object obj;
            Object object;
            AccessibleObject accessibleObject;
            Object object2;
            GenericDeclaration genericDeclaration;
            Class<?> clazz = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Object obj2 = clazz.cast(player);
            Class<?> clazz2 = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            Class<?> clazz3 = Class.forName("net.minecraft.server." + version + ".Packet");
            if (useOldMethods) {
                genericDeclaration = Class.forName("net.minecraft.server." + version + ".ChatSerializer");
                object2 = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
                accessibleObject = ((Class)genericDeclaration).getDeclaredMethod("a", String.class);
                object = ((Class)object2).cast(((Method)accessibleObject).invoke(genericDeclaration, "{\"text\": \"" + string + "\"}"));
                obj = clazz2.getConstructor(new Class[]{object2, Byte.TYPE}).newInstance(object, (byte)2);
            } else {
                genericDeclaration = Class.forName("net.minecraft.server." + version + ".ChatComponentText");
                object2 = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
                accessibleObject = ((Class)genericDeclaration).getConstructor(String.class).newInstance(string);
                obj = clazz2.getConstructor(new Class[]{object2, Byte.TYPE}).newInstance(accessibleObject, (byte)2);
            }
            genericDeclaration = clazz.getDeclaredMethod("getHandle", new Class[0]);
            object2 = ((Method)genericDeclaration).invoke(obj2, new Object[0]);
            accessibleObject = object2.getClass().getDeclaredField("playerConnection");
            object = ((Field)accessibleObject).get(object2);
            Method method = object.getClass().getDeclaredMethod("sendPacket", clazz3);
            method.invoke(object, obj);
        }
        catch (Exception exception) {
            exception.printStackTrace();
            works = false;
        }
    }
}

