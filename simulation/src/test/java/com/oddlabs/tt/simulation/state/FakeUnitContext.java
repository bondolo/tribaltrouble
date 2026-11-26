package com.oddlabs.tt.simulation.state;

import com.oddlabs.tt.simulation.model.Ability;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Supply;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.weapon.Magic;
import com.oddlabs.tt.simulation.model.weapon.MagicFactory;
import com.oddlabs.tt.simulation.pathfinder.Movable;
import com.oddlabs.tt.simulation.pathfinder.PathTracker;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Lightweight, World-free `UnitContext` test double recording actuations and exposing configurable entity state.
@NullMarked
final class FakeUnitContext implements UnitContext {
  private final List<String> events = new ArrayList<>();

  private int gridX;
  private int gridY;
  private boolean dead;
  private final EnumSet<Ability> abilities = EnumSet.noneOf(Ability.class);

  private float attackRange = 2f;
  private @Nullable Selectable<?> scannedEnemy;

  private boolean hasSupplyContainer;
  private int carriedSupplyCount;
  private int maxCarriedSupplyCount;
  private @Nullable SupplyType carriedSupplyType;

  private boolean legacyDeterministic;
  private Random random = new Random(42L);

  private float weaponSecondsPerRelease = 0.88f;
  private boolean buildingPlacementLegal = true;
  private boolean depositFullResult;
  private boolean friendlySupplyContainerBuilding;
  private boolean canEnterBuildingResult = true;

  private @Nullable Supply foundSupply;
  private @Nullable Building foundSupplyBuilding;
  private @Nullable Consumer<Building> enterBuildingHook;

  private final Queue<PathTracker.State> walkSteps = new ArrayDeque<>();
  private PathTracker.State defaultWalkState = PathTracker.State.DONE;
  private @Nullable Movable walkBlocker;
  private @Nullable Target lastWalkTarget;
  private float lastWalkRange;

  private float offsetZ;
  private Unit.@Nullable Animation lastAnimation;
  private float lastAnimationSpeed;
  private @Nullable Magic magicResult;
  private @Nullable Magic lastRegisteredMagic;

  @Override
  public int getGridX() {
    return gridX;
  }

  public void setGridX(int gridX) {
    this.gridX = gridX;
  }

  @Override
  public int getGridY() {
    return gridY;
  }

  public void setGridY(int gridY) {
    this.gridY = gridY;
  }

  public void setGridPosition(int gridX, int gridY) {
    this.gridX = gridX;
    this.gridY = gridY;
  }

  @Override
  public boolean isDead() {
    return dead;
  }

  public void setDead(boolean dead) {
    this.dead = dead;
  }

  @Override
  public boolean hasAbility(Ability ability) {
    return abilities.contains(ability);
  }

  public void addAbility(Ability ability) {
    abilities.add(ability);
  }

  @Override
  public boolean isCloseEnough(float maxDist, Target target) {
    int dx = gridX - target.getGridX();
    int dy = gridY - target.getGridY();
    if (maxDist == 0f) {
      return Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && (dx != 0 || dy != 0);
    }
    int distSquared = dx * dx + dy * dy;
    return distSquared <= maxDist * maxDist;
  }

  @Override
  public float getAttackRange(Target target) {
    return attackRange + target.getSize();
  }

  public void setAttackRange(float attackRange) {
    this.attackRange = attackRange;
  }

  @Override
  public @Nullable Selectable<?> scanForEnemy(int range) {
    events.add("scanForEnemy:" + range);
    return scannedEnemy;
  }

  public void setScannedEnemy(@Nullable Selectable<?> scannedEnemy) {
    this.scannedEnemy = scannedEnemy;
  }

  public void setHasSupplyContainer(boolean hasSupplyContainer) {
    this.hasSupplyContainer = hasSupplyContainer;
  }

  @Override
  public int getCarriedSupplyCount() {
    return carriedSupplyCount;
  }

  public void setCarriedSupplyCount(int carriedSupplyCount) {
    this.carriedSupplyCount = carriedSupplyCount;
  }

  public void setMaxCarriedSupplyCount(int maxCarriedSupplyCount) {
    this.maxCarriedSupplyCount = maxCarriedSupplyCount;
  }

  @Override
  public @Nullable SupplyType getCarriedSupplyType() {
    return carriedSupplyType;
  }

  public void setCarriedSupplyType(@Nullable SupplyType carriedSupplyType) {
    this.carriedSupplyType = carriedSupplyType;
  }

  @Override
  public boolean isSupplyFull() {
    return hasSupplyContainer && carriedSupplyCount == maxCarriedSupplyCount;
  }

  @Override
  public boolean isLegacyDeterministic() {
    return legacyDeterministic;
  }

  public void setLegacyDeterministic(boolean legacyDeterministic) {
    this.legacyDeterministic = legacyDeterministic;
  }

  public void setRandomSeed(long seed) {
    this.random = new Random(seed);
  }

  @Override
  public float nextRandomFloat() {
    return random.nextFloat();
  }

  @Override
  public void switchToIdleAnimation() {
    this.lastAnimation = Unit.Animation.IDLING;
    events.add("switchToIdleAnimation");
  }

  @Override
  public void switchToMovingAnimation() {
    this.lastAnimation = Unit.Animation.MOVING;
    events.add("switchToMovingAnimation");
  }

  @Override
  public void switchAnimation(float speed, Unit.Animation animation) {
    this.lastAnimationSpeed = speed;
    this.lastAnimation = animation;
    events.add("switchAnimation:" + animation.name());
  }

  @Override
  public void initWalkTarget(Target target, float range) {
    this.lastWalkTarget = target;
    this.lastWalkRange = range;
    events.add("initWalkTarget:" + range);
  }

  @Override
  public void initSupplyFinderWalk(SupplyType supplyType) {
    events.add("initSupplyFinderWalk:" + supplyType.name());
  }

  @Override
  public @Nullable Supply getFoundSupply() {
    return foundSupply;
  }

  public void setFoundSupply(@Nullable Supply foundSupply) {
    this.foundSupply = foundSupply;
  }

  @Override
  public void initSupplyBuildingFinderWalk() {
    events.add("initSupplyBuildingFinderWalk");
  }

  @Override
  public @Nullable Building getFoundSupplyBuilding() {
    return foundSupplyBuilding;
  }

  public void setFoundSupplyBuilding(@Nullable Building foundSupplyBuilding) {
    this.foundSupplyBuilding = foundSupplyBuilding;
  }

  @Override
  public PathTracker.State stepWalk(float dt) {
    PathTracker.State next = walkSteps.isEmpty() ? defaultWalkState : walkSteps.poll();
    events.add("stepWalk:" + next.name());
    return next;
  }

  public void enqueueWalkStep(PathTracker.State state) {
    walkSteps.add(state);
  }

  public void enqueueWalkSteps(PathTracker.State... states) {
    Collections.addAll(walkSteps, states);
  }

  public void setDefaultWalkState(PathTracker.State defaultWalkState) {
    this.defaultWalkState = defaultWalkState;
  }

  @Override
  public @Nullable Movable getCurrentWalkBlocker() {
    return walkBlocker;
  }

  public void setWalkBlocker(@Nullable Movable walkBlocker) {
    this.walkBlocker = walkBlocker;
  }

  public @Nullable Target getLastWalkTarget() {
    return lastWalkTarget;
  }

  public float getLastWalkRange() {
    return lastWalkRange;
  }

  public Unit.@Nullable Animation getLastAnimation() {
    return lastAnimation;
  }

  public float getLastAnimationSpeed() {
    return lastAnimationSpeed;
  }

  @Override
  public void aimAtTarget(Target target) {
    events.add("aimAtTarget");
  }

  @Override
  public void setOffsetZ(float z) {
    this.offsetZ = z;
    events.add("setOffsetZ:" + z);
  }

  public float getOffsetZ() {
    return offsetZ;
  }

  @Override
  public float getWeaponSecondsPerRelease(float animSpeed) {
    return weaponSecondsPerRelease;
  }

  public void setWeaponSecondsPerRelease(float weaponSecondsPerRelease) {
    this.weaponSecondsPerRelease = weaponSecondsPerRelease;
  }

  @Override
  public void launchWeaponAttack(Selectable<?> target) {
    events.add("launchWeaponAttack");
  }

  @Override
  public void onHarvestSwing(Supply supply) {
    SupplyType type = supply.getSupplyType();
    if (legacyDeterministic) {
      int soundCount =
          switch (type) {
            case WOOD -> 6;
            case ROCK, IRON, RUBBER -> 5;
          };
      random.nextInt(soundCount);
    }
    events.add("onHarvestSwing:" + type.name());
    if (supply.hit()) {
      if (carriedSupplyType != type) {
        carriedSupplyType = type;
        carriedSupplyCount = 0;
      }
      if (hasSupplyContainer && carriedSupplyCount < maxCarriedSupplyCount) {
        carriedSupplyCount++;
      }
      events.add("harvested:" + type.name());
    }
  }

  @Override
  public void consumeOneWoodForRepair() {
    if (carriedSupplyType == SupplyType.WOOD && carriedSupplyCount > 0) {
      carriedSupplyCount--;
    }
    events.add("consumeOneWoodForRepair");
  }

  @Override
  public void onRepairSwing() {
    if (legacyDeterministic) {
      random.nextInt(6);
    }
    events.add("onRepairSwing");
  }

  @Override
  public void applyRepairPoint(Building building) {
    building.repair(1);
    events.add("applyRepairPoint");
  }

  @Override
  public boolean depositSupplyIntoBuilding(Building building) {
    events.add("depositSupplyIntoBuilding");
    if (!depositFullResult) {
      carriedSupplyCount = 0;
    }
    return depositFullResult;
  }

  public void setDepositFullResult(boolean depositFullResult) {
    this.depositFullResult = depositFullResult;
  }

  @Override
  public boolean isBuildingPlacementLegal(Building building) {
    return buildingPlacementLegal;
  }

  public void setBuildingPlacementLegal(boolean buildingPlacementLegal) {
    this.buildingPlacementLegal = buildingPlacementLegal;
  }

  @Override
  public void placeBuilding(Building building) {
    if (!building.isPlaced()) {
      building.place();
    }
    events.add("placeBuilding");
  }

  @Override
  public boolean isFriendlySupplyContainerBuilding(Building building) {
    return friendlySupplyContainerBuilding;
  }

  public void setFriendlySupplyContainerBuilding(boolean friendlySupplyContainerBuilding) {
    this.friendlySupplyContainerBuilding = friendlySupplyContainerBuilding;
  }

  @Override
  public boolean canEnterBuilding(Building building) {
    return canEnterBuildingResult;
  }

  public void setCanEnterBuildingResult(boolean canEnterBuildingResult) {
    this.canEnterBuildingResult = canEnterBuildingResult;
  }

  public void setEnterBuildingHook(@Nullable Consumer<Building> enterBuildingHook) {
    this.enterBuildingHook = enterBuildingHook;
  }

  @Override
  public void enterBuilding(Building building) {
    events.add("enterBuilding");
    if (enterBuildingHook != null) {
      enterBuildingHook.accept(building);
    }
  }

  @Override
  public void enterBuildingWithoutDeposit(Building building) {
    events.add("enterBuildingWithoutDeposit");
    if (enterBuildingHook != null) {
      enterBuildingHook.accept(building);
    }
  }

  @Override
  public @Nullable Magic executeMagic(MagicFactory factory) {
    events.add("executeMagic");
    return magicResult;
  }

  public void setMagicResult(@Nullable Magic magicResult) {
    this.magicResult = magicResult;
  }

  @Override
  public void registerMagic(Magic magic) {
    this.lastRegisteredMagic = magic;
    events.add("registerMagic");
  }

  public @Nullable Magic getLastRegisteredMagic() {
    return lastRegisteredMagic;
  }

  @Override
  public void removeFromWorld() {
    this.dead = true;
    events.add("removeFromWorld");
  }

  public List<String> getEvents() {
    return Collections.unmodifiableList(events);
  }

  public void clearEvents() {
    events.clear();
  }
}
