package xyz.arcadiadevs.valoriatycoon.utils;

import org.bukkit.Bukkit;

public enum ServerVersion {
    UNKNOWN,
    V1_7,
    V1_8,
    V1_9,
    V1_10,
    V1_11,
    V1_12,
    V1_13,
    V1_14,
    V1_15,
    V1_16,
    V1_17,
    V1_18,
    V1_19,
    V1_20,
    V1_21,
    V1_22;

    private static final ServerVersion serverVersion;
    private static final String serverReleaseVersion;
    private static final String serverPackageVersion;
    private static final String serverPackagePath;

    static {
        serverPackagePath = Bukkit.getServer().getClass().getPackage().getName();
        serverPackageVersion = serverPackagePath.substring(serverPackagePath.lastIndexOf(46) + 1);
        serverReleaseVersion = serverPackageVersion.indexOf(82) != -1 ? serverPackageVersion.substring(serverPackageVersion.indexOf(82) + 1) : "";
        serverVersion = ServerVersion.getVersion();
    }

    private static ServerVersion getVersion() {
        ServerVersion[] serverVersionArray = ServerVersion.values();
        int n = serverVersionArray.length;
        int n2 = 0;
        while (n2 < n) {
            ServerVersion serverVersion = serverVersionArray[n2];
            if (serverPackageVersion.toUpperCase().startsWith(serverVersion.name())) {
                return serverVersion;
            }
            ++n2;
        }
        return UNKNOWN;
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

    public static String getServerVersionString() {
        return serverPackageVersion;
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

