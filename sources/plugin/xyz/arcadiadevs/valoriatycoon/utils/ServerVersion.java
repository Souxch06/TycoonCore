package xyz.arcadiadevs.valoriatycoon.utils;

import java.lang.reflect.Method;

/**
 * Versions de serveur connues de ValoriaTycoon, par ordre croissant.
 *
 * <p>Le préfixe {@code V1_x} correspond à l'ancien versionnage de Minecraft
 * (1.7 &rarr; 1.21.11). Le préfixe {@code V26_x} correspond au versionnage calendaire
 * introduit avec Minecraft 26.1 et utilisé par Paper 26.1 et suivants
 * ({@code 26.1}, {@code 26.2}, ...). Les correctifs et hotfix
 * ({@code 26.1.1}, {@code 26.1.2}, ...) sont rattachés au drop correspondant.</p>
 *
 * <p>La classe accède à Bukkit par réflexion afin de rester compilable sans dépendance
 * externe (le build du dépôt ne resolve aucun artifact Bukkit) et pour ne lever aucune
 * exception au chargement : une détection qui échoue donne {@link #UNKNOWN}, jamais une
 * {@code ExceptionInInitializerError}.</p>
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
    private static final String minecraftVersion;

    static {
        Object server = readServer();
        String serverPackagePath = readServerPackagePath(server);
        serverPackageVersion = serverPackagePath.substring(serverPackagePath.lastIndexOf(46) + 1);
        serverReleaseVersion = serverPackageVersion.indexOf(82) != -1 ? serverPackageVersion.substring(serverPackageVersion.indexOf(82) + 1) : "";

        // Depuis Paper 1.20.6, le paquet CraftBukkit n'est plus déplacé : lastIndexOf('.') ne
        // renvoie plus "v1_20_R1" mais "craftbukkit", et la version n'est donc plus déductible du
        // paquet. Sur Paper 26.x, Bukkit#getBukkitVersion() vaut par ailleurs
        // "26.2.build.112-stable" (nouveau schéma calendaire) au lieu de "1.21.11-R0.1-SNAPSHOT".
        // La détection lit donc la version de l'API Bukkit, puis la version Minecraft exposée par
        // Paper, et ne garde le nom du paquet CraftBukkit qu'en dernier recours (serveurs legacy).
        String releaseVersion = null;
        ServerVersion detectedVersion = UNKNOWN;
        String[] candidateVersions = new String[]{call(server, "getBukkitVersion"), call(server, "getMinecraftVersion"), serverPackageVersion};
        for (int n = 0; n < candidateVersions.length; ++n) {
            String candidateVersion = candidateVersions[n];
            int[] numericVersion = parseVersion(candidateVersion);
            if (numericVersion == null) continue;
            releaseVersion = candidateVersion;
            detectedVersion = atMost(numericVersion[0], numericVersion[1]);
            break;
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

    private static Object readServer() {
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Method method = bukkit.getMethod("getServer", new Class[0]);
            return method.invoke(null, new Object[0]);
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    private static String readServerPackagePath(Object server) {
        if (server == null) {
            return "";
        }
        Package serverPackage = server.getClass().getPackage();
        return serverPackage == null ? "" : serverPackage.getName();
    }

    private static String call(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName, new Class[0]);
            Object result = method.invoke(target, new Object[0]);
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
     * @return un tableau {@code {majeure, mineure, correctif}}, ou {@code null} si la version est illisible
     */
    private static int[] parseVersion(String string) {
        if (string == null) {
            return null;
        }
        String version = string.trim();
        int n = version.length() > 0 && (version.charAt(0) == 'v' || version.charAt(0) == 'V') ? 1 : 0;
        int[] parts = new int[3];
        int count = 0;
        while (n < version.length() && count < parts.length) {
            int start = n;
            while (n < version.length() && Character.isDigit(version.charAt(n))) {
                ++n;
            }
            if (n == start) break;
            String part = version.substring(start, n);
            if (part.length() > 9) {
                return null;
            }
            parts[count++] = Integer.parseInt(part);
            if (n < version.length() && (version.charAt(n) == 46 || version.charAt(n) == 95)) {
                ++n;
                continue;
            }
            break;
        }
        if (count < 2) {
            return null;
        }
        return parts;
    }

    /**
     * Renvoie la dernière version connue antérieure ou égale à la version détectée. Une version
     * plus récente que la dernière version connue ({@code 26.3}, {@code 27.1}, ...) est donc
     * rattachée à cette dernière, ce qui garde les vérifications {@code isAtLeast(...)} vraies
     * sur les prochaines mises à jour du serveur au lieu de retomber sur {@link #UNKNOWN}.
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
     * Résout une chaîne de version (format Bukkit, Paper ou paquet CraftBukkit) en une constante
     * de cet enum, ou {@link #UNKNOWN} si elle n'est pas exploitable.
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
     * Nom du paquet CraftBukkit du serveur (par exemple {@code v1_20_R1}), conservé pour le code
     * qui construit des chemins NMS par réflexion. Il n'est plus déplacé sur les versions récentes
     * et vaut alors {@code craftbukkit}.
     */
    public static String getServerVersionString() {
        return serverPackageVersion;
    }

    /**
     * Version Bukkit/Minecraft du serveur telle que détectée (par exemple
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
