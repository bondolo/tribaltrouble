package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.base.geom.BoundsProvider;
import com.oddlabs.tt.base.geom.SpriteGeometry;
import com.oddlabs.tt.simulation.model.Terrain;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.stream.IntStream;

/**
 * Pure CPU landscape geometry provider supplying physical bounding boxes for rocks, iron, plants, and chickens.
 */
public final class LandscapeGeometry implements LandscapeBoundsProvider {
    private final SpriteGeometry[] rock_fragment_bounds;
    private final SpriteGeometry[] iron_fragment_bounds;
    private final EnumMap<Terrain, SpriteGeometry[]> plant_bounds;
    private final SpriteGeometry chicken_bounds;

    public LandscapeGeometry() {
        this.rock_fragment_bounds = IntStream.rangeClosed(1, SUPPLY_FRAGMENT_COUNT)
                .mapToObj(i -> String.format("/geometry/misc/rock_%d.binsprite", i))
                .map(SpriteGeometry::load)
                .toArray(SpriteGeometry[]::new);

        this.iron_fragment_bounds = Arrays.copyOf(rock_fragment_bounds, rock_fragment_bounds.length);

        this.plant_bounds = new EnumMap<>(Terrain.class);
        this.plant_bounds.put(
                Terrain.NATIVE,
                IntStream.rangeClosed(1, 4)
                        .mapToObj(i -> String.format("/geometry/misc/plant_%d.binsprite", i))
                        .map(SpriteGeometry::load)
                        .toArray(SpriteGeometry[]::new)
        );
        this.plant_bounds.put(
                Terrain.VIKING,
                IntStream.rangeClosed(1, 4)
                        .mapToObj(i -> String.format("/geometry/misc/viking_plant_%d.binsprite", i))
                        .map(SpriteGeometry::load)
                        .toArray(SpriteGeometry[]::new)
        );

        this.chicken_bounds = SpriteGeometry.load("/geometry/misc/chicken.binsprite");
    }

    @Override
    public BoundsProvider getRockBounds(int index) {
        return rock_fragment_bounds[index % rock_fragment_bounds.length];
    }

    @Override
    public BoundsProvider getIronBounds(int index) {
        return iron_fragment_bounds[index % iron_fragment_bounds.length];
    }

    @Override
    public BoundsProvider getPlantBounds(Terrain terrain, int index) {
        SpriteGeometry[] bounds = plant_bounds.get(terrain);
        return bounds[index % bounds.length];
    }

    @Override
    public BoundsProvider getChickenBounds() {
        return chicken_bounds;
    }
}
