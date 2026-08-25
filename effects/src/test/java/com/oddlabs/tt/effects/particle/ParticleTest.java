package com.oddlabs.tt.effects.particle;

import com.oddlabs.tt.procedural.GeneratedLandscapeData;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.LandscapeBoundsProvider;
import com.oddlabs.tt.simulation.landscape.LandscapeData;
import com.oddlabs.tt.simulation.landscape.NotificationListener;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.base.geom.BoundsProvider;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.util.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link Particle} and {@link LinearParticle}.
 */
class ParticleTest {

    private static World world;

    @BeforeAll
    static void setUpWorld() {
        LandscapeBoundsProvider boundsProvider = new LandscapeBoundsProvider() {
            private final BoundsProvider stub = () -> new BoundingBox[]{new BoundingBox()};

            @Override
            public BoundsProvider getRockBounds(int index) {
                return stub;
            }

            @Override
            public BoundsProvider getIronBounds(int index) {
                return stub;
            }

            @Override
            public BoundsProvider getPlantBounds(Terrain terrain, int index) {
                return stub;
            }

            @Override
            public BoundsProvider getChickenBounds() {
                return stub;
            }
        };

        IslandConfig config = new IslandConfig(Terrain.NATIVE, 256, 0.5f, 0.5f, 0.5f, 42);
        Landscape landscape = new Landscape(2, config, 0.5f, 5, 0.5f);
        LandscapeData data = new GeneratedLandscapeData(config, landscape);
        WorldParameters params = new WorldParameters(0, "test", 0, 10);
        world = World.newWorld(boundsProvider, null, new NotificationListener() {
        }, params, data, List.of(), new Color.Linear[0], false);
    }

    @Test
    void testParticleUVCoordinates() {
        Particle particle = new Particle(world, 0f);

        assertEquals(0.0f, particle.getU1(), 1e-4f);
        assertEquals(1.0f, particle.getV1(), 1e-4f);
        assertEquals(1.0f, particle.getU2(), 1e-4f);
        assertEquals(1.0f, particle.getV2(), 1e-4f);
        assertEquals(1.0f, particle.getU3(), 1e-4f);
        assertEquals(0.0f, particle.getV3(), 1e-4f);
        assertEquals(0.0f, particle.getU4(), 1e-4f);
        assertEquals(0.0f, particle.getV4(), 1e-4f);
    }

    @Test
    void testLinearParticleKinematics() {
        LinearParticle particle = new LinearParticle(world, 0f);
        particle.setPos(10f, 20f, 30f);
        particle.setVelocity(1f, 2f, 3f);
        particle.setAcceleration(0.5f, -0.5f, 0f);
        particle.setEnergy(5f);
        particle.setColor(new Color.Linear(1f, 1f, 1f, 1f));
        particle.setDeltaColor(new Color.LinearDelta(-0.1f, -0.1f, -0.1f, -0.1f));

        particle.update(1.0f);

        // Velocity should become (1.5, 1.5, 3.0)
        assertEquals(1.5f, particle.getVelocityX(), 1e-4f);
        assertEquals(1.5f, particle.getVelocityY(), 1e-4f);
        assertEquals(3.0f, particle.getVelocityZ(), 1e-4f);

        // Position should update to (10 + 1.5, 20 + 1.5, 30 + 3.0) = (11.5, 21.5, 33.0)
        assertEquals(11.5f, particle.getPosX(), 1e-4f);
        assertEquals(21.5f, particle.getPosY(), 1e-4f);
        assertEquals(33.0f, particle.getPosZ(), 1e-4f);

        // Energy should decrease by t = 1.0
        assertEquals(4.0f, particle.getEnergy(), 1e-4f);

        // Color should update
        assertEquals(0.9f, particle.getColor().r(), 1e-4f);
        assertEquals(0.9f, particle.getColor().g(), 1e-4f);
        assertEquals(0.9f, particle.getColor().b(), 1e-4f);
        assertEquals(0.9f, particle.getColor().a(), 1e-4f);
    }
}
