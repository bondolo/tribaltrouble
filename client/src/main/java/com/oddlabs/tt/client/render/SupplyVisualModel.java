package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.simulation.model.SupplyModel;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

/// Visual model interface for supply objects managing spawn animations, transforms, and rendering effects.
interface SupplyVisualModel extends VisualModel {
  SpriteKey getSpriteKey();

  /// Properties defining the drop shadow for a supply model.
  record ShadowProperties(float diameter, float opacity, float verticalCenter) {}

  /// Properties defining the ground crack decal for a spawning supply model.
  record DecalProperties(Color.@Nullable Linear color, float opacity, float diameter, float pattern) {
    static final DecalProperties NONE = new DecalProperties(null, 0.0f, 0.0f, 0.0f);
  }

  @Override
  SupplyModel getModel();

  float getOffsetZ();

  float getRotation();

  Color.@Nullable Linear getSpawnColorTint();

  ShadowProperties getShadowProperties();

  DecalProperties getDecalProperties();

  void setSpawnProgress(float progress);

  void completeSpawn();

  boolean isSpawning();

  float getSpawnProgress();

  float getSpawnDuration();
}
