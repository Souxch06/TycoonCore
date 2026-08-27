/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  lombok.Generated
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package xyz.arcadiadevs.valoriatycoon.models;

import java.util.List;
import lombok.Generated;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.arcadiadevs.valoriatycoon.utils.ActionBarUtil;

public record GeneratorsData(List<Generator> generators) {
    public Generator getGenerator(int n) {
        return this.generators.stream().filter(generator -> generator.tier() == n).findFirst().orElse(null);
    }

    public double getUpgradePrice(Generator generator3, int n) {
        return this.generators.stream().filter(generator -> generator.tier() == n).findFirst().map(generator2 -> generator2.price() - generator3.price()).orElseThrow();
    }

    public void giveItemByTier(Player player, int n, int n2) {
        Generator generator = this.getGenerator(n);
        ItemStack itemStack = new ItemStack(generator.blockType());
        itemStack.setAmount(n2);
        player.getInventory().addItem(new ItemStack[]{itemStack});
    }

    @Generated
    public List<Generator> getGenerators() {
        return this.generators;
    }

    public record Generator(String name, int tier, double price, double sellPrice, int speed, ItemStack spawnItem, ItemStack blockType, List<String> lore, boolean instantBreak) {
        public void giveItem(Player player) {
            if (player.getInventory().firstEmpty() == -1) {
                ActionBarUtil.sendActionBar(player, "&cVotre inventaire est plein !");
                player.getWorld().dropItemNaturally(player.getLocation(), this.blockType);
                return;
            }
            player.getInventory().addItem(new ItemStack[]{this.blockType});
        }

        public void dropItem(Player player, Location location) {
            player.getWorld().dropItemNaturally(location, this.blockType);
        }

        @Override
        public String toString() {
            return "Generator{name='" + this.name + "', tier=" + this.tier + "}";
        }
    }
}

