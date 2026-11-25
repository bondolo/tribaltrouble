package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.base.geom.BoundsProvider;
import com.oddlabs.tt.base.geom.SpriteGeometry;
import com.oddlabs.tt.simulation.landscape.AbstractTreeGroup.TreeType;
import com.oddlabs.tt.simulation.model.Terrain;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Objects;
import java.util.stream.IntStream;

/// Pure CPU landscape geometry provider supplying physical bounding boxes for rocks, iron, plants, chickens, and trees.
public final class LandscapeGeometry implements LandscapeBoundsProvider {
  private final SpriteGeometry[] rock_fragment_bounds;
  private final SpriteGeometry[] iron_fragment_bounds;
  private final EnumMap<Terrain, SpriteGeometry[]> plant_bounds;
  private final SpriteGeometry chicken_bounds;
  private final EnumMap<TreeType, BoundsProvider> tree_bounds;

  public LandscapeGeometry() {
    this.rock_fragment_bounds =
        IntStream.rangeClosed(1, SUPPLY_FRAGMENT_COUNT)
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
            .toArray(SpriteGeometry[]::new));
    this.plant_bounds.put(
        Terrain.VIKING,
        IntStream.rangeClosed(1, 4)
            .mapToObj(i -> String.format("/geometry/misc/viking_plant_%d.binsprite", i))
            .map(SpriteGeometry::load)
            .toArray(SpriteGeometry[]::new));

    this.chicken_bounds = SpriteGeometry.load("/geometry/misc/chicken.binsprite");

    this.tree_bounds = new EnumMap<>(TreeType.class);
    for (TreeType type : TreeType.values()) {
      SpriteGeometry trunk = SpriteGeometry.load(trunkPath(type));
      SpriteGeometry crown = SpriteGeometry.load(crownPath(type));
      BoundingBox combined = new BoundingBox();
      combined.setBounds(trunk.bounds()[0]);
      combined.expandBounds(crown.bounds()[0]);
      combined.expandBoundsZ(0f);
      BoundingBox[] boxArray = new BoundingBox[] {combined};
      this.tree_bounds.put(type, () -> boxArray);
    }
  }

  public static String trunkPath(TreeType type) {
    return switch (type) {
      case JUNGLE -> "/geometry/misc/jungle_tree_trunk.binsprite";
      case PALM -> "/geometry/misc/palm_trunk.binsprite";
      case OAK -> "/geometry/misc/oak_tree_trunk.binsprite";
      case PINE -> "/geometry/misc/pine_tree_trunk.binsprite";
    };
  }

  public static String crownPath(TreeType type) {
    return switch (type) {
      case JUNGLE -> "/geometry/misc/jungle_tree_crown.binsprite";
      case PALM -> "/geometry/misc/palm_crown.binsprite";
      case OAK -> "/geometry/misc/oak_tree_crown.binsprite";
      case PINE -> "/geometry/misc/pine_tree_crown.binsprite";
    };
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

  @Override
  public BoundsProvider getTreeBounds(TreeType type) {
    return Objects.requireNonNull(tree_bounds.get(type), () -> "Missing tree bounds for " + type);
  }
}
