package com.oddlabs.tt.base.global;


import java.nio.file.Path;

/**
 * Foundational application configuration constants and default settings.
 */
public final class AppConfig {
    public static final String GAME_NAME = "TribalTrouble";
    public static final int REVISION = 4;
    public static final Path SETTINGS_FILE_NAME = Path.of("settings");
    public static final int DEFAULT_NET_PORT = 21000;
    public static final int DEFAULT_DETAIL_NORMAL = 1;
    /**
     * Default baked terrain texture resolution of 8 texels per 2-meter grid unit (25 cm/texel),
     * providing smooth macro-level color transitions while fine surface detail is supplied by
     * high-frequency repeating detail textures.
     */
    public static final int DEFAULT_TEXELS_PER_GRID_UNIT = 8;
    public static final boolean SAVE_EVENT_LOG = true;
    public static final boolean HIDE_MULTIPLAYER = true;
    public static final int FRAME_GRAB_MILLISECONDS_PER_FRAME = 40;

    private AppConfig() {
    }
}
