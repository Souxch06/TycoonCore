package fr.valoriatycoon.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyCodecTest {
    @Test
    void convertsExactAmountsWithoutFloatingPointRounding() {
        assertEquals(12345L, MoneyCodec.toCents(new BigDecimal("123.45")));
        assertEquals(new BigDecimal("123.45"), MoneyCodec.fromCents(12345L));
    }

    @Test
    void acceptsCommaAndAtMostTwoDecimalsFromCommands() {
        assertEquals(new BigDecimal("10.50"), MoneyCodec.parseUserAmount("10,5").orElseThrow());
        assertTrue(MoneyCodec.parseUserAmount("1.001").isEmpty());
        assertTrue(MoneyCodec.parseUserAmount("NaN").isEmpty());
        assertTrue(MoneyCodec.parseUserAmount("1e3").isEmpty());
    }

    @Test
    void rejectsUnrepresentableValues() {
        assertThrows(IllegalArgumentException.class, () -> MoneyCodec.toCents(new BigDecimal("1.001")));
        assertThrows(IllegalArgumentException.class, () -> MoneyCodec.toCents(new BigDecimal("999999999999999999999")));
    }
}
