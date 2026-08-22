package fr.valoriatycoon.tycoon;

import fr.valoriatycoon.economy.InternalEconomyService;
import fr.valoriatycoon.upgrades.PlotUpgradeDefinition;
import fr.valoriatycoon.upgrades.PlotUpgradeResult;
import fr.valoriatycoon.upgrades.PlotUpgradeSettings;
import fr.valoriatycoon.upgrades.PlotUpgradeStatus;
import fr.valoriatycoon.upgrades.PlotUpgradeType;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** In-memory read model backed by authoritative asynchronous repository mutations. */
public final class TycoonService {
    private final TycoonSettings settings;
    private final PlotUpgradeSettings upgrades;
    private final TycoonRepository repository;
    private final InternalEconomyService economy;
    private final Executor mainThread;
    private final Map<UUID, Tycoon> byId = new HashMap<>();
    private final Map<UUID, Tycoon> byOwner = new HashMap<>();
    private final Map<UUID, Set<UUID>> members = new HashMap<>();
    private final Map<UUID, Set<HopperPosition>> hoppers = new HashMap<>();
    private final Map<HopperPosition, UUID> hopperOwners = new HashMap<>();
    private final PlotSpatialIndex spatialIndex = new PlotSpatialIndex();

    public TycoonService(
            TycoonSettings settings,
            PlotUpgradeSettings upgrades,
            TycoonRepository repository,
            InternalEconomyService economy,
            Executor mainThread
    ) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.upgrades = Objects.requireNonNull(upgrades, "upgrades");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.mainThread = Objects.requireNonNull(mainThread, "mainThread");
    }

    public void initialize(TycoonDataSnapshot snapshot) {
        byId.clear();
        byOwner.clear();
        members.clear();
        spatialIndex.clear();
        hoppers.clear();
        hopperOwners.clear();
        for (Tycoon tycoon : snapshot.tycoons()) {
            addToCache(tycoon);
        }
        snapshot.membersByTycoon().forEach((id, playerIds) -> members.put(id, new HashSet<>(playerIds)));
        snapshot.hoppersByTycoon().forEach((id, positions) -> {
            Set<HopperPosition> copy = new HashSet<>(positions);
            hoppers.put(id, copy);
            copy.forEach(position -> hopperOwners.put(position, id));
        });
    }

    public CompletableFuture<TycoonAllocationResult> reserve(UUID ownerId, String groupId) {
        TycoonPlotGroup group = settings.group(groupId);
        return repository.allocate(ownerId, group).thenApplyAsync(result -> {
            if (result.successful()) {
                addToCache(result.tycoon());
            }
            return result;
        }, mainThread);
    }

    public CompletableFuture<Tycoon> markActive(UUID tycoonId) {
        return repository.updateStatus(tycoonId, TycoonStatus.ACTIVE).thenApplyAsync(updated -> {
            replaceCache(updated);
            return updated;
        }, mainThread);
    }

    public CompletableFuture<Optional<Tycoon>> beginPreparation(UUID ownerId) {
        return repository.beginPreparation(ownerId).thenApplyAsync(optional -> {
            optional.ifPresent(this::replaceCache);
            return optional;
        }, mainThread);
    }

    public CompletableFuture<Optional<Tycoon>> beginDeletion(UUID ownerId) {
        return repository.beginDeletion(ownerId).thenApplyAsync(optional -> {
            optional.ifPresent(this::replaceCache);
            return optional;
        }, mainThread);
    }

    public CompletableFuture<Void> finalizeDeletion(UUID tycoonId) {
        return repository.delete(tycoonId).thenRunAsync(() -> removeFromCache(tycoonId), mainThread);
    }

    public void applyRank(UUID ownerId, int rankLevel) {
        Tycoon current = byOwner.get(ownerId);
        if (current != null) replaceCache(current.withRank(rankLevel));
    }

    public CompletableFuture<Tycoon> addPlaytime(UUID ownerId, long seconds) {
        return repository.addPlaytime(ownerId, seconds).thenApplyAsync(updated -> {
            replaceCache(updated);
            return updated;
        }, mainThread);
    }

    public CompletableFuture<MemberOperationStatus> addMember(UUID ownerId, UUID memberId) {
        Tycoon ownerTycoon = byOwner.get(ownerId);
        if (ownerTycoon == null) {
            return CompletableFuture.completedFuture(MemberOperationStatus.NOT_FOUND);
        }
        int maximum = upgradeValue(ownerTycoon, PlotUpgradeType.MEMBER_LIMIT);
        return repository.addMember(ownerId, memberId, maximum).thenApplyAsync(status -> {
            if (status == MemberOperationStatus.SUCCESS) {
                members.computeIfAbsent(ownerTycoon.id(), ignored -> new HashSet<>()).add(memberId);
            }
            return status;
        }, mainThread);
    }

    public CompletableFuture<MemberOperationStatus> removeMember(UUID ownerId, UUID memberId) {
        Tycoon ownerTycoon = byOwner.get(ownerId);
        if (ownerTycoon == null) {
            return CompletableFuture.completedFuture(MemberOperationStatus.NOT_FOUND);
        }
        return repository.removeMember(ownerId, memberId).thenApplyAsync(status -> {
            if (status == MemberOperationStatus.SUCCESS) {
                Set<UUID> currentMembers = members.get(ownerTycoon.id());
                if (currentMembers != null) {
                    currentMembers.remove(memberId);
                }
            }
            return status;
        }, mainThread);
    }

    public CompletableFuture<PlotUpgradeResult> purchaseUpgrade(UUID ownerId, PlotUpgradeType type) {
        Tycoon current = byOwner.get(ownerId);
        if (current == null) {
            return CompletableFuture.completedFuture(new PlotUpgradeResult(
                    PlotUpgradeStatus.NO_ACTIVE_TYCOON,
                    type,
                    0,
                    0L,
                    0L
            ));
        }
        int expected = upgradeLevel(current, type);
        PlotUpgradeDefinition definition = upgrades.definition(type);
        return repository.purchaseUpgrade(ownerId, type, expected, definition).thenApplyAsync(result -> {
            if (result.successful()) {
                Tycoon updated = switch (type) {
                    case PLOT_SIZE -> current.withUpgradeLevels(
                            result.resultingLevel(), current.hopperLimitLevel(), current.memberLimitLevel()
                    );
                    case HOPPER_LIMIT -> current.withUpgradeLevels(
                            current.plotSizeLevel(), result.resultingLevel(), current.memberLimitLevel()
                    );
                    case MEMBER_LIMIT -> current.withUpgradeLevels(
                            current.plotSizeLevel(), current.hopperLimitLevel(), result.resultingLevel()
                    );
                };
                replaceCache(updated);
                economy.synchronizeCommittedBalance(ownerId, result.balanceCents());
            } else if (result.status() == PlotUpgradeStatus.PROFILE_STALE) {
                repository.loadAll().thenAcceptAsync(this::initialize, mainThread);
            }
            return result;
        }, mainThread);
    }

    public TycoonPlotGroup.Bounds buildBounds(Tycoon tycoon) {
        int size = upgradeValue(tycoon, PlotUpgradeType.PLOT_SIZE);
        int centerX = tycoon.bounds().centerX();
        int centerZ = tycoon.bounds().centerZ();
        int minimumX = centerX - (size - 1) / 2;
        int minimumZ = centerZ - (size - 1) / 2;
        return new TycoonPlotGroup.Bounds(
                minimumX,
                minimumX + size - 1,
                minimumZ,
                minimumZ + size - 1
        );
    }

    public boolean isInsideBuildArea(Tycoon tycoon, int x, int z) {
        return buildBounds(tycoon).contains(x, z);
    }

    public int upgradeLevel(Tycoon tycoon, PlotUpgradeType type) {
        return switch (type) {
            case PLOT_SIZE -> tycoon.plotSizeLevel();
            case HOPPER_LIMIT -> tycoon.hopperLimitLevel();
            case MEMBER_LIMIT -> tycoon.memberLimitLevel();
        };
    }

    public int upgradeValue(Tycoon tycoon, PlotUpgradeType type) {
        PlotUpgradeDefinition definition = upgrades.definition(type);
        return definition.level(upgradeLevel(tycoon, type)).orElseThrow().value();
    }

    public int hopperCount(UUID tycoonId) {
        return hoppers.getOrDefault(tycoonId, Set.of()).size();
    }

    public HopperReservation reserveHopper(Tycoon tycoon, HopperPosition position) {
        Set<HopperPosition> positions = hoppers.computeIfAbsent(tycoon.id(), ignored -> new HashSet<>());
        if (positions.size() >= upgradeValue(tycoon, PlotUpgradeType.HOPPER_LIMIT)
                || hopperOwners.containsKey(position)) {
            return HopperReservation.rejected();
        }
        positions.add(position);
        hopperOwners.put(position, tycoon.id());
        CompletableFuture<Void> persistence = repository.addHopper(tycoon.id(), position)
                .whenCompleteAsync((ignored, error) -> {
                    if (error != null) {
                        positions.remove(position);
                        hopperOwners.remove(position, tycoon.id());
                    }
                }, mainThread);
        return new HopperReservation(true, persistence);
    }

    public CompletableFuture<Void> clearHoppers(UUID tycoonId) {
        Set<HopperPosition> removed = hoppers.remove(tycoonId);
        if (removed != null) {
            removed.forEach(hopperOwners::remove);
        }
        return repository.clearHoppers(tycoonId);
    }

    public void releaseHopper(HopperPosition position) {
        UUID tycoonId = hopperOwners.remove(position);
        if (tycoonId == null) {
            return;
        }
        Set<HopperPosition> positions = hoppers.get(tycoonId);
        if (positions != null) {
            positions.remove(position);
        }
        repository.removeHopper(position);
    }

    public Optional<Tycoon> ownedBy(UUID ownerId) {
        return Optional.ofNullable(byOwner.get(ownerId));
    }

    public Optional<Tycoon> byId(UUID tycoonId) {
        return Optional.ofNullable(byId.get(tycoonId));
    }

    public Optional<Tycoon> at(String worldName, int x, int z) {
        return spatialIndex.find(worldName, x, z);
    }

    public Set<UUID> members(UUID tycoonId) {
        return Collections.unmodifiableSet(members.getOrDefault(tycoonId, Set.of()));
    }

    public boolean canBuild(UUID playerId, Tycoon tycoon) {
        return tycoon.status() == TycoonStatus.ACTIVE
                && (tycoon.ownerId().equals(playerId)
                || members.getOrDefault(tycoon.id(), Set.of()).contains(playerId));
    }

    public List<Tycoon> all() {
        return List.copyOf(byId.values());
    }

    private void addToCache(Tycoon tycoon) {
        byId.put(tycoon.id(), tycoon);
        byOwner.put(tycoon.ownerId(), tycoon);
        spatialIndex.add(tycoon);
    }

    private void replaceCache(Tycoon updated) {
        Tycoon previous = byId.put(updated.id(), updated);
        byOwner.put(updated.ownerId(), updated);
        if (previous == null) {
            spatialIndex.add(updated);
        } else {
            spatialIndex.replace(previous, updated);
        }
    }

    private void removeFromCache(UUID tycoonId) {
        Tycoon removed = byId.remove(tycoonId);
        if (removed != null) {
            byOwner.remove(removed.ownerId(), removed);
            spatialIndex.remove(removed);
            members.remove(tycoonId);
            Set<HopperPosition> removedHoppers = hoppers.remove(tycoonId);
            if (removedHoppers != null) {
                removedHoppers.forEach(hopperOwners::remove);
            }
        }
    }
}
