package fr.valoriatycoon.leaderboards;

import fr.valoriatycoon.economy.CurrencyFormatter;
import fr.valoriatycoon.ranks.RankSettings;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/** Shared French value rendering for leaderboard menus, holograms and future placeholders. */
public final class LeaderboardValueFormatter {
    private final CurrencyFormatter currency;
    private final RankSettings ranks;

    public LeaderboardValueFormatter(CurrencyFormatter currency, RankSettings ranks) {
        this.currency = Objects.requireNonNull(currency, "currency");
        this.ranks = Objects.requireNonNull(ranks, "ranks");
    }

    public String format(LeaderboardType type, long value) {
        return switch (type) {
            case MONEY -> currency.format(value);
            case ISLAND_LEVEL -> "Niveau " + integer(value);
            case RANK -> ranks.name((int) Math.min(ranks.maximumLevel(), value));
            case PRODUCTION -> integer(value) + " ressources";
            case PLAYTIME -> playtime(value);
        };
    }

    private String integer(long value) {
        return NumberFormat.getIntegerInstance(Locale.FRANCE).format(value);
    }

    private String playtime(long seconds) {
        long days = seconds / 86_400L;
        long hours = seconds % 86_400L / 3_600L;
        long minutes = seconds % 3_600L / 60L;
        return days > 0
                ? days + "j " + hours + "h"
                : hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }
}
