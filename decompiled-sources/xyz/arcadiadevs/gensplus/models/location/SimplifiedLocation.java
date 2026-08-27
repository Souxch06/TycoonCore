/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 */
package xyz.arcadiadevs.gensplus.models.location;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record SimplifiedLocation(UUID world, double x, double y, double z) {
    public Location getLocation() {
        World world = Bukkit.getWorld((UUID)this.world);
        if (world == null) {
            Bukkit.getLogger().severe("=============================================");
            Bukkit.getLogger().severe("This is not a bug or crash. Please read below");
            Bukkit.getLogger().severe("And make sure the world exists or remove block");
            Bukkit.getLogger().severe("data/block_data.yml to reset your gens data.");
            Bukkit.getLogger().severe("=============================================");
            Bukkit.getLogger().severe("World is null for generator location " + String.valueOf(this.world) + ", did you remove or rename your world? | Shutting down server...");
            Bukkit.shutdown();
            return null;
        }
        return new Location(world, this.x, this.y, this.z);
    }

    public static SimplifiedLocation fromLocation(Location location) {
        return new SimplifiedLocation(location.getWorld().getUID(), location.getX(), location.getY(), location.getZ());
    }
}

