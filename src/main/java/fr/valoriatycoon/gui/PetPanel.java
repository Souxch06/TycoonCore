package fr.valoriatycoon.gui;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.pets.PetDefinition;
import fr.valoriatycoon.pets.PetKeyService;
import fr.valoriatycoon.pets.PetOperationResult;
import fr.valoriatycoon.pets.PetOperationStatus;
import fr.valoriatycoon.pets.PetProfile;
import fr.valoriatycoon.pets.PetRarity;
import fr.valoriatycoon.pets.PetRarityDefinition;
import fr.valoriatycoon.pets.PetService;
import fr.valoriatycoon.pets.PetSettings;
import fr.valoriatycoon.pets.PetTextFormatter;
import fr.valoriatycoon.ranks.RankSettings;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import fr.valoriatycoon.tycoon.TycoonService;
import fr.valoriatycoon.tycoon.TycoonStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Pet collection, purchase, activation and progression interface. */
public final class PetPanel implements Listener {
    private final PetService pets;
    private final TycoonService tycoons;
    private final RankSettings ranks;
    private final PetKeyService keys;
    private final ItemVisualService visuals;
    private final MessageService messages;
    private final Executor mainThread;
    private final Set<UUID> operations = ConcurrentHashMap.newKeySet();

    public PetPanel(
            PetService pets,
            TycoonService tycoons,
            RankSettings ranks,
            PetKeyService keys,
            ItemVisualService visuals,
            MessageService messages,
            Executor mainThread
    ) {
        this.pets = Objects.requireNonNull(pets, "pets");
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.ranks = Objects.requireNonNull(ranks, "ranks");
        this.keys = Objects.requireNonNull(keys, "keys");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    public void open(Player player) {
        if (!player.hasPermission("tycoon.pets")) {
            messages.send(player, "errors.no-permission");
            return;
        }
        var tycoon = tycoons.ownedBy(player.getUniqueId()).orElse(null);
        if (tycoon == null || tycoon.status() != TycoonStatus.ACTIVE) {
            messages.send(player, "tycoon.not-ready");
            return;
        }
        PetPanelHolder holder = new PetPanelHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                pets.settings().menu().size(),
                messages.render(pets.settings().menu().title())
        );
        holder.bind(inventory);
        ItemStack filler = item(pets.settings().menu().filler(), " ", List.of());
        visuals.apply(filler, "ui/filler");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
        for (PetDefinition definition : pets.settings().pets().values()) {
            PetProfile profile = pets.profile(player.getUniqueId(), definition.id());
            inventory.setItem(
                    definition.menuSlot(),
                    petItem(definition, profile, tycoon.prestige())
            );
        }
        inventory.setItem(pets.settings().crate().menuSlot(), crateItem(player));
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof PetPanelHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        if (event.getRawSlot() == pets.settings().crate().menuSlot()) {
            openCrate(player);
            return;
        }
        PetDefinition definition = pets.settings().pets().values().stream()
                .filter(value -> value.menuSlot() == event.getRawSlot())
                .findFirst()
                .orElse(null);
        if (definition == null) {
            return;
        }
        PetProfile owned = pets.profile(player.getUniqueId(), definition.id());
        if (owned == null) {
            messages.send(player, "pets.crate-only");
            return;
        }
        if (owned.active()) {
            messages.send(player, "pets.already-active");
            return;
        }
        if (!operations.add(player.getUniqueId())) {
            return;
        }
        pets.activate(player.getUniqueId(), definition.id())
                .whenCompleteAsync(
                        (result, error) -> complete(player, result, error, null),
                        mainThread
                );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PetPanelHolder) {
            event.setCancelled(true);
        }
    }

    private void openCrate(Player player) {
        if (!operations.add(player.getUniqueId())) {
            return;
        }
        UUID keyId = keys.firstKey(player).orElse(null);
        if (keyId == null) {
            operations.remove(player.getUniqueId());
            messages.send(player, "pets.no-key");
            return;
        }
        player.closeInventory();
        pets.openCrate(player.getUniqueId(), keyId)
                .whenCompleteAsync(
                        (result, error) -> complete(player, result, error, keyId),
                        mainThread
                );
    }

    private void complete(
            Player player,
            PetOperationResult result,
            Throwable error,
            UUID physicalKeyId
    ) {
        operations.remove(player.getUniqueId());
        if (error != null || result == null) {
            messages.send(player, "errors.storage");
            return;
        }
        if (physicalKeyId != null
                && (result.status() == PetOperationStatus.SUCCESS
                || result.status() == PetOperationStatus.KEY_ALREADY_USED)) {
            keys.removeConsumed(player, physicalKeyId);
        }
        if (result.status() == PetOperationStatus.SUCCESS) {
            if (physicalKeyId == null && result.pet() != null) {
                messages.send(
                        player,
                        "pets.activated",
                        Placeholder.component(
                                "pet",
                                messages.render(pets.settings().pet(result.pet().petId()).displayName())
                        )
                );
            }
            if (player.isOnline()) {
                open(player);
            }
        } else if (result.status() == PetOperationStatus.RANK_LOCKED) {
            messages.send(
                    player,
                    "pets.rank-locked",
                    Placeholder.unparsed("rank", ranks.name(result.requiredRank()))
            );
        } else if (result.status() == PetOperationStatus.KEY_ALREADY_USED) {
            messages.send(player, "pets.key-already-used");
        } else if (result.status() == PetOperationStatus.NO_ACTIVE_ISLAND) {
            messages.send(player, "tycoon.not-ready");
        } else {
            messages.send(player, "pets.unavailable");
        }
    }

    private ItemStack petItem(PetDefinition definition, PetProfile profile, int currentRank) {
        ItemStack item = new ItemStack(definition.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(
                profile != null && profile.chromatic()
                        ? "<aqua><bold>Chromatique</bold></aqua> " + definition.displayName()
                        : definition.displayName()
        ));
        List<Component> lore = new ArrayList<>();
        PetRarityDefinition rarity = pets.settings().rarity(definition.rarity());
        lore.add(messages.render("<gray>Rareté : " + rarity.displayName() + "</gray>"));
        definition.lore().forEach(line -> lore.add(messages.render(line)));
        if (profile == null) {
            lore.add(messages.render("<gray>Rang requis pour l’activer : <yellow>"
                    + ranks.name(definition.requiredRank()) + "</yellow></gray>"));
            addEffects(lore, definition, 1, rarity.maximumLevel());
            lore.add(messages.render("<light_purple>À débloquer dans la Caisse Pets.</light_purple>"));
        } else {
            lore.add(messages.render(profile.chromatic()
                    ? "<aqua>Variante : Chromatique (cosmétique)</aqua>"
                    : "<gray>Variante : Normale</gray>"));
            lore.add(messages.render("<gray>Niveau : <yellow>" + profile.level()
                    + "/" + rarity.maximumLevel() + "</yellow></gray>"));
            long required = pets.requiredExperience(profile);
            lore.add(messages.render(required <= 0L
                    ? "<gold>Niveau maximum atteint.</gold>"
                    : "<gray>XP : <aqua>" + profile.experience() + "/" + required + "</aqua></gray>"));
            addEffects(lore, definition, profile.level(), profile.level());
            String activation = profile.active()
                    ? "<green><bold>Pet actif</bold></green>"
                    : currentRank < definition.requiredRank()
                    ? "<red>Activation au rang " + ranks.name(definition.requiredRank()) + ".</red>"
                    : "<yellow>Cliquez pour l’activer.</yellow>";
            lore.add(messages.render(activation));
        }
        meta.lore(lore);
        visuals.apply(meta, "ui/pet/" + ItemVisualService.segment(definition.id()));
        if (profile != null && profile.chromatic()) {
            meta.setEnchantmentGlintOverride(true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack crateItem(Player player) {
        PetSettings.Crate crate = pets.settings().crate();
        List<String> lore = new ArrayList<>(crate.lore());
        lore.add("<gray>Collection : <yellow>" + pets.count(player.getUniqueId())
                + "/" + pets.settings().pets().size() + " pets</yellow></gray>");
        lore.add("<gray>Clés physiques dans l’inventaire : <yellow>" + keys.count(player) + "</yellow></gray>");
        lore.add("<gray>Chance chromatique : <aqua>"
                + PetTextFormatter.percentage(crate.chromaticChance()) + "%</aqua></gray>");
        lore.add("<dark_gray>Poids des raretés :</dark_gray>");
        for (PetRarity rarity : PetRarity.values()) {
            lore.add("<gray>" + pets.settings().rarity(rarity).displayName()
                    + " : <yellow>" + crate.rarityWeights().get(rarity) + "</yellow></gray>");
        }
        lore.add("<light_purple>Cliquez pour utiliser une clé.</light_purple>");
        ItemStack item = item(crate.icon(), crate.displayName(), lore);
        visuals.apply(item, "ui/pet/crate");
        return item;
    }

    private void addEffects(
            List<Component> lore,
            PetDefinition definition,
            int currentLevel,
            int comparisonLevel
    ) {
        lore.add(messages.render("<dark_gray>Bonus :</dark_gray>"));
        definition.effects().forEach((effect, boost) -> {
            String value = PetTextFormatter.percentage(boost.atLevel(currentLevel));
            String maximum = comparisonLevel == currentLevel
                    ? ""
                    : " <dark_gray>(max "
                    + PetTextFormatter.percentage(boost.atLevel(comparisonLevel))
                    + "%)</dark_gray>";
            lore.add(messages.render(
                    "<green>+" + value + "% "
                    + PetTextFormatter.effectName(effect) + "</green>" + maximum
            ));
        });
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(name));
        meta.lore(lore.stream().map(messages::render).toList());
        item.setItemMeta(meta);
        return item;
    }
}
