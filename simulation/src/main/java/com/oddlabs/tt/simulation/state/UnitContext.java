package com.oddlabs.tt.simulation.state;

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
import org.jspecify.annotations.Nullable;

/// Domain-level query and actuation boundary between `UnitState` implementations and world entities.
public interface UnitContext {
    int getGridX();

    int getGridY();

    boolean isDead();

    boolean hasAbility(int abilityMask);

    /// Returns whether the entity is within `maxDist` (or adjacent when `maxDist == 0`) of `target`.
    boolean isCloseEnough(float maxDist, Target target);

    float getAttackRange(Target target);

    @Nullable
    Selectable<?> scanForEnemy(int range);

    int getCarriedSupplyCount();

    @Nullable
    SupplyType getCarriedSupplyType();

    boolean isSupplyFull();

    boolean isLegacyDeterministic();

    float nextRandomFloat();

    void switchToIdleAnimation();

    void switchToMovingAnimation();

    void switchAnimation(float speed, Unit.Animation animation);

    void initWalkTarget(Target target, float range);

    void initSupplyFinderWalk(SupplyType supplyType);

    @Nullable
    Supply getFoundSupply();

    void initSupplyBuildingFinderWalk();

    @Nullable
    Building getFoundSupplyBuilding();

    PathTracker.State stepWalk(float dt);

    @Nullable
    Movable getCurrentWalkBlocker();

    void aimAtTarget(Target target);

    void setOffsetZ(float z);

    float getWeaponSecondsPerRelease(float animSpeed);

    void launchWeaponAttack(Selectable<?> target);

    /// Consumes harvest swing PRNG (when legacy-deterministic), emits harvest notification, and increments carried
    /// supply if `supply.hit()`.
    void onHarvestSwing(Supply supply);

    void consumeOneWoodForRepair();

    /// Consumes repair swing PRNG (when legacy-deterministic) and emits repair notification.
    void onRepairSwing();

    void applyRepairPoint(Building building);

    /// Deposits carried supplies into `building`; returns `true` if supplies remain or cannot deposit more.
    boolean depositSupplyIntoBuilding(Building building);

    boolean isBuildingPlacementLegal(Building building);

    void placeBuilding(Building building);

    boolean isFriendlySupplyContainerBuilding(Building building);

    boolean canEnterBuilding(Building building);

    void enterBuilding(Building building);

    /// Enters `building` without depositing carried supplies or weapon tier.
    void enterBuildingWithoutDeposit(Building building);

    @Nullable
    Magic executeMagic(MagicFactory factory);

    void registerMagic(Magic magic);

    void removeFromWorld();
}
