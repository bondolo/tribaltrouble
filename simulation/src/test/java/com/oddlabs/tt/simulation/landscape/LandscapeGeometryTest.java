package com.oddlabs.tt.simulation.landscape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.base.geom.BoundsProvider;
import com.oddlabs.tt.simulation.model.Terrain;
import org.junit.jupiter.api.Test;

/// Unit tests for {@link LandscapeGeometry}.
class LandscapeGeometryTest {

  @Test
  void testLandscapeGeometryLoading() {
    LandscapeGeometry geometry = new LandscapeGeometry();

    for (int i = 0; i < LandscapeBoundsProvider.SUPPLY_FRAGMENT_COUNT; i++) {
      BoundsProvider rockBounds = geometry.getRockBounds(i);
      assertNotNull(rockBounds);
      BoundingBox[] rockBoxes = rockBounds.bounds();
      assertNotNull(rockBoxes);
      assertTrue(rockBoxes.length > 0);

      BoundsProvider ironBounds = geometry.getIronBounds(i);
      assertNotNull(ironBounds);
      BoundingBox[] ironBoxes = ironBounds.bounds();
      assertNotNull(ironBoxes);
      assertTrue(ironBoxes.length > 0);
    }

    for (Terrain terrain : new Terrain[] {Terrain.NATIVE, Terrain.VIKING}) {
      for (int i = 0; i < Landscape.NUM_PLANT_TYPES; i++) {
        BoundsProvider plantBounds = geometry.getPlantBounds(terrain, i);
        assertNotNull(plantBounds);
        BoundingBox[] plantBoxes = plantBounds.bounds();
        assertNotNull(plantBoxes);
        assertTrue(plantBoxes.length > 0);
      }
    }

    BoundsProvider chickenBounds = geometry.getChickenBounds();
    assertNotNull(chickenBounds);
    BoundingBox[] chickenBoxes = chickenBounds.bounds();
    assertNotNull(chickenBoxes);
    assertTrue(chickenBoxes.length > 0);
  }

  @Test
  void testLandscapeDefaultPlantsReturnsArrayOfEmptyLists() {
    Landscape landscape = FlatLandscape.create(64);
    var plants = landscape.plants();
    assertNotNull(plants);
    assertEquals(Landscape.NUM_PLANT_TYPES, plants.length);
    for (int i = 0; i < Landscape.NUM_PLANT_TYPES; i++) {
      assertNotNull(plants[i]);
      assertTrue(plants[i].isEmpty());
    }
  }
}
