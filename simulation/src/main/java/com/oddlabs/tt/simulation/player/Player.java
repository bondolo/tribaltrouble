package com.oddlabs.tt.simulation.player;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.landscape.SpawnLocation;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Army;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.Distributable;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.RaceInfo;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.SupplyContainer;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.model.WeaponTier;
import com.oddlabs.tt.simulation.pathfinder.BuildingSiteScanFilter;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.util.Color;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.Nullable;

/// Simulation player managing units, buildings, colour, metrics, and command dispatch.
public final class Player implements PlayerInterface, PlayerCommandExecutor {
  public static final int INITIAL_UNIT_COUNT = 20;
  public static final int MAX_BUILDING_COUNT = 20;
  public static final int DEFAULT_MAX_UNIT_COUNT = 250;
  private static final float RADIAL_SEARCH_RADIUS = 30f;
  private static final int MAX_SELECTION_SIZE = 1024;

  private final PlayerConfig config;
  private final @Nullable PlayerInterface playerInterface;
  private final World world;
  private final PlayerInfo player_info;
  private final Army units = new Army();
  private final SupplyContainer unit_count;
  private final SupplyContainer building_count = new SupplyContainer(MAX_BUILDING_COUNT);

  private Color.Linear color;

  private @Nullable Unit chieftain = null;
  private boolean training_chieftain = false;
  private float start_x;
  private float start_y;

  // stats
  private int units_lost;
  private int buildings_lost;
  private int units_killed;
  private int buildings_destroyed;
  private int units_moved;
  private int weapons_thrown;
  private int magics;

  private int tree_harvested;
  private int rock_harvested;
  private int iron_harvested;
  private int rubber_harvested;

  private boolean can_build_chieftains = true;
  private boolean can_repair = true;
  private boolean can_attack = true;
  private final EnumSet<BuildingType> can_build = EnumSet.allOf(BuildingType.class);
  private boolean can_move = true;
  private boolean can_exit_towers = true;
  private boolean can_use_rubber = true;
  private boolean can_set_rally = true;
  private boolean can_harvest = true;
  private boolean can_build_armies = true;
  private boolean can_build_weapons = true;
  private boolean can_transport = true;
  private final EnumSet<MagicType> enabled_magics = EnumSet.allOf(MagicType.class);

  private float hit_bonus;

  private int preferred_speed = World.GAMESPEED_DONTCARE;
  private @Nullable CommandRejectedListener command_rejected_listener = null;

  public Player(World world, PlayerConfig config, SpawnLocation startLocation) {
    this.world = world;
    this.config = config;
    this.color = config.color();
    this.player_info = config.info();
    this.unit_count = new SupplyContainer(world.getMaxUnitCount());
    this.playerInterface = config.playerInterface();
    this.start_x = startLocation.x();
    this.start_y = startLocation.y();
  }

  public Player(World world, PlayerConfig config) {
    this(world, config, new SpawnLocation(0f, 0f));
  }

  /// Returns the configuration record used to initialize this player.
  public PlayerConfig getConfig() {
    return config;
  }

  public void setCommandRejectedListener(@Nullable CommandRejectedListener listener) {
    this.command_rejected_listener = listener;
  }

  /// Returns the destination interface for domain actions dispatched on this player.
  public PlayerInterface getPlayerInterface() {
    return playerInterface != null ? playerInterface : this;
  }

  private void notifyRejected(String action, int entityId, String reason) {
    if (command_rejected_listener != null) {
      command_rejected_listener.onCommandRejected(action, entityId, reason);
    }
  }

  @Override
  public void changePreferredGamespeed(int delta) {
    int old_speed = getGamespeed();
    int new_speed = Math.clamp(old_speed + delta, Game.GAMESPEED_PAUSE, Game.GAMESPEED_LUDICROUS);
    setPreferredGamespeed(new_speed);
  }

  @Override
  public void setPreferredGamespeed(int speed) {
    if (!World.isValidPreferredGamespeed(speed)) return;
    if (preferred_speed != speed) {
      int old_speed = preferred_speed;
      this.preferred_speed = speed;
      if (World.isValidGamespeed(preferred_speed) && World.isValidGamespeed(old_speed))
        world.getNotificationListener().playerGamespeedChanged();
      world.gamespeedChanged();
    }
  }

  public int getGamespeed() {
    return World.isValidGamespeed(preferred_speed) ? preferred_speed : world.getGamespeed();
  }

  public int getPreferredGamespeed() {
    return preferred_speed;
  }

  public float getHitBonus() {
    return hit_bonus;
  }

  public void setHitBonus(float bonus) {
    this.hit_bonus = bonus;
  }

  public World getWorld() {
    return world;
  }

  public void enableArmies(boolean enabled) {
    can_build_armies = enabled;
  }

  public void enableWeapons(boolean enabled) {
    can_build_weapons = enabled;
  }

  public void enableTransporting(boolean enabled) {
    can_transport = enabled;
  }

  public void enableHarvesting(boolean enabled) {
    can_harvest = enabled;
  }

  public void enableRubber(boolean enabled) {
    can_use_rubber = enabled;
  }

  public void enableChieftains(boolean enabled) {
    can_build_chieftains = enabled;
  }

  public void enableTowerExits(boolean enabled) {
    can_exit_towers = enabled;
  }

  public void enableRepairing(boolean enabled) {
    can_repair = enabled;
  }

  public void enableBuilding(BuildingType building, boolean enabled) {
    if (enabled) {
      can_build.add(building);
    } else {
      can_build.remove(building);
    }
  }

  public void enableAttacking(boolean enabled) {
    can_attack = enabled;
  }

  public void enableRallyPoints(boolean enabled) {
    can_set_rally = enabled;
  }

  public void enableMoving(boolean enabled) {
    can_move = enabled;
  }

  public boolean canTransport() {
    return can_transport;
  }

  public boolean canBuildWeapons() {
    return can_build_weapons;
  }

  public boolean canHarvest() {
    return can_harvest;
  }

  public boolean canBuildArmies() {
    return can_build_armies;
  }

  public boolean canSetRallyPoints() {
    return can_set_rally;
  }

  public boolean canUseRubber() {
    return can_use_rubber;
  }

  public void enableMagic(MagicType type, boolean enabled) {
    if (enabled) {
      enabled_magics.add(type);
    } else {
      enabled_magics.remove(type);
    }
  }

  public boolean canDoMagic(MagicType type) {
    return enabled_magics.contains(type);
  }

  public boolean canDoMagic(int magic_index) {
    return canDoMagic(getRaceInfo().getMagicType(magic_index));
  }

  public boolean canExitTowers() {
    return can_exit_towers;
  }

  public boolean canAttack() {
    return can_attack;
  }

  public boolean canMove() {
    return can_move;
  }

  public boolean canBuild(BuildingType building) {
    return can_build.contains(building) && getBuildingCountContainer().getNumSupplies() < Player.MAX_BUILDING_COUNT;
  }

  public boolean canRepair() {
    return can_repair;
  }

  public boolean canBuildChieftains() {
    return can_build_chieftains;
  }

  @Override
  public String toString() {
    return player_info.toString();
  }

  public PlayerInfo getPlayerInfo() {
    return player_info;
  }

  public Optional<Building> buildBuilding(BuildingType building_type, int grid_x, int grid_y) {
    BuildingSiteScanFilter filter =
        new BuildingSiteScanFilter(world.getUnitGrid(), getRaceInfo().getBuildingTemplate(building_type), 40);
    world.getUnitGrid().scan(filter, grid_x, grid_y);
    return filter
        .getSingleResult()
        .map(t -> {
          Building b = new Building(this, getRaceInfo().getBuildingTemplate(building_type), t.getGridX(), t.getGridY());
          b.place();
          b.repair(1000);
          return b;
        });
  }

  /// Initializes starting entities for this player based on its configuration and spawn locations.
  public void initializeStartingEntities(List<SpawnLocation> startingLocations, int initialGamespeed) {
    UnitInfo unitInfo = config.unitInfo();
    if (unitInfo == null) {
      return;
    }
    switch (config.controllerType()) {
      case HUMAN -> initializeStartingArmy(startingLocations, unitInfo, initialGamespeed, config.chieftainName());
      case AI_STANDARD -> initializeStartingBase(unitInfo);
      case AI_TOWER_TUTORIAL -> {}
      case AI_CHIEFTAIN_TUTORIAL -> {
        new Unit(this, 100, 100, null, getRaceInfo().getUnitTemplate(UnitType.PEON));
        new Unit(this, 200, 100, null, getRaceInfo().getUnitTemplate(UnitType.PEON));
        new Unit(this, 40, 200, null, getRaceInfo().getUnitTemplate(UnitType.PEON));
      }
    }
  }

  /// Initializes this player's starting base (buildings, chieftain, and units) using radial placement.
  public void initializeStartingBase(UnitInfo unit_info) {
    int grid_start_x = UnitGrid.toGridCoordinate(start_x);
    int grid_start_y = UnitGrid.toGridCoordinate(start_y);
    if (unit_info.hasQuarters()) {
      buildBuilding(BuildingType.QUARTERS, grid_start_x, grid_start_y);
    }
    if (unit_info.hasArmory()) {
      buildBuilding(BuildingType.ARMORY, grid_start_x, grid_start_y);
    }
    for (int i = 0; i < unit_info.numTowers(); i++) {
      int center = world.getHeightMap().getGridUnitsPerWorld() / 2;
      int dx = center - grid_start_x;
      int dy = center - grid_start_y;
      float inv_dist = 1f / (float) Math.sqrt(dx * dx + dy * dy);
      int tx = (int) (grid_start_x + 10f * dx * inv_dist);
      int ty = (int) (grid_start_y + 10f * dy * inv_dist);
      buildBuilding(BuildingType.TOWER, tx, ty);
    }
    Random random = new Random(42);
    if (unit_info.hasChieftain()) {
      Target t = getRadialTarget(random);
      Unit chieftain =
          new Unit(this, t.getPositionX(), t.getPositionY(), null, getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN));
      setActiveChieftain(chieftain);
    }
    for (int i = 0; i < unit_info.numPeons(); i++) {
      Target t = getRadialTarget(random);
      new Unit(this, t.getPositionX(), t.getPositionY(), null, getRaceInfo().getUnitTemplate(UnitType.PEON));
    }
    for (int i = 0; i < unit_info.numRockWarriors(); i++) {
      Target t = getRadialTarget(random);
      new Unit(this, t.getPositionX(), t.getPositionY(), null, getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
    }
    for (int i = 0; i < unit_info.numIronWarriors(); i++) {
      Target t = getRadialTarget(random);
      new Unit(this, t.getPositionX(), t.getPositionY(), null, getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
    }
    for (int i = 0; i < unit_info.numRubberWarriors(); i++) {
      Target t = getRadialTarget(random);
      new Unit(this, t.getPositionX(), t.getPositionY(), null, getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
    }
  }

  /// Initializes this player's starting army using explicit spawn locations.
  public void initializeStartingArmy(
      List<SpawnLocation> startingLocations, UnitInfo unitInfo, int initialGamespeed, @Nullable String chieftainName) {
    setPreferredGamespeed(initialGamespeed);
    var raceInfo = getRaceInfo();

    if (AppConfig.LEGACY_DETERMINISTIC) {
      Random random = new Random(42);
      if (unitInfo.hasChieftain()) {
        Target t = getLegacyTarget(random);
        Unit chieftain =
            chieftainName != null
                ? new Unit(
                    this,
                    t.getPositionX(),
                    t.getPositionY(),
                    null,
                    raceInfo.getUnitTemplate(UnitType.CHIEFTAIN),
                    chieftainName,
                    false)
                : new Unit(
                    this, t.getPositionX(), t.getPositionY(), null, raceInfo.getUnitTemplate(UnitType.CHIEFTAIN));
        raceInfo.getMagics().forEach(chieftain::maxMagicEnergy);
        setActiveChieftain(chieftain);
      }
      for (int j = 0; j < unitInfo.numPeons(); j++) {
        Target t = getLegacyTarget(random);
        new Unit(this, t.getPositionX(), t.getPositionY(), null, raceInfo.getUnitTemplate(UnitType.PEON));
      }
      for (int j = 0; j < unitInfo.numRockWarriors(); j++) {
        Target t = getLegacyTarget(random);
        new Unit(this, t.getPositionX(), t.getPositionY(), null, raceInfo.getUnitTemplate(UnitType.WARRIOR_ROCK));
      }
      for (int j = 0; j < unitInfo.numIronWarriors(); j++) {
        Target t = getLegacyTarget(random);
        new Unit(this, t.getPositionX(), t.getPositionY(), null, raceInfo.getUnitTemplate(UnitType.WARRIOR_IRON));
      }
      for (int j = 0; j < unitInfo.numRubberWarriors(); j++) {
        Target t = getLegacyTarget(random);
        new Unit(this, t.getPositionX(), t.getPositionY(), null, raceInfo.getUnitTemplate(UnitType.WARRIOR_RUBBER));
      }
      return;
    }

    int idx = 0;
    for (int j = 0; j < unitInfo.numPeons(); j++, idx++) {
      SpawnLocation loc = startingLocations.get(idx);
      new Unit(this, loc.x(), loc.y(), null, raceInfo.getUnitTemplate(UnitType.PEON));
    }
    for (int j = 0; j < unitInfo.numRockWarriors(); j++, idx++) {
      SpawnLocation loc = startingLocations.get(idx);
      new Unit(this, loc.x(), loc.y(), null, raceInfo.getUnitTemplate(UnitType.WARRIOR_ROCK));
    }
    for (int j = 0; j < unitInfo.numIronWarriors(); j++, idx++) {
      SpawnLocation loc = startingLocations.get(idx);
      new Unit(this, loc.x(), loc.y(), null, raceInfo.getUnitTemplate(UnitType.WARRIOR_IRON));
    }
    for (int j = 0; j < unitInfo.numRubberWarriors(); j++, idx++) {
      SpawnLocation loc = startingLocations.get(idx);
      new Unit(this, loc.x(), loc.y(), null, raceInfo.getUnitTemplate(UnitType.WARRIOR_RUBBER));
    }
    if (unitInfo.hasChieftain()) {
      SpawnLocation loc = startingLocations.get(idx);
      Unit chieftain =
          chieftainName != null
              ? new Unit(
                  this, loc.x(), loc.y(), null, raceInfo.getUnitTemplate(UnitType.CHIEFTAIN), chieftainName, false)
              : new Unit(this, loc.x(), loc.y(), null, raceInfo.getUnitTemplate(UnitType.CHIEFTAIN));
      raceInfo.getMagics().forEach(chieftain::maxMagicEnergy);
      setActiveChieftain(chieftain);
    }
  }

  /// Finds a target position within radial distance from this player's starting coordinates.
  public Target getRadialTarget(RandomGenerator random) {
    float target_x = start_x + random.nextFloat(-RADIAL_SEARCH_RADIUS, RADIAL_SEARCH_RADIUS);
    float target_y = start_y + random.nextFloat(-RADIAL_SEARCH_RADIUS, RADIAL_SEARCH_RADIUS);
    Target target =
        world
            .getUnitGrid()
            .findGridTarget(UnitGrid.toGridCoordinate(target_x), UnitGrid.toGridCoordinate(target_y), false);
    return target != null
        ? target
        : new LandscapeTarget(UnitGrid.toGridCoordinate(target_x), UnitGrid.toGridCoordinate(target_y));
  }

  /// Finds a deterministic target position using the legacy search algorithm.
  public Target getLegacyTarget(Random random) {
    float radius = 30f;
    float target_x = start_x + (random.nextFloat() * 2f - 1f) * radius;
    float target_y = start_y + (random.nextFloat() * 2f - 1f) * radius;
    Target[] targets =
        world
            .getUnitGrid()
            .findGridTargets(UnitGrid.toGridCoordinate(target_x), UnitGrid.toGridCoordinate(target_y), 1, false);
    return targets[0];
  }

  public Optional<Selectable<?>> findNearestEnemy(int start_x, int start_y) {
    return findNearestEnemy(start_x, start_y, null);
  }

  public Optional<Selectable<?>> findNearestEnemy(int start_x, int start_y, @Nullable Selectable<?> target) {
    return findNearestEnemy(start_x, start_y, target, Selectable.genericClass());
  }

  public int getStatus() {
    int sum = 0;
    for (Selectable<?> s : getUnits().getSet()) {
      sum += s.getStatusValue();
    }
    return sum;
  }

  public Optional<Selectable<?>> findNearestEnemy(
      int start_x, int start_y, @Nullable Selectable<?> target, Class<? extends Selectable<?>> type) {
    Selectable<?> nearest = null;
    int minDistance = Integer.MAX_VALUE;
    for (Player player : world.getPlayers()) {
      if (isEnemy(player)) {
        for (Selectable<?> s : player.getUnits().getSet()) {
          if (type.isInstance(s) && s != target) {
            int dx = s.getGridX() - start_x;
            int dy = s.getGridY() - start_y;
            int dist = dx * dx + dy * dy;
            if (dist < minDistance) {
              minDistance = dist;
              nearest = s;
            }
          }
        }
      }
    }
    return Optional.ofNullable(nearest);
  }

  public Optional<Selectable<?>> findNearestEnemyBuilding(int start_x, int start_y) {
    return findNearestEnemy(start_x, start_y, null, Building.class);
  }

  public RaceInfo getRaceInfo() {
    return world.getRaceData().getRaceInfo(player_info.race());
  }

  public SupplyContainer getUnitCountContainer() {
    return unit_count;
  }

  public SupplyContainer getBuildingCountContainer() {
    return building_count;
  }

  public void setActiveChieftain(@Nullable Unit chieftain) {
    this.chieftain = chieftain;
  }

  public Optional<Building> getArmory() {
    for (Selectable<?> s : units.getSet()) {
      if (s instanceof Building b && b.isOperating() && b.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
        return Optional.of(b);
      }
    }
    return Optional.empty();
  }

  public Optional<Building> getQuarters() {
    for (Selectable<?> s : units.getSet()) {
      if (s instanceof Building b && b.isOperating() && b.getAbilities().hasAbilities(Abilities.REPRODUCE)) {
        return Optional.of(b);
      }
    }
    return Optional.empty();
  }

  public boolean isAlive() {
    int units = getUnitCountContainer().getNumSupplies();
    return units > 0 || hasActiveChieftain() || getQuarters().isPresent();
  }

  public boolean hasActiveChieftain() {
    return chieftain != null;
  }

  public @Nullable Unit getActiveChieftain() {
    return chieftain;
  }

  public Optional<Unit> getChieftain() {
    return Optional.ofNullable(chieftain);
  }

  public void setTrainingChieftain(boolean training_chieftain) {
    assert this.training_chieftain != training_chieftain;
    this.training_chieftain = training_chieftain;
  }

  public boolean isTrainingChieftain() {
    return training_chieftain;
  }

  public Color.Linear getColor() {
    return color;
  }

  /**
   * Sets the team color for this player.
   *
   * @param color the new team color
   */
  public void setColor(Color color) {
    this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
  }

  @Override
  public void deployUnits(@Nullable Building building, DeployType type, int num_units) {
    applyDeployUnits(building, type, num_units);
  }

  private void applyDeployUnits(@Nullable Building building, DeployType type, int num_units) {
    if (building == null) {
      notifyRejected("deployUnits", Distributable.NO_ID, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(building)) {
      notifyRejected("deployUnits", building.getId(), "NOT_OWNED");
      return;
    }
    if (num_units == 0 || num_units == Integer.MIN_VALUE) {
      notifyRejected("deployUnits", building.getId(), "INVALID_AMOUNT");
      return;
    }
    if (!building.canDeployUnits(type)) {
      notifyRejected("deployUnits", building.getId(), "UNSUPPORTED_ACTION");
      return;
    }
    building.deployUnits(type, num_units);
  }

  @Override
  public void createHarvesters(@Nullable Building building, int num_tree, int num_rock, int num_iron, int num_rubber) {
    applyCreateHarvesters(building, num_tree, num_rock, num_iron, num_rubber);
  }

  private void applyCreateHarvesters(
      @Nullable Building building, int num_tree, int num_rock, int num_iron, int num_rubber) {
    if (building == null) {
      notifyRejected("createHarvesters", Distributable.NO_ID, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(building)) {
      notifyRejected("createHarvesters", building.getId(), "NOT_OWNED");
      return;
    }
    if (num_tree < 0 || num_rock < 0 || num_iron < 0 || num_rubber < 0) {
      notifyRejected("createHarvesters", building.getId(), "INVALID_AMOUNT");
      return;
    }
    long total = (long) num_tree + num_rock + num_iron + num_rubber;
    if (!building.canCreateHarvesters(total)) {
      notifyRejected("createHarvesters", building.getId(), "INSUFFICIENT_SUPPLY");
      return;
    }
    building.createHarvesters(num_tree, num_rock, num_iron, num_rubber);
  }

  @Override
  public void buildWeapons(@Nullable Building building, WeaponTier tier, int num_weapons, boolean infinite) {
    applyBuildWeapons(building, tier, num_weapons, infinite);
  }

  private void applyBuildWeapons(@Nullable Building building, WeaponTier tier, int num_weapons, boolean infinite) {
    if (building == null) {
      notifyRejected("buildWeapons", Distributable.NO_ID, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(building)) {
      notifyRejected("buildWeapons", building.getId(), "NOT_OWNED");
      return;
    }
    if ((!infinite && num_weapons == 0) || num_weapons == Integer.MIN_VALUE) {
      notifyRejected("buildWeapons", building.getId(), "INVALID_AMOUNT");
      return;
    }
    if (!building.canBuildWeapons(tier)) {
      notifyRejected("buildWeapons", building.getId(), "UNSUPPORTED_ACTION");
      return;
    }
    building.buildWeapons(tier, num_weapons, infinite);
  }

  @Override
  public void doMagic(@Nullable Unit chieftain, MagicType magic) {
    applyDoMagic(chieftain, magic, true);
  }

  @Override
  public void doMagic(@Nullable Unit chieftain, MagicType magic, boolean clearStack) {
    applyDoMagic(chieftain, magic, clearStack);
  }

  private void applyDoMagic(@Nullable Unit chieftain, MagicType magic, boolean clearStack) {
    if (chieftain == null) {
      notifyRejected("doMagic", Distributable.NO_ID, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(chieftain)) {
      notifyRejected("doMagic", chieftain.getId(), "NOT_OWNED");
      return;
    }
    if (!chieftain.getAbilities().hasAbilities(Abilities.MAGIC) || !getRaceInfo().getMagics().contains(magic)) {
      notifyRejected("doMagic", chieftain.getId(), "UNSUPPORTED_ACTION");
      return;
    }
    chieftain.doMagic(magic, clearStack);
  }

  @Override
  public void exitTower(@Nullable Building building) {
    applyExitTower(building);
  }

  private void applyExitTower(@Nullable Building building) {
    if (building == null) {
      notifyRejected("exitTower", Distributable.NO_ID, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(building)) {
      notifyRejected("exitTower", building.getId(), "NOT_OWNED");
      return;
    }
    if (!building.canExitTower()) {
      notifyRejected("exitTower", building.getId(), "UNSUPPORTED_ACTION");
      return;
    }
    building.exitTower();
  }

  @Override
  public void trainChieftain(@Nullable Building building, boolean start) {
    applyTrainChieftain(building, start);
  }

  private void applyTrainChieftain(@Nullable Building building, boolean start) {
    if (building == null) {
      notifyRejected("trainChieftain", Distributable.NO_ID, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(building)) {
      notifyRejected("trainChieftain", building.getId(), "NOT_OWNED");
      return;
    }
    if ((start && !building.canBuildChieftain()) || (!start && !building.canStopChieftain())) {
      notifyRejected("trainChieftain", building.getId(), "UNSUPPORTED_ACTION");
      return;
    }
    building.trainChieftain(start);
  }

  @Override
  public void placeBuilding(
      List<? extends Selectable<?>> selection, BuildingType template_type, int placing_grid_x, int placing_grid_y) {
    applyPlaceBuilding(selection, template_type, placing_grid_x, placing_grid_y);
  }

  private boolean isGridInside(int gridX, int gridY) {
    int gridSize = world.getHeightMap().getGridUnitsPerWorld();
    return gridX >= 0 && gridX < gridSize && gridY >= 0 && gridY < gridSize;
  }

  private void applyPlaceBuilding(
      List<? extends Selectable<?>> selection, BuildingType template_type, int placing_grid_x, int placing_grid_y) {
    if (!isGridInside(placing_grid_x, placing_grid_y)) {
      notifyRejected("placeBuilding", Distributable.NO_ID, "OUT_OF_BOUNDS");
      return;
    }
    boolean anyOwned = false;
    boolean anyBuilder = false;
    for (var s : selection) {
      if (isValid(s)) {
        anyOwned = true;
        if (s.getAbilities().hasAbilities(Abilities.BUILD)) {
          anyBuilder = true;
          break;
        }
      }
    }
    if (!anyOwned) {
      Selectable<?> first = !selection.isEmpty() ? selection.getFirst() : null;
      int entityId = first != null ? first.getId() : Distributable.NO_ID;
      String reason = (first != null && first.getOwner() != this) ? "NOT_OWNED" : "ENTITY_NOT_FOUND";
      notifyRejected("placeBuilding", entityId, reason);
      return;
    }
    if (!anyBuilder) {
      notifyRejected("placeBuilding", Distributable.NO_ID, "UNSUPPORTED_ACTION");
      return;
    }
    var template = getRaceInfo().getBuildingTemplate(template_type);
    if (!canBuild(template_type)
        || !Building.isPlacingLegal(world.getUnitGrid(), template, placing_grid_x, placing_grid_y)) {
      notifyRejected("placeBuilding", Distributable.NO_ID, "ILLEGAL_PLACEMENT");
      return;
    }
    Building building = new Building(this, template, placing_grid_x, placing_grid_y);
    for (var selectable : selection) {
      if (isValid(selectable)) {
        selectable.initTarget(building, Action.DEFAULT, false);
      }
    }
  }

  @Override
  public void setRallyPoint(@Nullable Building building, @Nullable Target target) {
    applySetRallyPoint(building, target);
  }

  private void applySetRallyPoint(@Nullable Building building, @Nullable Target target) {
    if (building == null) {
      notifyRejected("setRallyPoint", Distributable.NO_ID, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(building)) {
      notifyRejected("setRallyPoint", building.getId(), "NOT_OWNED");
      return;
    }
    if (!building.canSetRallyPoint()) {
      notifyRejected("setRallyPoint", building.getId(), "UNSUPPORTED_ACTION");
      return;
    }
    if (target == null || (target instanceof Selectable<?> s && s.isDead())) {
      notifyRejected("setRallyPoint", Distributable.NO_ID, "ENTITY_NOT_FOUND");
      return;
    }
    building.setRallyPoint(target);
  }

  @Override
  public void setRallyPoint(@Nullable Building building, int grid_x, int grid_y) {
    applySetRallyPoint(building, grid_x, grid_y);
  }

  private void applySetRallyPoint(@Nullable Building building, int grid_x, int grid_y) {
    if (!isGridInside(grid_x, grid_y)) {
      int buildingId = building != null ? building.getId() : Distributable.NO_ID;
      notifyRejected("setRallyPoint", buildingId, "OUT_OF_BOUNDS");
      return;
    }
    applySetRallyPoint(building, new LandscapeTarget(grid_x, grid_y));
  }

  @Override
  public void setTarget(
      @Nullable List<? extends Selectable<?>> selection, @Nullable Target target, Action action, boolean aggressive) {
    if (selection != null) {
      applySetTarget(selection, target, action, aggressive);
    }
  }

  private void applySetTarget(
      List<? extends Selectable<?>> selection, @Nullable Target target, Action action, boolean aggressive) {
    if (target == null || (target instanceof Selectable<?> s && s.isDead())) {
      notifyRejected("setTarget", Distributable.NO_ID, "ENTITY_NOT_FOUND");
      return;
    }
    boolean anyOwned = false;
    for (var s : selection) {
      if (isValid(s)) {
        anyOwned = true;
        break;
      }
    }
    if (!anyOwned && !selection.isEmpty()) {
      Selectable<?> first = selection.getFirst();
      int entityId = first != null ? first.getId() : Distributable.NO_ID;
      String reason = (first != null && first.getOwner() != this) ? "NOT_OWNED" : "ENTITY_NOT_FOUND";
      notifyRejected("setTarget", entityId, reason);
      return;
    }
    for (Selectable<?> selectable : selection) {
      if (isValid(selectable)) {
        selectable.initTarget(target, action, aggressive);
      }
    }
  }

  @Override
  public void setTarget(@Nullable Selectable<?> unit, @Nullable Target target, Action action, boolean aggressive) {
    if (target == null || (target instanceof Selectable<?> s && s.isDead())) {
      notifyRejected("setTarget", Distributable.NO_ID, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(unit)) {
      int entityId = unit != null ? unit.getId() : Distributable.NO_ID;
      String reason = (unit != null && unit.getOwner() != this) ? "NOT_OWNED" : "ENTITY_NOT_FOUND";
      notifyRejected("setTarget", entityId, reason);
      return;
    }
    unit.initTarget(target, action, aggressive);
  }

  public void killSelection(Collection<? extends Selectable<?>> selection) {
    for (Selectable<?> selectable : selection) {
      if (selectable != null) {
        selectable.hit(10000, 0f, 1f, this);
      }
    }
  }

  @Override
  public void setLandscapeTarget(
      @Nullable List<? extends Selectable<?>> selection, int grid_x, int grid_y, Action action, boolean aggressive) {
    if (selection != null) {
      applySetLandscapeTarget(selection, grid_x, grid_y, action, aggressive);
    }
  }

  private void applySetLandscapeTarget(
      List<? extends Selectable<?>> selection, int grid_x, int grid_y, Action action, boolean aggressive) {
    if (!isGridInside(grid_x, grid_y)) {
      notifyRejected("setLandscapeTarget", Distributable.NO_ID, "OUT_OF_BOUNDS");
      return;
    }
    if (selection.isEmpty()) {
      return;
    }
    if (selection.size() == 1) {
      applySetLandscapeTarget(selection.getFirst(), grid_x, grid_y, action, aggressive);
      return;
    }
    boolean anyOwned = false;
    for (var s : selection) {
      if (isValid(s)) {
        anyOwned = true;
        break;
      }
    }
    if (!anyOwned) {
      Selectable<?> first = selection.getFirst();
      int entityId = first != null ? first.getId() : Distributable.NO_ID;
      String reason = (first != null && first.getOwner() != this) ? "NOT_OWNED" : "ENTITY_NOT_FOUND";
      notifyRejected("setLandscapeTarget", entityId, reason);
      return;
    }
    var targets = world.getUnitGrid().findGridTargets(grid_x, grid_y, selection.size(), true);
    for (int i = 0; i < selection.size(); i++) {
      Selectable<?> selectable = selection.get(i);
      if (isValid(selectable)) {
        selectable.initTarget(targets[i], action, aggressive);
      }
    }
  }

  @Override
  public void setLandscapeTarget(
      @Nullable Selectable<?> unit, int grid_x, int grid_y, Action action, boolean aggressive) {
    applySetLandscapeTarget(unit, grid_x, grid_y, action, aggressive);
  }

  private void applySetLandscapeTarget(
      @Nullable Selectable<?> unit, int grid_x, int grid_y, Action action, boolean aggressive) {
    if (!isGridInside(grid_x, grid_y)) {
      notifyRejected("setLandscapeTarget", Distributable.NO_ID, "OUT_OF_BOUNDS");
      return;
    }
    if (!isValid(unit)) {
      int entityId = unit != null ? unit.getId() : Distributable.NO_ID;
      String reason = (unit != null && unit.getOwner() != this) ? "NOT_OWNED" : "ENTITY_NOT_FOUND";
      notifyRejected("setLandscapeTarget", entityId, reason);
      return;
    }
    Target target = world.getUnitGrid().findGridTarget(grid_x, grid_y, false);
    if (target != null) {
      unit.initTarget(target, action, aggressive);
    }
  }

  public boolean isValid(@Nullable Selectable<?> s) {
    return s != null && !s.isDead() && s.getOwner() == this;
  }

  public float getStartX() {
    return start_x;
  }

  public void setStartX(float x) {
    start_x = x;
  }

  public float getStartY() {
    return start_y;
  }

  public void setStartY(float y) {
    start_y = y;
  }

  public boolean isEnemy(Player other_player) {
    if (other_player.player_info.team() == PlayerInfo.TEAM_NEUTRAL
        || this.player_info.team() == PlayerInfo.TEAM_NEUTRAL) {
      return false;
    }
    return other_player.player_info.team() != this.player_info.team();
  }

  public boolean teamHasBuilding() {
    for (Player player : world.getPlayers()) {
      if (player.getPlayerInfo().team() == player_info.team()
          && player.getBuildingCountContainer().getNumSupplies() > 0) {
        return true;
      }
    }
    return false;
  }

  public Army getUnits() {
    return units;
  }

  public void magicCast() {
    magics++;
  }

  public int getMagics() {
    return magics;
  }

  public void weaponThrown() {
    weapons_thrown++;
  }

  public int getWeaponsThrown() {
    return weapons_thrown;
  }

  public void unitMoved() {
    units_moved++;
  }

  public int getUnitsMoved() {
    return units_moved;
  }

  public void unitLost() {
    units_lost++;
  }

  public int getUnitsLost() {
    return units_lost;
  }

  public void buildingLost() {
    buildings_lost++;
  }

  public int getBuildingsLost() {
    return buildings_lost;
  }

  public void unitKilled() {
    units_killed++;
  }

  public int getUnitsKilled() {
    return units_killed;
  }

  public void buildingDestroyed() {
    buildings_destroyed++;
  }

  public int getBuildingsDestroyed() {
    return buildings_destroyed;
  }

  public void harvested(SupplyType type) {
    switch (type) {
      case WOOD -> tree_harvested++;
      case ROCK -> rock_harvested++;
      case IRON -> iron_harvested++;
      case RUBBER -> rubber_harvested++;
    }
  }

  public int getTreeHarvested() {
    return tree_harvested;
  }

  public int getRockHarvested() {
    return rock_harvested;
  }

  public int getIronHarvested() {
    return iron_harvested;
  }

  public int getRubberHarvested() {
    return rubber_harvested;
  }

  @Override
  public void execute(PlayerCommand command) {
    switch (command) {
      case PlayerCommand.DeployUnits cmd -> deployUnits(cmd.buildingId(), cmd.deployType(), cmd.numUnits());
      case PlayerCommand.CreateHarvesters cmd ->
          createHarvesters(cmd.buildingId(), cmd.numTree(), cmd.numRock(), cmd.numIron(), cmd.numRubber());
      case PlayerCommand.BuildWeapons cmd ->
          buildWeapons(cmd.buildingId(), cmd.tier(), cmd.numWeapons(), cmd.infinite());
      case PlayerCommand.DoMagic cmd -> doMagic(cmd.chieftainId(), cmd.magic(), cmd.clearStack());
      case PlayerCommand.ExitTower cmd -> exitTower(cmd.buildingId());
      case PlayerCommand.TrainChieftain cmd -> trainChieftain(cmd.buildingId(), cmd.start());
      case PlayerCommand.PlaceBuilding cmd ->
          placeBuilding(cmd.selectionIds(), cmd.templateType(), cmd.gridX(), cmd.gridY());
      case PlayerCommand.SetRallyPointTarget cmd -> setRallyPoint(cmd.buildingId(), cmd.targetEntityId());
      case PlayerCommand.SetRallyPointCoords cmd -> setRallyPoint(cmd.buildingId(), cmd.gridX(), cmd.gridY());
      case PlayerCommand.SetTarget cmd ->
          setTarget(cmd.selectionIds(), cmd.targetEntityId(), cmd.action(), cmd.aggressive());
      case PlayerCommand.SetLandscapeTarget cmd ->
          setLandscapeTarget(cmd.selectionIds(), cmd.gridX(), cmd.gridY(), cmd.action(), cmd.aggressive());
      case PlayerCommand.SetPreferredGamespeed cmd -> setPreferredGamespeed(cmd.speed());
      case PlayerCommand.ChangePreferredGamespeed cmd -> changePreferredGamespeed(cmd.delta());
    }
  }

  private void deployUnits(int buildingId, DeployType deployType, int numUnits) {
    var distributable = world.getDistributableTable().getDistributable(buildingId);
    if (!(distributable instanceof Building building) || building.isDead()) {
      notifyRejected("deployUnits", buildingId, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(building)) {
      notifyRejected("deployUnits", buildingId, "NOT_OWNED");
      return;
    }
    applyDeployUnits(building, deployType, numUnits);
  }

  private void createHarvesters(int buildingId, int numTree, int numRock, int numIron, int numRubber) {
    var distributable = world.getDistributableTable().getDistributable(buildingId);
    if (!(distributable instanceof Building building) || building.isDead()) {
      notifyRejected("createHarvesters", buildingId, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(building)) {
      notifyRejected("createHarvesters", buildingId, "NOT_OWNED");
      return;
    }
    applyCreateHarvesters(building, numTree, numRock, numIron, numRubber);
  }

  private void buildWeapons(int buildingId, WeaponTier tier, int numWeapons, boolean infinite) {
    var distributable = world.getDistributableTable().getDistributable(buildingId);
    if (!(distributable instanceof Building building) || building.isDead()) {
      notifyRejected("buildWeapons", buildingId, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(building)) {
      notifyRejected("buildWeapons", buildingId, "NOT_OWNED");
      return;
    }
    applyBuildWeapons(building, tier, numWeapons, infinite);
  }

  private void doMagic(int chieftainId, MagicType magic, boolean clearStack) {
    var distributable = world.getDistributableTable().getDistributable(chieftainId);
    if (!(distributable instanceof Unit chief) || chief.isDead()) {
      notifyRejected("doMagic", chieftainId, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(chief)) {
      notifyRejected("doMagic", chieftainId, "NOT_OWNED");
      return;
    }
    applyDoMagic(chief, magic, clearStack);
  }

  private void exitTower(int buildingId) {
    var distributable = world.getDistributableTable().getDistributable(buildingId);
    if (!(distributable instanceof Building building) || building.isDead()) {
      notifyRejected("exitTower", buildingId, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(building)) {
      notifyRejected("exitTower", buildingId, "NOT_OWNED");
      return;
    }
    applyExitTower(building);
  }

  private void trainChieftain(int buildingId, boolean start) {
    var distributable = world.getDistributableTable().getDistributable(buildingId);
    if (!(distributable instanceof Building building) || building.isDead()) {
      notifyRejected("trainChieftain", buildingId, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(building)) {
      notifyRejected("trainChieftain", buildingId, "NOT_OWNED");
      return;
    }
    applyTrainChieftain(building, start);
  }

  private void placeBuilding(int[] selectionIds, BuildingType templateType, int gridX, int gridY) {
    if (!isGridInside(gridX, gridY)) {
      notifyRejected("placeBuilding", Distributable.NO_ID, "OUT_OF_BOUNDS");
      return;
    }
    List<Selectable<?>> selectables = resolveSelectables(selectionIds);
    if (selectables.isEmpty() && selectionIds.length > 0) {
      notifyRejected("placeBuilding", selectionIds[0], "ENTITY_NOT_FOUND");
      return;
    }
    applyPlaceBuilding(selectables, templateType, gridX, gridY);
  }

  private void setRallyPoint(int buildingId, int targetEntityId) {
    var buildingDistributable = world.getDistributableTable().getDistributable(buildingId);
    if (!(buildingDistributable instanceof Building building) || building.isDead()) {
      notifyRejected("setRallyPoint", buildingId, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(building)) {
      notifyRejected("setRallyPoint", buildingId, "NOT_OWNED");
      return;
    }
    var targetDistributable = world.getDistributableTable().getDistributable(targetEntityId);
    if (!(targetDistributable instanceof Target target) || (target instanceof Selectable<?> s && s.isDead())) {
      notifyRejected("setRallyPoint", targetEntityId, "ENTITY_NOT_FOUND");
      return;
    }
    applySetRallyPoint(building, target);
  }

  private void setRallyPoint(int buildingId, int gridX, int gridY) {
    if (!isGridInside(gridX, gridY)) {
      notifyRejected("setRallyPoint", buildingId, "OUT_OF_BOUNDS");
      return;
    }
    var buildingDistributable = world.getDistributableTable().getDistributable(buildingId);
    if (!(buildingDistributable instanceof Building building) || building.isDead()) {
      notifyRejected("setRallyPoint", buildingId, "ENTITY_NOT_FOUND");
      return;
    }
    if (!isValid(building)) {
      notifyRejected("setRallyPoint", buildingId, "NOT_OWNED");
      return;
    }
    applySetRallyPoint(building, gridX, gridY);
  }

  private void setTarget(int[] selectionIds, int targetEntityId, Action action, boolean aggressive) {
    var targetDistributable = world.getDistributableTable().getDistributable(targetEntityId);
    if (!(targetDistributable instanceof Target target) || (target instanceof Selectable<?> s && s.isDead())) {
      notifyRejected("setTarget", targetEntityId, "ENTITY_NOT_FOUND");
      return;
    }
    List<Selectable<?>> selectables = resolveSelectables(selectionIds);
    if (selectables.isEmpty() && selectionIds.length > 0) {
      notifyRejected("setTarget", selectionIds[0], "ENTITY_NOT_FOUND");
      return;
    }
    applySetTarget(selectables, target, action, aggressive);
  }

  private void setLandscapeTarget(int[] selectionIds, int gridX, int gridY, Action action, boolean aggressive) {
    if (!isGridInside(gridX, gridY)) {
      notifyRejected("setLandscapeTarget", Distributable.NO_ID, "OUT_OF_BOUNDS");
      return;
    }
    List<Selectable<?>> selectables = resolveSelectables(selectionIds);
    if (selectables.isEmpty() && selectionIds.length > 0) {
      notifyRejected("setLandscapeTarget", selectionIds[0], "ENTITY_NOT_FOUND");
      return;
    }
    applySetLandscapeTarget(selectables, gridX, gridY, action, aggressive);
  }

  private List<Selectable<?>> resolveSelectables(int[] entityIds) {
    var table = world.getDistributableTable();
    int limit = Math.min(entityIds.length, MAX_SELECTION_SIZE);
    var unique = new LinkedHashSet<Selectable<?>>(limit);
    for (int i = 0; i < limit; i++) {
      var d = table.getDistributable(entityIds[i]);
      if (d instanceof Selectable<?> s && !s.isDead()) {
        unique.add(s);
      }
    }
    return List.copyOf(unique);
  }
}
