package fr.valoriatycoon.pets;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.gui.PetReclaimHolder;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerLeashEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Spawn NPC that securely turns owned pets back into eggs for money and vanilla XP levels. */
public final class PetReclaimService implements Listener {
    private final JavaPlugin plugin;
    private final PetService pets;
    private final PetEggService eggItems;
    private final ItemVisualService visuals;
    private final PetSettings.Reclaim settings;
    private final CurrencyFormatter currency;
    private final MessageService messages;
    private final Executor mainThread;
    private final NamespacedKey npcKey;
    private final Set<UUID> operations = ConcurrentHashMap.newKeySet();
    private Villager npc;

    public PetReclaimService(
            JavaPlugin plugin,
            PetService pets,
            PetEggService eggItems,
            ItemVisualService visuals,
            PetSettings settings,
            CurrencyFormatter currency,
            MessageService messages,
            Executor mainThread
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.pets = Objects.requireNonNull(pets, "pets");
        this.eggItems = Objects.requireNonNull(eggItems, "eggItems");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.settings = Objects.requireNonNull(settings, "settings").reclaim();
        this.currency = Objects.requireNonNull(currency, "currency");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
        this.npcKey = new NamespacedKey(plugin, "pet_reclaim_npc");
    }

    /** Spawns exactly one reclaim NPC near the generated Valoria spawn. */
    public void start() {
        if (!settings.enabled()) {
            return;
        }
        World world = Bukkit.getWorld(settings.worldName());
        if (world == null) {
            throw new IllegalStateException("Pet reclaim world is unavailable: " + settings.worldName());
        }
        Location location = world.getSpawnLocation().clone().add(
                settings.offsetX(),
                settings.offsetY(),
                settings.offsetZ()
        );
        location.setYaw(settings.yaw());
        world.getNearbyEntities(location, 12.0, 12.0, 12.0).stream()
                .filter(this::isNpc)
                .forEach(Entity::remove);
        npc = world.spawn(location, Villager.class, villager -> {
            villager.getPersistentDataContainer().set(npcKey, PersistentDataType.BYTE, (byte) 1);
            villager.customName(messages.render(settings.npcName()));
            villager.setCustomNameVisible(true);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            villager.setProfession(Villager.Profession.CLERIC);
        });
    }

    public void stop() {
        if (npc != null && npc.isValid()) {
            npc.remove();
        }
        npc = null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isNpc(event.getRightClicked())) {
            return;
        }
        event.setCancelled(true);
        if (!event.getPlayer().hasPermission("tycoon.pets")) {
            messages.send(event.getPlayer(), "errors.no-permission");
            return;
        }
        open(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (isNpc(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLeash(PlayerLeashEntityEvent event) {
        if (isNpc(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof PetReclaimHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        PetDefinition definition = pets.settings().pets().values().stream()
                .filter(value -> value.menuSlot() == event.getRawSlot())
                .findFirst()
                .orElse(null);
        PetProfile profile = definition == null
                ? null
                : pets.profile(player.getUniqueId(), definition.id());
        if (profile == null) {
            return;
        }
        if (!event.isShiftClick()) {
            messages.send(player, "pets.reclaim-confirm");
            return;
        }
        if (player.getLevel() < settings.vanillaExperienceLevels()) {
            messages.send(player, "pets.reclaim-xp-missing");
            return;
        }
        if (!operations.add(player.getUniqueId())) {
            return;
        }
        int reservedLevels = settings.vanillaExperienceLevels();
        player.giveExpLevels(-reservedLevels);
        pets.reclaim(player.getUniqueId(), definition.id())
                .whenCompleteAsync((result, error) -> {
                    operations.remove(player.getUniqueId());
                    if (error != null || result == null) {
                        player.giveExpLevels(reservedLevels);
                        messages.send(player, "errors.storage");
                        return;
                    }
                    if (!result.successful()) {
                        player.giveExpLevels(reservedLevels);
                        if (result.status() == PetOperationStatus.INSUFFICIENT_FUNDS) {
                            messages.send(player, "pets.reclaim-money-missing");
                        } else {
                            messages.send(player, "pets.unavailable");
                        }
                        return;
                    }
                    if (player.isOnline()) {
                        open(player);
                    }
                }, mainThread);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PetReclaimHolder) {
            event.setCancelled(true);
        }
    }

    private void open(Player player) {
        PetReclaimHolder holder = new PetReclaimHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                pets.settings().menu().size(),
                messages.render(settings.menuTitle())
        );
        holder.bind(inventory);
        visuals.fillMenu(inventory);
        for (PetProfile profile : pets.pets(player.getUniqueId())) {
            PetDefinition definition = pets.settings().pet(profile.petId());
            inventory.setItem(definition.menuSlot(), petItem(definition, profile));
        }
        player.openInventory(inventory);
    }

    private ItemStack petItem(PetDefinition definition, PetProfile profile) {
        ItemStack item = eggItems.createVisual(profile.chromatic());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(
                profile.chromatic()
                        ? "<aqua><bold>Chromatique</bold></aqua> " + definition.displayName()
                        : definition.displayName()
        ));
        List<Component> lore = new ArrayList<>();
        lore.add(messages.render("<gray>Niveau conservé : <yellow>" + profile.level() + "</yellow></gray>"));
        lore.add(messages.render("<gray>Coût : <gold>" + currency.format(settings.moneyCostCents())
                + "</gold> + <green>" + settings.vanillaExperienceLevels()
                + " niveaux XP</green></gray>"));
        lore.add(messages.render("<red>La variante sera conservée, sans nouveau tirage.</red>"));
        lore.add(messages.render("<yellow>Shift + clic pour remettre en œuf.</yellow>"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isNpc(Entity entity) {
        return entity != null
                && entity.getPersistentDataContainer().has(npcKey, PersistentDataType.BYTE);
    }
}
