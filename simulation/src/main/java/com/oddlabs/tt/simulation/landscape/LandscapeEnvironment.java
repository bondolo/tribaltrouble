package com.oddlabs.tt.simulation.landscape;

/// Composite environment interface providing height querying, dimensional bounds, and visual client state.
public interface LandscapeEnvironment {
  /// Returns the nearest terrain height at world coordinates (x, y).
  float getHeight(float x, float y);

  int getMetersPerWorld();

  float getSeaLevelMeters();

  int getPatchesPerWorld();

  int getMetersPerPatch();

  int getGridUnitsPerWorld();

  boolean isBelowSeaLevel(int patchX, int patchY);
}
