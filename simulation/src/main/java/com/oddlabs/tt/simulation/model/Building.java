package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.pathfinder.Occupant;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.state.Gathering;
import com.oddlabs.tt.simulation.state.Transporting;
import java.util.Optional;
import java.util.Random;
import org.jspecify.annotations.Nullable;

/// Represents a static building structure in the game world.
public final class Building extends Selectable<BuildingTemplate> implements Occupant {
  private static final float REMOVE_DELAY = 1f / 10f;

  private static final int PLACING_BORDER = 1;

  private BuildingRole role = BuildingRole.NONE;
  private float remove_delay = 0;
  private int hit_points = 1;
  private int build_points = 0; // 0 = not placed, > 0 = placed
  private float[][] old_landscape_heights;

  /// Represents the construction stages of a building in the game world.
  public enum BuildStage {
    /// The building template has not yet been placed on the terrain grid.
    UNPLACED,
    /// Construction has started but is under 50% completed.
    START,
    /// Construction progress is between 50% and 99%.
    HALFBUILT,
    /// Construction is completed (100% built).
    BUILT
  }

  private final @Nullable LegacyDamagedSmokeEmitter damaged_emitter;

  public Building(Player owner, BuildingTemplate template, int grid_x, int grid_y) {
    super(owner, template);
    setGridPosition(grid_x, grid_y);
    float x = UnitGrid.coordinateFromGrid(grid_x);
    float y = UnitGrid.coordinateFromGrid(grid_y);
    setPosition(x, y);
    if (AppConfig.LEGACY_DETERMINISTIC) {
      damaged_emitter = new LegacyDamagedSmokeEmitter(owner.getWorld());
      owner.getWorld().getAnimationManagerRealTime().registerAnimation(damaged_emitter);
    } else {
      damaged_emitter = null;
    }
  }

  public BuildingRole getRole() {
    return role;
  }

  public boolean hasRallyPoint() {
    return role.getDeployment().map(UnitDeployment::hasRallyPoint).orElse(false);
  }

  public Target getRallyPoint() {
    return role.getDeployment().map(UnitDeployment::getRallyPoint).orElse(this);
  }

  public boolean isProducing() {
    return role.isProducing();
  }

  @Override
  protected void doAnimate(float t) {
    if (!isDead()) {
      role.animate(t);
    }

    if (remove_delay > 0) {
      remove_delay -= t;
      if (remove_delay <= 0) {
        remove();
        if (AppConfig.LEGACY_DETERMINISTIC) {
          int numFragments = getTemplate().getNumFragments();
          getWorld()
              .getAnimationManagerRealTime()
              .registerAnimation(
                  new LegacyOneShotEmitter(
                      getWorld(),
                      getWorld().getAnimationManagerRealTime(),
                      numFragments,
                      (float) numFragments,
                      0.05f,
                      (float) Math.PI,
                      4));
        }
      }
    }
  }

  @Override
  public void remove() {
    super.remove();
    role.onRemove();
    if (damaged_emitter != null) {
      getOwner().getWorld().getAnimationManagerRealTime().removeAnimation(damaged_emitter);
    }
    getWorld().getNotificationListener().onModelRemoved(this);
  }

  public @Nullable UnitContainer getRawUnitContainer() {
    assert !isDead();
    return role.getRawUnitContainer();
  }

  public Optional<UnitContainer> getUnitContainer() {
    assert !isDead();
    return role.getUnitContainer();
  }

  public Optional<SupplyContainer> getSupplyContainer(SupplyType key) {
    assert !isDead();
    return role.getSupplyStorage().flatMap(s -> s.getSupplyContainer(key));
  }

  public @Nullable SupplyContainer getSupplyContainerDirect(SupplyType key) {
    assert !isDead();
    return role.getSupplyStorage().map(s -> s.getSupplyContainerDirect(key)).orElse(null);
  }

  public Optional<SupplyContainer> getWeaponContainer(WeaponTier tier) {
    assert !isDead();
    return role.getWeaponsWorkshop().flatMap(w -> w.getWeaponContainer(tier));
  }

  public int getWeaponSupplyCount(WeaponTier tier) {
    return role.getWeaponsWorkshop().map(w -> w.getWeaponSupplyCount(tier)).orElse(0);
  }

  public Optional<BuildProductionContainer> getWeaponBuildContainer(WeaponTier tier) {
    assert !isDead();
    return role.getWeaponsWorkshop().flatMap(w -> w.getWeaponBuildContainer(tier));
  }

  public @Nullable DeployContainer getDeployContainer(DeployType type) {
    assert !isDead();
    return role.getDeployment().map(d -> d.getContainer(type)).orElse(null);
  }

  public Optional<ChieftainContainer> getChieftainContainer() {
    assert !isDead();
    return role.getChieftainContainer();
  }

  public boolean hasSupplyStorage() {
    return role.getSupplyStorage().isPresent();
  }

  public boolean hasWeaponsWorkshop() {
    return role.getWeaponsWorkshop().isPresent();
  }

  public boolean canReproduce() {
    return role.canReproduce();
  }

  public boolean canAcceptRally() {
    return role.canAcceptRally();
  }

  @Override
  public boolean isEnabled() {
    return !isDead();
  }

  @Override
  public float getSelectionRadius() {
    return getTemplate().getSelectionRadius(getBuildStage());
  }

  @Override
  public float getSelectionHeight() {
    return getTemplate().getSelectionHeight(getBuildStage());
  }

  public int getUnitCount() {
    assert !isDead();
    UnitContainer unitContainer = getRawUnitContainer();
    return unitContainer != null ? unitContainer.getNumSupplies() : 0;
  }

  public boolean canExitTower() {
    return !isDead() && role.canExitTower();
  }

  public void exitTower() {
    assert !isDead();
    role.exitTower();
  }

  public boolean canDeployUnits(DeployType type) {
    return !isDead() && isBuilt() && role.getDeployment().map(d -> d.canDeploy(type)).orElse(false);
  }

  public void deployUnits(DeployType type, int num_units) {
    assert !isDead();
    role.getDeployment().ifPresent(d -> d.deploy(type, num_units));
  }

  void createHarvesters(SupplyType supplyType, int amount) {
    RaceInfo raceInfo = getOwner().getRaceInfo();
    UnitContainer unitContainer = getRawUnitContainer();
    for (int i = 0; i < amount; i++) {
      if (unitContainer != null) {
        unitContainer.prepareDeploy(-1);
        unitContainer.exit();
      }
      Unit unit = spawnUnit(null, raceInfo.getUnitTemplate(UnitType.PEON));
      unit.pushOrder(new Gathering(null, supplyType, unit.getCurrentState()));
    }
  }

  public boolean canBuildWeapons(WeaponTier tier) {
    return !isDead() && isBuilt() && role.getWeaponsWorkshop().map(w -> w.canBuildWeapons(tier)).orElse(false);
  }

  public void buildWeapons(WeaponTier tier, int num_weapons, boolean infinite) {
    assert !isDead();
    role.getWeaponsWorkshop().ifPresent(w -> w.buildWeapons(tier, num_weapons, infinite));
  }

  public boolean canBuildChieftain() {
    return !isDead() && role.canBuildChieftain();
  }

  public boolean canStopChieftain() {
    return !isDead() && role.canStopChieftain();
  }

  public boolean isTrainingChieftain() {
    return !isDead() && role.isTrainingChieftain();
  }

  public void trainChieftain(boolean start) {
    role.trainChieftain(start);
  }

  public void deployChieftain() {
    role.deployChieftain();
  }

  Unit spawnUnit(@Nullable Target rallyPoint, UnitTemplate template) {
    return new Unit(getOwner(), getPositionX(), getPositionY(), rallyPoint, template, null, true, true);
  }

  void createWarriors(WeaponTier tier, int amount) {
    assert !isDead();
    createArmy(amount, tier.getUnitType());
  }

  void createPeons(int amount) {
    assert !isDead();
    createArmy(amount, UnitType.PEON);
  }

  private void createArmy(int amount, UnitType template) {
    RaceInfo raceInfo = getOwner().getRaceInfo();
    Target activeRally = resolveActiveRallyPoint();
    UnitContainer unitContainer = getRawUnitContainer();
    boolean spawnTransporters = activeRally == null && role.spawnsUnralliedPeonsAsTransporters();
    for (int i = 0; i < amount; i++) {
      if (unitContainer != null) {
        unitContainer.prepareDeploy(-1);
        unitContainer.exit();
      }
      Unit unit = spawnUnit(activeRally, raceInfo.getUnitTemplate(template));
      if (spawnTransporters) {
        unit.pushOrder(new Transporting(unit.getCurrentState()));
      }
    }
  }

  void createTransporters(SupplyType supply, int amount) {
    assert !isDead();
    RaceInfo raceInfo = getOwner().getRaceInfo();
    Target activeRally = resolveActiveRallyPoint();
    UnitContainer unitContainer = getRawUnitContainer();
    for (int i = 0; i < amount; i++) {
      if (unitContainer != null) {
        unitContainer.prepareDeploy(-1);
        unitContainer.exit();
      }
      Unit unit = spawnUnit(activeRally, raceInfo.getUnitTemplate(UnitType.PEON));
      unit.getSupplyContainer().increaseSupply(unit.getSupplyContainer().getMaxSupplyCount(), supply);
    }
  }

  public void createTransporters(int num_tree, int num_rock, int num_iron, int num_rubber) {
    assert !isDead();
    if (num_tree > 0) createTransporters(SupplyType.WOOD, num_tree);
    if (num_rock > 0) createTransporters(SupplyType.ROCK, num_rock);
    if (num_iron > 0) createTransporters(SupplyType.IRON, num_iron);
    if (num_rubber > 0) createTransporters(SupplyType.RUBBER, num_rubber);
  }

  private @Nullable Target resolveActiveRallyPoint() {
    return role.getDeployment().map(UnitDeployment::resolveActiveRallyPoint).orElse(null);
  }

  public boolean isDamaged() {
    assert !isDead();
    return hit_points > 0 && hit_points < getTemplate().getMaxHitPoints();
  }

  public int getHitPoints() {
    return hit_points;
  }

  private void adjustHitPoints(int amount) {
    setHitPoints(hit_points + amount);
  }

  private void setHitPoints(int new_hit_points) {
    final int START_SMOKE = getTemplate().getMaxHitPoints() / 2;
    hit_points = Math.clamp(new_hit_points, 0, getTemplate().getMaxHitPoints());
    if (damaged_emitter != null) {
      if (build_points == getTemplate().getMaxHitPoints() && hit_points < START_SMOKE) {
        damaged_emitter.start();
      } else {
        damaged_emitter.stop();
      }
    }
  }

  public void repair(int amount) {
    assert !isDead();
    assert isPlaced();
    if (!isDamaged()) return;

    setHitPoints(hit_points + amount);
    if (!isBuilt()) {
      var max_hitpoints = getTemplate().getMaxHitPoints();
      build_points = Math.min(build_points + amount, max_hitpoints);
      reinsert();
      if (build_points == max_hitpoints) {
        buildingCompleted();
      }
    }
  }

  public static boolean isPlacingLegal(UnitGrid unit_grid, BuildingTemplate template, int grid_x, int grid_y) {
    return doIsPlacingLegal(unit_grid, grid_x, grid_y, template.getPlacingSize());
  }

  public boolean isPlacingLegal() {
    return !isDead()
        && getOwner().canBuild(getTemplate().getBuildingType())
        && doIsPlacingLegal(getUnitGrid(), getGridX(), getGridY(), getTemplate().getPlacingSize() - PLACING_BORDER);
  }

  public boolean isPlaced() {
    assert !isDead();
    return getBuildStage() != BuildStage.UNPLACED;
  }

  public boolean isBuilt() {
    return build_points == getTemplate().getMaxHitPoints();
  }

  public boolean isOperating() {
    return !isDead() && isBuilt();
  }

  @Override
  public EntityActivity getActivity() {
    if (isDead()) {
      return EntityActivity.DEAD;
    }
    if (getBuildStage() != BuildStage.BUILT) {
      return EntityActivity.UNDER_CONSTRUCTION;
    }
    if (isProducing()) {
      return EntityActivity.ACTIVE;
    }
    return isOperating() ? EntityActivity.IDLE : EntityActivity.OTHER;
  }

  public int getBuildPoints() {
    return build_points;
  }

  public BuildStage getBuildStage() {
    var max_points = getTemplate().getMaxHitPoints();
    return build_points == max_points
        ? BuildStage.BUILT
        : (float) build_points / max_points > .5f
            ? BuildStage.HALFBUILT
            : build_points > 0 ? BuildStage.START : BuildStage.UNPLACED;
  }

  private void buildingCompleted() {
    getOwner().getWorld().getNotificationListener().newSelectableNotification(this);
    getAbilities().addAll(getTemplate().getAbilities());
    role = getTemplate().getRoleFactory().createRole(this);
  }

  @Override
  public float getHitOffsetZ() {
    int index =
        switch (getBuildStage()) {
          case START -> 0;
          case HALFBUILT -> 1;
          case UNPLACED, BUILT -> 2;
        };
    return getTemplate().getHitOffsetZ(index);
  }

  public static boolean doIsPlacingLegal(UnitGrid unit_grid, int grid_x, int grid_y, int size) {
    if (!unit_grid.getHeightMap().canBuild(grid_x, grid_y, size)) return false;

    for (int y = 0; y < size * 2 - 1; y++) {
      for (int x = 0; x < size * 2 - 1; x++) {
        int current_grid_x = grid_x + x - (size - 1);
        int current_grid_y = grid_y + y - (size - 1);
        if (current_grid_x >= unit_grid.getGridSize()
            || current_grid_y >= unit_grid.getGridSize()
            || current_grid_x < 0
            || current_grid_y < 0
            || unit_grid.isGridOccupied(current_grid_x, current_grid_y)) return false;
      }
    }
    return true;
  }

  @Override
  public AttackScanFilter.Priority getAttackPriority() {
    return role.getAttackPriority();
  }

  @Override
  protected void setTarget(Target target, Action action, boolean aggressive) {
    role.onTarget(target, action, aggressive);
  }

  public void place() {
    assert !isDead();
    assert isPlacingLegal();
    register();
    occupy();
    flattenLandscape();
    int result = getOwner().getBuildingCountContainer().increaseSupply(1);
    assert (result == 1) : "Too many buildings";
    build_points = 1;
    reinsert();
  }

  @Override
  public float getSize() {
    assert !isDead();
    float radius = (getTemplate().getPlacingSize() - 1);
    return (float) Math.sqrt(2) * radius + .1f;
  }

  @Override
  public int getPenalty() {
    assert !isDead();
    return Occupant.STATIC;
  }

  @Override
  public void removeDying() {
    remove_delay = REMOVE_DELAY;
    role.onRemoveDying();
    free();
    undoLandscape();
    int result = getOwner().getBuildingCountContainer().increaseSupply(-1);
    assert result == -1;
    if (AppConfig.LEGACY_DETERMINISTIC) {
      getWorld()
          .getAnimationManagerRealTime()
          .registerAnimation(
              new LegacyOneShotEmitter(getWorld(), getWorld().getAnimationManagerRealTime(), 30, 400f, 1f, 1f, 1));
    }
    super.removeDying();
  }

  public boolean isValidRallyPoint(Target t) {
    return UnitDeployment.isValidRallyPoint(this, t);
  }

  public boolean canSetRallyPoint() {
    return !isDead() && role.getDeployment().map(UnitDeployment::canSetRallyPoint).orElse(false);
  }

  public void setRallyPoint(Target target) {
    role.getDeployment().ifPresent(d -> d.setRallyPoint(target));
  }

  @Override
  public float getShadowOpacity() {
    return switch (getBuildStage()) {
      case UNPLACED, START -> 0.0f;
      case HALFBUILT, BUILT -> super.getShadowOpacity();
    };
  }

  @Override
  protected BoundingBox[] getLocalBounds() {
    return switch (getBuildStage()) {
      case START -> getTemplate().getStartBounds();
      case HALFBUILT -> getTemplate().getHalfbuiltBounds();
      case UNPLACED, BUILT -> getTemplate().getBuiltBounds();
    };
  }

  private void flattenLandscape() {
    int size = getTemplate().getPlacingSize();
    int height_points = (size - PLACING_BORDER) * 2;
    int offset_x = getGridX() - (size - 1);
    int offset_y = getGridY() - (size - 1);
    float total_height = 0;
    old_landscape_heights = new float[height_points][height_points];
    for (int y = 0; y < height_points; y++) {
      for (int x = 0; x < height_points; x++) {
        float old_height =
            getOwner()
                .getWorld()
                .getHeightMap()
                .getWrappedHeight(offset_x + x + PLACING_BORDER, offset_y + y + PLACING_BORDER);
        old_landscape_heights[y][x] = old_height;
        total_height += old_height;
      }
    }

    float new_height = total_height / (height_points * height_points);
    for (int y = 0; y < height_points; y++) {
      for (int x = 0; x < height_points; x++) {
        getOwner()
            .getWorld()
            .getHeightMap()
            .editHeight(offset_x + x + PLACING_BORDER, offset_y + y + PLACING_BORDER, new_height);
      }
    }
  }

  private void undoLandscape() {
    int size = getTemplate().getPlacingSize();
    int offset_x = getGridX() - (size - 1);
    int offset_y = getGridY() - (size - 1);
    for (int y = 0; y < old_landscape_heights.length; y++) {
      for (int x = 0; x < old_landscape_heights[y].length; x++) {
        getOwner()
            .getWorld()
            .getHeightMap()
            .editHeight(offset_x + x + PLACING_BORDER, offset_y + y + PLACING_BORDER, old_landscape_heights[y][x]);
      }
    }
  }

  private void occupy() {
    UnitGrid grid = getUnitGrid();
    grid.getRegion(getGridX(), getGridY()).registerBuilding(this);
    int size = getTemplate().getPlacingSize() * 2 - 1;
    for (int y = PLACING_BORDER; y < size - PLACING_BORDER; y++) {
      for (int x = PLACING_BORDER; x < size - PLACING_BORDER; x++) {
        grid.occupyGrid(getGridX() - size / 2 + x, getGridY() - size / 2 + y, this);
      }
    }
  }

  private void free() {
    UnitGrid grid = getUnitGrid();
    grid.getRegion(getGridX(), getGridY()).unregisterBuilding(this);
    int size = getTemplate().getPlacingSize() * 2 - 1;
    for (int y = PLACING_BORDER; y < size - PLACING_BORDER; y++) {
      for (int x = PLACING_BORDER; x < size - PLACING_BORDER; x++) {
        grid.freeGrid(getGridX() - size / 2 + x, getGridY() - size / 2 + y, this);
      }
    }
  }

  @Override
  public void hit(int damage, float dir_x, float dir_y, Player owner) {
    super.hit(damage, dir_x, dir_y, owner);
    if (!isDead()) {
      if (AppConfig.LEGACY_DETERMINISTIC) {
        getWorld().getRandom().nextInt(4);
      }
      adjustHitPoints(-damage);
      getWorld().getNotificationListener().onBuildingHit(this);
      if (hit_points <= 0) {
        // stats
        getOwner().buildingLost();
        owner.buildingDestroyed();
        role.onDestroyed();
        removeDying();
      }
    }
  }

  @Override
  public String toString() {
    return "Building: isDead() = " + isDead();
  }

  public void fillUnits(int max) {
    getUnitContainer()
        .ifPresent(container -> {
          container.increaseSupply(Math.min(container.getMaxSupplyCount() - container.getNumSupplies(), max));
        });
  }

  public void removeUnits() {
    getUnitContainer()
        .ifPresent(container -> {
          container.increaseSupply(-container.getNumSupplies());
        });
  }

  public void fillWeapons(WeaponTier tier, int max) {
    getWeaponContainer(tier)
        .ifPresent(container -> {
          container.increaseSupply(Math.min(container.getMaxSupplyCount() - container.getNumSupplies(), max));
        });
  }

  public void removeWeapons(WeaponTier tier) {
    getWeaponContainer(tier)
        .ifPresent(container -> {
          container.increaseSupply(-container.getNumSupplies());
        });
  }

  @Override
  public int getStatusValue() {
    return role.getStatusValue();
  }

  public void printDebugInfo() {
    IO.println("-----------------------------------");
    role.printDebugInfo();
  }

  /// Simulates legacy deterministic smoke emitter particle timing and PRNG draws for damaged buildings.
  private static final class LegacyDamagedSmokeEmitter implements Animated {
    private final World world;
    private float particleCounter = 0f;
    private boolean started = false;
    private float xAngle = 0f;
    private float yAngle = 0f;

    LegacyDamagedSmokeEmitter(World world) {
      this.world = world;
    }

    void start() {
      started = true;
    }

    void stop() {
      started = false;
    }

    @Override
    public void animate(float t) {
      if (started) {
        particleCounter += 4f * t;
      }
      while (particleCounter >= 1f && started) {
        initParticle();
        particleCounter -= 1f;
      }
    }

    private void initParticle() {
      randomizeVelocity();
      world.getRandom().nextInt(1);
    }

    private void randomizeVelocity() {
      Random random = world.getRandom();
      float angleMaxJump = 0.7f;
      float angleBound = 0.5f;
      float dxAngle = random.nextFloat() * angleMaxJump - .5f * angleMaxJump;
      float dyAngle = random.nextFloat() * angleMaxJump - .5f * angleMaxJump;

      if ((xAngle + dxAngle < -angleBound) || (xAngle + dxAngle > angleBound)) {
        xAngle -= dxAngle;
      } else {
        xAngle += dxAngle;
      }

      if ((yAngle + dyAngle < -angleBound) || (yAngle + dyAngle > angleBound)) {
        yAngle -= dyAngle;
      } else {
        yAngle += dyAngle;
      }
    }
  }
}
