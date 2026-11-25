package com.oddlabs.tt.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingTemplate;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.Plants;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.RockSupply;
import com.oddlabs.tt.simulation.model.RubberGroup;
import com.oddlabs.tt.simulation.model.RubberSupply;
import com.oddlabs.tt.simulation.model.StatueScenery;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class ElementPickerTest {
  private static final float CAMERA_Z = 50f;
  private static final float DEFAULT_NEAR = 1f;
  private static final float DEFAULT_FAR = 100f;

  private World world;
  private Player player;

  @BeforeEach
  void setUp() {
    int gridSize = 64;
    Landscape landscape = FlatLandscape.create(gridSize);
    WorldParameters worldParams = new WorldParameters("1", 5, 20);
    List<PlayerInfo> playerInfos = List.of(new PlayerInfo(0, Race.VIKINGS, "Player"));
    Color.Linear[] playerColors = new Color.Linear[] {new Color.Linear(1f, 0f, 0f, 1f)};
    this.world = new World(worldParams, landscape, PlayerConfig.from(playerInfos, playerColors));
    this.player = world.getPlayers().getFirst();
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

  private List<Target> pick(CameraState camera) {
    List<Target> results = new ArrayList<>();
    ElementPicker.pick(world, camera, results);
    return results;
  }

  private List<Target> pick(CameraState camera, float interpolationAlpha) {
    List<Target> results = new ArrayList<>();
    ElementPicker.pick(world, camera, interpolationAlpha, results);
    return results;
  }

  @Test
  void selectableAndRubberPickingUseInterpolatedXY() {
    float startUnitX = UnitGrid.coordinateFromGrid(16);
    float unitY = UnitGrid.coordinateFromGrid(16);
    float deltaX = 4f;
    Unit unit = new Unit(player, startUnitX, unitY, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
    unit.setPosition(startUnitX + deltaX, unitY);
    unit.reinsert();
    float unitRadius = unit.getSelectionRadius();

    float startRubberX = UnitGrid.coordinateFromGrid(30);
    float rubberY = UnitGrid.coordinateFromGrid(30);
    RubberSupply rubber =
        new RubberSupply(world, 30, 30, startRubberX, rubberY, new RubberGroup(world), startRubberX + 1f, rubberY + 1f);
    rubber.setPosition(startRubberX + deltaX, rubberY);
    rubber.reinsert();
    float rubberRadius = Math.max(1f, rubber.getSize());

    CameraState unitPrevCam = createDownwardOrthoCamera(startUnitX - unitRadius, unitY, 0.25f);
    CameraState unitMidCam = createDownwardOrthoCamera(startUnitX + deltaX * 0.5f, unitY, 0.25f);
    CameraState unitCurrCam = createDownwardOrthoCamera(startUnitX + deltaX + unitRadius, unitY, 0.25f);
    assertEquals(List.of(unit), pick(unitPrevCam, 0f));
    assertTrue(pick(unitCurrCam, 0f).isEmpty());
    assertEquals(List.of(unit), pick(unitMidCam, 0.5f));
    assertTrue(pick(unitPrevCam, 0.5f).isEmpty());
    assertTrue(pick(unitCurrCam, 0.5f).isEmpty());
    assertEquals(List.of(unit), pick(unitCurrCam, 1f));
    assertTrue(pick(unitPrevCam, 1f).isEmpty());

    CameraState rubberPrevCam = createDownwardOrthoCamera(startRubberX - rubberRadius, rubberY, 0.25f);
    CameraState rubberMidCam = createDownwardOrthoCamera(startRubberX + deltaX * 0.5f, rubberY, 0.25f);
    CameraState rubberCurrCam = createDownwardOrthoCamera(startRubberX + deltaX + rubberRadius, rubberY, 0.25f);
    assertEquals(List.of(rubber), pick(rubberPrevCam, 0f));
    assertTrue(pick(rubberCurrCam, 0f).isEmpty());
    assertEquals(List.of(rubber), pick(rubberMidCam, 0.5f));
    assertTrue(pick(rubberPrevCam, 0.5f).isEmpty());
    assertTrue(pick(rubberCurrCam, 0.5f).isEmpty());
    assertEquals(List.of(rubber), pick(rubberCurrCam, 1f));
    assertTrue(pick(rubberPrevCam, 1f).isEmpty());
  }

  @Test
  void unitPickingRespectsFrustumNoDetailAndLeavesClientModelNull() {
    float unitX = UnitGrid.coordinateFromGrid(16);
    float unitY = UnitGrid.coordinateFromGrid(16);
    Unit unit = new Unit(player, unitX, unitY, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));

    CameraState insideCamera = createDownwardOrthoCamera(unitX, unitY, 5f);
    assertEquals(List.of(unit), pick(insideCamera));
    assertNull(unit.getClientModel(), "Picking must not instantiate a VisualModel");

    CameraState outsideCamera = createDownwardOrthoCamera(90f, 90f, 5f);
    assertTrue(pick(outsideCamera).isEmpty());

    outsideCamera.setNoDetailMode(true);
    assertEquals(List.of(unit), pick(outsideCamera));
  }

  @Test
  void disabledAndRemovedUnitsAreNeverPicked() {
    Building tower = new Building(player, player.getRaceInfo().getBuildingTemplate(BuildingType.TOWER), 20, 20);
    tower.place();
    tower.repair(tower.getTemplate().getMaxHitPoints());

    float peonX = UnitGrid.coordinateFromGrid(12);
    float peonY = UnitGrid.coordinateFromGrid(12);
    Unit mountedUnit = new Unit(player, peonX, peonY, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
    mountedUnit.mount(tower);
    assertFalse(mountedUnit.isEnabled());

    Unit removedUnit = new Unit(player, peonX, peonY, null, player.getRaceInfo().getUnitTemplate(UnitType.PEON));
    removedUnit.removeNow();
    assertFalse(removedUnit.isEnabled());

    CameraState noDetailCamera = createDownwardOrthoCamera(tower.getPositionX(), tower.getPositionY(), 5f);
    noDetailCamera.setNoDetailMode(true);
    assertEquals(List.of(tower), pick(noDetailCamera));
  }

  @Test
  void buildingPickingReflectsBuildStageBounds() {
    Building quarters = new Building(player, player.getRaceInfo().getBuildingTemplate(BuildingType.QUARTERS), 16, 16);
    quarters.place();
    BuildingTemplate template = quarters.getTemplate();
    float startRadius = template.getStartSelectionRadius();
    float builtRadius = template.getBuiltSelectionRadius();
    assertTrue(startRadius > builtRadius + 1f);

    float halfExtent = 0.5f;
    float testOffsetX = (startRadius + builtRadius) * 0.5f + halfExtent;
    CameraState edgeRadiusCamera =
        createDownwardOrthoCamera(quarters.getPositionX() + testOffsetX, quarters.getPositionY(), halfExtent);
    assertEquals(Building.BuildStage.START, quarters.getBuildStage());
    assertEquals(List.of(quarters), pick(edgeRadiusCamera));

    quarters.repair(template.getMaxHitPoints());
    assertEquals(Building.BuildStage.BUILT, quarters.getBuildStage());
    assertTrue(pick(edgeRadiusCamera).isEmpty());

    Building quartersForHeight = new Building(player, template, 36, 36);
    quartersForHeight.place();
    float startHeight = template.getStartSelectionHeight();
    float builtHeight = template.getBuiltSelectionHeight();
    assertTrue(builtHeight > startHeight + 2f);

    float baseZ =
        world
                .getHeightMap()
                .computeInterpolatedHeight(0, quartersForHeight.getPositionX(), quartersForHeight.getPositionY())
            + quartersForHeight.getOffsetZ();
    float zBandMin = baseZ + startHeight + 1.0f;
    float zBandMax = baseZ + builtHeight + 2.0f;
    CameraState upperHeightCamera =
        createDownwardOrthoCamera(
            quartersForHeight.getPositionX(),
            quartersForHeight.getPositionY(),
            1f,
            CAMERA_Z - zBandMax,
            CAMERA_Z - zBandMin);
    assertTrue(pick(upperHeightCamera).isEmpty());

    quartersForHeight.repair((template.getMaxHitPoints() / 2) + 2);
    assertEquals(Building.BuildStage.HALFBUILT, quartersForHeight.getBuildStage());
    assertEquals(List.of(quartersForHeight), pick(upperHeightCamera));

    quartersForHeight.repair(template.getMaxHitPoints());
    assertEquals(Building.BuildStage.BUILT, quartersForHeight.getBuildStage());
    assertEquals(List.of(quartersForHeight), pick(upperHeightCamera));
  }

  @Test
  void supplyPickingMatchesFrustumForRockAndRubber() {
    float rockX = UnitGrid.coordinateFromGrid(10);
    float rockY = UnitGrid.coordinateFromGrid(10);
    RockSupply rock = new RockSupply(world, 10, 10, rockX, rockY, false);

    float rubberX = UnitGrid.coordinateFromGrid(30);
    float rubberY = UnitGrid.coordinateFromGrid(30);
    RubberSupply rubber =
        new RubberSupply(world, 30, 30, rubberX, rubberY, new RubberGroup(world), rubberX + 1f, rubberY + 1f);

    CameraState rockCamera = createDownwardOrthoCamera(rockX, rockY, 3f);
    assertEquals(List.of(rock), pick(rockCamera));
    assertNull(rock.getClientModel());

    CameraState rubberCamera = createDownwardOrthoCamera(rubberX, rubberY, 3f);
    assertEquals(List.of(rubber), pick(rubberCamera));
    assertNull(rubber.getClientModel());

    CameraState emptyCamera = createDownwardOrthoCamera(90f, 90f, 3f);
    assertTrue(pick(emptyCamera).isEmpty());
  }

  @Test
  void sceneryPickedAndPlantsAndUnregisteredElementsExcluded() {
    float x = UnitGrid.coordinateFromGrid(20);
    float y = UnitGrid.coordinateFromGrid(20);
    BoundingBox[] statueBounds = new BoundingBox[] {new BoundingBox(-1f, 1f, -1f, 1f, 0f, 2f)};
    StatueScenery statue = new StatueScenery(world, x, y, 1f, 0f, 0, statueBounds, 2f, false, "statue");
    Plants plants = new Plants(world, x, y, 1f, 0f, Terrain.NATIVE, 0);

    CameraState camera = createDownwardOrthoCamera(x, y, 5f);
    List<Target> results = pick(camera);
    assertEquals(List.of(statue), results);
    assertFalse(results.contains(plants));

    statue.remove();
    assertTrue(pick(camera).isEmpty());
  }
}
