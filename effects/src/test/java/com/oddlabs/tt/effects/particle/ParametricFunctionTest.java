package com.oddlabs.tt.effects.particle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link StunFunction} and {@link CloudFunction}.
 */
class ParametricFunctionTest {

    @Test
    void testStunFunction() {
        StunFunction func = new StunFunction(5.0f, 2.0f);
        assertEquals(5.0f, func.getX(0.0f, 0.0f), 1e-4f);
        assertEquals(0.0f, func.getY(0.0f, 0.0f), 1e-4f);
        assertEquals(2.0f, func.getZ(0.0f, 0.0f), 1e-4f);

        assertEquals(0.0f, func.getX((float) (Math.PI / 2.0), 0.0f), 1e-4f);
        assertEquals(5.0f, func.getY((float) (Math.PI / 2.0), 0.0f), 1e-4f);
        assertEquals(0.0f, func.getZ(0.0f, (float) (Math.PI / 2.0)), 1e-4f);
    }

    @Test
    void testCloudFunction() {
        CloudFunction func = new CloudFunction(3.0f, 4.0f);
        assertEquals(func.getRadiusZ(), 4.0f, 1e-4f);
        assertEquals(0.0f, func.getX(0.0f, 0.0f), 1e-4f);
        assertEquals(0.0f, func.getY(0.0f, 0.0f), 1e-4f);
        assertEquals(4.0f * CloudFunction.TOP_PUFFINESS_PEAK, func.getZ(0.0f, 0.0f), 1e-4f);
    }
}
