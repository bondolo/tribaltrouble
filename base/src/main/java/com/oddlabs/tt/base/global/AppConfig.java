package com.oddlabs.tt.base.global;

import org.jspecify.annotations.NonNull;

import java.nio.file.Path;

/**
 * Foundational application configuration constants and default settings.
 */
public final class AppConfig {
    public static final @NonNull String GAME_NAME = "TribalTrouble";
    public static final int REVISION = 1;
    public static final @NonNull Path SETTINGS_FILE_NAME = Path.of("settings");
    public static final int DEFAULT_NET_PORT = 21000;
    public static final int DEFAULT_DETAIL_NORMAL = 1;
    public static final int DEFAULT_TEXELS_PER_GRID_UNIT = 8;
    public static final int[] DEFAULT_TEXTURE_MIP_SHIFT = new int[]{1, 0, 0};

    private AppConfig() {
    }
}
