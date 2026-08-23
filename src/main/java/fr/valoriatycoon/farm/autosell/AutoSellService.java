package fr.valoriatycoon.farm.autosell;

import fr.valoriatycoon.economy.InternalEconomyService;
import fr.valoriatycoon.farm.FarmSettings;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Write-through online profile cache and atomic auto-sell upgrade coordinator. */
public final class AutoSellService {
    private static final AutoSellProfile LOCKED_PROFILE = new AutoSellProfile(false, 0);

    private final AutoSellRepository repository;
    private final InternalEconomyService economy;
    private final FarmSettings.AutoSell settings;
    private final ConcurrentHashMap<UUID, AutoSellProfile> profiles = new ConcurrentHashMap<>();

    public AutoSellService(
            AutoSellRepository repository,
            InternalEconomyService economy,
            FarmSettings.AutoSell settings
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public CompletableFuture<AutoSellProfile> activate(UUID playerId) {
        return repository.load(playerId).thenApply(profile -> {
            profiles.put(playerId, profile);
            return profile;
        });
    }

    public void deactivate(UUID playerId) {
        profiles.remove(playerId);
    }

    public AutoSellProfile profile(UUID playerId) {
        return profiles.getOrDefault(playerId, LOCKED_PROFILE);
    }

    public boolean isEnabled(UUID playerId) {
        AutoSellProfile profile = profile(playerId);
        return profile.unlocked() && profile.enabled();
    }

    public BigDecimal saleMultiplier(UUID playerId) {
        int level = profile(playerId).level();
        return settings.level(level)
                .map(FarmSettings.AutoSellLevel::saleMultiplier)
                .orElse(BigDecimal.ONE);
    }

    /** Applies the configured exact multiplier and rounds sub-cent results down. */
    public long applyMultiplier(UUID playerId, long baseValueCents) {
        return AutoSellValueCalculator.apply(baseValueCents, saleMultiplier(playerId));
    }

    public CompletableFuture<AutoSellProfile> toggle(UUID playerId) {
        return repository.toggle(playerId).thenApply(profile -> {
            profiles.put(playerId, profile);
            return profile;
        });
    }

    public CompletableFuture<AutoSellPurchaseResult> purchaseNext(UUID playerId) {
        int expectedCurrentLevel = profile(playerId).level();
        return repository.purchaseNext(playerId, expectedCurrentLevel, settings.levels()).thenApply(result -> {
            profiles.put(playerId, result.profile());
            if (result.successful()) {
                economy.synchronizeCommittedBalance(playerId, result.balanceCents());
            }
            return result;
        });
    }

    public int maximumLevel() {
        return settings.maximumLevel();
    }

    public FarmSettings.AutoSellLevel level(int level) {
        return settings.level(level).orElseThrow(() -> new IllegalArgumentException("Unknown auto-sell level " + level));
    }

    public int cachedPreferenceCount() {
        return profiles.size();
    }
}
