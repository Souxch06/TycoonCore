/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package org.holoeasy.builder;

import kotlin.Pair;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.holoeasy.builder.HologramConfig;
import org.holoeasy.builder.Service;
import org.holoeasy.builder.interfaces.HologramConfigGroup;
import org.holoeasy.builder.interfaces.HologramRegisterGroup;
import org.holoeasy.builder.interfaces.HologramSetupGroup;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.line.ILine;
import org.holoeasy.line.ITextLine;
import org.holoeasy.pool.IHologramPool;
import org.holoeasy.reactive.MutableState;
import org.jetbrains.annotations.NotNull;

public class HologramBuilder {
    static Service getInstance() {
        return Service.INSTANCE;
    }

    public static void registerHolograms(@NotNull IHologramPool iHologramPool, @NotNull HologramRegisterGroup hologramRegisterGroup) {
        HologramBuilder.getInstance().getStaticPool().set(new Pair<Service.RegistrationType, IHologramPool>(Service.RegistrationType.POOL, iHologramPool));
        hologramRegisterGroup.register();
        HologramBuilder.getInstance().getStaticPool().remove();
    }

    public static void registerHolograms(@NotNull Plugin plugin, @NotNull HologramRegisterGroup hologramRegisterGroup) {
        HologramBuilder.getInstance().getStaticPool().set(new Pair<Service.RegistrationType, Plugin>(Service.RegistrationType.PLUGIN, plugin));
        hologramRegisterGroup.register();
        HologramBuilder.getInstance().getStaticPool().remove();
    }

    public static Hologram hologram(@NotNull Location location, @NotNull HologramSetupGroup hologramSetupGroup) {
        Pair<Service.RegistrationType, Object> pair = HologramBuilder.getInstance().getStaticRegistration();
        HologramConfig hologramConfig = null;
        IHologramPool iHologramPool = null;
        switch (pair.component1()) {
            case POOL: {
                iHologramPool = (IHologramPool)pair.component2();
                hologramConfig = new HologramConfig(iHologramPool.getPlugin(), location);
                break;
            }
            case PLUGIN: {
                hologramConfig = new HologramConfig((Plugin)pair.component2(), location);
                break;
            }
            default: {
                throw new RuntimeException("invalid registration type " + pair.component1().name());
            }
        }
        HologramBuilder.getInstance().getStaticHologram().set(hologramConfig);
        hologramSetupGroup.setup();
        HologramBuilder.getInstance().getStaticHologram().remove();
        Hologram hologram = new Hologram(hologramConfig.plugin, hologramConfig.location, hologramConfig.loader);
        hologram.load(hologramConfig.lines.toArray(new ILine[0]));
        if (iHologramPool != null) {
            iHologramPool.takeCareOf(hologram);
        }
        return hologram;
    }

    public static void config(@NotNull HologramConfigGroup hologramConfigGroup) {
        HologramBuilder.getInstance().config(hologramConfigGroup);
    }

    public static void textline(@NotNull String string, Object ... objectArray) {
        HologramBuilder.getInstance().textline(string, false, null, null, objectArray.length == 0 ? null : objectArray);
    }

    public static ITextLine clickable(@NotNull String string, Object ... objectArray) {
        return HologramBuilder.getInstance().textline(string, true, null, null, objectArray.length == 0 ? null : objectArray);
    }

    public static ITextLine clickable(@NotNull String string, float f, float f2, Object ... objectArray) {
        return HologramBuilder.getInstance().textline(string, true, Float.valueOf(f), Float.valueOf(f2), objectArray.length == 0 ? null : objectArray);
    }

    public static void item(@NotNull ItemStack itemStack) {
        HologramBuilder.getInstance().itemline(itemStack);
    }

    public static void item(@NotNull MutableState<ItemStack> mutableState) {
        HologramBuilder.getInstance().itemlineMutable(mutableState);
    }

    public static void block(@NotNull ItemStack itemStack) {
        HologramBuilder.getInstance().blockline(itemStack);
    }

    public static void block(@NotNull MutableState<ItemStack> mutableState) {
        HologramBuilder.getInstance().blocklineMutable(mutableState);
    }

    public static void customline(@NotNull ILine<?> iLine) {
        HologramBuilder.getInstance().customLine(iLine);
    }

    public static <T> MutableState<T> mutableStateOf(@NotNull T t) {
        return new MutableState<T>(t);
    }
}

