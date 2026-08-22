package fr.valoriatycoon.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RomanNumeralsTest {
    @Test
    void formatsUpgradeLevels() {
        assertEquals("0", RomanNumerals.format(0));
        assertEquals("I", RomanNumerals.format(1));
        assertEquals("IV", RomanNumerals.format(4));
        assertEquals("XIV", RomanNumerals.format(14));
        assertEquals("C", RomanNumerals.format(100));
    }
}
