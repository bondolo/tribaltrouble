package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.base.util.Utils;

import java.util.ResourceBundle;

public final class ServerMessageBundler {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(ServerMessageBundler.class.getName());

    private static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public static String getSizeString(int index) {
        return i18n(switch (index) {
            case Game.SIZE_SMALL -> "size_small";
            case Game.SIZE_MEDIUM -> "size_medium";
            case Game.SIZE_LARGE -> "size_large";
            default -> throw new IllegalArgumentException("unexpected size: " + index);
        });
    }

    public static String getTerrainTypeString(int index) {
        return i18n(switch (index) {
            case Game.TERRAIN_TYPE_NATIVE -> "terrain_type_native";
            case Game.TERRAIN_TYPE_VIKING -> "terrain_type_viking";
            default -> throw new IllegalArgumentException("unexpected terrain_type: " + index);
        });
    }

    public static String getRatedString(boolean rated) {
        return i18n(rated ? "rated_yes" : "rated_no");
    }

    public static String getGamespeedString(int index) {
        return i18n(switch (index) {
            case Game.GAMESPEED_PAUSE -> "gamespeed_pause";
            case Game.GAMESPEED_SLOW -> "gamespeed_slow";
            case Game.GAMESPEED_NORMAL -> "gamespeed_normal";
            case Game.GAMESPEED_FAST -> "gamespeed_fast";
            case Game.GAMESPEED_LUDICROUS -> "gamespeed_ludicrous";
            default -> throw new IllegalArgumentException("unexpected gamespeed: " + index);
        });
    }

    public static String getHillsString(int index) {
        return (10 * index) + "%";
    }

    public static String getTreesString(int index) {
        return (10 * index) + "%";
    }

    public static String getSuppliesString(int index) {
        return (10 * index) + "%";
    }

    private ServerMessageBundler() {
    }
}
