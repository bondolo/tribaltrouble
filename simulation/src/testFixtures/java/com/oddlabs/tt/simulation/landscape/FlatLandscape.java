package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.simulation.model.Terrain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/// Immutable flat landscape representation with uniform elevation and accessible terrain.
@NullMarked
public record FlatLandscape(
    int size,
    Terrain terrain,
    float[] heightmap,
    boolean[][] accessGrid,
    byte[][] buildGrid,
    List<List<SpawnLocation>> startingLocations)
    implements Landscape {

  public static FlatLandscape create(
      int size, Terrain terrain, float elevation, List<List<SpawnLocation>> startingLocations) {
    float[] heightmap = new float[size * size];
    Arrays.fill(heightmap, elevation);

    boolean[][] accessGrid = new boolean[size][size];
    for (boolean[] row : accessGrid) {
      Arrays.fill(row, true);
    }

    byte[][] buildGrid = new byte[size][size];
    for (byte[] row : buildGrid) {
      Arrays.fill(row, (byte) 10);
    }

    return new FlatLandscape(size, terrain, heightmap, accessGrid, buildGrid, startingLocations);
  }

  public static FlatLandscape create(int size, List<List<SpawnLocation>> startingLocations) {
    return create(size, Terrain.NATIVE, 5.0f, startingLocations);
  }

  public static FlatLandscape create(int size) {
    return create(size, generateStartingLocations(2, 20));
  }

  public static FlatLandscape withHeightmap(float[] heightmap, int size, Terrain terrain) {
    boolean[][] accessGrid = new boolean[size][size];
    return new FlatLandscape(size, terrain, heightmap, accessGrid, new byte[0][0], List.of());
  }

  public static FlatLandscape withHeightmap(float[] heightmap, int size) {
    return withHeightmap(heightmap, size, Terrain.NATIVE);
  }

  public static List<List<SpawnLocation>> generateStartingLocations(int numPlayers, int locationsPerPlayer) {
    return generateStartingLocations(numPlayers, locationsPerPlayer, 10f, 20f);
  }

  public static List<List<SpawnLocation>> generateStartingLocations(
      int numPlayers, int locationsPerPlayer, float base0, float base1) {
    List<List<SpawnLocation>> startingLocations = new ArrayList<>(numPlayers);
    for (int p = 0; p < numPlayers; p++) {
      List<SpawnLocation> playerLocations = new ArrayList<>(locationsPerPlayer);
      float baseX = (p == 0) ? base0 : base1;
      float baseY = (p == 0) ? base0 : base1;
      for (int i = 0; i < locationsPerPlayer; i++) {
        playerLocations.add(new SpawnLocation(baseX + (i % 5) * 1.5f, baseY + (i / 5f) * 1.5f));
      }
      startingLocations.add(List.copyOf(playerLocations));
    }
    return List.copyOf(startingLocations);
  }

  public static List<List<SpawnLocation>> generateSteppedStartingLocations(
      int numPlayers, int locationsPerPlayer, float startCoord, float stepCoord) {
    List<List<SpawnLocation>> startingLocations = new ArrayList<>(numPlayers);
    for (int p = 0; p < numPlayers; p++) {
      List<SpawnLocation> playerLocations = new ArrayList<>(locationsPerPlayer);
      float baseX = startCoord + (p * stepCoord);
      float baseY = startCoord + (p * stepCoord);
      for (int i = 0; i < locationsPerPlayer; i++) {
        playerLocations.add(new SpawnLocation(baseX + (i % 5) * 1.5f, baseY + (i / 5f) * 1.5f));
      }
      startingLocations.add(List.copyOf(playerLocations));
    }
    return List.copyOf(startingLocations);
  }

  @Override
  public int metersPerWorld() {
    return size * HeightMap.METERS_PER_UNIT_GRID;
  }

  @Override
  public IslandConfig config() {
    return new IslandConfig(terrain, metersPerWorld(), 0.5f, 0.5f, 0.5f, 42);
  }
}
