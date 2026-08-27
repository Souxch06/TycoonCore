/*
 * Décompilé avec CFR 0.152.
 */
package xyz.arcadiadevs.gensplus.utils.config;

public enum Permissions {
    ADMIN("gensplus.admin"),
    GENERATORS_GUI("gensplus.generator.open"),
    GENERATOR_GIVE("gensplus.admin.give"),
    GENERATOR_GIVE_ALL("gensplus.admin.give.all"),
    GENERATOR_RELOAD("gensplus.admin.reload"),
    START_EVENT("gensplus.admin.startevent"),
    STOP_EVENT("gensplus.admin.stopevent"),
    GENERATOR_DROPS_SELL_ALL("gensplus.drop.sell.all"),
    GENERATOR_DROPS_SELL_HAND("gensplus.drop.sell.hand"),
    GENERATOR_DROPS_SELL_GUI("gensplus.drop.sell.gui"),
    SELL_MULTIPLIER("gensplus.sell.multiplier."),
    GENERATOR_LIMIT("gensplus.limit."),
    CHUNK_RADIUS("gensplus.radius."),
    SET_LIMIT("gensplus.admin.setlimit"),
    ADD_LIMIT("gensplus.admin.addlimit"),
    GIVE_WAND("gensplus.admin.givewand");

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

