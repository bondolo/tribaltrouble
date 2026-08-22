package com.oddlabs.tt.base.global;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates application filesystem paths (data directory and log directory)
 * and provides OS-aware path discovery routines.
 */
public record GamePaths(Path dataDir, Path logDir) {
    private static final Logger logger = Logger.getLogger(GamePaths.class.getName());

    public GamePaths(Path dataDir) {
        this(dataDir, dataDir.resolve("logs"));
    }

    public GamePaths() {
        this(detect().dataDir(), detect().logDir());
    }

    public static GamePaths detect() {
        Path dataDir = null;
        Path logDir = null;
        boolean portable = false;

        // 1. Check for Portable Mode (CWD)
        Path localDir = Path.of(AppConfig.GAME_NAME);
        if (isUsable(localDir)) {
            dataDir = localDir;
            portable = true;
        }

        // 2. Check for Portable Mode (App/JAR Directory)
        if (!portable) {
            try {
                java.security.CodeSource codeSource = GamePaths.class.getProtectionDomain().getCodeSource();
                if (codeSource != null) {
                    Path jarPath = Path.of(codeSource.getLocation().toURI());
                    Path appDir = jarPath.getParent();
                    if (appDir != null) {
                        Path appGameDir = appDir.resolve(AppConfig.GAME_NAME);
                        if (!appGameDir.equals(localDir.toAbsolutePath()) && isUsable(appGameDir)) {
                            dataDir = appGameDir;
                            portable = true;
                        }
                    }
                }
            } catch (Exception _) {
                // Ignore errors determining app directory
            }
        }

        String os_name;
        try {
            os_name = System.getProperty("os.name").toLowerCase();
        } catch (SecurityException _) {
            os_name = "unknown";
        }

        Path userHome = getUserHomePath();

        // 3. Resolve Data Directory (if not portable)
        if (!portable) {
            String xdgConfigHome = null;
            String appData = null;
            try {
                xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
                appData = System.getenv("APPDATA");
            } catch (SecurityException _) {
                /* ignore */
            }

            Path preferred = null;
            Path fallback = null;
            Path existing = null;

            if (os_name.contains("mac")) {
                if (userHome != null) {
                    Path appSupport = userHome.resolve("Library/Application Support/" + AppConfig.GAME_NAME);
                    Path config = userHome.resolve(".config/tribaltrouble");

                    if (isUsable(appSupport)) existing = appSupport;
                    else if (isUsable(config)) existing = config;

                    preferred = appSupport;
                    fallback = config;
                }
            } else if (os_name.contains("linux") || os_name.contains("unix")) {
                Path legacyDot = userHome != null ? userHome.resolve(".tribaltrouble") : null;
                Path currentDot = Path.of(".tribaltrouble");

                Path xdg;
                if (xdgConfigHome != null && !xdgConfigHome.isEmpty()) {
                    xdg = Path.of(xdgConfigHome).resolve("tribaltrouble");
                } else if (userHome != null) {
                    xdg = userHome.resolve(".config/tribaltrouble");
                } else {
                    xdg = null;
                }

                if (isUsable(legacyDot)) existing = legacyDot;
                else if (isUsable(currentDot)) existing = currentDot;
                else if (isUsable(xdg)) existing = xdg;

                preferred = xdg;
                fallback = legacyDot;
            } else {
                Path roaming = appData != null ? Path.of(appData).resolve(AppConfig.GAME_NAME) : null;
                Path homeGame = userHome != null ? userHome.resolve(AppConfig.GAME_NAME) : null;

                if (isUsable(roaming)) existing = roaming;
                else if (isUsable(homeGame)) existing = homeGame;

                preferred = roaming;
                fallback = homeGame;
            }

            if (existing != null) {
                dataDir = existing;
            } else if (preferred != null) {
                try {
                    Files.createDirectories(preferred);
                    if (isUsable(preferred)) dataDir = preferred;
                } catch (IOException | SecurityException _) {
                    // Ignore
                }
            }

            if (dataDir == null && fallback != null) {
                try {
                    Files.createDirectories(fallback);
                    if (isUsable(fallback)) dataDir = fallback;
                } catch (IOException | SecurityException _) {
                    // Ignore
                }
            }

            if (dataDir == null) {
                try {
                    dataDir = Files.createTempDirectory(AppConfig.GAME_NAME);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }

        // 4. Resolve Log Directory
        if (portable) {
            logDir = dataDir.resolve("logs");
        } else {
            Path preferredLog = null;
            if (os_name.contains("mac")) {
                if (userHome != null) {
                    preferredLog = userHome.resolve("Library/Logs/TribalTrouble");
                }
            } else if (os_name.contains("linux") || os_name.contains("unix")) {
                String xdgStateHome = null;
                try {
                    xdgStateHome = System.getenv("XDG_STATE_HOME");
                } catch (SecurityException _) {
                    /* ignore */
                }

                if (xdgStateHome != null && !xdgStateHome.isEmpty()) {
                    preferredLog = Path.of(xdgStateHome).resolve("tribaltrouble/logs");
                } else if (userHome != null) {
                    preferredLog = userHome.resolve(".local/state/tribaltrouble/logs");
                }
            } else {
                String localAppData = null;
                try {
                    localAppData = System.getenv("LOCALAPPDATA");
                } catch (SecurityException _) {
                    /* ignore */
                }

                if (localAppData != null) {
                    preferredLog = Path.of(localAppData).resolve("TribalTrouble\\logs");
                }
            }

            if (preferredLog != null) {
                try {
                    Files.createDirectories(preferredLog);
                    if (isUsable(preferredLog)) {
                        logDir = preferredLog;
                    }
                } catch (IOException | SecurityException _) {
                    // Ignore
                }
            }

            if (logDir == null) {
                logDir = dataDir.resolve("logs");
            }
        }

        return new GamePaths(dataDir, logDir);
    }

    public static @Nullable Path getUserHomePath() {
        return getPropertyPath("user.home");
    }

    public static @Nullable Path getPropertyPath(String property) {
        String propertyValue;
        try {
            propertyValue = System.getProperty(property);
        } catch (SecurityException e) {
            logger.log(Level.WARNING, "SecurityException reading property: " + property, e);
            propertyValue = null;
        }

        Path result = null;
        if (propertyValue != null) {
            try {
                result = Path.of(propertyValue);
                if (Files.notExists(result)) {
                    result = Files.createDirectories(result);
                }
                if (!Files.isDirectory(result) || !Files.isReadable(result)) {
                    result = null;
                }
            } catch (IOException _) {
                result = null;
            }
        }

        if (null == result) {
            try {
                result = Files.createTempDirectory(property);
            } catch (IOException | SecurityException totalFailure) {
                logger.log(Level.WARNING, "Failed to create temp directory for " + property, totalFailure);
            }
        }

        return result;
    }

    private static boolean isUsable(@Nullable Path path) {
        if (path == null) return false;
        try {
            if (!Files.exists(path) || !Files.isDirectory(path) || !Files.isWritable(path)) {
                return false;
            }
            try {
                Path testFile = Files.createTempFile(path, ".tt_write_test", null);
                Files.delete(testFile);
                return true;
            } catch (IOException | SecurityException _) {
                return false;
            }
        } catch (Throwable _) {
            return false;
        }
    }
}
