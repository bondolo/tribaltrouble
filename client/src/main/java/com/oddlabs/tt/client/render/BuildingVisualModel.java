package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.TextureKey;
import com.oddlabs.tt.simulation.model.Building;

/// {@link VisualModel} implementation for buildings managing damage and production accessories.
final class BuildingVisualModel extends SelectableVisualModel<Building> {
  BuildingVisualModel(
      Building building,
      AudioImplementation audio,
      TextureKey[] smokeTextures,
      TextureKey[] damageSmokeTextures,
      SpriteKey[] woodFragments) {
    super(building);
    float hitOffsetZ = building.getHitOffsetZ();
    addAccessory(
        new BuildingDamagedAccessory(building, hitOffsetZ, audio, damageSmokeTextures, smokeTextures, woodFragments));
    addAccessory(new BuildingProductionAccessory(building, audio, smokeTextures));
  }
}
