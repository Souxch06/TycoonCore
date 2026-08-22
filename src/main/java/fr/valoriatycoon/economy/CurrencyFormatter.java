package fr.valoriatycoon.economy;

import fr.valoriatycoon.config.CoreSettings;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.function.Supplier;

/** Formats integer cents using the current display configuration. */
public final class CurrencyFormatter {
    private final Supplier<CoreSettings.Economy> settingsSupplier;

    public CurrencyFormatter(Supplier<CoreSettings.Economy> settingsSupplier) {
        this.settingsSupplier = Objects.requireNonNull(settingsSupplier, "settingsSupplier");
    }

    public String format(long cents) {
        CoreSettings.Economy settings = settingsSupplier.get();
        return MoneyCodec.fromCents(cents)
                .setScale(settings.displayDecimals(), RoundingMode.DOWN)
                .toPlainString() + settings.displaySuffix();
    }
}
