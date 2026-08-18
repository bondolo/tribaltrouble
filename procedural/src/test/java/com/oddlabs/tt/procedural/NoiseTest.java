package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class NoiseTest {

    @Test
    void testMidpoint() {
        Midpoint midpoint = new Midpoint(64, 4, 0.5f, 42);
        Channel channel = midpoint.toChannel();
        assertNotNull(channel);
        assertEquals(64, channel.width);
        assertEquals(64, channel.height);
    }

    @Test
    void testPerlin() {
        Perlin perlin = new Perlin(64, 64, 4, 4, 0.5f, 2, 42, Perlin.Interpolation.CUBIC, Perlin.Summation.NORMAL);
        Channel channel = perlin.toChannel();
        assertNotNull(channel);
        assertEquals(64, channel.width);
        assertEquals(64, channel.height);
    }

    @Test
    void testVoronoi() {
        Voronoi voronoi = new Voronoi(64, 4, 4, 1, 1f, 42);
        Channel distance = voronoi.getDistance(-1f, 1f, 0f);
        assertNotNull(distance);
        assertEquals(64, distance.width);
        assertEquals(64, distance.height);
    }
}
