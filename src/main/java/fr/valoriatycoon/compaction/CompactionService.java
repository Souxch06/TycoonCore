package fr.valoriatycoon.compaction;

import fr.valoriatycoon.config.MessageService;
import fr.valoriatycoon.resourcepack.ItemVisualService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Creates authenticated compact resources, recipes and the spawn decompactor NPC. */
public final class CompactionService {
    private static final String RECIPE_PREFIX = "compact_";

    private final JavaPlugin plugin;
    private final CompactionSettings settings;
    private final ItemVisualService visuals;
    private final MessageService messages;
    private final Logger logger;
    private final NamespacedKey materialKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey npcKey;
    private final NamespacedKey legacyMaterialKey;
    private final NamespacedKey legacyLevelKey;
    private final NamespacedKey legacyNpcKey;
    private final Map<NamespacedKey, Material> recipes = new LinkedHashMap<>();
    private Villager npc;

    public CompactionService(
            JavaPlugin plugin,
            CompactionSettings settings,
            ItemVisualService visuals,
            MessageService messages,
            Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.visuals = Objects.requireNonNull(visuals, "visuals");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.materialKey = new NamespacedKey(plugin, "compacted_material");
        this.levelKey = new NamespacedKey(plugin, "compaction_level");
        this.npcKey = new NamespacedKey(plugin, "decompactor_npc");
        this.legacyMaterialKey = new NamespacedKey("tycooncore", "compacted_material");
        this.legacyLevelKey = new NamespacedKey("tycooncore", "compaction_level");
        this.legacyNpcKey = new NamespacedKey("tycooncore", "decompactor_npc");
    }

    /** Registers every recursive 3x3 recipe and spawns the configured decompactor. */
    public void start() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Compaction must start on the server thread");
        }
        registerRecipes();
        Bukkit.getOnlinePlayers().forEach(this::discoverRecipes);
        spawnNpc();
    }

    /** Removes runtime recipes and the built-in NPC during a clean shutdown. */
    public void stop() {
        for (NamespacedKey key : recipes.keySet()) {
            Bukkit.removeRecipe(key);
        }
        recipes.clear();
        if (npc != null && npc.isValid()) {
            npc.remove();
        }
        npc = null;
    }

    /** Returns the validated immutable compact-resource settings. */
    public CompactionSettings settings() {
        return settings;
    }

    /** Makes every registered compact recipe visible in a player's recipe book. */
    public void discoverRecipes(Player player) {
        recipes.keySet().forEach(player::discoverRecipe);
    }

    /** Warns that compact resources are storage/crafting objects, not normal consumables. */
    public void notifyCannotUse(Player player) {
        messages.send(player, "compaction.cannot-use");
    }

    /** Warns that recursive compact recipes are intentionally crafted one at a time. */
    public void notifyCraftOneAtTime(Player player) {
        messages.send(player, "compaction.craft-one-at-time");
    }

    /** Builds one PDC-authenticated compact item. */
    public ItemStack create(Material material, int level, int amount) {
        if (level < 1 || level > settings.maximumLevel()) {
            throw new IllegalArgumentException("Unsupported compaction level " + level);
        }
        if (amount < 1 || amount > material.getMaxStackSize()) {
            throw new IllegalArgumentException("Invalid compact item amount " + amount);
        }
        CompactionSettings.ResourceDefinition definition = settings.resource(material);
        ItemStack item = new ItemStack(definition.craftingMaterial(), amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.render(
                settings.levelName(level),
                Placeholder.unparsed("name", definition.displayName())
        ));
        meta.lore(settings.itemLore().stream()
                .map(line -> messages.render(
                        line,
                        Placeholder.unparsed("level", Integer.toString(level)),
                        Placeholder.unparsed(
                                "units",
                                Long.toString(new CompactedResource(material, level).baseUnits())
                        )
                ))
                .toList());
        visuals.apply(
                meta,
                "item/compact/" + ItemVisualService.segment(material.name()) + '/' + level
        );
        meta.getPersistentDataContainer().set(materialKey, PersistentDataType.STRING, material.name());
        meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);
        item.setItemMeta(meta);
        return item;
    }

    /** Returns zero for a normal item, -1 for malformed compact metadata, or the valid level. */
    public int level(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return -1;
        }
        ItemMeta meta = item.getItemMeta();
        String storedMaterial = meta.getPersistentDataContainer().get(
                materialKey,
                PersistentDataType.STRING
        );
        if (storedMaterial == null) {
            storedMaterial = meta.getPersistentDataContainer().get(
                    legacyMaterialKey,
                    PersistentDataType.STRING
            );
        }
        Integer storedLevel = meta.getPersistentDataContainer().get(
                levelKey,
                PersistentDataType.INTEGER
        );
        if (storedLevel == null) {
            storedLevel = meta.getPersistentDataContainer().get(
                    legacyLevelKey,
                    PersistentDataType.INTEGER
            );
        }
        if (storedMaterial == null && storedLevel == null) {
            return 0;
        }
        Material logicalMaterial = storedMaterial == null
                ? null
                : Material.matchMaterial(storedMaterial);
        CompactionSettings.ResourceDefinition definition = logicalMaterial == null
                ? null
                : settings.resources().get(logicalMaterial);
        if (storedLevel == null
                || definition == null
                || item.getType() != definition.craftingMaterial()
                || storedLevel < 1
                || storedLevel > settings.maximumLevel()) {
            return -1;
        }
        return storedLevel;
    }

    private Optional<Material> logicalMaterial(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        String stored = meta.getPersistentDataContainer().get(
                materialKey,
                PersistentDataType.STRING
        );
        if (stored == null) {
            stored = meta.getPersistentDataContainer().get(
                    legacyMaterialKey,
                    PersistentDataType.STRING
            );
        }
        Material material = stored == null ? null : Material.matchMaterial(stored);
        return material != null && settings.resources().containsKey(material)
                ? Optional.of(material)
                : Optional.empty();
    }

    /** Checks both the logical base material and authenticated PDC compaction level. */
    public boolean matches(ItemStack item, CompactedResource resource) {
        return item != null
                && logicalMaterial(item).orElse(null) == resource.material()
                && level(item) == resource.level();
    }

    /** Returns whether an item is an authenticated compact resource. */
    public boolean isCompacted(ItemStack item) {
        return level(item) > 0;
    }

    /** User-facing plain name used by requirement panels. */
    public String displayName(CompactedResource resource) {
        String qualifier = switch (resource.level()) {
            case 1 -> "compacte";
            case 2 -> "super compacte";
            case 3 -> "ultime";
            default -> throw new IllegalArgumentException(
                    "Unsupported compaction level " + resource.level()
            );
        };
        return "Ressource " + qualifier + " — "
                + settings.resource(resource.material()).displayName();
    }

    /** Resolves whether a crafting recipe belongs to this compaction service. */
    public Optional<Material> recipeMaterial(Recipe recipe) {
        if (!(recipe instanceof Keyed keyed)) {
            return Optional.empty();
        }
        return Optional.ofNullable(recipes.get(keyed.getKey()));
    }

    /** Validates all nine ingredients and returns the dynamically selected next level. */
    public ItemStack craftResult(ItemStack[] matrix, Material material) {
        if (matrix == null || matrix.length != 9 || !settings.resources().containsKey(material)) {
            return null;
        }
        CompactionSettings.ResourceDefinition definition = settings.resource(material);
        Integer inputLevel = null;
        for (ItemStack ingredient : matrix) {
            if (ingredient == null) {
                return null;
            }
            int level = level(ingredient);
            if (level < 0
                    || (level == 0 && ingredient.getType() != definition.craftingMaterial())
                    || (level > 0 && logicalMaterial(ingredient).orElse(null) != material)
                    || (inputLevel != null && inputLevel != level)) {
                return null;
            }
            inputLevel = level;
        }
        if (inputLevel == null || inputLevel >= settings.maximumLevel()) {
            return null;
        }
        return create(material, inputLevel + 1, 1);
    }

    /** Returns whether an entity is the plugin-owned decompactor NPC. */
    public boolean isNpc(Entity entity) {
        return entity != null
                && (entity.getPersistentDataContainer().has(npcKey, PersistentDataType.BYTE)
                || entity.getPersistentDataContainer().has(legacyNpcKey, PersistentDataType.BYTE));
    }

    /** Decompacts one held item into nine items of the previous level. */
    public void decompactHeldItem(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        int currentLevel = level(held);
        if (currentLevel <= 0) {
            messages.send(player, "compaction.hold-compacted-item");
            return;
        }
        Material material = logicalMaterial(held).orElseThrow();
        CompactionSettings.ResourceDefinition definition = settings.resource(material);
        held.setAmount(held.getAmount() - 1);
        if (held.getAmount() <= 0) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
        ItemStack output = currentLevel == 1
                ? new ItemStack(definition.craftingMaterial(), 9)
                : create(material, currentLevel - 1, 9);
        player.getInventory().addItem(output).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover)
        );
        messages.send(
                player,
                "compaction.decompacted",
                Placeholder.unparsed("resource", settings.resource(material).displayName()),
                Placeholder.unparsed(
                        "level",
                        currentLevel == 1 ? "normal" : Integer.toString(currentLevel - 1)
                )
        );
    }

    private void registerRecipes() {
        recipes.clear();
        for (Material material : settings.resources().keySet()) {
            NamespacedKey key = new NamespacedKey(
                    plugin,
                    RECIPE_PREFIX + material.name().toLowerCase(java.util.Locale.ROOT)
            );
            Bukkit.removeRecipe(key);
            CompactionSettings.ResourceDefinition definition = settings.resource(material);
            ShapedRecipe recipe = new ShapedRecipe(key, create(material, 1, 1));
            recipe.shape("AAA", "AAA", "AAA");
            recipe.setIngredient('A', definition.craftingMaterial());
            if (!Bukkit.addRecipe(recipe)) {
                logger.warning("Could not register compaction recipe " + key);
                continue;
            }
            recipes.put(key, material);
        }
    }

    private void spawnNpc() {
        if (!settings.npc().enabled()) {
            return;
        }
        World world = resolveNpcWorld();
        Location location = world.getSpawnLocation().clone().add(
                settings.npc().offsetX(),
                settings.npc().offsetY(),
                settings.npc().offsetZ()
        );
        location.setYaw(settings.npc().yaw());
        world.getNearbyEntities(location, 12.0, 12.0, 12.0).stream()
                .filter(this::isNpc)
                .forEach(Entity::remove);
        npc = world.spawn(location, Villager.class, villager -> {
            villager.getPersistentDataContainer().set(npcKey, PersistentDataType.BYTE, (byte) 1);
            villager.customName(messages.render(settings.npc().name()));
            villager.setCustomNameVisible(true);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            villager.setProfession(Villager.Profession.LIBRARIAN);
        });
    }

    private World resolveNpcWorld() {
        if (!settings.npc().worldName().isBlank()) {
            World configured = Bukkit.getWorld(settings.npc().worldName());
            if (configured != null) {
                return configured;
            }
            logger.warning(
                    "Decompactor world '" + settings.npc().worldName()
                            + "' is unavailable; using the first loaded world"
            );
        }
        List<World> worlds = new ArrayList<>(Bukkit.getWorlds());
        if (worlds.isEmpty()) {
            throw new IllegalStateException("No world is available for the decompactor NPC");
        }
        return worlds.getFirst();
    }

}
