package fr.valoriatycoon.warps;

import java.util.List;
import java.util.Objects;
import org.bukkit.Material;

/** One configured direct/menu warp destination. */
public record WarpDefinition(
        String id,
        List<String> aliases,
        String worldName,
        boolean relativeToSpawn,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        int menuSlot,
        Material icon,
        String itemModel,
        String displayName,
        List<String> lore
) {
    public WarpDefinition {
        id = Objects.requireNonNull(id, "id");
        aliases = List.copyOf(aliases);
        worldName = Objects.requireNonNull(worldName, "worldName");
        icon = Objects.requireNonNull(icon, "icon");
        itemModel = Objects.requireNonNull(itemModel, "itemModel");
        displayName = Objects.requireNonNull(displayName, "displayName");
        lore = List.copyOf(lore);
        if (id.isBlank() || worldName.isBlank() || itemModel.isBlank() || displayName.isBlank()
                || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)
                || menuSlot < 0 || !icon.isItem()) {
            throw new IllegalArgumentException("Invalid warp definition: " + id);
        }
    }
}
