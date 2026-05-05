package com.oddlabs.util;

import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

/**
 * Non-mutable OpenGL color with unspecified color space.
 *
 * <p>ARGB/RGBA integer constants in this class are interpreted as sRGB-authored display values.
 * {@link Color} constants in this class are in sRGB space unless otherwise noted.
 *
 * <p>Only mutable colors should use concrete Linear/Standard classes.
 */
public sealed interface Color extends Vector4fc permits Color.Linear, Color.Standard {
    int TRANSPARENT_INT = 0x00_00_00_00;
    int BLACK_INT = 0xFF_00_00_00;
    int WHITE_INT = 0xFF_FF_FF_FF;
    int RED_INT = 0xFF_FF_00_00;
    int GREEN_INT = 0xFF_00_FF_00;
    int BLUE_INT = 0xFF_00_00_FF;
    int CYAN_INT = 0xFF_00_FF_FF;
    int MAGENTA_INT = 0xFF_FF_00_FF;
    int YELLOW_INT = 0xFF_FF_FF_00;

    Color BLACK = argb4v(BLACK_INT);
    Color WHITE = argb4v(WHITE_INT);
    Color DARK_RED = argb4v(0xFF_7F_00_00);
    Color DARK_GREEN = argb4v(0xFF_00_7F_00);
    Color DARK_BLUE = argb4v(0xFF_00_00_7F);
    Color RED = argb4v(RED_INT);
    Color GREEN = argb4v(GREEN_INT);
    Color BLUE = argb4v(BLUE_INT);
    Color YELLOW = argb4v(YELLOW_INT);
    Color CYAN = argb4v(CYAN_INT);
    Color MAGENTA = argb4v(MAGENTA_INT);
    Color TRANSPARENT = argb4v(TRANSPARENT_INT);

    Color BLACK_LINEAR = new Linear(BLACK);
    Color WHITE_LINEAR = new Linear(WHITE);
    Color RED_LINEAR = new Linear(RED);
    Color GREEN_LINEAR = new Linear(GREEN);
    Color BLUE_LINEAR = new Linear(BLUE);
    Color YELLOW_LINEAR = new Linear(YELLOW);
    Color TRANSPARENT_LINEAR = new Linear(TRANSPARENT);

    /**
     * The normalization factor for converting 8-bit color components to/from floats.
     */
    float NORMALIZE_8_BIT = 255.0f;

    static float toLinear(float srgb) {
        return srgb <= 0.04045f ? srgb / 12.92f : (float) Math.pow((srgb + 0.055) / 1.055, 2.4);
    }

    static float toStandard(float linear) {
        return linear <= 0.0031308f ? linear * 12.92f : 1.055f * (float) Math.pow(linear, 1.0 / 2.4) - 0.055f;
    }

    /**
     *  A mutable {@link Vector4f} OpenGL color in linear RGB space.
     */
    final class Linear extends Vector4f implements Color {
        public Linear() {
        }

        public Linear(float r, float g, float b, float a) {
            super(r, g, b, a);
        }

        public Linear(@NonNull Standard standard) {
            super(toLinear(standard.x()), toLinear(standard.y()), toLinear(standard.z()), standard.w());
        }

        public Linear(@NonNull Vector4fc color) {
            float r;
            float g;
            float b;
            switch (color) {
                case Standard standard-> {
                    r = toLinear(standard.x());
                    g = toLinear(standard.y());
                    b = toLinear(standard.z());
                }

                default -> {
                    r = color.x();
                    g = color.y();
                    b = color.z();
                }
            }
            this(r, g, b, color.w());
        }

        @Override
        public Color.Linear set(@NonNull Vector4fc color) {
            float r;
            float g;
            float b;
            switch (color) {
                case Standard standard-> {
                    r = toLinear(standard.x());
                    g = toLinear(standard.y());
                    b = toLinear(standard.z());
                }

                default -> {
                    r = color.x();
                    g = color.y();
                    b = color.z();
                }
            }
            this.set(r, g, b, color.w());

            return this;
        }
    }

    /**
     * A mutable {@link Vector4f} OpenGL color in standard RGB space.
     */
    final class Standard extends Vector4f implements Color {
        public Standard() {
        }

        public Standard(float r, float g, float b, float a) {
            super(r, g, b, a);
        }

        public Standard(@NonNull Linear linear) {
            super(toStandard(linear.x()), toStandard(linear.y()), toStandard(linear.z()), linear.w());
        }

        public Standard(@NonNull Vector4fc color) {
            float r;
            float g;
            float b;
            switch (color) {
                case Linear linear-> {
                    r = toStandard(linear.x());
                    g = toStandard(linear.y());
                    b = toStandard(linear.z());
                }

                default -> {
                    r = color.x();
                    g = color.y();
                    b = color.z();
                }
            }
            this(r, g, b, color.w());
        }

        @Override
        public Color.Standard set(@NonNull Vector4fc color) {
            float r;
            float g;
            float b;
            switch (color) {
                case Linear linear-> {
                    r = toStandard(linear.x());
                    g = toStandard(linear.y());
                    b = toStandard(linear.z());
                }

                default -> {
                    r = color.x();
                    g = color.y();
                    b = color.z();
                }
            }
            this.set(r, g, b, color.w());

            return this;
        }
    }

    /**
     * Converts a {@code Vector4fc} color to a 32-bit packed integer in AARRGGBB format
     *
     * @return A 32-bit packed integer in AARRGGBB format
     */
    static int argbi(@NonNull Vector4fc color) {
        return argbi(color.w(), color.x(), color.y(), color.z());
    }

    /**
     * Converts float colors to a 32-bit packed integer in AARRGGBB format
     *
     * @return A 32-bit packed integer in AARRGGBB format
     */
    static int argbi(float a, float r, float g, float b) {
        return argbi((byte) (a * NORMALIZE_8_BIT),
                (byte) (r * NORMALIZE_8_BIT),
                (byte) (g * NORMALIZE_8_BIT),
                (byte) (b * NORMALIZE_8_BIT));
    }

    /**
     * Converts byte colors to a 32-bit packed integer in AARRGGBB format
     *
     * @return A 32-bit packed integer in AARRGGBB format
     */
    static int argbi(byte a, byte r, byte g, byte b) {
        return (Byte.toUnsignedInt(a) << 24) | (Byte.toUnsignedInt(r) << 16) | (Byte.toUnsignedInt(g) << 8) | Byte.toUnsignedInt(b);
    }

    /**
     * Converts a {@code Vector4fc} color to packed abgr for storing little endian byte buffer
     *
     * @param color The 32-bit float color vector
     * @return A 32-bit packed integer in AABBGGRR format
     */
    static int abgri(@NonNull Vector4fc color) {
        return abgri(color.w(), color.z(), color.y(), color.x());
    }

    static int abgri(float a, float b, float g, float r) {
        return ((int) (a * NORMALIZE_8_BIT) << 24) |
                ((int) (b * NORMALIZE_8_BIT) << 16) |
                ((int) (g * NORMALIZE_8_BIT) << 8) |
                ((int) (r * NORMALIZE_8_BIT));
    }

    /**
     * Converts a 32-bit ARGB integer ({@code 0xAARRGGBB}) to a mutable {@link Standard}.
     * The output vector components are in ({@code r, g, b, a}) order as expected by OpenGL.
     *
     * <p>Input integer channel values are treated as sRGB-authored display values and normalized to
     * {@code [0, 1]} without applying sRGB-to-linear conversion.
     *
     * @param color The 32-bit ARGB integer color.
     * @return A new {@link Standard} with components ({@code r, g, b, a}), normalized to {@code [0, 1]}.
     */
    static @NonNull Standard argb4v(int color) {
        return new Standard(
                ((color >> 16) & 0xFF) / NORMALIZE_8_BIT,
                ((color >> 8) & 0xFF) / NORMALIZE_8_BIT,
                (color & 0xFF) / NORMALIZE_8_BIT,
                ((color >> 24) & 0xFF) / NORMALIZE_8_BIT);
    }

    /**
     * Converts a 32-bit RGBA integer ({@code 0xRRGGBBAA}) to a mutable {@link Standard}.
     *
     * <p>Input integer channel values are treated as sRGB-authored display values and normalized to
     * {@code [0, 1]} without applying sRGB-to-linear conversion.
     *
     * @param color The 32-bit RGBA integer color.
     * @return A new {@link Standard} with components ({@code r, g, b, a}), normalized to {@code [0, 1]}.
     */
    static @NonNull Standard rgba4v(int color) {
        return new Standard(
                ((color >> 24) & 0xFF) / NORMALIZE_8_BIT,
                ((color >> 16) & 0xFF) / NORMALIZE_8_BIT,
                ((color >> 8) & 0xFF) / NORMALIZE_8_BIT,
                (color & 0xFF) / NORMALIZE_8_BIT);
    }
}
