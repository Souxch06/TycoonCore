package fr.valoriatycoon.economy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.regex.Pattern;

/** Converts exact decimal currency values to the integer cents used by storage. */
public final class MoneyCodec {
    public static final int SCALE = 2;
    private static final Pattern USER_AMOUNT = Pattern.compile("[0-9]+(?:[.,][0-9]{1,2})?");

    private MoneyCodec() {
    }

    public static long toCents(BigDecimal amount) {
        try {
            return amount.setScale(SCALE, RoundingMode.UNNECESSARY)
                    .movePointRight(SCALE)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount must fit in a signed 64-bit cents value with at most two decimals", exception);
        }
    }

    public static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, SCALE);
    }

    public static Optional<BigDecimal> parseUserAmount(String input) {
        if (input == null || !USER_AMOUNT.matcher(input).matches()) {
            return Optional.empty();
        }
        try {
            BigDecimal amount = new BigDecimal(input.replace(',', '.'));
            toCents(amount);
            return Optional.of(amount.setScale(SCALE));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
