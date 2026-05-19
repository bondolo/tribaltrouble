package com.oddlabs.tt.render;

import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;

/**
 * A monitor display mode (resolution, bit depth, refresh rate)
 */
public final class SerializableDisplayMode implements Serializable, Comparable<SerializableDisplayMode> {
    @Serial
    private static final long serialVersionUID = 1;

    public static final int MIN_WIDTH = 1024;
    public static final int MIN_HEIGHT = 768;
    public static final int MIN_FREQ = 24; // may also be zero for "unkown"
    public static final int MIN_BPP = 8;

    private static final SerializableDisplayMode DEFAULT_MODE = new SerializableDisplayMode(0, 0, 0, 0);

    private static final Comparator<SerializableDisplayMode> COMPARATOR = Comparator
            .comparingInt(SerializableDisplayMode::getDistanceFromBestMode)
            .thenComparingInt(m -> m.getBitsPerPixel() - DEFAULT_MODE.getBitsPerPixel())
            .thenComparingInt(m -> m.getFrequency() - DEFAULT_MODE.getFrequency());

    private final int width;
    private final int height;
    private final int freq;
    private final int bpp;

    public SerializableDisplayMode(int width, int height, int bpp, int freq) {
        this.width = width;
        this.height = height;
        this.bpp = bpp;
        this.freq = freq;
    }

    @Override
    public int compareTo(@NonNull SerializableDisplayMode o) {
        /*
         * Elias: sort after largest bpp first, then lowest freq
         * to accommodate broken monitors lying about their
         * capabilities
         */
        return COMPARATOR.compare(this, o);
    }

    private static int getDistanceFromBestMode(@NonNull SerializableDisplayMode mode) {
        int dx = Math.abs(DEFAULT_MODE.getWidth() - mode.getWidth());
        int dy = Math.abs(DEFAULT_MODE.getHeight() - mode.getHeight());
        return dx + dy;
    }


    public static boolean isModeValid(@NonNull SerializableDisplayMode mode) {
        return mode.getWidth() >= MIN_WIDTH &&
                mode.getHeight() >= MIN_HEIGHT
                && mode.getBitsPerPixel() >= MIN_BPP
                && (mode.getFrequency() == 0 || mode.getFrequency() >= MIN_FREQ);
    }

    @Override
    public @NonNull String toString() {
        return width + "x" + height + " " + bpp + "bit " + freq + "Hz";
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFrequency() {
        return freq;
    }

    public int getBitsPerPixel() {
        return bpp;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SerializableDisplayMode other_mode &&
                isEquivalent(other_mode) && getFrequency() == other_mode.getFrequency() &&
                getBitsPerPixel() == other_mode.getBitsPerPixel();
    }

    public boolean isEquivalent(@NonNull SerializableDisplayMode other_mode) {
        return getWidth() == other_mode.getWidth() && getHeight() == other_mode.getHeight();
    }

    @Override
    public int hashCode() {
        return width ^ height ^ freq ^ bpp;
    }
}