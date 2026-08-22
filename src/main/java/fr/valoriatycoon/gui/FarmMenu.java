package fr.valoriatycoon.gui;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.farm.FarmDefinition;
import fr.valoriatycoon.farm.FarmSettings;
import fr.valoriatycoon.farm.FarmTeleportAttempt;
import fr.valoriatycoon.farm.FarmWorld;
import fr.valoriatycoon.farm.FarmZoneDefinition;
import fr.valoriatycoon.farm.FarmWorldService;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Configurable public-farm destination GUI. Auto-sell controls are delegated to their controller. */
public final class FarmMenu implements Listener {
    private final FarmSettings settings;
    private final FarmWorldService worlds;
    private final ItemVisualService visuals;
    private final MessageService messages;
    private final Executor mainThread;

    public FarmMenu(
            FarmSettings settings,
            FarmWorldService worlds,
            ItemVisualService visuals,
            MessageService messages,
            Executor mainThread
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.worlds = Objects.requireNonNull(worlds, "worlds");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    public void open(Player player) {
        Map<Integer, String> destinations = new HashMap<>();
        for (FarmWorld farm : worlds.farms().values()) {
            destinations.put(farm.definition().menuSlot(), farm.definition().id());
        }
        FarmMenuHolder holder = new FarmMenuHolder(destinations);
        Inventory inventory = Bukkit.createInventory(
                holder,
                settings.menu().size(),
                messages.render(settings.menu().title())
        );
        holder.bind(inventory);
        visuals.fillMenu(inventory);

        for (FarmWorld farm : worlds.farms().values()) {
            FarmDefinition definition = farm.definition();
            inventory.setItem(definition.menuSlot(), destinationItem(definition));
        }
        player.openInventory(inventory);
    }

    /** Teleports to a farm's central island; fishing remains a direct common destination. */
    public void openFarm(Player player, String farmId) {
        if (!player.hasPermission("tycoon.farm")) {
            messages.send(player, "errors.no-permission");
            return;
        }
        FarmWorld farm = worlds.farm(farmId).orElse(null);
        if (farm == null) {
            messages.send(player, "farm.unavailable");
        } else if (farm.definition().zoned()) {
            teleport(player, farmId, 1);
        } else {
            teleport(player, farmId, null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof FarmMenuHolder)
                && !(event.getView().getTopInventory().getHolder() instanceof FarmZoneMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        if (!player.hasPermission("tycoon.farm")) {
            messages.send(player, "errors.no-permission");
            return;
        }

        if (event.getView().getTopInventory().getHolder() instanceof FarmMenuHolder holder) {
            String farmId = holder.destinations().get(event.getRawSlot());
            FarmWorld farm = farmId == null ? null : worlds.farm(farmId).orElse(null);
            if (farm == null) {
                return;
            }
            if (farm.definition().zoned()
                    && event.isShiftClick()
                    && player.hasPermission("tycoon.bypass")) {
                openZones(player, farm);
            } else if (farm.definition().zoned()) {
                teleport(player, farmId, 1);
            } else {
                teleport(player, farmId, null);
            }
            return;
        }

        FarmZoneMenuHolder holder = (FarmZoneMenuHolder) event.getView().getTopInventory().getHolder();
        Integer zoneIndex = holder.zonesBySlot().get(event.getRawSlot());
        FarmWorld farm = worlds.farm(holder.farmId()).orElse(null);
        FarmZoneDefinition zone = farm == null || zoneIndex == null
                ? null
                : farm.definition().zone(zoneIndex).orElse(null);
        if (zone == null) {
            return;
        }
        if (!player.hasPermission("tycoon.bypass")
                && !worlds.canAccess(player.getUniqueId(), zone)) {
            messages.send(
                    player,
                    "farm.zone-locked",
                    Placeholder.unparsed("rank", Integer.toString(zone.requiredRank()))
            );
            return;
        }
        teleport(player, holder.farmId(), zoneIndex);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof FarmMenuHolder)
                && !(event.getView().getTopInventory().getHolder() instanceof FarmZoneMenuHolder)) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
            event.setCancelled(true);
        }
    }

    private void openZones(Player player, FarmWorld farm) {
        Map<Integer, Integer> zonesBySlot = new HashMap<>();
        for (FarmZoneDefinition zone : farm.definition().zones()) {
            zonesBySlot.put(zone.menuSlot(), zone.index());
        }
        FarmZoneMenuHolder holder = new FarmZoneMenuHolder(farm.definition().id(), zonesBySlot);
        Inventory inventory = Bukkit.createInventory(
                holder,
                settings.zoneMenu().size(),
                messages.render(
                        settings.zoneMenu().title(),
                        Placeholder.unparsed("farm", farm.definition().id())
                )
        );
        holder.bind(inventory);
        visuals.fillMenu(inventory);
        int currentRank = worlds.currentRank(player.getUniqueId());
        for (FarmZoneDefinition zone : farm.definition().zones()) {
            inventory.setItem(zone.menuSlot(), zoneItem(zone, currentRank));
        }
        player.openInventory(inventory);
    }

    private ItemStack zoneItem(FarmZoneDefinition zone, int currentRank) {
        boolean unlocked = currentRank >= zone.requiredRank();
        ItemStack item = new ItemStack(
                unlocked ? zone.menuIcon() : settings.zoneMenu().lockedIcon()
        );
        ItemMeta meta = item.getItemMeta();
        var placeholders = new net.kyori.adventure.text.minimessage.tag.resolver.TagResolver[]{
                Placeholder.unparsed("zone", zone.id()),
                Placeholder.unparsed("required_rank", Integer.toString(zone.requiredRank())),
                Placeholder.unparsed("current_rank", Integer.toString(currentRank))
        };
        meta.displayName(messages.render(
                unlocked ? zone.menuName() : settings.zoneMenu().lockedName(),
                placeholders
        ));
        meta.lore((unlocked ? zone.menuLore() : settings.zoneMenu().lockedLore()).stream()
                .map(line -> messages.render(line, placeholders))
                .toList());
        visuals.apply(
                meta,
                unlocked
                        ? "ui/farm/zone/" + ItemVisualService.segment(zone.id())
                        : "ui/status/locked"
        );
        item.setItemMeta(meta);
        return item;
    }

    private void teleport(Player player, String farmId, Integer zoneIndex) {
        FarmTeleportAttempt attempt = zoneIndex == null
                ? worlds.teleport(player, farmId)
                : worlds.teleport(player, farmId, zoneIndex);
        if (!attempt.accepted()) {
            if (attempt.waitSeconds() > 0) {
                messages.send(
                        player,
                        "farm.teleport-cooldown",
                        Placeholder.unparsed("seconds", Long.toString(attempt.waitSeconds()))
                );
            } else {
                messages.send(player, "farm.unavailable");
            }
            return;
        }
        player.closeInventory();
        attempt.completion().whenCompleteAsync((success, error) -> {
            if (error != null || !Boolean.TRUE.equals(success)) {
                messages.send(player, "farm.teleport-failed");
                return;
            }
            messages.send(player, "farm.teleported", Placeholder.unparsed("farm", farmId));
        }, mainThread);
    }

    private ItemStack destinationItem(FarmDefinition definition) {
        ItemStack item = new ItemStack(definition.menuIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(definition.menuName()));
        meta.lore(definition.menuLore().stream().map(line -> messages.render(line)).toList());
        visuals.apply(meta, "ui/farm/" + ItemVisualService.segment(definition.id()));
        item.setItemMeta(meta);
        return item;
    }
}
