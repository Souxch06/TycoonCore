package fr.valoriatycoon.pets;

import fr.valoriatycoon.database.SqliteDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/** Authoritative asynchronous persistence for pet crates, ownership, activation and XP. */
public final class PetRepository {
    private final SqliteDatabase database;
    private final PetSettings settings;

    public PetRepository(SqliteDatabase database, PetSettings settings) {
        this.database = Objects.requireNonNull(database, "database");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public CompletableFuture<PetSnapshot> loadAll() {
        return database.submit(connection -> {
            List<PetProfile> profiles = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM player_pets");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    profiles.add(map(result));
                }
            }
            List<PetEgg> pendingEggs = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT * FROM issued_pet_eggs
                    WHERE delivered = 0 AND consumed_by_uuid IS NULL
                    """);
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    pendingEggs.add(mapEgg(result));
                }
            }
            return new PetSnapshot(profiles, pendingEggs);
        });
    }

    /** Atomically consumes one globally unique physical key and issues a fixed-variant pet egg. */
    public CompletableFuture<PetOperationResult> openCrate(UUID playerId, UUID keyId) {
        Objects.requireNonNull(keyId, "keyId");
        return database.submit(connection -> inTransaction(connection, () -> {
            IslandRank island = islandRank(connection, playerId);
            if (island == null || !island.active()) {
                return result(PetOperationStatus.NO_ACTIVE_ISLAND, null, null, -1L, 0);
            }
            if (consumed(connection, keyId)) {
                return result(PetOperationStatus.KEY_ALREADY_USED, null, null, -1L, 0);
            }
            PetDefinition reward = drawReward();
            long now = System.currentTimeMillis();
            boolean chromatic = ThreadLocalRandom.current().nextDouble()
                    < settings.crate().chromaticChance().doubleValue();
            PetEgg egg = new PetEgg(
                    UUID.randomUUID(),
                    reward.id(),
                    chromatic,
                    1,
                    0L,
                    playerId,
                    Instant.ofEpochMilli(now)
            );
            consumeKey(connection, playerId, keyId, reward.id(), now);
            issueEgg(connection, egg, "PET_CRATE", now);
            auditPet(connection, playerId, reward.id(), "CRATE_EGG", now);
            return result(PetOperationStatus.SUCCESS, null, egg, -1L, 0);
        }));
    }

    /** Marks a physically delivered egg so crash recovery will not redeliver it. */
    public CompletableFuture<Void> markEggDelivered(UUID eggId) {
        return database.submit(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE issued_pet_eggs
                    SET delivered = 1, delivered_at = ?
                    WHERE egg_id = ? AND delivered = 0
                    """)) {
                statement.setLong(1, System.currentTimeMillis());
                statement.setString(2, eggId.toString());
                statement.executeUpdate();
            }
            return null;
        });
    }

    /** Redeems one issued egg without changing its persisted normal/chromatic variant. */
    public CompletableFuture<PetOperationResult> redeemEgg(
            UUID playerId,
            UUID eggId,
            String petId,
            boolean chromatic
    ) {
        return database.submit(connection -> inTransaction(connection, () -> {
            IssuedEgg issued = selectEgg(connection, eggId);
            if (issued == null
                    || !issued.petId().equals(petId)
                    || issued.chromatic() != chromatic) {
                return result(PetOperationStatus.INVALID_EGG, null, null, -1L, 0);
            }
            if (issued.consumed()) {
                return result(PetOperationStatus.EGG_ALREADY_USED, null, null, -1L, 0);
            }
            PetProfile existing = select(connection, playerId, petId);
            if (existing != null) {
                return result(PetOperationStatus.ALREADY_OWNED, existing, null, -1L, 0);
            }
            PetDefinition definition = settings.pet(petId);
            IslandRank island = islandRank(connection, playerId);
            if (island == null || !island.active()) {
                return result(
                        PetOperationStatus.NO_ACTIVE_ISLAND,
                        null,
                        null,
                        -1L,
                        definition.requiredRank()
                );
            }
            if (island.rank() < definition.requiredRank()) {
                return result(
                        PetOperationStatus.RANK_LOCKED,
                        null,
                        null,
                        -1L,
                        definition.requiredRank()
                );
            }
            long now = System.currentTimeMillis();
            try (PreparedStatement clear = connection.prepareStatement(
                    "UPDATE player_pets SET active = 0, updated_at = ? WHERE player_uuid = ?")) {
                clear.setLong(1, now);
                clear.setString(2, playerId.toString());
                clear.executeUpdate();
            }
            PetProfile profile = new PetProfile(
                    playerId,
                    petId,
                    issued.level(),
                    issued.experience(),
                    chromatic,
                    true,
                    Instant.ofEpochMilli(now)
            );
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO player_pets
                        (player_uuid, pet_id, pet_level, pet_experience,
                         active, obtained_at, updated_at, chromatic)
                    VALUES (?, ?, ?, ?, 1, ?, ?, ?)
                    """)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, petId);
                statement.setInt(3, issued.level());
                statement.setLong(4, issued.experience());
                statement.setLong(5, now);
                statement.setLong(6, now);
                statement.setInt(7, chromatic ? 1 : 0);
                requireOne(statement);
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE issued_pet_eggs
                    SET delivered = 1,
                        delivered_at = COALESCE(delivered_at, ?),
                        consumed_by_uuid = ?,
                        consumed_at = ?
                    WHERE egg_id = ? AND consumed_by_uuid IS NULL
                    """)) {
                statement.setLong(1, now);
                statement.setString(2, playerId.toString());
                statement.setLong(3, now);
                statement.setString(4, eggId.toString());
                requireOne(statement);
            }
            auditPet(connection, playerId, petId, "EGG_REDEEM", now);
            return result(PetOperationStatus.SUCCESS, profile, null, -1L, definition.requiredRank());
        }));
    }

    /** Removes an owned pet and issues an egg preserving its immutable variant. */
    public CompletableFuture<PetOperationResult> reclaim(
            UUID playerId,
            String petId,
            long moneyCostCents
    ) {
        return database.submit(connection -> inTransaction(connection, () -> {
            PetProfile profile = select(connection, playerId, petId);
            if (profile == null) {
                return result(PetOperationStatus.NOT_OWNED, null, null, -1L, 0);
            }
            long balance = balance(connection, playerId);
            if (balance < moneyCostCents) {
                return result(PetOperationStatus.INSUFFICIENT_FUNDS, profile, null, balance, 0);
            }
            long now = System.currentTimeMillis();
            PetEgg egg = new PetEgg(
                    UUID.randomUUID(),
                    petId,
                    profile.chromatic(),
                    profile.level(),
                    profile.experience(),
                    playerId,
                    Instant.ofEpochMilli(now)
            );
            try (PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM player_pets
                    WHERE player_uuid = ? AND pet_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, petId);
                requireOne(statement);
            }
            issueEgg(connection, egg, "RECLAIM", now);
            long remaining = balance - moneyCostCents;
            updateBalance(connection, playerId, remaining, now);
            auditEconomy(connection, playerId, petId, moneyCostCents, now);
            auditPet(connection, playerId, petId, "RECLAIM", now);
            return result(PetOperationStatus.SUCCESS, null, egg, remaining, 0);
        }));
    }

    public CompletableFuture<PetOperationResult> activate(UUID playerId, String petId) {
        return database.submit(connection -> inTransaction(connection, () -> {
            PetProfile profile = select(connection, playerId, petId);
            if (profile == null) {
                return result(PetOperationStatus.NOT_OWNED, null, null, -1L, 0);
            }
            PetDefinition definition = settings.pet(petId);
            IslandRank island = islandRank(connection, playerId);
            if (island == null || !island.active()) {
                return result(
                        PetOperationStatus.NO_ACTIVE_ISLAND,
                        profile,
                        null,
                        -1L,
                        definition.requiredRank()
                );
            }
            if (island.rank() < definition.requiredRank()) {
                return result(
                        PetOperationStatus.RANK_LOCKED,
                        profile,
                        null,
                        -1L,
                        definition.requiredRank()
                );
            }
            long now = System.currentTimeMillis();
            try (PreparedStatement clear = connection.prepareStatement(
                    "UPDATE player_pets SET active = 0, updated_at = ? WHERE player_uuid = ?")) {
                clear.setLong(1, now);
                clear.setString(2, playerId.toString());
                clear.executeUpdate();
            }
            try (PreparedStatement activate = connection.prepareStatement("""
                    UPDATE player_pets
                    SET active = 1, updated_at = ?
                    WHERE player_uuid = ? AND pet_id = ?
                    """)) {
                activate.setLong(1, now);
                activate.setString(2, playerId.toString());
                activate.setString(3, petId);
                requireOne(activate);
            }
            auditPet(connection, playerId, petId, "ACTIVATE", now);
            return result(
                    PetOperationStatus.SUCCESS,
                    profile.withActive(true),
                    null,
                    -1L,
                    definition.requiredRank()
            );
        }));
    }

    public CompletableFuture<PetProfile> addExperience(UUID playerId, long experience) {
        if (experience <= 0L) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Pet experience must be positive")
            );
        }
        return database.submit(connection -> inTransaction(connection, () -> {
            PetProfile profile = selectActive(connection, playerId);
            if (profile == null) {
                return null;
            }
            PetDefinition definition = settings.pets().get(profile.petId());
            if (definition == null) {
                return profile;
            }
            PetRarityDefinition rarity = settings.rarity(definition.rarity());
            PetExperienceCalculator.Progress progress = PetExperienceCalculator.add(
                    profile.level(),
                    profile.experience(),
                    experience,
                    rarity
            );
            long now = System.currentTimeMillis();
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE player_pets
                    SET pet_level = ?, pet_experience = ?, updated_at = ?
                    WHERE player_uuid = ? AND pet_id = ? AND active = 1
                    """)) {
                statement.setInt(1, progress.level());
                statement.setLong(2, progress.experience());
                statement.setLong(3, now);
                statement.setString(4, playerId.toString());
                statement.setString(5, profile.petId());
                requireOne(statement);
            }
            return profile.withProgress(progress.level(), progress.experience());
        }));
    }

    private PetDefinition drawReward() throws SQLException {
        Map<PetRarity, List<PetDefinition>> candidates = new EnumMap<>(PetRarity.class);
        for (PetDefinition definition : settings.pets().values()) {
            candidates.computeIfAbsent(definition.rarity(), ignored -> new ArrayList<>())
                    .add(definition);
        }
        long totalWeight = settings.crate().rarityWeights().values().stream()
                .mapToLong(Integer::longValue)
                .sum();
        long roll = ThreadLocalRandom.current().nextLong(totalWeight);
        PetRarity selectedRarity = null;
        for (PetRarity rarity : PetRarity.values()) {
            int weight = settings.crate().rarityWeights().get(rarity);
            if (roll < weight) {
                selectedRarity = rarity;
                break;
            }
            roll -= weight;
        }
        if (selectedRarity == null || candidates.getOrDefault(selectedRarity, List.of()).isEmpty()) {
            throw new SQLException("Pet crate weighted draw did not select a configured pet");
        }
        List<PetDefinition> rarityPets = candidates.get(selectedRarity);
        return rarityPets.get(ThreadLocalRandom.current().nextInt(rarityPets.size()));
    }

    private PetProfile select(Connection connection, UUID playerId, String petId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM player_pets
                WHERE player_uuid = ? AND pet_id = ?
                """)) {
            statement.setString(1, playerId.toString());
            statement.setString(2, petId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    private PetProfile selectActive(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM player_pets
                WHERE player_uuid = ? AND active = 1
                """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? map(result) : null;
            }
        }
    }

    private void issueEgg(Connection connection, PetEgg egg, String source, long now)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO issued_pet_eggs
                    (egg_id, pet_id, chromatic, pet_level, pet_experience,
                     issued_to_uuid, source, delivered, issued_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?)
                """)) {
            statement.setString(1, egg.eggId().toString());
            statement.setString(2, egg.petId());
            statement.setInt(3, egg.chromatic() ? 1 : 0);
            statement.setInt(4, egg.level());
            statement.setLong(5, egg.experience());
            statement.setString(6, egg.recipientId().toString());
            statement.setString(7, source);
            statement.setLong(8, now);
            requireOne(statement);
        }
    }

    private IssuedEgg selectEgg(Connection connection, UUID eggId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT pet_id, chromatic, pet_level, pet_experience, consumed_by_uuid
                FROM issued_pet_eggs
                WHERE egg_id = ?
                """)) {
            statement.setString(1, eggId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? new IssuedEgg(
                                result.getString(1),
                                result.getInt(2) == 1,
                                result.getInt(3),
                                result.getLong(4),
                                result.getString(5) != null
                        )
                        : null;
            }
        }
    }

    private boolean consumed(Connection connection, UUID keyId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM consumed_pet_keys WHERE key_id = ?")) {
            statement.setString(1, keyId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private void consumeKey(
            Connection connection,
            UUID playerId,
            UUID keyId,
            String petId,
            long now
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO consumed_pet_keys (key_id, player_uuid, pet_id, consumed_at)
                VALUES (?, ?, ?, ?)
                """)) {
            statement.setString(1, keyId.toString());
            statement.setString(2, playerId.toString());
            statement.setString(3, petId);
            statement.setLong(4, now);
            requireOne(statement);
        }
    }

    private long balance(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance_cents FROM tycoon_players WHERE player_uuid = ?")) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Missing pet owner account");
                }
                return result.getLong(1);
            }
        }
    }

    private void updateBalance(Connection connection, UUID playerId, long balance, long now)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE tycoon_players
                SET balance_cents = ?, updated_at = ?
                WHERE player_uuid = ?
                """)) {
            statement.setLong(1, balance);
            statement.setLong(2, now);
            statement.setString(3, playerId.toString());
            requireOne(statement);
        }
    }

    private IslandRank islandRank(Connection connection, UUID playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT prestige_level, status
                FROM tycoons
                WHERE owner_uuid = ?
                """)) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? new IslandRank(result.getInt(1), "ACTIVE".equals(result.getString(2)))
                        : null;
            }
        }
    }

    private void auditPet(
            Connection connection,
            UUID playerId,
            String petId,
            String action,
            long now
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO pet_transactions
                    (transaction_id, player_uuid, pet_id, action, amount_cents, created_at)
                VALUES (?, ?, ?, ?, 0, ?)
                """)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, playerId.toString());
            statement.setString(3, petId);
            statement.setString(4, action);
            statement.setLong(5, now);
            statement.executeUpdate();
        }
    }

    private void auditEconomy(
            Connection connection,
            UUID playerId,
            String petId,
            long amount,
            long now
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO economy_transactions
                    (transaction_id, transaction_type, source_uuid, target_uuid,
                     amount_cents, reason, created_at)
                VALUES (?, 'PET_RECLAIM', ?, NULL, ?, ?, ?)
                """)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, playerId.toString());
            statement.setLong(3, amount);
            statement.setString(4, "pet:reclaim:" + petId);
            statement.setLong(5, now);
            statement.executeUpdate();
        }
    }

    private PetProfile map(ResultSet result) throws SQLException {
        return new PetProfile(
                UUID.fromString(result.getString("player_uuid")),
                result.getString("pet_id"),
                result.getInt("pet_level"),
                result.getLong("pet_experience"),
                result.getInt("chromatic") == 1,
                result.getInt("active") == 1,
                Instant.ofEpochMilli(result.getLong("obtained_at"))
        );
    }

    private PetEgg mapEgg(ResultSet result) throws SQLException {
        return new PetEgg(
                UUID.fromString(result.getString("egg_id")),
                result.getString("pet_id"),
                result.getInt("chromatic") == 1,
                result.getInt("pet_level"),
                result.getLong("pet_experience"),
                UUID.fromString(result.getString("issued_to_uuid")),
                Instant.ofEpochMilli(result.getLong("issued_at"))
        );
    }

    private PetOperationResult result(
            PetOperationStatus status,
            PetProfile profile,
            PetEgg egg,
            long balanceCents,
            int requiredRank
    ) {
        return new PetOperationResult(status, profile, egg, balanceCents, requiredRank);
    }

    private <T> T inTransaction(Connection connection, Work<T> work) throws Exception {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = work.execute();
            connection.commit();
            return result;
        } catch (Exception exception) {
            try {
                connection.rollback();
            } catch (SQLException rollback) {
                exception.addSuppressed(rollback);
            }
            throw exception;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private void requireOne(PreparedStatement statement) throws SQLException {
        if (statement.executeUpdate() != 1) {
            throw new SQLException("Expected one pet row mutation");
        }
    }

    @FunctionalInterface
    private interface Work<T> {
        T execute() throws Exception;
    }

    private record IslandRank(int rank, boolean active) {
    }

    private record IssuedEgg(
            String petId,
            boolean chromatic,
            int level,
            long experience,
            boolean consumed
    ) {
    }
}
