package xyz.arcadiadevs.valoriatycoon.utils;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;

/**
 * Versions de serveur connues de ValoriaTycoon, par ordre croissant.
 *
 * <p>Le préfixe {@code V1_x} correspond à l'ancien versionnage de Minecraft
 * (1.7 &rarr; 1.21.11, et 1.22 jamais publié mais conservé pour ne pas casser les
 * ordinaux existants). Le préfixe {@code V26_x} correspond au nouveau versionnage
 * calendaire introduit avec Minecraft 26.1 ({@code 26.1}, {@code 26.2}, ...),
 * utilisé par Paper 26.1 et suivants. Les correctifs et hotfix
 * ({@code 26.1.1}, {@code 26.1.2}, ...) sont rattachés au drop correspondant.</p>
 */
public enum ServerVersion {
    UNKNOWN(-1, -1),
    V1_7(1, 7),
    V1_8(1, 8),
    V1_9(1, 9),
    V1_10(1, 10),
    V1_11(1, 11),
    V1_12(1, 12),
    V1_13(1, 13),
    V1_14(1, 14),
    V1_15(1, 15),
    V1_16(1, 16),
    V1_17(1, 17),
    V1_18(1, 18),
    V1_19(1, 19),
    V1_20(1, 20),
    V1_21(1, 21),
    V1_22(1, 22),
    V26_1(26, 1),
    V26_2(26, 2);

    private static final ServerVersion serverVersion;
    private static final String serverReleaseVersion;
    private static final String serverPackageVersion;
    private static final String serverPackagePath;
    private static final String minecraftVersion;

    static {
        serverPackagePath = Bukkit.getServer().getClass().getPackage().getName();
        serverPackageVersion = serverPackagePath.substring(serverPackagePath.lastIndexOf(46) + 1);
        serverReleaseVersion = serverPackageVersion.indexOf(82) != -1 ? serverPackageVersion.substring(serverPackageVersion.indexOf(82) + 1) : "";

        // Depuis Paper 1.20.6, le paquet CraftBukkit n'est plus déplacé (org.bukkit.craftbukkit
        // ne contient plus de segment "v1_20_R1") : le paquet ne permet donc plus de déduire la
        // version, et renverrait UNKNOWN sur 26.x. On interroge donc la version de l'API Bukkit
        // ("26.2.build.112-stable" sur Paper 26.x, "1.21.11-R0.1-SNAPSHOT" sur les versions
        // antérieures), puis la version Minecraft exposée par Paper, et on ne garde le nom du
        // paquet CraftBukkit qu'en dernier recours pour les serveurs legacy.
        String releaseVersion = null;
        ServerVersion detectedVersion = UNKNOWN;
        String[] candidateVersions = new String[]{readBukkitVersion(), readMinecraftVersion(), serverPackageVersion};
        try {
            for (int n = 0; n < candidateVersions.length; ++n) {
                String candidateVersion = candidateVersions[n];
                int[] numericVersion = parseVersion(candidateVersion);
                if (numericVersion == null) continue;
                releaseVersion = candidateVersion;
                detectedVersion = atMost(numericVersion[0], numericVersion[1]);
                break;
            }
        }
        catch (Throwable throwable) {
            detectedVersion = UNKNOWN;
        }
        minecraftVersion = releaseVersion != null ? releaseVersion : serverPackageVersion;
        serverVersion = detectedVersion;
    }

    private final int major;
    private final int minor;

    ServerVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * Version Bukkit de l'API implémentée par le serveur, par exemple
     * {@code 26.2.build.112-stable} sur Paper 26.2 ou {@code 1.20.4-R0.1-SNAPSHOT} sur
     * les versions antérieures.
     */
    private static String readBukkitVersion() {
        try {
            return Bukkit.getBukkitVersion();
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    /**
     * Version Minecraft exposée par Paper ({@code Server#getMinecraftVersion()}), absente de
     * l'API Spigot : l'appel passe par la réflexion pour rester compatible avec tous les forks.
     */
    private static String readMinecraftVersion() {
        try {
            Object server = Bukkit.getServer();
            Method method = server.getClass().getMethod("getMinecraftVersion", new Class[0]);
            Object result = method.invoke(server, new Object[0]);
            return result == null ? null : result.toString();
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    /**
     * Extrait les trois premiers segments numériques d'une version. Les séparateurs {@code .}
     * et {@code _} ainsi que le préfixe {@code v} des paquets CraftBukkit legacy sont acceptés.
     *
     * @return un tableau {@code {majeure, mineure, patch}}, ou {@code null} si la version est illisible
     */
    private static int[] parseVersion(String string) {
        if (string == null) {
            return null;
        }
        String version = string.trim();
        int n = version.length() > 0 && (version.charAt(0) == 'v' || version.charAt(0) == 'V') ? 1 : 0;
        int[] parts = new int[3];
        int count = 0;
        try {
            while (n < version.length() && count < parts.length) {
                int start = n;
                while (n < version.length() && Character.isDigit(version.charAt(n))) {
                    ++n;
                }
                if (n == start) break;
                parts[count++] = Integer.parseInt(version.substring(start, n));
                if (n < version.length() && (version.charAt(n) == '.' || version.charAt(n) == '_')) {
                    ++n;
                    continue;
                }
                break;
            }
        }
        catch (NumberFormatException numberFormatException) {
            return null;
        }
        if (count < 2) {
            return null;
        }
        return parts;
    }

    /**
     * Renvoie la dernière version connue strictement antérieure ou égale à la version détectée.
     * Une version plus récente que la dernière version connue (par exemple {@code 26.3}) est
     * donc rattachée à cette dernière, ce qui garde les vérifications {@code isAtLeast(...)}
     * vraies sur les futures mises à jour du serveur.
     */
    private static ServerVersion atMost(int n, int n2) {
        ServerVersion[] serverVersionArray = ServerVersion.values();
        ServerVersion result = UNKNOWN;
        int length = serverVersionArray.length;
        int n3 = 0;
        while (n3 < length) {
            ServerVersion candidate = serverVersionArray[n3];
            if (candidate.major >= 0) {
                if (candidate.major > n || candidate.major == n && candidate.minor > n2) break;
                result = candidate;
            }
            ++n3;
        }
        return result;
    }

    /**
     * Résout une chaîne de version (au format Bukkit, Paper ou paquet CraftBukkit) en une
     * constante de cet enum, ou {@link #UNKNOWN} si elle n'est pas exploitable.
     */
    public static ServerVersion fromVersionString(String string) {
        int[] numericVersion = parseVersion(string);
        return numericVersion == null ? UNKNOWN : atMost(numericVersion[0], numericVersion[1]);
    }

    /**
     * Version du serveur détectée au chargement du plugin.
     */
    public static ServerVersion get() {
        return serverVersion;
    }

    public int getMajor() {
        return this.major;
    }

    public int getMinor() {
        return this.minor;
    }

    public boolean isLessThan(ServerVersion serverVersion) {
        return this.ordinal() < serverVersion.ordinal();
    }

    public boolean isAtOrBelow(ServerVersion serverVersion) {
        return this.ordinal() <= serverVersion.ordinal();
    }

    public boolean isGreaterThan(ServerVersion serverVersion) {
        return this.ordinal() > serverVersion.ordinal();
    }

    public boolean isAtLeast(ServerVersion serverVersion) {
        return this.ordinal() >= serverVersion.ordinal();
    }

    /**
     * Nom du paquet CraftBukkit du serveur (par exemple {@code v1_20_R1}), conservé pour le
     * code qui construit des chemins NMS par réflexion. Il n'est plus déplacé sur les versions
     * récentes et vaut alors {@code craftbukkit}.
     */
    public static String getServerVersionString() {
        return serverPackageVersion;
    }

    /**
     * Version Minecraft/Bukkit du serveur telle que détectée (par exemple
     * {@code 26.2.build.112-stable}), à utiliser pour les logs et les rapports de bug.
     */
    public static String getMinecraftVersionString() {
        return minecraftVersion;
    }

    public static String getVersionReleaseNumber() {
        return serverReleaseVersion;
    }

    public static boolean isServerVersion(ServerVersion serverVersion) {
        return ServerVersion.serverVersion == serverVersion;
    }

    public static boolean isServerVersion(ServerVersion ... serverVersionArray) {
        ServerVersion[] serverVersionArray2 = serverVersionArray;
        int n = serverVersionArray.length;
        int n2 = 0;
        while (n2 < n) {
            ServerVersion serverVersion = serverVersionArray2[n2];
            if (serverVersion == ServerVersion.serverVersion) {
                return true;
            }
            ++n2;
        }
        return false;
    }

    public static boolean isServerVersionAbove(ServerVersion serverVersion) {
        return ServerVersion.serverVersion.ordinal() > serverVersion.ordinal();
    }

    public static boolean isServerVersionAtLeast(ServerVersion serverVersion) {
        return ServerVersion.serverVersion.ordinal() >= serverVersion.ordinal();
    }

    public static boolean isServerVersionAtOrBelow(ServerVersion serverVersion) {
        return ServerVersion.serverVersion.ordinal() <= serverVersion.ordinal();
    }

    public static boolean isServerVersionBelow(ServerVersion serverVersion) {
        return ServerVersion.serverVersion.ordinal() < serverVersion.ordinal();
    }
}
