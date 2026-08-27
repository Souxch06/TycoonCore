/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 */
package io.github.bananapuncher714.nbteditor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class NBTEditor {
    private static final Set<ReflectionTarget> reflectionTargets;
    private static final Map<ClassId, Class<?>> classCache;
    private static final Map<MethodId, Method> methodCache;
    private static final Map<ClassId, Constructor<?>> constructorCache;
    private static final Map<Class<?>, Constructor<?>> NBTConstructors;
    private static final Map<Class<?>, Class<?>> NBTClasses;
    private static final Map<Class<?>, Field> NBTTagFieldCache;
    private static Field NBTListData;
    private static Field NBTCompoundMap;
    private static Field skullProfile;
    private static final String VERSION;
    private static final MinecraftVersion LOCAL_VERSION;
    private static final String BUKKIT_VERSION;
    public static final Type COMPOUND;
    public static final Type LIST;
    public static final Type NEW_ELEMENT;
    public static final Type DELETE;
    public static final Type CUSTOM_DATA;
    public static final Type ITEMSTACK_COMPONENTS;

    private static Constructor<?> getNBTTagConstructor(Class<?> clazz) {
        return NBTConstructors.get(NBTEditor.getNBTTag(clazz));
    }

    private static Class<?> getNBTTag(Class<?> clazz) {
        if (NBTClasses.containsKey(clazz)) {
            return NBTClasses.get(clazz);
        }
        return clazz;
    }

    private static Object getNBTVar(Object object) {
        if (object == null) {
            return null;
        }
        Class<?> clazz = object.getClass();
        try {
            if (NBTTagFieldCache.containsKey(clazz)) {
                return NBTTagFieldCache.get(clazz).get(object);
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    private static Method getMethod(MethodId methodId) {
        if (methodCache.containsKey((Object)methodId)) {
            return methodCache.get((Object)methodId);
        }
        for (ReflectionTarget reflectionTarget : reflectionTargets) {
            if (!reflectionTarget.getVersion().lessThanOrEqualTo(LOCAL_VERSION)) continue;
            try {
                Method method = reflectionTarget.fetchMethod(methodId);
                if (method == null) continue;
                methodCache.put(methodId, method);
                return method;
            }
            catch (ClassNotFoundException | NoSuchMethodException | SecurityException exception) {
                exception.printStackTrace();
            }
        }
        methodCache.put(methodId, null);
        return null;
    }

    private static Constructor<?> getConstructor(ClassId classId) {
        if (constructorCache.containsKey((Object)classId)) {
            return constructorCache.get((Object)classId);
        }
        for (ReflectionTarget reflectionTarget : reflectionTargets) {
            if (!reflectionTarget.getVersion().lessThanOrEqualTo(LOCAL_VERSION)) continue;
            try {
                Constructor<?> constructor = reflectionTarget.fetchConstructor(classId);
                if (constructor == null) continue;
                constructorCache.put(classId, constructor);
                return constructor;
            }
            catch (ClassNotFoundException | NoSuchMethodException | SecurityException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    private static Class<?> getNMSClass(ClassId classId) {
        if (classCache.containsKey((Object)classId)) {
            return classCache.get((Object)classId);
        }
        for (ReflectionTarget reflectionTarget : reflectionTargets) {
            if (!reflectionTarget.getVersion().lessThanOrEqualTo(LOCAL_VERSION)) continue;
            try {
                Class<?> clazz = reflectionTarget.fetchClass(classId);
                if (clazz == null) continue;
                classCache.put(classId, clazz);
                return clazz;
            }
            catch (ClassNotFoundException classNotFoundException) {
                classNotFoundException.printStackTrace();
            }
        }
        return null;
    }

    private static String getMatch(String string, String string2) {
        Pattern pattern = Pattern.compile(string2);
        Matcher matcher = pattern.matcher(string);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static Object createItemStack(Object object) {
        if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4)) {
            Method method = NBTEditor.getMethod(MethodId.createStack);
            if (method.getParameterCount() == 2) {
                return NBTEditor.getMethod(MethodId.createStack).invoke(null, NBTEditor.registryAccess(), object);
            }
            return NBTEditor.getMethod(MethodId.createStack).invoke(null, object);
        }
        if (LOCAL_VERSION == MinecraftVersion.v1_11 || LOCAL_VERSION == MinecraftVersion.v1_12) {
            return NBTEditor.getConstructor(ClassId.ItemStack).newInstance(object);
        }
        return NBTEditor.getMethod(MethodId.createStack).invoke(null, object);
    }

    public static String getVersion() {
        return VERSION;
    }

    public static MinecraftVersion getMinecraftVersion() {
        return LOCAL_VERSION;
    }

    public static ItemStack getHead(String string) {
        Method method;
        Object object;
        Material material = Material.getMaterial((String)"SKULL_ITEM");
        if (material == null) {
            material = Material.getMaterial((String)"PLAYER_HEAD");
        }
        ItemStack itemStack = new ItemStack(material, 1, 3);
        if (string == null || string.isEmpty()) {
            return itemStack;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        Object var4_4 = null;
        try {
            var4_4 = NBTEditor.getConstructor(ClassId.GameProfile).newInstance(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "Notch");
            object = NBTEditor.getMethod(MethodId.getProperties).invoke(var4_4, new Object[0]);
            method = NBTEditor.getConstructor(ClassId.Property).newInstance("textures", new String(Base64.getEncoder().encode(String.format("{textures:{SKIN:{\"url\":\"%s\"}}}", string).getBytes())));
            NBTEditor.getMethod(MethodId.putProperty).invoke(object, "textures", method);
        }
        catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exception) {
            exception.printStackTrace();
        }
        object = NBTEditor.getMethod(MethodId.setCraftMetaSkullResolvableProfile);
        method = NBTEditor.getMethod(MethodId.setCraftMetaSkullProfile);
        if (object != null) {
            try {
                ((Method)object).invoke(itemMeta, NBTEditor.getConstructor(ClassId.ResolvableProfile).newInstance(var4_4));
            }
            catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exception) {
                exception.printStackTrace();
            }
        } else if (method != null) {
            try {
                method.invoke(itemMeta, var4_4);
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                exception.printStackTrace();
            }
        } else {
            try {
                skullProfile.set(itemMeta, var4_4);
            }
            catch (IllegalAccessException | IllegalArgumentException exception) {
                exception.printStackTrace();
            }
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static String getTexture(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (!(itemMeta instanceof SkullMeta)) {
            throw new IllegalArgumentException("Item is not a player skull!");
        }
        try {
            Object object;
            Object object2 = skullProfile.get(itemMeta);
            Class<?> clazz = NBTEditor.getNMSClass(ClassId.ResolvableProfile);
            if (clazz != null && clazz.isInstance(object2)) {
                object = NBTEditor.getMethod(MethodId.getResolvableProfileGameProfile);
                object2 = ((Method)object).invoke(object2, new Object[0]);
            }
            if (object2 == null) {
                return null;
            }
            object = (Collection)NBTEditor.getMethod(MethodId.propertyValues).invoke(NBTEditor.getMethod(MethodId.getProperties).invoke(object2, new Object[0]), new Object[0]);
            Iterator iterator2 = object.iterator();
            while (iterator2.hasNext()) {
                Object e = iterator2.next();
                if (!"textures".equals(NBTEditor.getMethod(MethodId.getPropertyName).invoke(e, new Object[0]))) continue;
                String string = new String(Base64.getDecoder().decode((String)NBTEditor.getMethod(MethodId.getPropertyValue).invoke(e, new Object[0])));
                return NBTEditor.getMatch(string, "\\{\"url\":\"(.*?)\"\\}");
            }
            return null;
        }
        catch (IllegalAccessException | IllegalArgumentException | SecurityException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static Object getItemTag(ItemStack itemStack, Object ... objectArray) {
        try {
            Object object = NBTEditor.getCompound(itemStack);
            if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4) && !BUKKIT_VERSION.startsWith("1.20.4") && !BUKKIT_VERSION.startsWith("1.20.5")) {
                object = NBTEditor.getMethod(MethodId.compoundGet).invoke(object, "components");
            }
            return NBTEditor.getTag(object, objectArray);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static Object getCompound(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        try {
            Object object = NBTEditor.getMethod(MethodId.asNMSCopy).invoke(null, itemStack);
            Object object2 = null;
            Method method = null;
            if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4)) {
                method = NBTEditor.getMethod(MethodId.saveOptional);
            }
            object2 = method != null ? method.invoke(object, NBTEditor.registryAccess()) : (NBTEditor.getMethod(MethodId.itemHasTag).invoke(object, new Object[0]).equals(true) ? NBTEditor.getMethod(MethodId.getItemTag).invoke(object, new Object[0]) : NBTEditor.getNMSClass(ClassId.NBTTagCompound).newInstance());
            return object2;
        }
        catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static NBTCompound getItemNBTTag(ItemStack itemStack, Object ... objectArray) {
        if (itemStack == null) {
            return null;
        }
        try {
            Object object = null;
            object = NBTEditor.getMethod(MethodId.asNMSCopy).invoke(null, itemStack);
            Object object2 = NBTEditor.getNMSClass(ClassId.NBTTagCompound).newInstance();
            Method method = null;
            if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4)) {
                method = NBTEditor.getMethod(MethodId.saveOptional);
            }
            object2 = method != null ? method.invoke(object, NBTEditor.registryAccess()) : NBTEditor.getMethod(MethodId.itemSave).invoke(object, object2);
            return NBTEditor.getNBTTag(object2, objectArray);
        }
        catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static ItemStack setItemTag(ItemStack itemStack, Object object, Object ... objectArray) {
        if (itemStack == null) {
            return null;
        }
        try {
            Object object2 = NBTEditor.getMethod(MethodId.asNMSCopy).invoke(null, itemStack);
            Object object3 = null;
            Method method = null;
            if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4)) {
                method = NBTEditor.getMethod(MethodId.saveOptional);
            }
            object3 = method != null ? method.invoke(object2, NBTEditor.registryAccess()) : (NBTEditor.getMethod(MethodId.itemHasTag).invoke(object2, new Object[0]).equals(true) ? NBTEditor.getMethod(MethodId.getItemTag).invoke(object2, new Object[0]) : NBTEditor.getNMSClass(ClassId.NBTTagCompound).newInstance());
            if (objectArray.length == 0 && object instanceof NBTCompound) {
                object3 = ((NBTCompound)object).tag;
            } else {
                if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4) && !BUKKIT_VERSION.startsWith("1.20.4") && !BUKKIT_VERSION.startsWith("1.20.5")) {
                    ArrayList<Object> arrayList = new ArrayList<Object>(Arrays.asList(objectArray));
                    arrayList.add(0, "components");
                    objectArray = arrayList.toArray();
                }
                NBTEditor.setTag(object3, object, objectArray);
            }
            if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4) && !BUKKIT_VERSION.startsWith("1.20.4") && !BUKKIT_VERSION.startsWith("1.20.5")) {
                return (ItemStack)NBTEditor.getMethod(MethodId.asBukkitCopy).invoke(null, NBTEditor.createItemStack(object3));
            }
            NBTEditor.getMethod(MethodId.setItemTag).invoke(object2, object3);
            return (ItemStack)NBTEditor.getMethod(MethodId.asBukkitCopy).invoke(null, object2);
        }
        catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static ItemStack getItemFromTag(NBTCompound nBTCompound) {
        if (nBTCompound == null) {
            return null;
        }
        try {
            if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4) && !BUKKIT_VERSION.startsWith("1.20.4") && !BUKKIT_VERSION.startsWith("1.20.5")) {
                return (ItemStack)NBTEditor.getMethod(MethodId.asBukkitCopy).invoke(null, NBTEditor.createItemStack(nBTCompound.tag));
            }
            Object object = nBTCompound.tag;
            Object object2 = NBTEditor.getTag(object, "Count");
            Object object3 = NBTEditor.getTag(object, "id");
            if (object2 == null || object3 == null) {
                return null;
            }
            if (object2 instanceof Byte && object3 instanceof String) {
                return (ItemStack)NBTEditor.getMethod(MethodId.asBukkitCopy).invoke(null, NBTEditor.createItemStack(object));
            }
            return null;
        }
        catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static Object getEntityTag(Entity entity, Object ... objectArray) {
        try {
            return NBTEditor.getTag(NBTEditor.getCompound(entity), objectArray);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static Object getCompound(Entity entity) {
        if (entity == null) {
            return entity;
        }
        try {
            Object object = NBTEditor.getMethod(MethodId.getEntityHandle).invoke(entity, new Object[0]);
            Object obj = NBTEditor.getNMSClass(ClassId.NBTTagCompound).newInstance();
            NBTEditor.getMethod(MethodId.getEntityTag).invoke(object, obj);
            return obj;
        }
        catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static NBTCompound getEntityNBTTag(Entity entity, Object ... objectArray) {
        if (entity == null) {
            return null;
        }
        try {
            Object object = NBTEditor.getMethod(MethodId.getEntityHandle).invoke(entity, new Object[0]);
            Object obj = NBTEditor.getNMSClass(ClassId.NBTTagCompound).newInstance();
            NBTEditor.getMethod(MethodId.getEntityTag).invoke(object, obj);
            return NBTEditor.getNBTTag(obj, objectArray);
        }
        catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static void setEntityTag(Entity entity, Object object, Object ... objectArray) {
        if (entity == null) {
            return;
        }
        try {
            Object object2 = NBTEditor.getMethod(MethodId.getEntityHandle).invoke(entity, new Object[0]);
            Object object3 = NBTEditor.getNMSClass(ClassId.NBTTagCompound).newInstance();
            NBTEditor.getMethod(MethodId.getEntityTag).invoke(object2, object3);
            if (objectArray.length == 0 && object instanceof NBTCompound) {
                object3 = ((NBTCompound)object).tag;
            } else {
                NBTEditor.setTag(object3, object, objectArray);
            }
            NBTEditor.getMethod(MethodId.setEntityTag).invoke(object2, object3);
        }
        catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exception) {
            exception.printStackTrace();
        }
    }

    private static Object getBlockTag(Block block, Object ... objectArray) {
        try {
            return NBTEditor.getTag(NBTEditor.getCompound(block), objectArray);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static Object getCompound(Block block) {
        try {
            Object object;
            if (block == null || !NBTEditor.getNMSClass(ClassId.CraftBlockState).isInstance(block.getState())) {
                return null;
            }
            Location location = block.getLocation();
            Object obj = NBTEditor.getConstructor(ClassId.BlockPosition).newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            Object object2 = NBTEditor.getMethod(MethodId.getWorldHandle).invoke(location.getWorld(), new Object[0]);
            Object object3 = NBTEditor.getMethod(MethodId.getTileEntity).invoke(object2, obj);
            if (object3 == null) {
                throw new IllegalArgumentException(block + " is not a tile entity!");
            }
            if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4)) {
                Method method = NBTEditor.getMethod(MethodId.getTileTag);
                object = method.getParameterCount() == 1 ? method.invoke(object3, NBTEditor.registryAccess()) : method.invoke(object3, new Object[0]);
            } else if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_18_R1)) {
                object = NBTEditor.getMethod(MethodId.getTileTag).invoke(object3, new Object[0]);
            } else {
                object = NBTEditor.getNMSClass(ClassId.NBTTagCompound).newInstance();
                NBTEditor.getMethod(MethodId.getTileTag).invoke(object3, object);
            }
            return object;
        }
        catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static NBTCompound getBlockNBTTag(Block block, Object ... objectArray) {
        try {
            Object object;
            if (block == null || !NBTEditor.getNMSClass(ClassId.CraftBlockState).isInstance(block.getState())) {
                return null;
            }
            Location location = block.getLocation();
            Object obj = NBTEditor.getConstructor(ClassId.BlockPosition).newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            Object object2 = NBTEditor.getMethod(MethodId.getWorldHandle).invoke(location.getWorld(), new Object[0]);
            Object object3 = NBTEditor.getMethod(MethodId.getTileEntity).invoke(object2, obj);
            if (object3 == null) {
                throw new IllegalArgumentException(block + " is not a tile entity!");
            }
            if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4)) {
                Method method = NBTEditor.getMethod(MethodId.getTileTag);
                object = method.getParameterCount() == 1 ? method.invoke(object3, NBTEditor.registryAccess()) : method.invoke(object3, new Object[0]);
            } else if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_18_R1)) {
                object = NBTEditor.getMethod(MethodId.getTileTag).invoke(object3, new Object[0]);
            } else {
                object = NBTEditor.getNMSClass(ClassId.NBTTagCompound).newInstance();
                NBTEditor.getMethod(MethodId.getTileTag).invoke(object3, object);
            }
            return NBTEditor.getNBTTag(object, objectArray);
        }
        catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private static void setBlockTag(Block block, Object object, Object ... objectArray) {
        try {
            Object object2;
            Method method;
            if (block == null || !NBTEditor.getNMSClass(ClassId.CraftBlockState).isInstance(block.getState())) {
                return;
            }
            Location location = block.getLocation();
            Object obj = NBTEditor.getConstructor(ClassId.BlockPosition).newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            Object object3 = NBTEditor.getMethod(MethodId.getWorldHandle).invoke(location.getWorld(), new Object[0]);
            Object object4 = NBTEditor.getMethod(MethodId.getTileEntity).invoke(object3, obj);
            if (object4 == null) {
                throw new IllegalArgumentException(block + " is not a tile entity!");
            }
            if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4)) {
                method = NBTEditor.getMethod(MethodId.getTileTag);
                object2 = method.getParameterCount() == 1 ? method.invoke(object4, NBTEditor.registryAccess()) : method.invoke(object4, new Object[0]);
            } else if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_18_R1)) {
                object2 = NBTEditor.getMethod(MethodId.getTileTag).invoke(object4, new Object[0]);
            } else {
                object2 = NBTEditor.getNMSClass(ClassId.NBTTagCompound).newInstance();
                NBTEditor.getMethod(MethodId.getTileTag).invoke(object4, object2);
            }
            if (objectArray.length == 0 && object instanceof NBTCompound) {
                object2 = ((NBTCompound)object).tag;
            } else {
                NBTEditor.setTag(object2, object, objectArray);
            }
            if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4)) {
                method = NBTEditor.getMethod(MethodId.setTileTag);
                if (method.getParameterCount() == 2) {
                    method.invoke(object4, object2, NBTEditor.registryAccess());
                } else {
                    method.invoke(object4, object2);
                }
            } else if (LOCAL_VERSION == MinecraftVersion.v1_16) {
                NBTEditor.getMethod(MethodId.setTileTag).invoke(object4, NBTEditor.getMethod(MethodId.getTileType).invoke(object3, obj), object2);
            } else {
                NBTEditor.getMethod(MethodId.setTileTag).invoke(object4, object2);
            }
        }
        catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exception) {
            exception.printStackTrace();
        }
    }

    public static void setSkullTexture(Block block, String string) {
        try {
            Object obj = NBTEditor.getConstructor(ClassId.GameProfile).newInstance(UUID.randomUUID(), null);
            Object object = NBTEditor.getMethod(MethodId.getProperties).invoke(obj, new Object[0]);
            Object obj2 = NBTEditor.getConstructor(ClassId.Property).newInstance("textures", new String(Base64.getEncoder().encode(String.format("{textures:{SKIN:{\"url\":\"%s\"}}}", string).getBytes())));
            NBTEditor.getMethod(MethodId.putProperty).invoke(object, "textures", obj2);
            Location location = block.getLocation();
            Object obj3 = NBTEditor.getConstructor(ClassId.BlockPosition).newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            Object object2 = NBTEditor.getMethod(MethodId.getWorldHandle).invoke(location.getWorld(), new Object[0]);
            Object object3 = NBTEditor.getMethod(MethodId.getTileEntity).invoke(object2, obj3);
            if (!NBTEditor.getNMSClass(ClassId.TileEntitySkull).isInstance(object3)) {
                throw new IllegalArgumentException(block + " is not a skull!");
            }
            NBTEditor.getMethod(MethodId.setGameProfile).invoke(object3, obj);
        }
        catch (IllegalAccessException | InstantiationException | InvocationTargetException reflectiveOperationException) {
            reflectiveOperationException.printStackTrace();
        }
    }

    private static Object getValue(Object object, Object ... objectArray) {
        if (object instanceof ItemStack) {
            return NBTEditor.getItemTag((ItemStack)object, objectArray);
        }
        if (object instanceof Entity) {
            return NBTEditor.getEntityTag((Entity)object, objectArray);
        }
        if (object instanceof Block) {
            return NBTEditor.getBlockTag((Block)object, objectArray);
        }
        if (object instanceof NBTCompound) {
            try {
                return NBTEditor.getTag(((NBTCompound)object).tag, objectArray);
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                exception.printStackTrace();
                return null;
            }
        }
        throw new IllegalArgumentException("Object provided must be of type ItemStack, Entity, Block, or NBTCompound!");
    }

    public static NBTCompound getNBTCompound(Object object, Object ... objectArray) {
        if (object == null) {
            throw new NullPointerException("Provided object was null!");
        }
        if (object instanceof ItemStack) {
            return NBTEditor.getItemNBTTag((ItemStack)object, objectArray);
        }
        if (object instanceof Entity) {
            return NBTEditor.getEntityNBTTag((Entity)object, objectArray);
        }
        if (object instanceof Block) {
            return NBTEditor.getBlockNBTTag((Block)object, objectArray);
        }
        if (object instanceof NBTCompound) {
            try {
                return NBTEditor.getNBTTag(((NBTCompound)object).tag, objectArray);
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                exception.printStackTrace();
                return null;
            }
        }
        if (NBTEditor.getNMSClass(ClassId.NBTTagCompound).isInstance(object)) {
            try {
                return NBTEditor.getNBTTag(object, objectArray);
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                exception.printStackTrace();
                return null;
            }
        }
        throw new IllegalArgumentException("Object provided must be of type ItemStack, Entity, Block, or NBTCompound!");
    }

    public static String getString(Object object, Object ... objectArray) {
        Object object2 = NBTEditor.getValue(object, objectArray);
        return object2 instanceof String ? (String)object2 : null;
    }

    public static int getInt(Object object, Object ... objectArray) {
        Object object2 = NBTEditor.getValue(object, objectArray);
        return object2 instanceof Integer ? (Integer)object2 : 0;
    }

    public static double getDouble(Object object, Object ... objectArray) {
        Object object2 = NBTEditor.getValue(object, objectArray);
        return object2 instanceof Double ? (Double)object2 : 0.0;
    }

    public static long getLong(Object object, Object ... objectArray) {
        Object object2 = NBTEditor.getValue(object, objectArray);
        return object2 instanceof Long ? (Long)object2 : 0L;
    }

    public static float getFloat(Object object, Object ... objectArray) {
        Object object2 = NBTEditor.getValue(object, objectArray);
        return object2 instanceof Float ? ((Float)object2).floatValue() : 0.0f;
    }

    public static short getShort(Object object, Object ... objectArray) {
        Object object2 = NBTEditor.getValue(object, objectArray);
        return object2 instanceof Short ? (Short)object2 : (short)0;
    }

    public static byte getByte(Object object, Object ... objectArray) {
        Object object2 = NBTEditor.getValue(object, objectArray);
        return object2 instanceof Byte ? (Byte)object2 : (byte)0;
    }

    public static boolean getBoolean(Object object, Object ... objectArray) {
        return NBTEditor.getByte(object, objectArray) == 1;
    }

    public static byte[] getByteArray(Object object, Object ... objectArray) {
        Object object2 = NBTEditor.getValue(object, objectArray);
        return object2 instanceof byte[] ? (byte[])object2 : null;
    }

    public static int[] getIntArray(Object object, Object ... objectArray) {
        Object object2 = NBTEditor.getValue(object, objectArray);
        return object2 instanceof int[] ? (int[])object2 : null;
    }

    public static boolean contains(Object object, Object ... objectArray) {
        Object object2 = NBTEditor.getValue(object, objectArray);
        return object2 != null;
    }

    public static Collection<String> getKeys(Object object, Object ... objectArray) {
        Object object2;
        Object object3;
        if (object instanceof ItemStack) {
            object3 = NBTEditor.getCompound((ItemStack)object);
            object2 = new ArrayList<Object>(Arrays.asList(objectArray));
            object2.add(0, (Object)((Object)ITEMSTACK_COMPONENTS));
            objectArray = object2.toArray();
        } else if (object instanceof Entity) {
            object3 = NBTEditor.getCompound((Entity)object);
        } else if (object instanceof Block) {
            object3 = NBTEditor.getCompound((Block)object);
        } else if (object instanceof NBTCompound) {
            object3 = ((NBTCompound)object).tag;
        } else {
            throw new IllegalArgumentException("Object provided must be of type ItemStack, Entity, Block, or NBTCompound!");
        }
        try {
            object2 = NBTEditor.getNBTTag(object3, objectArray);
            if (object2 != null && ((NBTCompound)object2).tag != null) {
                Object object4 = ((NBTCompound)object2).tag;
                if (NBTEditor.getNMSClass(ClassId.NBTTagCompound).isInstance(object4)) {
                    return (Collection)NBTEditor.getMethod(MethodId.compoundKeys).invoke(object4, new Object[0]);
                }
                return Collections.EMPTY_LIST;
            }
            return Collections.EMPTY_LIST;
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
            exception.printStackTrace();
            return Collections.EMPTY_LIST;
        }
    }

    public static int getSize(Object object, Object ... objectArray) {
        Object object2;
        if (object instanceof ItemStack) {
            object2 = NBTEditor.getCompound((ItemStack)object);
        } else if (object instanceof Entity) {
            object2 = NBTEditor.getCompound((Entity)object);
        } else if (object instanceof Block) {
            object2 = NBTEditor.getCompound((Block)object);
        } else if (object instanceof NBTCompound) {
            object2 = ((NBTCompound)object).tag;
        } else {
            throw new IllegalArgumentException("Object provided must be of type ItemStack, Entity, Block, or NBTCompound!");
        }
        try {
            NBTCompound nBTCompound = NBTEditor.getNBTTag(object2, objectArray);
            if (NBTEditor.getNMSClass(ClassId.NBTTagCompound).isInstance(nBTCompound.tag)) {
                return NBTEditor.getKeys(nBTCompound, new Object[0]).size();
            }
            if (NBTEditor.getNMSClass(ClassId.NBTTagList).isInstance(nBTCompound.tag)) {
                return (Integer)NBTEditor.getMethod(MethodId.listSize).invoke(nBTCompound.tag, new Object[0]);
            }
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
            exception.printStackTrace();
            return 0;
        }
        throw new IllegalArgumentException("Value is not a compound or list!");
    }

    public static <T> T set(T t, Object object, Object ... objectArray) {
        if (t instanceof ItemStack) {
            return (T)NBTEditor.setItemTag((ItemStack)t, object, objectArray);
        }
        if (t instanceof Entity) {
            NBTEditor.setEntityTag((Entity)t, object, objectArray);
        } else if (t instanceof Block) {
            NBTEditor.setBlockTag((Block)t, object, objectArray);
        } else if (t instanceof NBTCompound) {
            try {
                NBTEditor.setTag(((NBTCompound)t).tag, object, objectArray);
            }
            catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException exception) {
                exception.printStackTrace();
            }
        } else {
            throw new IllegalArgumentException("Object provided must be of type ItemStack, Entity, Block, or NBTCompound!");
        }
        return t;
    }

    public static NBTCompound getNBTCompound(String string) {
        return NBTCompound.fromJson(string);
    }

    public static NBTCompound getEmptyNBTCompound() {
        try {
            return new NBTCompound(NBTEditor.getNMSClass(ClassId.NBTTagCompound).newInstance());
        }
        catch (IllegalAccessException | InstantiationException reflectiveOperationException) {
            reflectiveOperationException.printStackTrace();
            return null;
        }
    }

    /*
     * WARNING - void declaration
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private static void setTag(Object constructor, Object object, Object ... objectArray) {
        void var3_9;
        Object object2;
        if (object != null && object != Type.DELETE) {
            if (object instanceof NBTCompound) {
                Object object3 = ((NBTCompound)object).tag;
            } else if (NBTEditor.getNMSClass(ClassId.NBTTagList).isInstance(object) || NBTEditor.getNMSClass(ClassId.NBTTagCompound).isInstance(object)) {
                Object object4 = object;
            } else if (object == Type.COMPOUND) {
                Object obj = NBTEditor.getNMSClass(ClassId.NBTTagCompound).newInstance();
            } else if (object == Type.LIST) {
                Object obj = NBTEditor.getNMSClass(ClassId.NBTTagList).newInstance();
            } else {
                if (object instanceof Boolean) {
                    object = (byte)((Boolean)object == true ? 1 : 0);
                }
                if ((object2 = NBTEditor.getNBTTagConstructor(object.getClass())) == null) throw new IllegalArgumentException("Provided value type(" + object.getClass() + ") is not supported!");
                Object obj = ((Constructor)object2).newInstance(object);
            }
        } else {
            Type type = Type.DELETE;
        }
        object2 = constructor;
        for (int i = 0; i < objectArray.length - 1; ++i) {
            void var6_14;
            Object object5 = objectArray[i];
            Constructor<?> constructor2 = object2;
            if (object5 == Type.CUSTOM_DATA) {
                if (!LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4)) continue;
                String string = "minecraft:custom_data";
            } else if (object5 == Type.ITEMSTACK_COMPONENTS) {
                if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4)) {
                    String string = "components";
                } else {
                    String string = "tag";
                }
            }
            if (var6_14 instanceof Integer) {
                int n = (Integer)var6_14;
                List list = (List)NBTListData.get(object2);
                object2 = n >= 0 && n < list.size() ? list.get(n) : null;
            } else if (var6_14 != null && var6_14 != Type.NEW_ELEMENT) {
                object2 = NBTEditor.getMethod(MethodId.compoundGet).invoke(object2, (String)var6_14);
            }
            if (object2 != null && var6_14 != null && var6_14 != Type.NEW_ELEMENT) continue;
            object2 = objectArray[i + 1] == null || objectArray[i + 1] instanceof Integer || objectArray[i + 1] == Type.NEW_ELEMENT ? NBTEditor.getNMSClass(ClassId.NBTTagList).newInstance() : NBTEditor.getNMSClass(ClassId.NBTTagCompound).newInstance();
            if (NBTEditor.getNMSClass(ClassId.NBTTagList).isInstance(constructor2)) {
                if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_14)) {
                    NBTEditor.getMethod(MethodId.listAdd).invoke(constructor2, NBTEditor.getMethod(MethodId.listSize).invoke(constructor2, new Object[0]), object2);
                    continue;
                }
                NBTEditor.getMethod(MethodId.listAdd).invoke(constructor2, object2);
                continue;
            }
            NBTEditor.getMethod(MethodId.compoundSet).invoke(constructor2, (String)var6_14, object2);
        }
        if (objectArray.length > 0) {
            Object object6 = objectArray[objectArray.length - 1];
            if (object6 == null || object6 == Type.NEW_ELEMENT) {
                if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_14)) {
                    NBTEditor.getMethod(MethodId.listAdd).invoke(object2, NBTEditor.getMethod(MethodId.listSize).invoke(object2, new Object[0]), var3_9);
                    return;
                } else {
                    NBTEditor.getMethod(MethodId.listAdd).invoke(object2, var3_9);
                }
                return;
            } else if (object6 instanceof Integer) {
                if (var3_9 == Type.DELETE) {
                    NBTEditor.getMethod(MethodId.listRemove).invoke(object2, (int)((Integer)object6));
                    return;
                } else {
                    NBTEditor.getMethod(MethodId.listSet).invoke(object2, (int)((Integer)object6), var3_9);
                }
                return;
            } else if (var3_9 == Type.DELETE) {
                NBTEditor.getMethod(MethodId.compoundRemove).invoke(object2, (String)object6);
                return;
            } else {
                NBTEditor.getMethod(MethodId.compoundSet).invoke(object2, (String)object6, var3_9);
            }
            return;
        } else {
            if (var3_9 == null || !NBTEditor.getNMSClass(ClassId.NBTTagCompound).isInstance(var3_9) || !NBTEditor.getNMSClass(ClassId.NBTTagCompound).isInstance(object2)) return;
            for (String string : NBTEditor.getKeys(var3_9, new Object[0])) {
                NBTEditor.getMethod(MethodId.compoundSet).invoke(object2, string, NBTEditor.getMethod(MethodId.compoundGet).invoke(var3_9, string));
            }
        }
    }

    private static NBTCompound getNBTTag(Object object, Object ... objectArray) {
        Object object2 = object;
        for (Object object3 : objectArray) {
            if (object2 == null) {
                return null;
            }
            if (NBTEditor.getNMSClass(ClassId.NBTTagCompound).isInstance(object2)) {
                if (object3 == Type.CUSTOM_DATA) {
                    if (!LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4)) continue;
                    object3 = "minecraft:custom_data";
                } else if (object3 == Type.ITEMSTACK_COMPONENTS) {
                    object3 = LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4) ? "components" : "tag";
                }
                object2 = NBTEditor.getMethod(MethodId.compoundGet).invoke(object2, (String)object3);
                continue;
            }
            if (!NBTEditor.getNMSClass(ClassId.NBTTagList).isInstance(object2)) continue;
            int n = (Integer)object3;
            List list = (List)NBTListData.get(object2);
            object2 = n >= 0 && n < list.size() ? list.get(n) : null;
        }
        return new NBTCompound(object2);
    }

    private static Object getTag(Object object, Object ... objectArray) {
        if (objectArray.length == 0) {
            return NBTEditor.getTags(object);
        }
        Object object2 = object;
        for (Object object3 : objectArray) {
            if (object2 == null) {
                return null;
            }
            if (NBTEditor.getNMSClass(ClassId.NBTTagCompound).isInstance(object2)) {
                if (object3 == Type.CUSTOM_DATA) {
                    if (!LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4)) continue;
                    object3 = "minecraft:custom_data";
                } else if (object3 == Type.ITEMSTACK_COMPONENTS) {
                    object3 = LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_20_R4) ? "components" : "tag";
                }
                if (object3 instanceof String) {
                    object2 = NBTEditor.getMethod(MethodId.compoundGet).invoke(object2, (String)object3);
                    continue;
                }
                throw new IllegalArgumentException("Key " + object3 + " is not a string! Must provide a valid key for an NBT Tag Compound");
            }
            if (NBTEditor.getNMSClass(ClassId.NBTTagList).isInstance(object2)) {
                if (!(object3 instanceof Integer)) {
                    throw new IllegalArgumentException("Key " + object3 + " is not an integer! Must provide a valid index for an NBT Tag List");
                }
                int n = (Integer)object3;
                List list = (List)NBTListData.get(object2);
                if (n >= 0 && n < list.size()) {
                    object2 = list.get(n);
                    continue;
                }
                object2 = null;
                continue;
            }
            return NBTEditor.getNBTVar(object2);
        }
        if (object2 == null) {
            return null;
        }
        if (NBTEditor.getNMSClass(ClassId.NBTTagList).isInstance(object2)) {
            return NBTEditor.getTags(object2);
        }
        if (NBTEditor.getNMSClass(ClassId.NBTTagCompound).isInstance(object2)) {
            return NBTEditor.getTags(object2);
        }
        return NBTEditor.getNBTVar(object2);
    }

    private static Object getTags(Object object) {
        HashMap<Object, Object> hashMap = new HashMap<Object, Object>();
        try {
            if (NBTEditor.getNMSClass(ClassId.NBTTagCompound).isInstance(object)) {
                Map map = (Map)NBTCompoundMap.get(object);
                for (String string : map.keySet()) {
                    Object v = map.get(string);
                    if (NBTEditor.getNMSClass(ClassId.NBTTagEnd).isInstance(v)) continue;
                    hashMap.put(string, NBTEditor.getTag(v, new Object[0]));
                }
            } else if (NBTEditor.getNMSClass(ClassId.NBTTagList).isInstance(object)) {
                List list = (List)NBTListData.get(object);
                for (int i = 0; i < list.size(); ++i) {
                    Object e = list.get(i);
                    if (NBTEditor.getNMSClass(ClassId.NBTTagEnd).isInstance(e)) continue;
                    hashMap.put(i, NBTEditor.getTag(e, new Object[0]));
                }
            } else {
                return NBTEditor.getNBTVar(object);
            }
            return hashMap;
        }
        catch (Exception exception) {
            exception.printStackTrace();
            return hashMap;
        }
    }

    private static Object registryAccess() {
        try {
            return NBTEditor.getMethod(MethodId.registryAccess).invoke(NBTEditor.getMethod(MethodId.getServer).invoke(Bukkit.getServer(), new Object[0]), new Object[0]);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    static {
        COMPOUND = Type.COMPOUND;
        LIST = Type.LIST;
        NEW_ELEMENT = Type.NEW_ELEMENT;
        DELETE = Type.DELETE;
        CUSTOM_DATA = Type.CUSTOM_DATA;
        ITEMSTACK_COMPONENTS = Type.ITEMSTACK_COMPONENTS;
        String string = Bukkit.getServer().getClass().getPackage().getName();
        String string2 = string.substring(string.lastIndexOf(46) + 1);
        String string3 = Bukkit.getServer().getBukkitVersion();
        if (!string2.startsWith("v")) {
            string2 = string3;
        }
        VERSION = string2;
        LOCAL_VERSION = MinecraftVersion.get(VERSION);
        BUKKIT_VERSION = string3;
        classCache = new HashMap();
        methodCache = new HashMap<MethodId, Method>();
        constructorCache = new HashMap();
        reflectionTargets = new TreeSet<ReflectionTarget>();
        reflectionTargets.addAll(Arrays.asList(new ReflectionTarget.v1_8().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_9().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_11().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_12().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_13().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_15().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_16().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_17().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_18_R1().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_18_R2().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_19_R1().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_19_R2().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_20_R1().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_20_R2().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_20_R3().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_20_R4().setClassFetcher(NBTEditor::getNMSClass), new ReflectionTarget.v1_21_R1().setClassFetcher(NBTEditor::getNMSClass)));
        NBTClasses = new HashMap();
        try {
            if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_17)) {
                NBTClasses.put(Byte.class, Class.forName("net.minecraft.nbt.NBTTagByte"));
                NBTClasses.put(Boolean.class, Class.forName("net.minecraft.nbt.NBTTagByte"));
                NBTClasses.put(String.class, Class.forName("net.minecraft.nbt.NBTTagString"));
                NBTClasses.put(Double.class, Class.forName("net.minecraft.nbt.NBTTagDouble"));
                NBTClasses.put(Integer.class, Class.forName("net.minecraft.nbt.NBTTagInt"));
                NBTClasses.put(Long.class, Class.forName("net.minecraft.nbt.NBTTagLong"));
                NBTClasses.put(Short.class, Class.forName("net.minecraft.nbt.NBTTagShort"));
                NBTClasses.put(Float.class, Class.forName("net.minecraft.nbt.NBTTagFloat"));
                NBTClasses.put(Class.forName("[B"), Class.forName("net.minecraft.nbt.NBTTagByteArray"));
                NBTClasses.put(Class.forName("[I"), Class.forName("net.minecraft.nbt.NBTTagIntArray"));
            } else {
                NBTClasses.put(Byte.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagByte"));
                NBTClasses.put(Boolean.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagByte"));
                NBTClasses.put(String.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagString"));
                NBTClasses.put(Double.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagDouble"));
                NBTClasses.put(Integer.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagInt"));
                NBTClasses.put(Long.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagLong"));
                NBTClasses.put(Short.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagShort"));
                NBTClasses.put(Float.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagFloat"));
                NBTClasses.put(Class.forName("[B"), Class.forName("net.minecraft.server." + VERSION + ".NBTTagByteArray"));
                NBTClasses.put(Class.forName("[I"), Class.forName("net.minecraft.server." + VERSION + ".NBTTagIntArray"));
            }
        }
        catch (ClassNotFoundException classNotFoundException) {
            classNotFoundException.printStackTrace();
        }
        NBTConstructors = new HashMap();
        try {
            NBTConstructors.put(NBTEditor.getNBTTag(Byte.class), NBTEditor.getNBTTag(Byte.class).getDeclaredConstructor(Byte.TYPE));
            NBTConstructors.put(NBTEditor.getNBTTag(Boolean.class), NBTEditor.getNBTTag(Boolean.class).getDeclaredConstructor(Byte.TYPE));
            NBTConstructors.put(NBTEditor.getNBTTag(String.class), NBTEditor.getNBTTag(String.class).getDeclaredConstructor(String.class));
            NBTConstructors.put(NBTEditor.getNBTTag(Double.class), NBTEditor.getNBTTag(Double.class).getDeclaredConstructor(Double.TYPE));
            NBTConstructors.put(NBTEditor.getNBTTag(Integer.class), NBTEditor.getNBTTag(Integer.class).getDeclaredConstructor(Integer.TYPE));
            NBTConstructors.put(NBTEditor.getNBTTag(Long.class), NBTEditor.getNBTTag(Long.class).getDeclaredConstructor(Long.TYPE));
            NBTConstructors.put(NBTEditor.getNBTTag(Float.class), NBTEditor.getNBTTag(Float.class).getDeclaredConstructor(Float.TYPE));
            NBTConstructors.put(NBTEditor.getNBTTag(Short.class), NBTEditor.getNBTTag(Short.class).getDeclaredConstructor(Short.TYPE));
            NBTConstructors.put(NBTEditor.getNBTTag(Class.forName("[B")), NBTEditor.getNBTTag(Class.forName("[B")).getDeclaredConstructor(Class.forName("[B")));
            NBTConstructors.put(NBTEditor.getNBTTag(Class.forName("[I")), NBTEditor.getNBTTag(Class.forName("[I")).getDeclaredConstructor(Class.forName("[I")));
            for (Constructor annotatedElement : NBTConstructors.values()) {
                annotatedElement.setAccessible(true);
            }
        }
        catch (ClassNotFoundException | NoSuchMethodException | SecurityException exception) {
            exception.printStackTrace();
        }
        NBTTagFieldCache = new HashMap();
        try {
            if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_17)) {
                NBTTagFieldCache.put(NBTClasses.get(Byte.class), NBTClasses.get(Byte.class).getDeclaredField("x"));
                NBTTagFieldCache.put(NBTClasses.get(Boolean.class), NBTClasses.get(Boolean.class).getDeclaredField("x"));
                NBTTagFieldCache.put(NBTClasses.get(String.class), NBTClasses.get(String.class).getDeclaredField("A"));
                NBTTagFieldCache.put(NBTClasses.get(Double.class), NBTClasses.get(Double.class).getDeclaredField("w"));
                NBTTagFieldCache.put(NBTClasses.get(Integer.class), NBTClasses.get(Integer.class).getDeclaredField("c"));
                NBTTagFieldCache.put(NBTClasses.get(Long.class), NBTClasses.get(Long.class).getDeclaredField("c"));
                NBTTagFieldCache.put(NBTClasses.get(Float.class), NBTClasses.get(Float.class).getDeclaredField("w"));
                NBTTagFieldCache.put(NBTClasses.get(Short.class), NBTClasses.get(Short.class).getDeclaredField("c"));
                NBTTagFieldCache.put(NBTClasses.get(Class.forName("[B")), NBTClasses.get(Class.forName("[B")).getDeclaredField("c"));
                NBTTagFieldCache.put(NBTClasses.get(Class.forName("[I")), NBTClasses.get(Class.forName("[I")).getDeclaredField("c"));
                for (Field field : NBTTagFieldCache.values()) {
                    field.setAccessible(true);
                }
            } else {
                for (Class<?> clazz : NBTClasses.values()) {
                    Field field = clazz.getDeclaredField("data");
                    field.setAccessible(true);
                    NBTTagFieldCache.put(clazz, field);
                }
            }
        }
        catch (ClassNotFoundException | NoSuchFieldException | SecurityException exception) {
            exception.printStackTrace();
        }
        try {
            if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_17)) {
                NBTListData = NBTEditor.getNMSClass(ClassId.NBTTagList).getDeclaredField("c");
                NBTCompoundMap = NBTEditor.getNMSClass(ClassId.NBTTagCompound).getDeclaredField("x");
            } else {
                NBTListData = NBTEditor.getNMSClass(ClassId.NBTTagList).getDeclaredField("list");
                NBTCompoundMap = NBTEditor.getNMSClass(ClassId.NBTTagCompound).getDeclaredField("map");
            }
            NBTListData.setAccessible(true);
            NBTCompoundMap.setAccessible(true);
            skullProfile = NBTEditor.getNMSClass(ClassId.CraftMetaSkull).getDeclaredField("profile");
            skullProfile.setAccessible(true);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static abstract class ReflectionTarget
    implements Comparable<ReflectionTarget> {
        private final MinecraftVersion version;
        private Function<ClassId, Class<?>> classFetcher;
        private final Map<ClassId, String> classTargets = new HashMap<ClassId, String>();
        private final Map<MethodId, ConstructorTarget> methodTargets = new HashMap<MethodId, ConstructorTarget>();
        private final Map<ClassId, ConstructorTarget> constructorTargets = new HashMap<ClassId, ConstructorTarget>();

        protected ReflectionTarget(MinecraftVersion minecraftVersion) {
            this.version = minecraftVersion;
        }

        protected MinecraftVersion getVersion() {
            return this.version;
        }

        protected final ReflectionTarget setClassFetcher(Function<ClassId, Class<?>> function) {
            this.classFetcher = function;
            return this;
        }

        protected final void addClass(ClassId classId, String string) {
            this.classTargets.put(classId, string);
        }

        protected final MethodTarget addMethod(MethodId methodId, ClassId classId, String string, Object ... objectArray) {
            MethodTarget methodTarget = new MethodTarget(classId, string, objectArray);
            this.methodTargets.put(methodId, methodTarget);
            return methodTarget;
        }

        protected final ReturnMethodTarget addMethod(MethodId methodId, ClassId classId, ClassId classId2, Object ... objectArray) {
            ReturnMethodTarget returnMethodTarget = new ReturnMethodTarget(classId, classId2, objectArray);
            this.methodTargets.put(methodId, returnMethodTarget);
            return returnMethodTarget;
        }

        protected final void addConstructor(ClassId classId, Object ... objectArray) {
            this.constructorTargets.put(classId, new ConstructorTarget(classId, objectArray));
        }

        protected final Class<?> fetchClass(ClassId classId) {
            String string = this.classTargets.get((Object)classId);
            return string != null ? Class.forName(string) : null;
        }

        protected final Method fetchMethod(MethodId methodId) {
            ConstructorTarget constructorTarget = this.methodTargets.get((Object)methodId);
            if (constructorTarget instanceof MethodTarget) {
                MethodTarget methodTarget = (MethodTarget)constructorTarget;
                Class<?> clazz = this.findClass(methodTarget.clazz);
                Class<?>[] classArray = this.convert(methodTarget.params);
                try {
                    return clazz.getMethod(methodTarget.name, classArray);
                }
                catch (NoSuchMethodException noSuchMethodException) {
                    try {
                        Method method = clazz.getDeclaredMethod(methodTarget.name, classArray);
                        method.setAccessible(true);
                        return method;
                    }
                    catch (NoSuchMethodException noSuchMethodException2) {
                        if (methodTarget.silent) {
                            return null;
                        }
                        throw noSuchMethodException;
                    }
                }
            }
            if (constructorTarget instanceof ReturnMethodTarget) {
                ReturnMethodTarget returnMethodTarget = (ReturnMethodTarget)constructorTarget;
                Class<?> clazz = this.findClass(returnMethodTarget.clazz);
                Class<?> clazz2 = this.findClass(returnMethodTarget.returnType);
                Class<?>[] classArray = this.convert(returnMethodTarget.params);
                for (Method method : clazz.getDeclaredMethods()) {
                    if (!method.getReturnType().equals(clazz2) || !this.matches(method.getParameterTypes(), classArray)) continue;
                    method.setAccessible(true);
                    return method;
                }
                return null;
            }
            return null;
        }

        protected final Constructor<?> fetchConstructor(ClassId classId) {
            ConstructorTarget constructorTarget = this.constructorTargets.get((Object)classId);
            return constructorTarget != null ? this.findClass(constructorTarget.clazz).getConstructor(this.convert(constructorTarget.params)) : null;
        }

        private final boolean matches(Class<?>[] classArray, Class<?>[] classArray2) {
            if (classArray.length != classArray2.length) {
                return false;
            }
            for (int i = 0; i < classArray.length; ++i) {
                if (classArray2[i].isAssignableFrom(classArray[i])) continue;
                return false;
            }
            return true;
        }

        private final Class<?>[] convert(Object[] objectArray) {
            Class[] classArray = new Class[objectArray.length];
            for (int i = 0; i < objectArray.length; ++i) {
                Object object = objectArray[i];
                if (object instanceof Class) {
                    classArray[i] = (Class)object;
                    continue;
                }
                if (object instanceof ClassId) {
                    classArray[i] = this.findClass((ClassId)((Object)object));
                    continue;
                }
                throw new IllegalArgumentException("Invalid parameter type: " + object);
            }
            return classArray;
        }

        private final Class<?> findClass(ClassId classId) {
            return this.classFetcher != null ? this.classFetcher.apply(classId) : this.fetchClass(classId);
        }

        @Override
        public int compareTo(ReflectionTarget reflectionTarget) {
            return reflectionTarget.version.compareTo(this.version);
        }

        private static class v1_21_R1
        extends ReflectionTarget {
            protected v1_21_R1() {
                super(MinecraftVersion.v1_21_R1);
                this.addClass(ClassId.ResolvableProfile, "net.minecraft.world.item.component.ResolvableProfile");
                this.addClass(ClassId.IRegistryCustomDimension, "net.minecraft.core.IRegistryCustom$Dimension");
                this.addMethod(MethodId.setCraftMetaSkullResolvableProfile, ClassId.CraftMetaSkull, "setProfile", new Object[]{ClassId.ResolvableProfile}).failSilently(true);
                this.addMethod(MethodId.getResolvableProfileGameProfile, ClassId.ResolvableProfile, "f", new Object[0]);
                this.addMethod(MethodId.registryAccess, ClassId.MinecraftServer, ClassId.IRegistryCustomDimension, new Object[0]);
                this.addConstructor(ClassId.ResolvableProfile, new Object[]{ClassId.GameProfile});
            }
        }

        private static class v1_20_R4
        extends ReflectionTarget {
            protected v1_20_R4() {
                super(MinecraftVersion.v1_20_R4);
                if (!BUKKIT_VERSION.startsWith("1.20.4") && !BUKKIT_VERSION.startsWith("1.20.5")) {
                    this.addClass(ClassId.MinecraftServer, "net.minecraft.server.MinecraftServer");
                    this.addClass(ClassId.RegistryAccess, "net.minecraft.core.HolderLookup$a");
                    this.addClass(ClassId.ResolvableProfile, "net.minecraft.world.item.component.ResolvableProfile");
                    this.addMethod(MethodId.getServer, ClassId.CraftServer, "getServer", new Object[0]);
                    this.addMethod(MethodId.registryAccess, ClassId.MinecraftServer, "bc", new Object[0]);
                    this.addMethod(MethodId.saveOptional, ClassId.ItemStack, "a", new Object[]{ClassId.RegistryAccess});
                    this.addMethod(MethodId.createStack, ClassId.ItemStack, "a", new Object[]{ClassId.RegistryAccess, ClassId.NBTTagCompound});
                    this.addMethod(MethodId.getTileTag, ClassId.TileEntity, "b", new Object[]{ClassId.RegistryAccess});
                    this.addMethod(MethodId.setTileTag, ClassId.TileEntity, "a", new Object[]{ClassId.NBTTagCompound, ClassId.RegistryAccess});
                }
            }
        }

        private static class v1_20_R3
        extends ReflectionTarget {
            protected v1_20_R3() {
                super(MinecraftVersion.v1_20_R3);
                this.addMethod(MethodId.getTileTag, ClassId.TileEntity, "o", new Object[0]);
            }
        }

        private static class v1_20_R2
        extends ReflectionTarget {
            protected v1_20_R2() {
                super(MinecraftVersion.v1_20_R2);
                this.addMethod(MethodId.getPropertyName, ClassId.Property, "name", new Object[0]);
                this.addMethod(MethodId.getPropertyValue, ClassId.Property, "value", new Object[0]);
            }
        }

        private static class v1_20_R1
        extends ReflectionTarget {
            protected v1_20_R1() {
                super(MinecraftVersion.v1_20_R1);
                this.addMethod(MethodId.itemHasTag, ClassId.ItemStack, "u", new Object[0]);
                this.addMethod(MethodId.getItemTag, ClassId.ItemStack, "v", new Object[0]);
            }
        }

        private static class v1_19_R2
        extends ReflectionTarget {
            protected v1_19_R2() {
                super(MinecraftVersion.v1_19_R2);
                this.addMethod(MethodId.compoundKeys, ClassId.NBTTagCompound, "e", new Object[0]);
            }
        }

        private static class v1_19_R1
        extends ReflectionTarget {
            protected v1_19_R1() {
                super(MinecraftVersion.v1_19_R1);
                this.addMethod(MethodId.itemHasTag, ClassId.ItemStack, "t", new Object[0]);
                this.addMethod(MethodId.getItemTag, ClassId.ItemStack, "u", new Object[0]);
            }
        }

        private static class v1_18_R2
        extends ReflectionTarget {
            protected v1_18_R2() {
                super(MinecraftVersion.v1_18_R2);
                this.addMethod(MethodId.itemHasTag, ClassId.ItemStack, "s", new Object[0]);
                this.addMethod(MethodId.getItemTag, ClassId.ItemStack, "t", new Object[0]);
            }
        }

        private static class v1_18_R1
        extends ReflectionTarget {
            protected v1_18_R1() {
                super(MinecraftVersion.v1_18_R1);
                this.addMethod(MethodId.compoundGet, ClassId.NBTTagCompound, "c", String.class);
                this.addMethod(MethodId.compoundSet, ClassId.NBTTagCompound, "a", new Object[]{String.class, ClassId.NBTBase});
                this.addMethod(MethodId.compoundHasKey, ClassId.NBTTagCompound, "e", String.class);
                this.addMethod(MethodId.listSet, ClassId.NBTTagList, "d", new Object[]{Integer.TYPE, ClassId.NBTBase});
                this.addMethod(MethodId.listAdd, ClassId.NBTTagList, "c", new Object[]{Integer.TYPE, ClassId.NBTBase});
                this.addMethod(MethodId.listRemove, ClassId.NBTTagList, "c", Integer.TYPE);
                this.addMethod(MethodId.compoundRemove, ClassId.NBTTagCompound, "r", String.class);
                this.addMethod(MethodId.compoundKeys, ClassId.NBTTagCompound, "d", new Object[0]);
                this.addMethod(MethodId.itemHasTag, ClassId.ItemStack, "r", new Object[0]);
                this.addMethod(MethodId.getItemTag, ClassId.ItemStack, "s", new Object[0]);
                this.addMethod(MethodId.setItemTag, ClassId.ItemStack, "c", new Object[]{ClassId.NBTTagCompound});
                this.addMethod(MethodId.itemSave, ClassId.ItemStack, "b", new Object[]{ClassId.NBTTagCompound});
                this.addMethod(MethodId.getEntityTag, ClassId.Entity, "f", new Object[]{ClassId.NBTTagCompound});
                this.addMethod(MethodId.setEntityTag, ClassId.Entity, "g", new Object[]{ClassId.NBTTagCompound});
                this.addMethod(MethodId.setTileTag, ClassId.TileEntity, "a", new Object[]{ClassId.NBTTagCompound});
                this.addMethod(MethodId.getTileTag, ClassId.TileEntity, "m", new Object[0]);
                this.addMethod(MethodId.getTileEntity, ClassId.World, "c_", new Object[]{ClassId.BlockPosition});
                this.addMethod(MethodId.setGameProfile, ClassId.TileEntitySkull, "a", new Object[]{ClassId.GameProfile});
                this.addMethod(MethodId.loadNBTTagCompound, ClassId.MojangsonParser, "a", String.class);
            }
        }

        private static class v1_17
        extends ReflectionTarget {
            protected v1_17() {
                super(MinecraftVersion.v1_17);
                this.addClass(ClassId.NBTBase, "net.minecraft.nbt.NBTBase");
                this.addClass(ClassId.NBTTagCompound, "net.minecraft.nbt.NBTTagCompound");
                this.addClass(ClassId.NBTTagList, "net.minecraft.nbt.NBTTagList");
                this.addClass(ClassId.NBTTagEnd, "net.minecraft.nbt.NBTTagEnd");
                this.addClass(ClassId.MojangsonParser, "net.minecraft.nbt.MojangsonParser");
                this.addClass(ClassId.ItemStack, "net.minecraft.world.item.ItemStack");
                this.addClass(ClassId.Entity, "net.minecraft.world.entity.Entity");
                this.addClass(ClassId.EntityLiving, "net.minecraft.world.entity.EntityLiving");
                this.addClass(ClassId.BlockPosition, "net.minecraft.core.BlockPosition");
                this.addClass(ClassId.IBlockData, "net.minecraft.world.level.block.state.IBlockData");
                this.addClass(ClassId.World, "net.minecraft.world.level.World");
                this.addClass(ClassId.TileEntity, "net.minecraft.world.level.block.entity.TileEntity");
                this.addClass(ClassId.TileEntitySkull, "net.minecraft.world.level.block.entity.TileEntitySkull");
                this.addMethod(MethodId.listSet, ClassId.NBTTagList, "set", new Object[]{Integer.TYPE, ClassId.NBTBase});
                this.addMethod(MethodId.setTileTag, ClassId.TileEntity, "load", new Object[]{ClassId.NBTTagCompound});
            }
        }

        private static class v1_16
        extends ReflectionTarget {
            protected v1_16() {
                super(MinecraftVersion.v1_16);
                this.addMethod(MethodId.getEntityTag, ClassId.Entity, "save", new Object[]{ClassId.NBTTagCompound});
                this.addMethod(MethodId.setEntityTag, ClassId.Entity, "load", new Object[]{ClassId.NBTTagCompound});
                this.addMethod(MethodId.getTileType, ClassId.World, "getType", new Object[]{ClassId.BlockPosition});
                this.addMethod(MethodId.setTileTag, ClassId.TileEntity, "load", new Object[]{ClassId.IBlockData, ClassId.NBTTagCompound});
            }
        }

        private static class v1_15
        extends ReflectionTarget {
            protected v1_15() {
                super(MinecraftVersion.v1_15);
                this.addMethod(MethodId.setCraftMetaSkullProfile, ClassId.CraftMetaSkull, "setProfile", new Object[]{ClassId.GameProfile}).failSilently(MinecraftVersion.v1_21_R1.lessThanOrEqualTo(LOCAL_VERSION));
            }
        }

        private static class v1_13
        extends ReflectionTarget {
            protected v1_13() {
                super(MinecraftVersion.v1_13);
                this.addMethod(MethodId.compoundKeys, ClassId.NBTTagCompound, "getKeys", new Object[0]);
                this.addMethod(MethodId.createStack, ClassId.ItemStack, "a", new Object[]{ClassId.NBTTagCompound});
            }
        }

        private static class v1_12
        extends ReflectionTarget {
            protected v1_12() {
                super(MinecraftVersion.v1_12);
                this.addMethod(MethodId.setTileTag, ClassId.TileEntity, "load", new Object[]{ClassId.NBTTagCompound});
            }
        }

        private static class v1_11
        extends ReflectionTarget {
            protected v1_11() {
                super(MinecraftVersion.v1_11);
                this.addConstructor(ClassId.ItemStack, new Object[]{ClassId.NBTTagCompound});
            }
        }

        private static class v1_9
        extends ReflectionTarget {
            protected v1_9() {
                super(MinecraftVersion.v1_9);
                this.addMethod(MethodId.listRemove, ClassId.NBTTagList, "remove", Integer.TYPE);
                this.addMethod(MethodId.compoundKeys, ClassId.NBTTagCompound, "c", new Object[0]);
                this.addMethod(MethodId.getTileTag, ClassId.TileEntity, "save", new Object[]{ClassId.NBTTagCompound});
            }
        }

        private static class v1_8
        extends ReflectionTarget {
            protected v1_8() {
                super(MinecraftVersion.v1_8);
                String string = Bukkit.getServer().getClass().getPackage().getName();
                this.addClass(ClassId.NBTBase, "net.minecraft.server." + VERSION + ".NBTBase");
                this.addClass(ClassId.NBTTagCompound, "net.minecraft.server." + VERSION + ".NBTTagCompound");
                this.addClass(ClassId.NBTTagList, "net.minecraft.server." + VERSION + ".NBTTagList");
                this.addClass(ClassId.NBTTagEnd, "net.minecraft.server." + VERSION + ".NBTTagEnd");
                this.addClass(ClassId.MojangsonParser, "net.minecraft.server." + VERSION + ".MojangsonParser");
                this.addClass(ClassId.ItemStack, "net.minecraft.server." + VERSION + ".ItemStack");
                this.addClass(ClassId.Entity, "net.minecraft.server." + VERSION + ".Entity");
                this.addClass(ClassId.EntityLiving, "net.minecraft.server." + VERSION + ".EntityLiving");
                this.addClass(ClassId.BlockPosition, "net.minecraft.server." + VERSION + ".BlockPosition");
                this.addClass(ClassId.IBlockData, "net.minecraft.server." + VERSION + ".IBlockData");
                this.addClass(ClassId.World, "net.minecraft.server." + VERSION + ".World");
                this.addClass(ClassId.TileEntity, "net.minecraft.server." + VERSION + ".TileEntity");
                this.addClass(ClassId.TileEntitySkull, "net.minecraft.server." + VERSION + ".TileEntitySkull");
                this.addClass(ClassId.CraftServer, string + ".CraftServer");
                this.addClass(ClassId.CraftItemStack, string + ".inventory.CraftItemStack");
                this.addClass(ClassId.CraftMetaSkull, string + ".inventory.CraftMetaSkull");
                this.addClass(ClassId.CraftEntity, string + ".entity.CraftEntity");
                this.addClass(ClassId.CraftWorld, string + ".CraftWorld");
                this.addClass(ClassId.CraftBlockState, string + ".block.CraftBlockState");
                this.addClass(ClassId.GameProfile, "com.mojang.authlib.GameProfile");
                this.addClass(ClassId.Property, "com.mojang.authlib.properties.Property");
                this.addClass(ClassId.PropertyMap, "com.mojang.authlib.properties.PropertyMap");
                this.addMethod(MethodId.compoundGet, ClassId.NBTTagCompound, "get", String.class);
                this.addMethod(MethodId.compoundSet, ClassId.NBTTagCompound, "set", new Object[]{String.class, ClassId.NBTBase});
                this.addMethod(MethodId.compoundHasKey, ClassId.NBTTagCompound, "hasKey", String.class);
                this.addMethod(MethodId.listSet, ClassId.NBTTagList, "a", new Object[]{Integer.TYPE, ClassId.NBTBase});
                this.addMethod(MethodId.listAdd, ClassId.NBTTagList, "add", new Object[]{ClassId.NBTBase});
                this.addMethod(MethodId.listSize, ClassId.NBTTagList, "size", new Object[0]);
                this.addMethod(MethodId.listRemove, ClassId.NBTTagList, "a", Integer.TYPE);
                this.addMethod(MethodId.compoundRemove, ClassId.NBTTagCompound, "remove", String.class);
                this.addMethod(MethodId.compoundKeys, ClassId.NBTTagCompound, "c", new Object[0]);
                this.addMethod(MethodId.itemHasTag, ClassId.ItemStack, "hasTag", new Object[0]);
                this.addMethod(MethodId.getItemTag, ClassId.ItemStack, "getTag", new Object[0]);
                this.addMethod(MethodId.setItemTag, ClassId.ItemStack, "setTag", new Object[]{ClassId.NBTTagCompound});
                this.addMethod(MethodId.itemSave, ClassId.ItemStack, "save", new Object[]{ClassId.NBTTagCompound});
                this.addMethod(MethodId.asNMSCopy, ClassId.CraftItemStack, "asNMSCopy", ItemStack.class);
                this.addMethod(MethodId.asBukkitCopy, ClassId.CraftItemStack, "asBukkitCopy", new Object[]{ClassId.ItemStack});
                this.addMethod(MethodId.getEntityHandle, ClassId.CraftEntity, "getHandle", new Object[0]);
                this.addMethod(MethodId.getEntityTag, ClassId.Entity, "c", new Object[]{ClassId.NBTTagCompound});
                this.addMethod(MethodId.setEntityTag, ClassId.Entity, "f", new Object[]{ClassId.NBTTagCompound});
                this.addMethod(MethodId.createStack, ClassId.ItemStack, "createStack", new Object[]{ClassId.NBTTagCompound});
                this.addMethod(MethodId.setTileTag, ClassId.TileEntity, "a", new Object[]{ClassId.NBTTagCompound});
                this.addMethod(MethodId.getTileTag, ClassId.TileEntity, "b", new Object[]{ClassId.NBTTagCompound});
                this.addMethod(MethodId.getWorldHandle, ClassId.CraftWorld, "getHandle", new Object[0]);
                this.addMethod(MethodId.getTileEntity, ClassId.World, "getTileEntity", new Object[]{ClassId.BlockPosition});
                this.addMethod(MethodId.getProperties, ClassId.GameProfile, "getProperties", new Object[0]);
                this.addMethod(MethodId.setGameProfile, ClassId.TileEntitySkull, "setGameProfile", new Object[]{ClassId.GameProfile});
                this.addMethod(MethodId.propertyValues, ClassId.PropertyMap, "values", new Object[0]);
                this.addMethod(MethodId.putProperty, ClassId.PropertyMap, "put", Object.class, Object.class);
                this.addMethod(MethodId.getPropertyName, ClassId.Property, "getName", new Object[0]);
                this.addMethod(MethodId.getPropertyValue, ClassId.Property, "getValue", new Object[0]);
                this.addMethod(MethodId.loadNBTTagCompound, ClassId.MojangsonParser, "parse", String.class);
                this.addConstructor(ClassId.BlockPosition, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                this.addConstructor(ClassId.GameProfile, UUID.class, String.class);
                this.addConstructor(ClassId.Property, String.class, String.class);
            }
        }

        private static class ReturnMethodTarget
        extends ConstructorTarget {
            final ClassId returnType;

            public ReturnMethodTarget(ClassId classId, ClassId classId2, Object ... objectArray) {
                super(classId, objectArray);
                this.returnType = classId2;
            }
        }

        private static class MethodTarget
        extends ConstructorTarget {
            final String name;
            boolean silent = false;

            public MethodTarget(ClassId classId, String string, Object ... objectArray) {
                super(classId, objectArray);
                this.name = string;
            }

            public MethodTarget failSilently(boolean bl) {
                this.silent = bl;
                return this;
            }
        }

        private static class ConstructorTarget {
            final ClassId clazz;
            final Object[] params;

            public ConstructorTarget(ClassId classId, Object ... objectArray) {
                this.clazz = classId;
                this.params = objectArray;
            }
        }
    }

    private static enum MethodId {
        compoundGet,
        compoundSet,
        compoundHasKey,
        listSet,
        listAdd,
        listSize,
        listRemove,
        compoundRemove,
        compoundKeys,
        itemHasTag,
        getItemTag,
        setItemTag,
        itemSave,
        asNMSCopy,
        asBukkitCopy,
        getEntityHandle,
        getWorldHandle,
        getTileEntity,
        getTileType,
        getEntityTag,
        setEntityTag,
        createStack,
        setTileTag,
        getTileTag,
        getProperties,
        setGameProfile,
        setCraftMetaSkullProfile,
        propertyValues,
        putProperty,
        getPropertyName,
        getPropertyValue,
        loadNBTTagCompound,
        getServer,
        registryAccess,
        saveOptional,
        setCraftMetaSkullResolvableProfile,
        getResolvableProfileGameProfile;

    }

    private static enum ClassId {
        NBTBase,
        NBTTagCompound,
        NBTTagList,
        NBTTagEnd,
        MojangsonParser,
        ItemStack,
        Entity,
        EntityLiving,
        BlockPosition,
        IBlockData,
        World,
        TileEntity,
        TileEntitySkull,
        CraftItemStack,
        CraftMetaSkull,
        CraftEntity,
        CraftWorld,
        CraftBlockState,
        GameProfile,
        Property,
        PropertyMap,
        CraftServer,
        MinecraftServer,
        RegistryAccess,
        ResolvableProfile,
        IRegistryCustomDimension;

    }

    private static enum Type {
        COMPOUND,
        LIST,
        NEW_ELEMENT,
        DELETE,
        CUSTOM_DATA,
        ITEMSTACK_COMPONENTS;

    }

    public static enum MinecraftVersion {
        v1_8,
        v1_9,
        v1_10,
        v1_11,
        v1_12,
        v1_13,
        v1_14,
        v1_15,
        v1_16,
        v1_17,
        v1_18_R1,
        v1_18_R2,
        v1_19_R1,
        v1_19_R2,
        v1_19_R3,
        v1_20_R1,
        v1_20(false),
        v1_20_1(false),
        v1_20_R2,
        v1_20_2(false),
        v1_20_R3,
        v1_20_3(false),
        v1_20_R4,
        v1_20_4(false),
        v1_20_5(false),
        v1_20_6(false),
        v1_21_R1,
        v1_21(false),
        v1_21_R2,
        v1_21_3(false),
        v1_21_R3,
        v1_21_4(false),
        v1_22;

        private boolean implemented = true;

        private MinecraftVersion() {
            this(true);
        }

        private MinecraftVersion(boolean bl) {
            this.implemented = bl;
        }

        public boolean greaterThanOrEqualTo(MinecraftVersion minecraftVersion) {
            return this.ordinal() >= minecraftVersion.ordinal();
        }

        public boolean lessThanOrEqualTo(MinecraftVersion minecraftVersion) {
            return this.ordinal() <= minecraftVersion.ordinal();
        }

        public static MinecraftVersion get(String string) {
            string = string.replace('.', '_');
            for (int i = MinecraftVersion.values().length; i > 0; --i) {
                MinecraftVersion minecraftVersion = MinecraftVersion.values()[i - 1];
                if (!string.contains(minecraftVersion.name().substring(1))) continue;
                return MinecraftVersion.getLastImplemented(minecraftVersion);
            }
            return MinecraftVersion.values()[MinecraftVersion.values().length - 1];
        }

        private static MinecraftVersion getLastImplemented(MinecraftVersion minecraftVersion) {
            for (int i = minecraftVersion.ordinal(); i >= 0; --i) {
                MinecraftVersion minecraftVersion2 = MinecraftVersion.values()[i];
                if (!minecraftVersion2.implemented) continue;
                return minecraftVersion2;
            }
            return null;
        }
    }

    public static final class NBTCompound {
        protected final Object tag;

        protected NBTCompound(Object object) {
            this.tag = object;
        }

        public void set(Object object, Object ... objectArray) {
            try {
                NBTEditor.setTag(this.tag, object, objectArray);
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        public String toJson() {
            return this.tag.toString();
        }

        public static NBTCompound fromJson(String string) {
            try {
                return new NBTCompound(NBTEditor.getMethod(MethodId.loadNBTTagCompound).invoke(null, string));
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                exception.printStackTrace();
                return null;
            }
        }

        public String toString() {
            return this.tag.toString();
        }

        public int hashCode() {
            return this.tag.hashCode();
        }

        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null) {
                return false;
            }
            if (this.getClass() != object.getClass()) {
                return false;
            }
            NBTCompound nBTCompound = (NBTCompound)object;
            return !(this.tag == null ? nBTCompound.tag != null : !this.tag.equals(nBTCompound.tag));
        }
    }
}

