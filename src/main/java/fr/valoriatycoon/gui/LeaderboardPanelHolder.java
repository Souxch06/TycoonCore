package fr.valoriatycoon.gui;

import fr.valoriatycoon.leaderboards.LeaderboardType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Identifies root/detail leaderboard inventories without relying on inventory titles. */
public final class LeaderboardPanelHolder implements InventoryHolder {
    private final LeaderboardType type;
    private Inventory inventory;

    public LeaderboardPanelHolder(@Nullable LeaderboardType type) {
        this.type = type;
    }

    public @Nullable LeaderboardType type() {
        return type;
    }

    public void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Leaderboard inventory is not bound");
        }
        return inventory;
    }
}
