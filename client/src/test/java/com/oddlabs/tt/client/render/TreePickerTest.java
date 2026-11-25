package com.oddlabs.tt.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.simulation.landscape.AbstractTreeGroup.TreeType;
import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class TreePickerTest {
  private static final float CAMERA_Z = 50f;
  private static final float DEFAULT_NEAR = 1f;
  private static final float DEFAULT_FAR = 100f;

  private World world;

  @BeforeEach
  void setUp() {
    int gridSize = 64;
    Landscape landscape = FlatLandscape.create(gridSize);
    WorldParameters worldParams = new WorldParameters("1", 5, 20);
    List<PlayerInfo> playerInfos = List.of(new PlayerInfo(0, Race.VIKINGS, "Player"));
    Color.Linear[] playerColors = new Color.Linear[] {new Color.Linear(1f, 0f, 0f, 1f)};
    this.world = new World(worldParams, landscape, PlayerConfig.from(playerInfos, playerColors));
  }

  private static CameraState createDownwardOrthoCamera(float centerX, float centerY, float halfExtent) {
    return createDownwardOrthoCamera(centerX, centerY, halfExtent, DEFAULT_NEAR, DEFAULT_FAR);
  }

  private static CameraState createDownwardOrthoCamera(
      float centerX, float centerY, float halfExtent, float near, float far) {
    CameraState camera = new CameraState();
    camera.setCamera(centerX, centerY, CAMERA_Z, CameraState.MIN_ANGLE, 0f);
    Matrix4f proj = new Matrix4f().ortho(-halfExtent, halfExtent, -halfExtent, halfExtent, near, far);
    camera.setView(proj, 100, 100);
    return camera;
  }

  private List<TreeSupply> pick(CameraState camera) {
    List<TreeSupply> results = new ArrayList<>();
    TreePicker.pick(world, camera, results);
    return results;
  }

  private TreeSupply spawnTree(TreeType type, int gridX, int gridY) {
    float x = UnitGrid.coordinateFromGrid(gridX);
    float y = UnitGrid.coordinateFromGrid(gridY);
    return world.getTreeRoot().spawnTree(world, type, x, y);
  }

  @Test
  void liveTreesPickedInsideFrustumAndInNoDetailMode() {
    TreeSupply jungle = spawnTree(TreeType.JUNGLE, 10, 10);
    TreeSupply palm = spawnTree(TreeType.PALM, 20, 20);
    TreeSupply oak = spawnTree(TreeType.OAK, 30, 30);
    TreeSupply pine = spawnTree(TreeType.PINE, 40, 40);

    assertEquals(List.of(jungle), pick(createDownwardOrthoCamera(jungle.getPositionX(), jungle.getPositionY(), 3f)));
    assertEquals(List.of(palm), pick(createDownwardOrthoCamera(palm.getPositionX(), palm.getPositionY(), 3f)));
    assertEquals(List.of(oak), pick(createDownwardOrthoCamera(oak.getPositionX(), oak.getPositionY(), 3f)));
    assertEquals(List.of(pine), pick(createDownwardOrthoCamera(pine.getPositionX(), pine.getPositionY(), 3f)));

    CameraState outsideCamera = createDownwardOrthoCamera(120f, 120f, 3f);
    assertTrue(pick(outsideCamera).isEmpty());

    outsideCamera.setNoDetailMode(true);
    assertEquals(4, pick(outsideCamera).size());
  }

  @Test
  void felledTreeIsNeverPicked() {
    TreeSupply oak = spawnTree(TreeType.OAK, 20, 20);
    while (!oak.isDead()) {
      oak.hit();
    }
    assertTrue(oak.isDead());

    CameraState camera = createDownwardOrthoCamera(oak.getPositionX(), oak.getPositionY(), 5f);
    assertTrue(pick(camera).isEmpty());

    camera.setNoDetailMode(true);
    assertTrue(pick(camera).isEmpty());
  }

  @Test
  void heightScaleLimitsUpperCrownPicking() {
    TreeSupply pine = spawnTree(TreeType.PINE, 24, 24);
    BoundingBox modelBounds = world.getLandscapeResources().getTreeBounds(TreeType.PINE).bounds()[0];
    BoundingBox worldBounds = new BoundingBox();
    worldBounds.setTransformedBounds(modelBounds, pine.getMatrix());

    float minZ = worldBounds.bmin_z;
    float maxZ = worldBounds.bmax_z;
    float scaledMaxZ = minZ + (maxZ - minZ) * TreeType.PINE.pickHeightScale();
    assertTrue(maxZ > scaledMaxZ + 1f);

    CameraState midBandCamera =
        createDownwardOrthoCamera(
            pine.getPositionX(), pine.getPositionY(), 1f, CAMERA_Z - (scaledMaxZ - 0.5f), CAMERA_Z - (minZ + 0.5f));
    assertEquals(List.of(pine), pick(midBandCamera));

    CameraState topCrownCamera =
        createDownwardOrthoCamera(
            pine.getPositionX(), pine.getPositionY(), 1f, CAMERA_Z - maxZ, CAMERA_Z - (scaledMaxZ + 0.25f));
    assertTrue(pick(topCrownCamera).isEmpty());
  }

  @Test
  void selectionRadiusLimitsHorizontalPicking() {
    TreeSupply jungle = spawnTree(TreeType.JUNGLE, 16, 16);
    float halfExtent = 0.5f;

    CameraState insideRadiusCamera =
        createDownwardOrthoCamera(jungle.getPositionX() + 1.0f + halfExtent, jungle.getPositionY(), halfExtent);
    assertEquals(List.of(jungle), pick(insideRadiusCamera));

    CameraState outsideRadiusCamera =
        createDownwardOrthoCamera(jungle.getPositionX() + 1.75f + halfExtent, jungle.getPositionY(), halfExtent);
    assertTrue(pick(outsideRadiusCamera).isEmpty());
  }
}
