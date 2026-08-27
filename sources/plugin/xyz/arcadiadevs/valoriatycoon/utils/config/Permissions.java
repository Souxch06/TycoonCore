/*
 * Décompilé avec CFR 0.152.
 */
package xyz.arcadiadevs.valoriatycoon.utils.config;

public enum Permissions {
    ADMIN("valoriatycoon.admin"),
    GENERATORS_GUI("valoriatycoon.generator.open"),
    GENERATOR_GIVE("valoriatycoon.admin.give"),
    GENERATOR_GIVE_ALL("valoriatycoon.admin.give.all"),
    GENERATOR_RELOAD("valoriatycoon.admin.reload"),
    START_EVENT("valoriatycoon.admin.startevent"),
    STOP_EVENT("valoriatycoon.admin.stopevent"),
    GENERATOR_DROPS_SELL_ALL("valoriatycoon.drop.sell.all"),
    GENERATOR_DROPS_SELL_HAND("valoriatycoon.drop.sell.hand"),
    GENERATOR_DROPS_SELL_GUI("valoriatycoon.drop.sell.gui"),
    SELL_MULTIPLIER("valoriatycoon.sell.multiplier."),
    GENERATOR_LIMIT("valoriatycoon.limit."),
    CHUNK_RADIUS("valoriatycoon.radius."),
    SET_LIMIT("valoriatycoon.admin.setlimit"),
    ADD_LIMIT("valoriatycoon.admin.addlimit"),
    GIVE_WAND("valoriatycoon.admin.givewand");

    private final String permission;

    private Permissions(String string2) {
        this.permission = string2;
    }

    public String getPermission(String ... stringArray) {
        String string = this.permission;
        int n = this.countBrackets(this.permission);
        if (n != stringArray.length) {
            throw new IllegalArgumentException("Nombre d'arguments invalide");
        }
        String[] stringArray2 = stringArray;
        int n2 = stringArray.length;
        int n3 = 0;
        while (n3 < n2) {
            String string2 = stringArray2[n3];
            string = this.permission.replace("{}", string2);
            ++n3;
        }
        return string;
    }

    private int countBrackets(String string) {
        int n = 0;
        int n2 = 0;
        while (n2 < string.length()) {
            if (string.charAt(n2) == '{') {
                ++n;
            } else if (string.charAt(n2) == '}') {
                --n;
            }
            ++n2;
        }
        return n;
    }
}

