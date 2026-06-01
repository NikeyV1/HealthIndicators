package io.github.adytech99.healthindicators;

public final class ServerPermissions {
    private static boolean allowInvisiblePlayers = false;

    private ServerPermissions() {}

    public static boolean allowsInvisiblePlayers() {
        return allowInvisiblePlayers;
    }

    public static void setAllowInvisiblePlayers(boolean value) {
        allowInvisiblePlayers = value;
    }

    public static void reset() {
        allowInvisiblePlayers = false;
    }
}