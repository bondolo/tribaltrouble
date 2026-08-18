package com.oddlabs.tt.engine.render.state;

import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.util.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FogInfoTest {

    @Test
    void testDistanceFogInfoNative() {
        DistanceFogInfo fog = DistanceFogInfo.forTerrain(Terrain.NATIVE, 512);
        assertNotNull(fog);
        assertEquals(FogInfo.Mode.EXP2, fog.getMode());
        assertTrue(fog.isEnabled());
        assertEquals(0.001f, fog.getDensity(), 0.0001f);
        assertEquals(1.2f * 512, fog.getHeightFactor(), 0.001f);
        assertEquals(0f, fog.getStart(), 0.0001f);
        assertEquals(128f, fog.getEnd(), 0.0001f);
        assertNotNull(fog.getColor());
    }

    @Test
    void testDistanceFogInfoViking() {
        DistanceFogInfo fog = DistanceFogInfo.forTerrain(Terrain.VIKING, 1024);
        assertNotNull(fog);
        assertEquals(FogInfo.Mode.EXP2, fog.getMode());
        assertTrue(fog.isEnabled());
        assertEquals(0.0015f, fog.getDensity(), 0.0001f);
        assertEquals(1.4f * 1024, fog.getHeightFactor(), 0.001f);
        assertEquals(0f, fog.getStart(), 0.0001f);
        assertEquals(256f, fog.getEnd(), 0.0001f);
        assertNotNull(fog.getColor());
    }

    @Test
    void testRadialFogInfo() {
        RadialFogInfo fog = new RadialFogInfo(Color.Standard.WHITE, 0.25f, 1.5f);
        assertEquals(FogInfo.Mode.RADIAL, fog.getMode());
        assertEquals(0.25f, fog.getDensity(), 0.0001f);
        assertEquals(1.5f, fog.getRadiusScale(), 0.0001f);
        assertTrue(fog.isEnabled());

        fog.setEnabled(false);
        assertFalse(fog.isEnabled());
    }
}
