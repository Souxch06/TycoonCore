package fr.valoriatycoon.farm.regeneration;

import fr.valoriatycoon.database.SqliteDatabase;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Persists delayed restorations so clean restarts cannot permanently consume farm resources. */
public final class BlockRegenerationRepository {
    private final SqliteDatabase database;

    public BlockRegenerationRepository(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public CompletableFuture<Void> save(PendingBlockRegeneration pending) {
        return database.submit(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO pending_block_regenerations
                        (world_name, block_x, block_y, block_z, block_data, due_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(world_name, block_x, block_y, block_z) DO UPDATE SET
                        block_data = excluded.block_data,
                        due_at = excluded.due_at
                    """)) {
                bindPosition(statement, pending.position());
                statement.setString(5, pending.blockData());
                statement.setLong(6, pending.dueAtEpochMillis());
                statement.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Void> delete(BlockPosition position) {
        return database.submit(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM pending_block_regenerations
                    WHERE world_name = ? AND block_x = ? AND block_y = ? AND block_z = ?
                    """)) {
                bindPosition(statement, position);
                statement.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<List<PendingBlockRegeneration>> loadAll() {
        return database.submit(connection -> {
            List<PendingBlockRegeneration> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT world_name, block_x, block_y, block_z, block_data, due_at
                    FROM pending_block_regenerations
                    ORDER BY due_at ASC
                    """); ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new PendingBlockRegeneration(
                            new BlockPosition(
                                    rows.getString(1),
                                    rows.getInt(2),
                                    rows.getInt(3),
                                    rows.getInt(4)
                            ),
                            rows.getString(5),
                            rows.getLong(6)
                    ));
                }
            }
            return result;
        });
    }

    private void bindPosition(PreparedStatement statement, BlockPosition position) throws java.sql.SQLException {
        statement.setString(1, position.worldName());
        statement.setInt(2, position.x());
        statement.setInt(3, position.y());
        statement.setInt(4, position.z());
    }
}
