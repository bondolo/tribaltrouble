package com.oddlabs.tt.simulation.model;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.model.weapon.Magic;
import com.oddlabs.tt.simulation.model.weapon.MagicFactory;
import com.oddlabs.tt.simulation.model.weapon.WeaponFactory;
import com.oddlabs.tt.simulation.pathfinder.FinderTrackerAlgorithm;
import com.oddlabs.tt.simulation.pathfinder.Movable;
import com.oddlabs.tt.simulation.pathfinder.Occupant;
import com.oddlabs.tt.simulation.pathfinder.PathTracker;
import com.oddlabs.tt.simulation.pathfinder.ScanFilter;
import com.oddlabs.tt.simulation.pathfinder.TargetTrackerAlgorithm;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.state.CastingMagic;
import com.oddlabs.tt.simulation.state.Defending;
import com.oddlabs.tt.simulation.state.Dying;
import com.oddlabs.tt.simulation.state.EnteringBuilding;
import com.oddlabs.tt.simulation.state.Gathering;
import com.oddlabs.tt.simulation.state.Hunting;
import com.oddlabs.tt.simulation.state.Idling;
import com.oddlabs.tt.simulation.state.PlacingBuilding;
import com.oddlabs.tt.simulation.state.Repairing;
import com.oddlabs.tt.simulation.state.Stunned;
import com.oddlabs.tt.simulation.state.UnitContext;
import com.oddlabs.tt.simulation.state.UnitState;
import com.oddlabs.tt.simulation.state.UnitStateMachine;
import com.oddlabs.tt.simulation.state.Walking;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// Represents a mobile unit in the game world.
public final class Unit extends Selectable<UnitTemplate> implements Occupant, Movable, UnitContext {

  private static final float IDLE_SPEED = 1f / 2.5f;

  // Unit speed scale when transporting resources.
  private static final float TRANSPORT_SPEED_SCALE = 4f / 5f;

  private static final int PENALTY_INCREMENT = 3;
  private static final int INITIAL_PATH_PENALTY = 5;

  /// Animation clip indices for unit models.
  public enum Animation {
    IDLING,
    MOVING,
    THROWING, // or Harvest/Build/Hit for peon/chieftan
    DYING,
    MAGIC,
    THOR
  }

  // Non-null for peons, which can carry resources.
  private final @Nullable UnitSupplyContainer supply_container;
  private final @Nullable MagicCapability magicCapability;

  private final @Nullable String name;
  private final PathTracker path_tracker;

  private int hit_points;
  private Animation animation = Animation.IDLING;
  private float anim_speed;
  private float anim_time;
  private int path_penalty;

  private @Nullable UnitMount mount;
  private boolean legacyDeferMountRangeBonus;
  private float deathOffsetZ;
  private @Nullable FinderTrackerAlgorithm<? extends Supply> activeSupplyFinderTracker;
  private @Nullable FinderTrackerAlgorithm<Building> activeBuildingFinderTracker;
  private UnitState defaultState = new Idling(AttackScanFilter.UNIT_RANGE, true);
  private final UnitStateMachine stateMachine;

  public Unit(Player owner, float x, float y, @Nullable Target rally_point, UnitTemplate unit_template) {
    this(owner, x, y, rally_point, unit_template, null, true, false);
  }

  public Unit(
      Player owner,
      float x,
      float y,
      @Nullable Target rally_point,
      UnitTemplate unit_template,
      @Nullable String name,
      boolean notify_by_chieftain) {
    this(owner, x, y, rally_point, unit_template, name, notify_by_chieftain, false);
  }

  public Unit(
      Player owner,
      float x,
      float y,
      @Nullable Target rally_point,
      UnitTemplate unit_template,
      @Nullable String name,
      boolean notify_by_chieftain,
      boolean grid_targets_only) {
    super(owner, unit_template);
    this.name = name;
    getAbilities().addAbilities(unit_template.getAbilities());
    register();
    hit_points = unit_template.getMaxHitPoints();
    this.path_tracker = new PathTracker(getUnitGrid(), this);
    UnitSupplyContainerFactory factory = unit_template.getUnitSupplyContainerFactory();
    supply_container = factory != null ? (UnitSupplyContainer) factory.createContainer(this) : null;
    magicCapability = unit_template.hasMagic() ? new MagicCapability() : null;

    findInitialPosition(x, y, grid_targets_only);
    this.stateMachine = new UnitStateMachine(defaultState, this);
    if (!isChieftain()) {
      int result = getOwner().getUnitCountContainer().increaseSupply(1);
      assert (result == 1) : "No room for new unit in player unit container.";
    } else if (notify_by_chieftain) {
      owner.getWorld().getNotificationListener().newSelectableNotification(this);
    }
    if (rally_point != null) {
      Target unit_target;
      if (rally_point instanceof LandscapeTarget lt) {
        UnitGrid grid = getUnitGrid();
        List<Target> temp_occupants = new ArrayList<>();
        for (var s : getOwner().getUnits().getSet()) {
          if (s instanceof Unit u) {
            Target target = u.getStateMachine().getWalkTarget();
            if (target != null) {
              if (!grid.isGridOccupied(target.getGridX(), target.getGridY())) {
                grid.occupyGrid(target.getGridX(), target.getGridY(), this);
                temp_occupants.add(target);
              }
            }
          }
        }
        unit_target = grid.findGridTarget(lt.getGridX(), lt.getGridY(), true);
        for (Target target : temp_occupants) {
          grid.freeGrid(target.getGridX(), target.getGridY(), this);
        }
      } else unit_target = rally_point;

      boolean aggressive = unit_template.getAbilities().hasAbilities(Abilities.THROW);
      applyTarget(unit_target, Action.DEFAULT, aggressive, false);
    }
  }

  @Override
  protected Unit self() {
    return this;
  }

  @Override
  protected float getZError() {
    return getLandscapeError();
  }

  public @Nullable UnitSupplyContainer getSupplyContainer() {
    return supply_container;
  }

  public Optional<MagicCapability> getMagicCapability() {
    return Optional.ofNullable(magicCapability);
  }

  public Optional<UnitMount> getMount() {
    return Optional.ofNullable(mount);
  }

  public boolean isPeon() {
    return getTemplate().getUnitType().isPeon();
  }

  public boolean isWarrior() {
    return getTemplate().getUnitType().isWarrior();
  }

  public boolean isChieftain() {
    return getTemplate().getUnitType().isChieftain();
  }

  @Override
  public String toString() {
    if (!isDead())
      return "Unit: "
          + hashCode()
          + " | getOwner() = "
          + getOwner()
          + " | mounted = "
          + isMounted()
          + " | getGridX() = "
          + getGridX()
          + " | getGridY() = "
          + getGridY();
    else return super.toString();
  }

  private void findInitialPosition(float x, float y, boolean grid_targets_only) {
    UnitGrid unit_grid = getUnitGrid();
    Target reserved_target =
        Objects.requireNonNull(
            unit_grid.findGridTarget(UnitGrid.toGridCoordinate(x), UnitGrid.toGridCoordinate(y), grid_targets_only));
    setGridPosition(reserved_target.getGridX(), reserved_target.getGridY());
    setPosition(reserved_target.getPositionX(), reserved_target.getPositionY());

    // The original game left units facing +X; orienting towards the world center is a modern addition.
    if (!AppConfig.LEGACY_DETERMINISTIC) {
      float center = getOwner().getWorld().getHeightMap().getMetersPerWorld() / 2f;
      float dx = center - reserved_target.getPositionX();
      float dy = center - reserved_target.getPositionY();
      float len = (float) Math.hypot(dx, dy);
      if (len > 0) {
        setDirection(dx / len, dy / len);
      }
    }

    occupy();
    reinsert();
    capturePreviousTransform();
  }

  @Override
  public int getStatusValue() {
    int mountFactor = mount != null ? mount.statusValueMultiplier() : 1;
    return getTemplate().getStatusValue() * mountFactor;
  }

  @Override
  public AttackScanFilter.Priority getAttackPriority() {
    assert !isDead();
    return isPeon() ? AttackScanFilter.Priority.PEON : AttackScanFilter.Priority.WARRIOR;
  }

  public @Nullable String getName() {
    return name;
  }

  public int getHitPoints() {
    return hit_points;
  }

  public void unmount() {
    assert !isDead();
    assert mount != null : "Unit is not mounted";
    UnitMount previous = mount;
    float exitX = previous.unmountPositionX();
    float exitY = previous.unmountPositionY();
    if (AppConfig.LEGACY_DETERMINISTIC) {
      replaceDefaultAndRequestOrder(new Idling(AttackScanFilter.UNIT_RANGE, true));
      mount = null;
    } else {
      mount = null;
      replaceDefaultAndRequestOrder(new Idling(AttackScanFilter.UNIT_RANGE, true));
    }
    enable();
    findInitialPosition(exitX, exitY, true);
  }

  public void mount(Building building) {
    mount(new TowerMount(building));
  }

  public void mount(UnitMount newMount) {
    assert !isDead();
    assert mount == null : "Unit is already mounted in " + mount;
    mount = newMount;
    disable();
    free();
    setPosition(newMount.host().getPositionX(), newMount.host().getPositionY());
    if (AppConfig.LEGACY_DETERMINISTIC) {
      legacyDeferMountRangeBonus = true;
      try {
        replaceDefaultAndRequestOrder(newMount.mountedDefaultState());
      } finally {
        legacyDeferMountRangeBonus = false;
      }
    } else {
      replaceDefaultAndRequestOrder(newMount.mountedDefaultState());
    }
  }

  public boolean isMounted() {
    return mount != null;
  }

  @Override
  public boolean isEnabled() {
    return !isDead() && !isMounted();
  }

  @Override
  public float getSelectionRadius() {
    return getTemplate().getSelectionRadius();
  }

  @Override
  public float getSelectionHeight() {
    return getTemplate().getSelectionHeight();
  }

  public float getMetersPerSecond() {
    assert !isDead();
    return supply_container != null && supply_container.getNumSupplies() > 0
        ? TRANSPORT_SPEED_SCALE * getTemplate().getMetersPerSecond()
        : getTemplate().getMetersPerSecond();
  }

  @Override
  public void aimAtTarget(Target target) {
    assert !isDead();
    float dx = target.getPositionX() - getPositionX();
    float dy = target.getPositionY() - getPositionY();
    float dir_len_inv = 1f / (float) Math.hypot(dx, dy);
    dx *= dir_len_inv;
    dy *= dir_len_inv;
    setDirection(dx, dy);
  }

  @Override
  public void switchToIdleAnimation() {
    assert !isDead();
    switchAnimation(IDLE_SPEED, Animation.IDLING);
  }

  @Override
  public void switchToMovingAnimation() {
    assert !isDead();
    switchAnimation(getMetersPerSecond(), Animation.MOVING);
  }

  public WeaponFactory getWeaponFactory() {
    assert !isDead();
    return getTemplate().getWeaponFactory();
  }

  public float getRange(Target target) {
    assert !isDead();
    float rangeBonus = (mount != null && !legacyDeferMountRangeBonus) ? mount.rangeBonus() : 0f;
    return getWeaponFactory().getRange() + rangeBonus + target.getSize();
  }

  @Override
  public float getSize() {
    assert !isDead();
    return 1.9f;
  }

  @Override
  protected BoundingBox[] getLocalBounds() {
    return getTemplate().getBounds();
  }

  @Override
  public void doAnimate(float t) {
    getStateMachine().animate(t, this);
    anim_time += anim_speed * t;
    if (isDead() || isMounted()) reinsert();
    getOwner().getWorld().updateGlobalChecksum(animation.ordinal());

    if (magicCapability != null) {
      magicCapability.rechargeAll(getOwner().getRaceInfo().getMagics(), t);
    }
  }

  public void increaseMagicEnergy(MagicType type, float amount) {
    if (magicCapability != null) {
      magicCapability.increaseEnergy(type, amount);
    }
  }

  public void maxMagicEnergy(MagicType type) {
    if (magicCapability != null) {
      magicCapability.maxEnergy(type);
    }
  }

  @Override
  public PathTracker getTracker() {
    assert !isDead();
    return path_tracker;
  }

  @Override
  public void markBlocking() {
    assert !isDead();
    path_penalty = Math.min(path_penalty + PENALTY_INCREMENT, STATIC - 1); // never gets STATIC
  }

  @Override
  public int getPenalty() {
    assert !isDead();
    return isBlocking() ? Occupant.STATIC : path_penalty;
  }

  @Override
  public void removeDying() {
    if (isChieftain()) {
      getOwner().setActiveChieftain(null);
    }
    if (mount != null) {
      mount = null;
    } else {
      free();
    }
    if (!isChieftain()) {
      int result = getOwner().getUnitCountContainer().increaseSupply(-1);
      assert result == -1;
    }
    super.removeDying();
  }

  public void removeNow() {
    assert !isDead();
    removeDying();
    remove();
  }

  @Override
  public void free() {
    assert !isDead();
    UnitGrid unit_grid = getUnitGrid();
    unit_grid.freeGrid(getGridX(), getGridY(), this);
    path_penalty = INITIAL_PATH_PENALTY;
  }

  @Override
  public void occupy() {
    assert !isDead();
    UnitGrid unit_grid = getUnitGrid();
    unit_grid.occupyGrid(getGridX(), getGridY(), this);

    // stats
    getOwner().unitMoved();
  }

  @Override
  public boolean isMoving() {
    return getStateMachine().isMoving();
  }

  @Override
  public void hit(int damage, float direction_x, float direction_y, Player owner) {
    super.hit(damage, direction_x, direction_y, owner);
    if (mount != null && mount.forwardsDamageToHost()) {
      mount.host().hit(damage, direction_x, direction_y, owner);
    } else if (!isDead()) {
      hit_points = Math.clamp(hit_points - damage, 0, getTemplate().getMaxHitPoints());
      if (hit_points == 0) {
        // stats
        owner.unitKilled();
        getOwner().unitLost();

        getWorld().getNotificationListener().onUnitDeath(this);

        forceStateTransition(new Dying());
        if (AppConfig.LEGACY_DETERMINISTIC) {
          getWorld().getRandom().nextFloat();
        }
        setDirection(-direction_x, -direction_y);
        removeDying();
      }
    }
  }

  public void stun(float time) {
    if (AppConfig.LEGACY_DETERMINISTIC) {
      var rand = getWorld().getRandom();
      for (int i = 0; i < 5; i++) {
        rand.nextFloat();
        rand.nextFloat();
        rand.nextFloat();
        rand.nextInt(1);
      }
    }
    forceStateTransition(
        Stunned.preempt(time, getStateMachine().getActiveOrPendingState(), AppConfig.LEGACY_DETERMINISTIC));
  }

  public boolean canAttack(Target target, boolean kill_friendly) {
    assert !isDead();
    if (!(target instanceof Selectable<?> selectable) || !getAbilities().hasAbilities(Abilities.ATTACK)) return false;
    Player target_player = selectable.getOwner();
    return kill_friendly || getOwner().isEnemy(target_player);
  }

  private boolean canBuild(Target target) {
    return target instanceof Building building && getAbilities().hasAbilities(Abilities.BUILD) && !building.isPlaced();
  }

  private boolean canGather(Target target) {
    return target instanceof Supply && getAbilities().hasAbilities(Abilities.BUILD);
  }

  private boolean canRepair(Target target, boolean action_repair) {
    return target instanceof Building building
        && getAbilities().hasAbilities(Abilities.BUILD)
        && (action_repair || !building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER) || !building.isBuilt())
        &&
        // getOwner() == building.getOwner() && building.isPlaced() && building.isDamaged();
        !getOwner().isEnemy(building.getOwner())
        && building.isPlaced()
        && building.isDamaged();
  }

  private boolean canEnter(Target target) {
    return target instanceof Building building
        && !isChieftain()
        && getOwner() == building.getOwner()
        && building.getUnitContainer().map(c -> c.canEnter(this)).orElse(false);
  }

  @Override
  public float getDefenseChance() {
    return isStunned() ? 0 : super.getDefenseChance();
  }

  public float getStunTimeLeft() {
    return !isDead() ? getStateMachine().getStunTimeLeft() : 0f;
  }

  public boolean isStunned() {
    return !isDead() && getStateMachine().isStunned();
  }

  public boolean isCastingMagic() {
    return !isDead() && getStateMachine().isCastingMagic();
  }

  public boolean isAttacking() {
    return !isDead() && getStateMachine().isAttacking();
  }

  public boolean isAggressiveWalking() {
    return !isDead() && getStateMachine().isAggressiveWalking();
  }

  public boolean isAttackWeaponReleased() {
    return getStateMachine().isAttackWeaponReleased();
  }

  public @Nullable Selectable<?> getAttackTarget() {
    return getStateMachine().getAttackTarget();
  }

  public void resetGiveUpCounters() {
    if (!isDead()) {
      getStateMachine().resetGiveUpCounters();
    }
  }

  @Override
  public EntityActivity getActivity() {
    if (isDead()) {
      return EntityActivity.DEAD;
    }
    return getStateMachine().getActivity();
  }

  private void walkToTarget(Target target, boolean scan_attack, boolean replaceOrder) {
    Target walkable_target = getUnitGrid().findGridTarget(target.getGridX(), target.getGridY(), false);
    Target resolvedTarget = walkable_target != null ? walkable_target : target;
    if (replaceOrder) {
      requestOrder(new Walking(resolvedTarget, scan_attack, getDefaultState()));
    } else {
      pushOrder(new Walking(resolvedTarget, scan_attack, getCurrentState()));
    }
  }

  @Override
  public void setTarget(Target target, Action action, boolean aggressive) {
    applyTarget(target, action, aggressive, true);
  }

  private void applyTarget(Target target, Action action, boolean aggressive, boolean replaceOrder) {
    if (target == this) return;
    assert !target.isDead() : "Setting dead target";
    assert mount == null : "Unit is mounted in " + mount;
    UnitState fallback = replaceOrder ? getDefaultState() : getCurrentState();
    switch (action) {
      case DEFAULT -> {
        if (canBuild(target)) {
          submitOrder(new PlacingBuilding((Building) target, fallback), replaceOrder);
        } else if (canGather(target)) {
          submitOrder(new Gathering((Supply) target, ((Supply) target).getSupplyType(), fallback), replaceOrder);
        } else if (canRepair(target, false)) {
          submitOrder(new Repairing((Building) target, fallback), replaceOrder);
        } else if (canEnter(target)) {
          submitOrder(new EnteringBuilding((Building) target, fallback), replaceOrder);
        } else if (canAttack(target, false)) {
          submitOrder(new Hunting((Selectable<?>) target, fallback), replaceOrder);
        } else {
          walkToTarget(target, aggressive, replaceOrder);
        }
      }
      case MOVE -> {
        if (canEnter(target)) {
          submitOrder(new EnteringBuilding((Building) target, fallback), replaceOrder);
        } else {
          walkToTarget(target, false, replaceOrder);
        }
      }
      case ATTACK -> {
        if (canAttack(target, true)) {
          submitOrder(new Hunting((Selectable<?>) target, fallback), replaceOrder);
        } else {
          walkToTarget(target, true, replaceOrder);
        }
      }
      case GATHER_REPAIR -> {
        if (canGather(target)) {
          submitOrder(new Gathering((Supply) target, ((Supply) target).getSupplyType(), fallback), replaceOrder);
        } else if (canRepair(target, true)) {
          submitOrder(new Repairing((Building) target, fallback), replaceOrder);
        }
      }
      case DEFEND -> submitOrder(new Defending(target, fallback), replaceOrder);
      default -> IO.println("Invalid action: " + action);
    }
  }

  private void submitOrder(UnitState order, boolean replaceOrder) {
    if (replaceOrder) {
      requestOrder(order);
    } else {
      pushOrder(order);
    }
  }

  public void printDebugInfo() {
    IO.println("-----------------------------------");
    IO.println("Primary State = " + getStateMachine().getActiveOrPendingState());
    if (magicCapability != null) {
      IO.println("Hit Points = " + hit_points);
      for (MagicType type : getOwner().getRaceInfo().getMagics()) {
        IO.println("Magic Energy " + type.name() + " = " + magicCapability.getEnergy(type));
      }
      IO.println("State = " + getStateMachine().getActiveOrPendingState());
    }
  }

  public boolean canDoMagic(MagicType type) {
    return !isDead() && magicCapability != null && getOwner().canDoMagic(type) && magicCapability.isReady(type);
  }

  public boolean canDoMagic(int slotIndex) {
    return canDoMagic(getOwner().getRaceInfo().getMagicType(slotIndex));
  }

  public void doMagic(MagicType type, boolean clear_stack) {
    if (canDoMagic(type) && magicCapability != null) {
      UnitState fallback = clear_stack ? getDefaultState() : getStateMachine().getActiveOrPendingState();
      CastingMagic magicState = new CastingMagic(getOwner().getRaceInfo().getMagicFactory(type), fallback);
      if (clear_stack) {
        requestOrder(magicState);
      } else {
        pushOrder(magicState);
      }
      magicCapability.consumeAll(type);

      // stats
      getOwner().magicCast();
    }
  }

  public @Nullable MagicType getLastMagicType() {
    return magicCapability != null ? magicCapability.getLastMagicType() : null;
  }

  public float getMagicProgress(MagicType type) {
    return magicCapability != null ? magicCapability.getProgress(type) : 0f;
  }

  @Override
  public void switchAnimation(float anim_speed, Animation animation) {
    assert !isDead();
    this.anim_speed = anim_speed;
    if (this.animation != animation) {
      this.animation = animation;
      this.anim_time = 0f;
    } else if (getTemplate().getAnimationType(animation) == AnimationInfo.AnimationType.PLAIN) {
      this.anim_time = 0f;
    }
  }

  @Override
  public int getAnimation() {
    return animation.ordinal();
  }

  @Override
  public float getAnimationTicks() {
    return anim_time;
  }

  public float getMountOffset() {
    assert !isDead();
    return mount != null ? mount.offsetZ() : 0f;
  }

  @Override
  public float getOffsetZ() {
    return mount != null ? mount.host().getOffsetZ() + mount.offsetZ() : isDead() ? deathOffsetZ : 0f;
  }

  public PathTracker getPathTracker() {
    return path_tracker;
  }

  private UnitState getDefaultState() {
    return defaultState;
  }

  UnitStateMachine getStateMachine() {
    return stateMachine;
  }

  public UnitState getCurrentState() {
    return getStateMachine().getCurrentState();
  }

  public void requestOrder(UnitState newOrder) {
    assert !isDead();
    getStateMachine().requestOrder(newOrder, this);
  }

  public void pushOrder(UnitState newOrder) {
    assert !isDead();
    getStateMachine().pushOrder(newOrder, this);
  }

  public void forceStateTransition(UnitState preemptingState) {
    getStateMachine().forceTransition(preemptingState, this);
  }

  private void replaceDefaultAndRequestOrder(UnitState newDefaultState) {
    this.defaultState = newDefaultState;
    getStateMachine().replaceDefaultAndRequestOrder(newDefaultState, this);
  }

  boolean isShouldDecide() {
    return getStateMachine().isShouldDecide();
  }

  String getLastBehaviourStateName() {
    return getStateMachine().getLastStepStatusName();
  }

  private boolean isBlocking() {
    return getStateMachine().isBlocking();
  }

  public boolean isIdle() {
    return !isDead() && getStateMachine().isIdle();
  }

  public boolean isDefending() {
    return !isDead() && getStateMachine().isDefending();
  }

  public boolean isPlacingBuilding() {
    return !isDead() && getStateMachine().isPlacingBuilding();
  }

  public @Nullable SupplyType getGatheringSupplyType() {
    return !isDead() ? getStateMachine().getGatheringSupplyType() : null;
  }

  public @Nullable BuildingTemplate getPlacingBuilding() {
    if (isDead()) {
      return null;
    }
    Building target = getStateMachine().getPlacingBuildingTarget();
    return target != null ? target.getTemplate() : null;
  }

  private void scanVicinity(ScanFilter filter) {
    assert !isDead();
    getUnitGrid().scan(filter, getGridX(), getGridY());
  }

  @Override
  public boolean isCloseEnough(float max_dist, Target target) {
    assert !isDead();
    return isCloseEnough(getUnitGrid(), max_dist, getGridX(), getGridY(), target);
  }

  @Override
  public boolean hasAbility(int abilityMask) {
    return getAbilities().hasAbilities(abilityMask);
  }

  @Override
  public float getAttackRange(Target target) {
    return getRange(target);
  }

  @Override
  public @Nullable Selectable<?> scanForEnemy(int range) {
    AttackScanFilter scanFilter = new AttackScanFilter(getOwner(), range);
    scanVicinity(scanFilter);
    return scanFilter.removeTarget();
  }

  @Override
  public int getCarriedSupplyCount() {
    return supply_container != null ? supply_container.getNumSupplies() : 0;
  }

  @Override
  public @Nullable SupplyType getCarriedSupplyType() {
    return supply_container != null ? supply_container.getSupplyType().orElse(null) : null;
  }

  @Override
  public boolean isSupplyFull() {
    return supply_container != null && supply_container.isSupplyFull();
  }

  @Override
  public boolean isLegacyDeterministic() {
    return AppConfig.LEGACY_DETERMINISTIC;
  }

  @Override
  public float nextRandomFloat() {
    return getWorld().getRandom().nextFloat();
  }

  @Override
  public void initWalkTarget(Target target, float range) {
    path_tracker.setTarget(new TargetTrackerAlgorithm(getUnitGrid(), range, target));
  }

  @Override
  public void initSupplyFinderWalk(SupplyType supplyType) {
    activeSupplyFinderTracker = new FinderTrackerAlgorithm<>(getUnitGrid(), new SupplyFinder<>(this, supplyType));
    path_tracker.setTarget(activeSupplyFinderTracker);
  }

  @Override
  public @Nullable Supply getFoundSupply() {
    return activeSupplyFinderTracker != null ? activeSupplyFinderTracker.getOccupant().orElse(null) : null;
  }

  @Override
  public void initSupplyBuildingFinderWalk() {
    activeBuildingFinderTracker =
        new FinderTrackerAlgorithm<>(getUnitGrid(), new BuildingFinder(getOwner(), Abilities.SUPPLY_CONTAINER));
    path_tracker.setTarget(activeBuildingFinderTracker);
  }

  @Override
  public @Nullable Building getFoundSupplyBuilding() {
    return activeBuildingFinderTracker != null ? activeBuildingFinderTracker.getOccupant().orElse(null) : null;
  }

  @Override
  public PathTracker.State stepWalk(float dt) {
    return path_tracker.animate(getMetersPerSecond() * dt);
  }

  @Override
  public @Nullable Movable getCurrentWalkBlocker() {
    Occupant blocker = path_tracker.getBlocker();
    return blocker instanceof Movable movable ? movable : null;
  }

  @Override
  public void setOffsetZ(float z) {
    this.deathOffsetZ = z;
  }

  @Override
  public float getWeaponSecondsPerRelease(float animSpeed) {
    return getWeaponFactory().getSecondsPerRelease(animSpeed);
  }

  @Override
  public void launchWeaponAttack(Selectable<?> target) {
    if (mount != null) {
      getWeaponFactory().attack(this, target, mount.attackSpeedMultiplier());
    } else {
      getWeaponFactory().attack(this, target);
    }
  }

  @Override
  public void onHarvestSwing(Supply supply) {
    if (AppConfig.LEGACY_DETERMINISTIC) {
      int soundCount =
          switch (supply.getSupplyType()) {
            case WOOD -> 6;
            case ROCK, IRON, RUBBER -> 5;
          };
      getWorld().getRandom().nextInt(soundCount);
    }
    getWorld().getNotificationListener().onHarvest(this, supply.getSupplyType());
    if (supply_container != null && supply.hit()) {
      supply_container.increaseSupply(1, supply.getSupplyType());
      getOwner().harvested(supply.getSupplyType());
    }
  }

  @Override
  public void consumeOneWoodForRepair() {
    if (supply_container != null) {
      supply_container.increaseSupply(-1, SupplyType.WOOD);
    }
  }

  @Override
  public void onRepairSwing() {
    if (AppConfig.LEGACY_DETERMINISTIC) {
      getWorld().getRandom().nextInt(6);
    }
    getWorld().getNotificationListener().onRepair(this);
  }

  @Override
  public void applyRepairPoint(Building building) {
    building.repair(1);
  }

  @Override
  public boolean depositSupplyIntoBuilding(Building building) {
    if (supply_container == null) {
      return false;
    }
    supply_container
        .getSupplyType()
        .ifPresent(unitSupplyType -> {
          int numSupplies =
              building
                  .getSupplyContainer(unitSupplyType)
                  .orElseThrow()
                  .increaseSupply(supply_container.getNumSupplies());
          supply_container.increaseSupply(-numSupplies, unitSupplyType);
        });
    return supply_container.getNumSupplies() > 0;
  }

  @Override
  public boolean isBuildingPlacementLegal(Building building) {
    return building.isPlacingLegal();
  }

  @Override
  public void placeBuilding(Building building) {
    building.place();
  }

  @Override
  public boolean isFriendlySupplyContainerBuilding(Building building) {
    return building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER) && getOwner() == building.getOwner();
  }

  @Override
  public boolean canEnterBuilding(Building building) {
    return canEnter(building);
  }

  @Override
  public void enterBuilding(Building building) {
    UnitContainer container = building.getRawUnitContainer();
    if (container == null || !container.canEnter(this)) {
      return;
    }
    if (building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER)) {
      if (supply_container != null && supply_container.getNumSupplies() > 0) {
        supply_container
            .getSupplyType()
            .ifPresent(type ->
                building.getSupplyContainer(type).orElseThrow().increaseSupply(supply_container.getNumSupplies()));
      }
      WeaponTier tier = getWeaponFactory().getTier();
      if (tier != null) {
        building.getWeaponContainer(tier).ifPresent(weaponContainer -> weaponContainer.increaseSupply(1));
      }
    }
    container.enter(this);
  }

  @Override
  public void enterBuildingWithoutDeposit(Building building) {
    UnitContainer container = building.getRawUnitContainer();
    if (container != null && container.canEnter(this)) {
      container.enter(this);
    }
  }

  @Override
  public @Nullable Magic executeMagic(MagicFactory factory) {
    return factory.execute(this);
  }

  @Override
  public void registerMagic(Magic magic) {
    getOwner().getWorld().getAnimationManagerGameTime().registerAnimation(magic);
  }

  @Override
  public void removeFromWorld() {
    remove();
  }
}
