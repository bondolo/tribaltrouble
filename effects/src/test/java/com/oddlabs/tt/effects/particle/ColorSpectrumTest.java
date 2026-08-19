package com.oddlabs.tt.effects.particle;

import com.oddlabs.util.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link ColorSpectrum}.
 */
class ColorSpectrumTest {

    @Test
    void testColorSpectrumResolution() {
        ColorSpectrum spectrum = (pos, base) -> {
            float r = base.r() * pos;
            float g = base.g() * (1.0f - pos);
            float b = base.b();
            float a = base.a();
            return new Color.Linear(r, g, b, a);
        };

        Color.Linear baseColor = new Color.Linear(1.0f, 0.8f, 0.5f, 1.0f);
        Color.Linear resultAtZero = spectrum.getColor(0.0f, baseColor);
        assertNotNull(resultAtZero);
        assertEquals(0.0f, resultAtZero.r(), 1e-4f);
        assertEquals(0.8f, resultAtZero.g(), 1e-4f);
        assertEquals(0.5f, resultAtZero.b(), 1e-4f);
        assertEquals(1.0f, resultAtZero.a(), 1e-4f);

        Color.Linear resultAtOne = spectrum.getColor(1.0f, baseColor);
        assertNotNull(resultAtOne);
        assertEquals(1.0f, resultAtOne.r(), 1e-4f);
        assertEquals(0.0f, resultAtOne.g(), 1e-4f);
        assertEquals(0.5f, resultAtOne.b(), 1e-4f);
    }
}
