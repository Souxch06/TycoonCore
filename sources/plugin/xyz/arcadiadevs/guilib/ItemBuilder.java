package xyz.arcadiadevs.guilib;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import xyz.arcadiadevs.guilib.ItemDataColor;

public class ItemBuilder {
    private final ItemStack stack;

    public ItemBuilder(Material material) {
        this.stack = new ItemStack(material);
    }

    public ItemBuilder(ItemStack itemStack) {
        this.stack = itemStack;
    }

    public ItemBuilder type(Material material) {
        this.stack.setType(material);
        return this;
    }

    public Material getType() {
        return this.stack.getType();
    }

    public ItemBuilder name(String string) {
        ItemMeta itemMeta = this.stack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes((char)'&', (String)string));
        this.stack.setItemMeta(itemMeta);
        return this;
    }

    public String getName() {
        return this.stack.hasItemMeta() && this.stack.getItemMeta().hasDisplayName() ? this.stack.getItemMeta().getDisplayName() : null;
    }

    public ItemBuilder amount(int n) {
        this.stack.setAmount(n);
        return this;
    }

    public int getAmount() {
        return this.stack.getAmount();
    }

    public ItemBuilder lore(String ... stringArray) {
        return this.lore(Arrays.asList(stringArray));
    }

    public ItemBuilder lore(List<String> list) {
        list.replaceAll(string -> ChatColor.translateAlternateColorCodes((char)'&', (String)string));
        ItemMeta itemMeta = this.stack.getItemMeta();
        itemMeta.setLore(list);
        this.stack.setItemMeta(itemMeta);
        return this;
    }

    public List<String> getLore() {
        return this.stack.hasItemMeta() && this.stack.getItemMeta().hasLore() ? this.stack.getItemMeta().getLore() : null;
    }

    public ItemBuilder color(ItemDataColor itemDataColor) {
        return this.durability(itemDataColor.getValue());
    }

    public ItemBuilder data(short s) {
        return this.durability(s);
    }

    public ItemBuilder durability(short s) {
        this.stack.setDurability(s);
        return this;
    }

    public short getDurability() {
        return this.stack.getDurability();
    }

    public ItemDataColor getColor() {
        return ItemDataColor.getByValue(this.stack.getDurability());
    }

    public ItemBuilder enchant(Enchantment enchantment, int n) {
        this.stack.addUnsafeEnchantment(enchantment, n);
        return this;
    }

    public ItemBuilder unenchant(Enchantment enchantment) {
        this.stack.removeEnchantment(enchantment);
        return this;
    }

    public ItemBuilder flag(ItemFlag ... itemFlagArray) {
        ItemMeta itemMeta = this.stack.getItemMeta();
        itemMeta.addItemFlags(itemFlagArray);
        this.stack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder deflag(ItemFlag ... itemFlagArray) {
        ItemMeta itemMeta = this.stack.getItemMeta();
        itemMeta.removeItemFlags(itemFlagArray);
        this.stack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder skullOwner(String string) {
        if (!(this.stack.getItemMeta() instanceof SkullMeta)) {
            return this;
        }
        this.stack.setDurability((short)3);
        SkullMeta skullMeta = (SkullMeta)this.stack.getItemMeta();
        skullMeta.setOwner(string);
        this.stack.setItemMeta((ItemMeta)skullMeta);
        return this;
    }

    public ItemBuilder ifThen(Predicate<ItemBuilder> predicate, Function<ItemBuilder, Object> function) {
        if (predicate.test(this)) {
            function.apply(this);
        }
        return this;
    }

    public ItemStack build() {
        return this.get();
    }

    public ItemStack get() {
        return this.stack;
    }
}

