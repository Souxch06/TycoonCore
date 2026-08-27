/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
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
            Bukkit.getLogger().severe("Ceci n'est ni un bug ni un crash. Veuillez lire ci-dessous");
            Bukkit.getLogger().severe("Vérifiez que le monde existe ou supprimez le fichier");
            Bukkit.getLogger().severe("data/block_data.yml pour réinitialiser vos données de générateurs.");
            Bukkit.getLogger().severe("=============================================");
            Bukkit.getLogger().severe("Le monde est null pour la position de générateur " + String.valueOf(this.world) + ", avez-vous supprimé ou renommé votre monde ? | Arrêt du serveur...");
            Bukkit.shutdown();
            return null;
        }
        return new Location(world, this.x, this.y, this.z);
    }

    public static SimplifiedLocation fromLocation(Location location) {
        return new SimplifiedLocation(location.getWorld().getUID(), location.getX(), location.getY(), location.getZ());
    }
}

