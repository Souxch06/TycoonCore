package fr.valoriatycoon.config;

import fr.valoriatycoon.economy.MoneyCodec;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Locale;
import org.bukkit.configuration.file.FileConfiguration;

/** Validated immutable snapshot of the core configuration. */
public record CoreSettings(
        Database database,
        Economy economy,
        boolean debugLogging
) {
    public static CoreSettings from(FileConfiguration config) {
        String type = config.getString("database.type", "SQLITE").toUpperCase(Locale.ROOT);
        if (!"SQLITE".equals(type)) {
            throw new IllegalArgumentException("Only SQLITE is supported in ValoriaTycoon stage 1; configured: " + type);
        }

        String file = requireText(config.getString("database.sqlite.file"), "database.sqlite.file");
        int busyTimeout = config.getInt("database.sqlite.busy-timeout-ms", 5000);
        if (busyTimeout < 100 || busyTimeout > 60_000) {
            throw new IllegalArgumentException("database.sqlite.busy-timeout-ms must be between 100 and 60000");
        }

        long starting = money(config, "economy.starting-balance");
        long minimumPayment = money(config, "economy.minimum-payment");
        long maximumPayment = money(config, "economy.maximum-payment");
        if (starting < 0) {
            throw new IllegalArgumentException("economy.starting-balance cannot be negative");
        }
        if (minimumPayment <= 0 || maximumPayment < minimumPayment) {
            throw new IllegalArgumentException("Invalid economy payment limits");
        }

        int cooldownSeconds = config.getInt("economy.pay-cooldown-seconds", 3);
        if (cooldownSeconds < 0 || cooldownSeconds > 3600) {
            throw new IllegalArgumentException("economy.pay-cooldown-seconds must be between 0 and 3600");
        }

        int decimals = config.getInt("economy.display.decimals", 2);
        if (decimals < 0 || decimals > MoneyCodec.SCALE) {
            throw new IllegalArgumentException("economy.display.decimals must be between 0 and 2");
        }
        String suffix = config.getString("economy.display.suffix", " $");

        return new CoreSettings(
                new Database(type, file, busyTimeout),
                new Economy(
                        starting,
                        minimumPayment,
                        maximumPayment,
                        Duration.ofSeconds(cooldownSeconds),
                        decimals,
                        suffix == null ? "" : suffix
                ),
                config.getBoolean("logging.debug", false)
        );
    }

    private static long money(FileConfiguration config, String path) {
        String raw = requireText(config.getString(path), path);
        try {
            return MoneyCodec.toCents(new BigDecimal(raw));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(path + " must be an exact amount with at most two decimals", exception);
        }
    }

    private static String requireText(String value, String path) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(path + " cannot be blank");
        }
        return value.trim();
    }

    public record Database(String type, String sqliteFile, int busyTimeoutMillis) {
    }

    public record Economy(
            long startingBalanceCents,
            long minimumPaymentCents,
            long maximumPaymentCents,
            Duration payCooldown,
            int displayDecimals,
            String displaySuffix
    ) {
    }
}
