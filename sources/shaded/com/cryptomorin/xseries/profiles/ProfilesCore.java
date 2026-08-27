package com.cryptomorin.xseries.profiles;

import com.cryptomorin.xseries.reflection.ReflectiveHandle;
import com.cryptomorin.xseries.reflection.ReflectiveNamespace;
import com.cryptomorin.xseries.reflection.XReflection;
import com.cryptomorin.xseries.reflection.jvm.FieldMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.FlaggedNamedMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.MethodMemberHandle;
import com.cryptomorin.xseries.reflection.jvm.classes.ClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftClassHandle;
import com.cryptomorin.xseries.reflection.minecraft.MinecraftMapping;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import java.lang.invoke.MethodHandle;
import java.net.Proxy;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class ProfilesCore {
    public static final Logger LOGGER;
    public static final Object USER_CACHE;
    public static final Object MINECRAFT_SESSION_SERVICE;
    public static final Proxy PROXY;
    public static final LoadingCache<Object, Object> YggdrasilMinecraftSessionService_insecureProfiles;
    public static final Map<String, Object> UserCache_profilesByName;
    public static final Map<UUID, Object> UserCache_profilesByUUID;
    public static final MethodHandle FILL_PROFILE_PROPERTIES;
    public static final MethodHandle GET_PROFILE_BY_NAME;
    public static final MethodHandle GET_PROFILE_BY_UUID;
    public static final MethodHandle CACHE_PROFILE;
    public static final MethodHandle CRAFT_META_SKULL_PROFILE_GETTER;
    public static final MethodHandle CRAFT_META_SKULL_PROFILE_SETTER;
    public static final MethodHandle CRAFT_SKULL_PROFILE_SETTER;
    public static final MethodHandle CRAFT_SKULL_PROFILE_GETTER;
    public static final MethodHandle Property_getValue;
    public static final MethodHandle UserCache_getNextOperation;
    public static final MethodHandle UserCacheEntry_getProfile;
    public static final MethodHandle UserCacheEntry_setLastAccess;
    public static final boolean NULLABILITY_RECORD_UPDATE;

    public static void debug(String string, Object ... objectArray) {
        LOGGER.debug(string, objectArray);
    }

    private static /* synthetic */ MethodMemberHandle lambda$static$3(MethodMemberHandle methodMemberHandle) {
        return methodMemberHandle.signature("public Optional<GameProfile> get(UUID id);");
    }

    private static /* synthetic */ MethodMemberHandle lambda$static$2(MethodMemberHandle methodMemberHandle) {
        return methodMemberHandle.signature("public GameProfile get(UUID id);");
    }

    private static /* synthetic */ MethodMemberHandle lambda$static$1(MethodMemberHandle methodMemberHandle) {
        return methodMemberHandle.signature("public Optional<GameProfile> get(String username);");
    }

    private static /* synthetic */ MethodMemberHandle lambda$static$0(MethodMemberHandle methodMemberHandle) {
        return methodMemberHandle.signature("public GameProfile get(String username);");
    }

    static {
        Proxy proxy;
        MethodHandle methodHandle;
        MethodHandle methodHandle2;
        MethodHandle methodHandle3;
        Object object;
        Object object2;
        Object object3;
        ReflectiveHandle<Class<?>> reflectiveHandle;
        MethodHandle methodHandle4;
        MethodHandle methodHandle5;
        MinecraftClassHandle minecraftClassHandle;
        LOGGER = LogManager.getLogger((String)"XSkull");
        NULLABILITY_RECORD_UPDATE = XReflection.supports(1, 20, 2);
        Object object4 = null;
        MethodHandle methodHandle6 = null;
        ReflectiveNamespace reflectiveNamespace = XReflection.namespaced().imports(GameProfile.class, MinecraftSessionService.class, LoadingCache.class);
        MinecraftClassHandle minecraftClassHandle2 = reflectiveNamespace.ofMinecraft("package nms.server.players; public class GameProfileCache {}").map(MinecraftMapping.SPIGOT, "UserCache");
        try {
            minecraftClassHandle = reflectiveNamespace.ofMinecraft("package cb.inventory; class CraftMetaSkull extends CraftMetaItem implements SkullMeta {}");
            methodHandle5 = minecraftClassHandle.field("private GameProfile profile;").getter().reflect();
            try {
                methodHandle4 = minecraftClassHandle.method("private void setProfile(GameProfile profile);").reflect();
            }
            catch (NoSuchMethodException noSuchMethodException) {
                methodHandle4 = minecraftClassHandle.field("private GameProfile profile;").setter().reflect();
            }
            reflectiveHandle = reflectiveNamespace.ofMinecraft("package nms.server; public abstract class MinecraftServer {}");
            object3 = ((ClassHandle)reflectiveHandle).method("public static MinecraftServer getServer();").reflect().invoke();
            object2 = ((ClassHandle)reflectiveHandle).method("public MinecraftSessionService getSessionService();").named("ay", "getMinecraftSessionService", "az", "ao", "am", "aD", "ar").reflect().invoke(object3);
            FlaggedNamedMemberHandle flaggedNamedMemberHandle = reflectiveNamespace.ofMinecraft("package com.mojang.authlib.yggdrasil;public class YggdrasilMinecraftSessionService implements MinecraftSessionService {}").field().getter();
            if (NULLABILITY_RECORD_UPDATE) {
                flaggedNamedMemberHandle.signature("private final LoadingCache<UUID, Optional<ProfileResult>> insecureProfiles;");
            } else {
                flaggedNamedMemberHandle.signature("private final LoadingCache<GameProfile, GameProfile> insecureProfiles;");
            }
            Object object5 = (MethodHandle)flaggedNamedMemberHandle.reflectOrNull();
            if (object5 != null) {
                object4 = object5.invoke(object2);
            }
            object = ((ClassHandle)reflectiveHandle).method("public GameProfileCache getProfileCache();").named("ar", "ao", "ap", "au").map(MinecraftMapping.OBFUSCATED, "getUserCache").reflect().invoke(object3);
            if (!NULLABILITY_RECORD_UPDATE) {
                methodHandle6 = reflectiveNamespace.of(MinecraftSessionService.class).method("public GameProfile fillProfileProperties(GameProfile profile, boolean flag);").reflect();
            }
            UserCache_getNextOperation = (MethodHandle)minecraftClassHandle2.method("private long getNextOperation();").map(MinecraftMapping.OBFUSCATED, XReflection.v(21, "e").v(16, "d").orElse("d")).reflectOrNull();
            flaggedNamedMemberHandle = minecraftClassHandle2.method().named("getProfile", "a");
            object5 = minecraftClassHandle2.method().named("getProfile", "a");
            methodHandle3 = (MethodHandle)XReflection.anyOf(() -> ProfilesCore.lambda$static$0((MethodMemberHandle)flaggedNamedMemberHandle), () -> ProfilesCore.lambda$static$1((MethodMemberHandle)flaggedNamedMemberHandle)).reflect();
            methodHandle2 = (MethodHandle)XReflection.anyOf(() -> ProfilesCore.lambda$static$2((MethodMemberHandle)object5), () -> ProfilesCore.lambda$static$3((MethodMemberHandle)object5)).reflect();
            methodHandle = minecraftClassHandle2.method("public void add(GameProfile profile);").map(MinecraftMapping.OBFUSCATED, "a").reflect();
            proxy = ((ClassHandle)reflectiveHandle).field("protected final java.net.Proxy proxy;").getter().map(MinecraftMapping.OBFUSCATED, XReflection.v(20, 5, "h").v(20, 3, "i").v(19, "j").v(18, 2, "n").v(18, "o").v(17, "m").v(14, "proxy").v(13, "c").orElse("e")).reflect().invoke(object3);
        }
        catch (Throwable throwable) {
            throw XReflection.throwCheckedException(throwable);
        }
        minecraftClassHandle = reflectiveNamespace.ofMinecraft("package cb.block; public class CraftSkull extends CraftBlockEntityState implements Skull {}");
        reflectiveHandle = minecraftClassHandle.field("private GameProfile profile;");
        Property_getValue = NULLABILITY_RECORD_UPDATE ? null : (MethodHandle)reflectiveNamespace.of(Property.class).method("public String getValue();").unreflect();
        PROXY = proxy;
        USER_CACHE = object;
        YggdrasilMinecraftSessionService_insecureProfiles = (LoadingCache)object4;
        MINECRAFT_SESSION_SERVICE = object2;
        FILL_PROFILE_PROPERTIES = methodHandle6;
        GET_PROFILE_BY_NAME = methodHandle3;
        GET_PROFILE_BY_UUID = methodHandle2;
        CACHE_PROFILE = methodHandle;
        CRAFT_META_SKULL_PROFILE_SETTER = methodHandle4;
        CRAFT_META_SKULL_PROFILE_GETTER = methodHandle5;
        CRAFT_SKULL_PROFILE_SETTER = (MethodHandle)((FieldMemberHandle)reflectiveHandle).setter().unreflect();
        CRAFT_SKULL_PROFILE_GETTER = (MethodHandle)((FieldMemberHandle)reflectiveHandle).getter().unreflect();
        object3 = minecraftClassHandle2.inner("private static class GameProfileInfo {}").map(MinecraftMapping.SPIGOT, "UserCacheEntry");
        UserCacheEntry_getProfile = (MethodHandle)((ClassHandle)object3).method("public GameProfile getProfile();").map(MinecraftMapping.OBFUSCATED, "a").makeAccessible().unreflect();
        UserCacheEntry_setLastAccess = (MethodHandle)((ClassHandle)object3).method("public void setLastAccess(long i);").map(MinecraftMapping.OBFUSCATED, "a").reflectOrNull();
        try {
            UserCache_profilesByName = minecraftClassHandle2.field("private final Map<String, UserCache.UserCacheEntry> profilesByName;").getter().map(MinecraftMapping.OBFUSCATED, XReflection.v(17, "e").v(16, 2, "c").v(9, "d").orElse("c")).reflect().invoke(object);
            UserCache_profilesByUUID = minecraftClassHandle2.field("private final Map<UUID, UserCache.UserCacheEntry> profilesByUUID;").getter().map(MinecraftMapping.OBFUSCATED, XReflection.v(17, "f").v(16, 2, "d").v(9, "e").orElse("d")).reflect().invoke(object);
        }
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}

