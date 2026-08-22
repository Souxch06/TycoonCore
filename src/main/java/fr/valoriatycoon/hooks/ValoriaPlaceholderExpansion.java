package fr.valoriatycoon.hooks;

import fr.valoriatycoon.economy.InternalEconomyService;
import fr.valoriatycoon.economy.MoneyCodec;
import fr.valoriatycoon.machines.MachineService;
import fr.valoriatycoon.pets.PetKeyService;
import fr.valoriatycoon.pets.PetProfile;
import fr.valoriatycoon.pets.PetService;
import fr.valoriatycoon.ranks.RankSettings;
import fr.valoriatycoon.tycoon.Tycoon;
import fr.valoriatycoon.tycoon.TycoonService;
import fr.valoriatycoon.upgrades.PlotUpgradeType;
import java.util.Objects;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Non-blocking PlaceholderAPI expansion backed only by online caches. */
public final class ValoriaPlaceholderExpansion extends PlaceholderExpansion {
    private final JavaPlugin plugin;
    private final String identifier;
    private final InternalEconomyService economy;
    private final TycoonService tycoons;
    private final MachineService machines;
    private final PetService pets;
    private final PetKeyService petKeys;
    private final RankSettings ranks;

    /** Creates one cache-only expansion for the supplied current or legacy identifier. */
    public ValoriaPlaceholderExpansion(
            JavaPlugin plugin,
            String identifier,
            InternalEconomyService economy,
            TycoonService tycoons,
            MachineService machines,
            PetService pets,
            PetKeyService petKeys,
            RankSettings ranks
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.identifier = Objects.requireNonNull(identifier, "identifier");
        if (!identifier.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid PlaceholderAPI identifier: " + identifier);
        }
        this.economy = Objects.requireNonNull(economy, "economy");
        this.tycoons = Objects.requireNonNull(tycoons, "tycoons");
        this.machines = Objects.requireNonNull(machines, "machines");
        this.pets = Objects.requireNonNull(pets, "pets");
        this.petKeys = Objects.requireNonNull(petKeys, "petKeys");
        this.ranks = Objects.requireNonNull(ranks, "ranks");
    }

    @Override public @NotNull String getIdentifier() { return identifier; }
    @Override public @NotNull String getAuthor() { return "ValoriaTycoon"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "0";
        Tycoon tycoon = tycoons.ownedBy(player.getUniqueId()).orElse(null);
        PetProfile activePet = pets.activePet(player.getUniqueId());
        return switch (identifier.toLowerCase(java.util.Locale.ROOT)) {
            case "money" -> economy.cachedBalanceCents(player.getUniqueId()).isPresent()
                    ? MoneyCodec.fromCents(economy.cachedBalanceCents(player.getUniqueId()).getAsLong()).toPlainString()
                    : "0.00";
            case "level" -> tycoon == null ? "0" : Integer.toString(tycoon.level());
            case "rank" -> tycoon == null ? "0" : Integer.toString(tycoon.prestige());
            case "rank_name" -> tycoon == null ? ranks.name(0) : ranks.name(tycoon.prestige());
            case "production" -> tycoon == null ? "0" : Long.toString(tycoon.totalProduction());
            case "pets" -> Integer.toString(pets.count(player.getUniqueId()));
            case "pet_keys" -> Integer.toString(petKeys.count(player));
            case "pet_active" -> activePet == null ? "Aucun" : activePet.petId();
            case "pet_level" -> activePet == null ? "0" : Integer.toString(activePet.level());
            case "pet_variant" -> activePet == null
                    ? "Aucune"
                    : activePet.chromatic() ? "Chromatique" : "Normale";
            case "pet_rarity" -> activePet == null
                    ? "Aucune"
                    : pets.settings().pet(activePet.petId()).rarity().name();
            case "playtime" -> tycoon == null ? "0" : Long.toString(tycoon.playtimeSeconds());
            case "island_size" -> tycoon == null ? "0"
                    : Integer.toString(tycoons.upgradeValue(tycoon, PlotUpgradeType.PLOT_SIZE));
            case "hopper_limit" -> tycoon == null ? "0"
                    : Integer.toString(tycoons.upgradeValue(tycoon, PlotUpgradeType.HOPPER_LIMIT));
            case "machines" -> tycoon == null ? "0" : Integer.toString(machines.count(tycoon.id()));
            default -> null;
        };
    }
}
