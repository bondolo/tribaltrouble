package com.oddlabs.tt.net;

/**
 * Networking configuration constants and connection defaults.
 */
public final class NetConfig {
    public static final int DEFAULT_NET_PORT = 21000;

    /**
     * Flag indicating whether a deterministic state checksum mismatch occurred during the last game.
     */
    public static boolean checksum_error_in_last_game = false;

    private NetConfig() {
    }
}
