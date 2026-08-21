package com.oddlabs.util;


import java.io.Serializable;
import java.nio.FloatBuffer;

/**
 * Immutable representations of OpenGL colors in standard (sRGB) and linear RGB spaces,
 * along with a distinct type for linear color differences.
 */
public sealed interface Color extends Serializable permits Color.Linear, Color.Standard, Color.LinearDelta {
    // ARGB SRGB constants
    int TRANSPARENT_INT = 0x00_00_00_00;
    int BLACK_INT = 0xFF_00_00_00;
    int WHITE_INT = 0xFF_FF_FF_FF;
    int RED_INT = 0xFF_FF_00_00;
    int GREEN_INT = 0xFF_00_FF_00;
    int BLUE_INT = 0xFF_00_00_FF;
    int CYAN_INT = 0xFF_00_FF_FF;
    int MAGENTA_INT = 0xFF_FF_00_FF;
    int YELLOW_INT = 0xFF_FF_FF_00;

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

    float r();

    float g();

    float b();

    float a();

    /** stores color in the provided float buffer in RGBA order */
    default void get(int offset, FloatBuffer dest) {
        dest.put(offset++, r());
        dest.put(offset++, g());
        dest.put(offset++, b());
        dest.put(offset, a());
    }

    default Color desaturate(float factor) {
        return this;
    }

    default Color saturate(float factor) {
        return this;
    }

    default Color alpha(float alpha) {
        return this;
    }

    /**
     * An immutable representation of a color in linear RGB space.
     */
    record Linear(float r, float g, float b, float a) implements Color {
        public static final Color.Linear BLACK = new Linear(Standard.BLACK);
        public static final Color.Linear WHITE = new Linear(Standard.WHITE);
        public static final Color.Linear RED = new Linear(Standard.RED);
        public static final Color.Linear GREEN = new Linear(Standard.GREEN);
        public static final Color.Linear BLUE = new Linear(Standard.BLUE);
        public static final Color.Linear YELLOW = new Linear(Standard.YELLOW);
        public static final Color.Linear TRANSPARENT = new Linear(Standard.TRANSPARENT);

        public Linear(Color color) {
            this(
                    color instanceof Standard ? toLinear(color.r()) : color.r(),
                    color instanceof Standard ? toLinear(color.g()) : color.g(),
                    color instanceof Standard ? toLinear(color.b()) : color.b(),
                    color.a()
            );
        }

        public Linear(float gray, float alpha) {
            this(gray, gray, gray, alpha);
        }

        public Linear add(Color.LinearDelta delta) {
            return new Linear(r + delta.r(), g + delta.g(), b + delta.b(), a + delta.a());
        }

        public Linear sub(Color.LinearDelta delta) {
            return new Linear(r - delta.r(), g - delta.g(), b - delta.b(), a - delta.a());
        }

        public Linear mul(float factor) {
            return new Linear(r * factor, g * factor, b * factor, a * factor);
        }

        public Linear mul(Color.Linear other) {
            return new Linear(r * other.r(), g * other.g(), b * other.b(), a * other.a());
        }

        @Override
        public Linear alpha(float alpha) {
            return new Linear(r, g, b, alpha);
        }

        /**
         * Linearly interpolates between this color and another color.
         *
         * @param other destination color to interpolate towards
         * @param t interpolation factor, typically in [0, 1]
         * @return a new Linear color representing the interpolated state
         */
        public Linear lerp(Color.Linear other, float t) {
            return new Linear(
                    r + (other.r() - r) * t,
                    g + (other.g() - g) * t,
                    b + (other.b() - b) * t,
                    a + (other.a() - a) * t
            );
        }

        public LinearDelta delta(Color.Linear other) {
            return new LinearDelta(r - other.r(), g - other.g(), b - other.b(), a - other.a());
        }

        @Override
        public Linear desaturate(float factor) {
            float gray = 0.2126f * r + 0.7152f * g + 0.0722f * b;
            return this.lerp(new Linear(gray, a), factor);
        }

        @Override
        public Linear saturate(float factor) {
            return desaturate(-factor);
        }
    }

    /**
     * An immutable representation of a color in standard sRGB space.
     */
    record Standard(float r, float g, float b, float a) implements Color {
        public static final Color.Standard BLACK = new Color.Standard(BLACK_INT);
        public static final Color.Standard WHITE = new Color.Standard(WHITE_INT);
        public static final Color.Standard DARK_RED = new Color.Standard(0xFF_7F_00_00);
        public static final Color.Standard DARK_GREEN = new Color.Standard(0xFF_00_7F_00);
        public static final Color.Standard DARK_BLUE = new Color.Standard(0xFF_00_00_7F);
        public static final Color.Standard RED = new Color.Standard(RED_INT);
        public static final Color.Standard GREEN = new Color.Standard(GREEN_INT);
        public static final Color.Standard BLUE = new Color.Standard(BLUE_INT);
        public static final Color.Standard YELLOW = new Color.Standard(YELLOW_INT);
        public static final Color.Standard CYAN = new Color.Standard(CYAN_INT);
        public static final Color.Standard MAGENTA = new Color.Standard(MAGENTA_INT);
        public static final Color.Standard TRANSPARENT = new Color.Standard(TRANSPARENT_INT);

        public Standard(Color color) {
            this(
                    color instanceof Linear ? toStandard(color.r()) : color.r(),
                    color instanceof Linear ? toStandard(color.g()) : color.g(),
                    color instanceof Linear ? toStandard(color.b()) : color.b(),
                    color.a()
            );
        }

        /**
         * @param color The 32-bit ARGB integer color.
         */
        public Standard(int color) {
            this(((color >> 16) & 0xFF) / NORMALIZE_8_BIT,
                    ((color >> 8) & 0xFF) / NORMALIZE_8_BIT,
                    (color & 0xFF) / NORMALIZE_8_BIT,
                    ((color >> 24) & 0xFF) / NORMALIZE_8_BIT);
        }

        public Standard(float gray, float alpha) {
            this(gray, gray, gray, alpha);
        }

        public int toInt() {
            return (Math.clamp(Math.round(a() * NORMALIZE_8_BIT), 0, 255) << 24) |
                    (Math.clamp(Math.round(r() * NORMALIZE_8_BIT), 0, 255) << 16) |
                    (Math.clamp(Math.round(g() * NORMALIZE_8_BIT), 0, 255) << 8) |
                    Math.clamp(Math.round(b() * NORMALIZE_8_BIT), 0, 255);
        }

        @Override
        public Standard alpha(float alpha) {
            return new Standard(r, g, b, alpha);
        }

        public LinearDelta delta(Color.Standard other) {
            return new LinearDelta(r - other.r(), g - other.g(), b - other.b(), a - other.a());
        }

        public Linear linear() {
            return new Linear(Color.toLinear(r()), Color.toLinear(g()), Color.toLinear(b()), a());
        }

        @Override
        public Standard desaturate(float factor) {
            return new Standard(this.linear().desaturate(factor));
        }

        @Override
        public Standard saturate(float factor) {
            return new Standard(this.linear().saturate(factor));
        }

        /**
         * Converts hue, saturation, and brightness values to an sRGB color.
         *
         * @param hue the hue component, typically in range [0, 1]
         * @param saturation the saturation component, typically in range [0, 1]
         * @param brightness the brightness component, typically in range [0, 1]
         * @return a new standard sRGB color with alpha set to 1.0
         */
        public static Color.Standard hsbToRgb(float hue, float saturation, float brightness) {
            float r = 0, g = 0, b = 0;
            if (saturation == 0) {
                r = g = b = brightness;
            } else {
                float h = (hue - (float) Math.floor(hue)) * 6.0f;
                float f = h - (float) Math.floor(h);
                float p = brightness * (1.0f - saturation);
                float q = brightness * (1.0f - saturation * f);
                float t = brightness * (1.0f - saturation * (1.0f - f));
                switch ((int) h) {
                    case 0 -> {
                        r = brightness;
                        g = t;
                        b = p;
                    }
                    case 1 -> {
                        r = q;
                        g = brightness;
                        b = p;
                    }
                    case 2 -> {
                        r = p;
                        g = brightness;
                        b = t;
                    }
                    case 3 -> {
                        r = p;
                        g = q;
                        b = brightness;
                    }
                    case 4 -> {
                        r = t;
                        g = p;
                        b = brightness;
                    }
                    case 5 -> {
                        r = brightness;
                        g = p;
                        b = q;
                    }
                }
            }
            return new Color.Standard(r, g, b, 1.0f);
        }

        /**
         * Converts the standard sRGB color values to hue, saturation, and brightness.
         *
         * @param color the standard sRGB color to convert
         * @return a new 3-element float array containing the HSB values in order:
         *         index 0: hue (in range [0, 1]),
         *         index 1: saturation (in range [0, 1]),
         *         index 2: brightness (in range [0, 1])
         */
        public static float[] rgbToHsb(Color.Standard color) {
            float r = color.r();
            float g = color.g();
            float b = color.b();
            float hue, saturation, brightness;
            float[] vals = new float[3];
            float cmax = Math.max(Math.max(r, g), b);
            float cmin = Math.min(Math.min(r, g), b);

            brightness = cmax;
            saturation = cmax != 0 ? (cmax - cmin) / cmax : 0;
            if (saturation == 0) {
                hue = 0;
            } else {
                float redc = (cmax - r) / (cmax - cmin);
                float greenc = (cmax - g) / (cmax - cmin);
                float bluec = (cmax - b) / (cmax - cmin);
                if (r == cmax) {
                    hue = bluec - greenc;
                } else if (g == cmax) {
                    hue = 2.0f + redc - bluec;
                } else {
                    hue = 4.0f + greenc - redc;
                }
                hue = hue / 6.0f;
                if (hue < 0) {
                    hue = hue + 1.0f;
                }
            }
            vals[0] = hue;
            vals[1] = saturation;
            vals[2] = brightness;
            return vals;
        }
    }

    /**
     * An immutable representation of a linear color difference/delta.
     */
    record LinearDelta(float r, float g, float b, float a) implements Color {
        public static final Color.LinearDelta ZERO = new LinearDelta(0, 0, 0, 0);

        public LinearDelta(float gray, float alpha) {
            this(gray, gray, gray, alpha);
        }

        public static LinearDelta red(float r) {
            return new LinearDelta(r, 0f, 0f, 0f);
        }

        public LinearDelta mul(float factor) {
            return new LinearDelta(r * factor, g * factor, b * factor, a * factor);
        }

        public LinearDelta add(Color.LinearDelta delta) {
            return new LinearDelta(r + delta.r, g + delta.g, b + delta.b, a + delta.a);
        }

        public LinearDelta sub(Color.LinearDelta delta) {
            return new LinearDelta(r - delta.r, g - delta.g, b - delta.b, a - delta.a);
        }

        @Override
        public LinearDelta alpha(float alpha) {
            return new LinearDelta(r, g, b, alpha);
        }

        public LinearDelta negate() {
            return new LinearDelta(-r, -g, -b, -a);
        }
    }
}
