package xyz.arcadiadevs.valoriatycoon.utils;

import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.holoeasy.builder.HologramBuilder;
import org.holoeasy.hologram.Hologram;
import org.jetbrains.annotations.NotNull;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;
import xyz.arcadiadevs.valoriatycoon.utils.ServerVersion;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;

public class HologramsUtil {
    public static Hologram createHologram(Location location, List<String> list, Material material) {
        if (!Config.HOLOGRAMS_ENABLED.getBoolean()) {
            return null;
        }
        try {
            Hologram[] hologramArray = new Hologram[1];
            Location location2 = location.clone().subtract(0.0, 1.0, 0.0);
            ValoriaTycoon.getInstance().getHologramPool().registerHolograms(() -> {
                hologramArray[0] = HologramBuilder.hologram(location2, () -> {
                    for (String string : list) {
                        HologramBuilder.textline(string, new Object[0]);
                    }
                    if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)) {
                        HologramBuilder.item(new ItemStack(material));
                    }
                });
            });
            return hologramArray[0];
        }
        catch (Exception exception) {
            if (Config.DEVELOPER_OPTIONS.getBoolean()) {
                exception.printStackTrace();
            }
            return null;
        }
    }

    public static Hologram getHologram(String string) {
        if (string == null) {
            return null;
        }
        return ValoriaTycoon.getInstance().getHologramPool().get(UUID.fromString(string));
    }

    public static void removeHologram(@NotNull Hologram hologram) {
        block2: {
            try {
                ValoriaTycoon.getInstance().getHologramPool().remove(hologram.getId());
            }
            catch (Exception exception) {
                if (!Config.DEVELOPER_OPTIONS.getBoolean()) break block2;
                exception.printStackTrace();
            }
        }
    }
}

