package fr.valoriatycoon.crates;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.machines.MachineSettings;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import fr.valoriatycoon.tools.ToolSettings;
import fr.valoriatycoon.tools.ToolType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Builds and validates uniquely identified, tradable physical generic reward tokens. */
public final class CrateRewardItemService {
    private final ToolSettings toolSettings;
    private final MachineSettings machineSettings;
    private final ItemVisualService visuals;
    private final CurrencyFormatter currency;
    private final MessageService messages;
    private final NamespacedKey markerKey;
    private final NamespacedKey idKey;
    private final NamespacedKey kindKey;

    public CrateRewardItemService(
            JavaPlugin plugin,
            ToolSettings toolSettings,
            MachineSettings machineSettings,
            ItemVisualService visuals,
            CurrencyFormatter currency,
            MessageService messages
    ) {
        this.toolSettings = Objects.requireNonNull(toolSettings, "toolSettings");
        this.machineSettings = Objects.requireNonNull(machineSettings, "machineSettings");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.markerKey = new NamespacedKey(plugin, "crate_reward");
        this.idKey = new NamespacedKey(plugin, "crate_reward_id");
        this.kindKey = new NamespacedKey(plugin, "crate_reward_kind");
    }

    public ItemStack create(CrateReward reward) {
        Presentation presentation = presentation(reward);
        ItemStack item = new ItemStack(presentation.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(presentation.name()));
        List<Component> lore = presentation.lore().stream().map(messages::render).collect(
                java.util.stream.Collectors.toCollection(ArrayList::new)
        );
        lore.add(messages.render("<yellow>Clic droit pour utiliser cette récompense.</yellow>"));
        lore.add(messages.render("<dark_gray>Jeton unique protégé contre la duplication.</dark_gray>"));
        meta.lore(lore);
        visuals.apply(meta, presentation.model());
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, reward.rewardId().toString());
        meta.getPersistentDataContainer().set(kindKey, PersistentDataType.STRING, reward.kind().name());
        item.setItemMeta(meta);
        return item;
    }

    public void give(Player player, CrateReward reward) {
        player.getInventory().addItem(create(reward)).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover)
        );
    }

    public Optional<RewardToken> token(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        Byte marker = meta.getPersistentDataContainer().get(markerKey, PersistentDataType.BYTE);
        String rawId = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        String rawKind = meta.getPersistentDataContainer().get(kindKey, PersistentDataType.STRING);
        if (marker == null || marker.byteValue() != 1 || rawId == null || rawKind == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new RewardToken(
                    UUID.fromString(rawId),
                    CrateRewardKind.valueOf(rawKind)
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /** Removes every local copy after the authoritative row is consumed or rejected. */
    public void removeCopies(Player player, UUID rewardId) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();
        boolean changed = false;
        for (int slot = 0; slot < storage.length; slot++) {
            RewardToken token = token(storage[slot]).orElse(null);
            if (token != null && token.rewardId().equals(rewardId)) {
                storage[slot] = null;
                changed = true;
            }
        }
        if (changed) {
            inventory.setStorageContents(storage);
        }
        RewardToken offHand = token(inventory.getItemInOffHand()).orElse(null);
        if (offHand != null && offHand.rewardId().equals(rewardId)) {
            inventory.setItemInOffHand(null);
        }
    }

    public Component displayName(CrateReward reward) {
        return messages.render(presentation(reward).name());
    }

    private Presentation presentation(CrateReward reward) {
        CrateRewardPayload payload = reward.payload();
        int count = payload.requireInt("count");
        return switch (reward.kind()) {
            case MONEY_BAG -> {
                int tier = payload.requireInt("tier");
                yield new Presentation(
                        Material.BUNDLE,
                        "<gold><bold>Sac d'argent " + roman(tier) + "</bold></gold>",
                        List.of("<gray>Montant exact : <yellow>" + currency.format(
                                payload.requireLong("amount_cents")
                        ) + "</yellow></gray>"),
                        "item/reward/money_bag/" + tier
                );
            }
            case COIN_BAG -> {
                int tier = payload.requireInt("tier");
                ToolType type = ToolType.valueOf(payload.require("tool"));
                yield new Presentation(
                        Material.BUNDLE,
                        "<green><bold>Sac de " + toolSettings.tool(type).currencyName()
                                + ' ' + roman(tier) + "</bold></green>",
                        List.of("<gray>Montant exact : <green>" + payload.requireLong("amount")
                                + " coins</green></gray>"),
                        "item/reward/coin_bag/" + tier
                );
            }
            case UNIVERSAL_COIN_BAG -> new Presentation(
                    Material.BUNDLE,
                    "<light_purple><bold>Sac de coins universel</bold></light_purple>",
                    List.of(
                            "<gray>Crédite <light_purple>" + payload.requireLong("amount_each")
                                    + "</light_purple> dans chacune des quatre monnaies.</gray>",
                            count > 1
                                    ? "<gray>Contient " + count + " sacs cumulés.</gray>"
                                    : "<gray>Pioche, houe, hache et canne.</gray>"
                    ),
                    "item/reward/coin_bag/universal"
            );
            case XP_VIAL -> {
                int tier = payload.requireInt("tier");
                yield new Presentation(
                        Material.EXPERIENCE_BOTTLE,
                        "<aqua><bold>Fiole d'XP " + roman(tier) + "</bold></aqua>",
                        List.of("<gray>Niveaux XP vanilla : <aqua>+" + payload.requireLong("levels")
                                + "</aqua></gray>"),
                        "item/reward/xp_vial/" + tier
                );
            }
            case RESOURCE_BUNDLE -> {
                int tier = payload.requireInt("tier");
                Material material = Material.valueOf(payload.require("material"));
                yield new Presentation(
                        Material.CHEST,
                        "<yellow><bold>Paquet de ressources " + roman(tier) + "</bold></yellow>",
                        List.of("<gray>Contenu : <yellow>" + payload.requireLong("amount") + " × "
                                + materialName(material) + "</yellow></gray>"),
                        "item/reward/resource_bundle/" + tier
                );
            }
            case VANILLA_ITEM -> {
                Material material = Material.valueOf(payload.require("material"));
                int amount = payload.requireInt("amount");
                yield new Presentation(
                        Material.PAPER,
                        "<yellow><bold>Bon d'équipement</bold></yellow>",
                        List.of("<gray>Contenu : <yellow>" + amount + " × " + materialName(material)
                                + "</yellow></gray>"),
                        "item/reward/voucher/item"
                );
            }
            case CRATE_KEYS -> {
                CrateType type = CrateType.valueOf(payload.require("crate_type"));
                int amount = payload.requireInt("amount");
                yield new Presentation(
                        Material.PAPER,
                        "<gold><bold>Bon de clé" + (amount > 1 ? "s" : "") + "</bold></gold>",
                        List.of("<gray>Contenu : <gold>" + amount + " × Clé "
                                + type.displayName() + "</gold></gray>"),
                        "item/reward/voucher/key"
                );
            }
            case PET_KEYS -> {
                int amount = payload.requireInt("amount");
                yield new Presentation(
                        Material.PAPER,
                        "<light_purple><bold>Bon de Clé Pets</bold></light_purple>",
                        List.of("<gray>Contenu : <light_purple>" + amount
                                + " × Clé de la Caisse Pets</light_purple></gray>"),
                        "item/reward/voucher/pet_key"
                );
            }
            case GENERATORS -> {
                List<String> types = List.of(payload.require("types").split(","));
                List<String> lore = new ArrayList<>();
                lore.add("<gray>Contient <yellow>" + types.size() + " générateur(s)</yellow> :</gray>");
                for (String type : types) {
                    lore.add("<dark_gray>•</dark_gray> " + machineSettings.machine(type).displayName());
                }
                yield new Presentation(
                        Material.PAPER,
                        "<yellow><bold>Bon de générateur</bold></yellow>",
                        lore,
                        "item/reward/voucher/generator"
                );
            }
        };
    }

    private String roman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> throw new IllegalArgumentException("Invalid reward tier " + tier);
        };
    }

    private String materialName(Material material) {
        return switch (material) {
            case COAL -> "Charbon";
            case WHEAT -> "Blé";
            case OAK_LOG -> "Bûche de chêne";
            case RAW_IRON -> "Fer brut";
            case RAW_COPPER -> "Cuivre brut";
            case CARROT -> "Carotte";
            case BIRCH_LOG -> "Bûche de bouleau";
            case GOLD_INGOT -> "Lingot d'or";
            case REDSTONE -> "Redstone";
            case LAPIS_LAZULI -> "Lapis-lazuli";
            case POTATO -> "Pomme de terre";
            case SPRUCE_LOG -> "Bûche de sapin";
            case DIAMOND -> "Diamant";
            case EMERALD -> "Émeraude";
            case BEETROOT -> "Betterave";
            case DARK_OAK_LOG -> "Bûche de chêne noir";
            case HOPPER -> "Entonnoir";
            case SHULKER_BOX -> "Boîte de Shulker";
            case BEACON -> "Balise";
            default -> material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }

    public record RewardToken(UUID rewardId, CrateRewardKind kind) {
        public RewardToken {
            rewardId = Objects.requireNonNull(rewardId, "rewardId");
            kind = Objects.requireNonNull(kind, "kind");
        }
    }

    private record Presentation(Material material, String name, List<String> lore, String model) {
        private Presentation {
            material = Objects.requireNonNull(material, "material");
            name = Objects.requireNonNull(name, "name");
            lore = List.copyOf(lore);
            model = Objects.requireNonNull(model, "model");
        }
    }
}
