/*
 * Décompilé avec CFR 0.152.
 */
package xyz.arcadiadevs.gensplus.utils;

public class TimeUtil {
    public static long parseTime(String string) {
        String[] stringArray;
        long l = 0L;
        String[] stringArray2 = stringArray = string.split("\\s+");
        int n = stringArray.length;
        int n2 = 0;
        while (n2 < n) {
            String string2 = stringArray2[n2];
            char c = string2.charAt(string2.length() - 1);
            int n3 = Integer.parseInt(string2.substring(0, string2.length() - 1));
            switch (c) {
                case 's': {
                    l += (long)n3 * 20L;
                    break;
                }
                case 'm': {
                    l += (long)n3 * 20L * 60L;
                    break;
                }
                case 'h': {
                    l += (long)n3 * 20L * 60L * 60L;
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Unité invalide dans le délai de disparition : " + c);
                }
            }
            ++n2;
        }
        return l;
    }

    public static String ticksToTime(long l) {
        long l2 = l / 20L;
        long l3 = l2 / 60L;
        long l4 = l3 / 60L;
        return String.format("%02d:%02d:%02d", l4, l3 % 60L, l2 % 60L);
    }

    public static long parseTimeMillis(String string) {
        String[] stringArray;
        long l = 0L;
        String[] stringArray2 = stringArray = string.split("\\s+");
        int n = stringArray.length;
        int n2 = 0;
        while (n2 < n) {
            String string2 = stringArray2[n2];
            char c = string2.charAt(string2.length() - 1);
            int n3 = Integer.parseInt(string2.substring(0, string2.length() - 1));
            switch (c) {
                case 's': {
                    l += (long)n3 * 1000L;
                    break;
                }
                case 'm': {
                    l += (long)n3 * 1000L * 60L;
                    break;
                }
                case 'h': {
                    l += (long)n3 * 1000L * 60L * 60L;
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Unité invalide dans le délai de disparition : " + c);
                }
            }
            ++n2;
        }
        return l;
    }

    public static String millisToTime(long l) {
        long l2 = l / 1000L;
        long l3 = l2 / 60L;
        long l4 = l3 / 60L;
        return String.format("%02d:%02d:%02d", l4, l3 % 60L, l2 % 60L);
    }
}

