package fr.valoriatycoon.utils;

/** Formats positive upgrade levels as conventional Roman numerals. */
public final class RomanNumerals {
    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private RomanNumerals() {
    }

    public static String format(int value) {
        if (value <= 0) {
            return "0";
        }
        if (value > 3999) {
            return Integer.toString(value);
        }
        StringBuilder result = new StringBuilder();
        int remaining = value;
        for (int index = 0; index < VALUES.length; index++) {
            while (remaining >= VALUES[index]) {
                result.append(SYMBOLS[index]);
                remaining -= VALUES[index];
            }
        }
        return result.toString();
    }
}
