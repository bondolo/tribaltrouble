package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.model.BuildingType;

import com.oddlabs.tt.simulation.model.UnitType;

import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.simulation.SimulationConfig;
import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.BuildProductionContainer;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.behaviour.Controller;
import com.oddlabs.tt.simulation.behaviour.DefendController;
import com.oddlabs.tt.simulation.behaviour.GatherController;
import com.oddlabs.tt.simulation.behaviour.IdleController;
import com.oddlabs.tt.simulation.behaviour.NullController;
import com.oddlabs.tt.simulation.behaviour.PlaceBuildingController;
import com.oddlabs.tt.simulation.behaviour.WalkController;
import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.model.Target;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * Base abstract class for artificial intelligence players controlling units and building construction.
 */
public abstract class AI {
    private static final float SLEEP_SECONDS = 2f;
    private static final float MIN_SLEEP_SECONDS = 5f;
    private static boolean run_ai = SimulationConfig.DEFAULT_RUN_AI;

    public static boolean isRunAI() {
        return run_ai;
    }

    public static void setRunAI(boolean run) {
        run_ai = run;
    }

    private final Player owner;
    private final PlayerInterface playerInterface;
    private final RandomGenerator aiRandom;
    private final List<Selectable<?>> idlePeons = new ArrayList<>();
    private final List<Selectable<?>> idleChieftains = new ArrayList<>();
    private final List<Selectable<?>> idleWarriors = new ArrayList<>();
    private final List<Selectable<?>> gatherTreePeons = new ArrayList<>();
    private final List<Selectable<?>> gatherRockPeons = new ArrayList<>();
    private final List<Selectable<?>> gatherIronPeons = new ArrayList<>();
    private final List<Selectable<?>> gatherRubberPeons = new ArrayList<>();
    private final List<Building> armories = new ArrayList<>();
    private final List<Building> quarters = new ArrayList<>();
    private final List<Building> towers = new ArrayList<>();
    private final List<Building> constructionSites = new ArrayList<>();
    private final List<Selectable<?>> placeBuildingPeons = new ArrayList<>();
    private final List<Selectable<?>> defendingUnits = new ArrayList<>();
    private boolean armory_under_construction = false;
    private boolean quarters_under_construction = false;
    private boolean tower_under_construction = false;
    private float sleep_time;

    public AI(Player owner, PlayerInterface playerInterface, @Nullable UnitInfo unit_info) {
        this.owner = owner;
        this.playerInterface = playerInterface;
        int playerIndex = owner.getWorld().getPlayers().indexOf(owner);
        long mapSeed = owner.getWorld().getWorldParameters().mapCode().hashCode();
        long seed = (mapSeed ^ 0x9E3779B97F4A7C15L) + 10007L * (playerIndex >= 0 ? playerIndex : 0);
        this.aiRandom = new SplittableRandom(seed);
        reset();

        if (unit_info != null) {
            int grid_start_x = UnitGrid.toGridCoordinate(owner.getStartX());
            int grid_start_y = UnitGrid.toGridCoordinate(owner.getStartY());
            if (unit_info.hasQuarters()) {
                owner.buildBuilding(BuildingType.QUARTERS, grid_start_x, grid_start_y);
            }
            if (unit_info.hasArmory()) {
                owner.buildBuilding(BuildingType.ARMORY, grid_start_x, grid_start_y);
            }
            for (int i = 0; i < unit_info.numTowers(); i++) {
                int center = owner.getWorld().getHeightMap().getGridUnitsPerWorld() / 2;
                int dx = center - grid_start_x;
                int dy = center - grid_start_y;
                float inv_dist = 1f / (float) Math.sqrt(dx * dx + dy * dy);
                int tx = (int) (grid_start_x + 10f * dx * inv_dist);
                int ty = (int) (grid_start_y + 10f * dy * inv_dist);
                owner.buildBuilding(BuildingType.TOWER, tx, ty);
            }
            Random random = new Random(42);
            if (unit_info.hasChieftain()) {
                Target t = getTarget(random);
                Unit chieftain = new Unit(owner, t.getPositionX(), t.getPositionY(), null, owner.getRaceInfo()
                        .getUnitTemplate(UnitType.CHIEFTAIN));
                owner.setActiveChieftain(chieftain);
            }
            for (int i = 0; i < unit_info.numPeons(); i++) {
                Target t = getTarget(random);
                new Unit(owner, t.getPositionX(), t.getPositionY(), null, owner.getRaceInfo().getUnitTemplate(
                        UnitType.PEON));
            }
            for (int i = 0; i < unit_info.numRockWarriors(); i++) {
                Target t = getTarget(random);
                new Unit(owner, t.getPositionX(), t.getPositionY(), null, owner.getRaceInfo().getUnitTemplate(
                        UnitType.WARRIOR_ROCK));
            }
            for (int i = 0; i < unit_info.numIronWarriors(); i++) {
                Target t = getTarget(random);
                new Unit(owner, t.getPositionX(), t.getPositionY(), null, owner.getRaceInfo().getUnitTemplate(
                        UnitType.WARRIOR_IRON));
            }
            for (int i = 0; i < unit_info.numRubberWarriors(); i++) {
                Target t = getTarget(random);
                new Unit(owner, t.getPositionX(), t.getPositionY(), null, owner.getRaceInfo().getUnitTemplate(
                        UnitType.WARRIOR_RUBBER));
            }
        }
    }

    protected final UnitGrid getUnitGrid() {
        return owner.getWorld().getUnitGrid();
    }

    protected final Player getOwner() {
        return owner;
    }

    public final PlayerInterface getPlayerInterface() {
        return playerInterface;
    }

    protected final void reclassify() {
        idlePeons.clear();
        idleChieftains.clear();
        idleWarriors.clear();
        gatherTreePeons.clear();
        gatherRockPeons.clear();
        gatherIronPeons.clear();
        gatherRubberPeons.clear();
        armories.clear();
        quarters.clear();
        towers.clear();
        constructionSites.clear();
        placeBuildingPeons.clear();
        defendingUnits.clear();

        for (Selectable<?> s : owner.getUnits().getSet()) {
            Controller controller = s.getPrimaryController();
            switch (controller) {
                case IdleController _ -> {
                    if (s.getAbilities().hasAbilities(Abilities.BUILD)) {
                        idlePeons.add(s);
                    } else if (s.getAbilities().hasAbilities(Abilities.MAGIC)) {
                        idleChieftains.add(s);
                    } else if (s.getAbilities().hasAbilities(Abilities.ATTACK)) {
                        idleWarriors.add(s);
                    }
                }
                case GatherController<?> gc -> {
                    SupplyType supply_type = gc.getSupplyType();
                    var gatherPeons = switch (supply_type) {
                        case WOOD -> gatherTreePeons;
                        case IRON -> gatherIronPeons;
                        case ROCK -> gatherRockPeons;
                        case RUBBER -> gatherRubberPeons;
                    };
                    gatherPeons.add(s);
                }
                case NullController _ -> {
                    Building b = (Building) s;
                    if (b.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
                        armories.add(b);
                    } else if (b.getAbilities().hasAbilities(Abilities.REPRODUCE)) {
                        quarters.add(b);
                    } else if (b.getAbilities().hasAbilities(Abilities.ATTACK)) {
                        towers.add(b);
                    } else {
                        constructionSites.add(b);
                    }
                }
                case PlaceBuildingController _ -> placeBuildingPeons.add(s);
                case DefendController _ -> defendingUnits.add(s);
                default -> {
                }
            }
        }

        if (!armories.isEmpty()) {
            armory_under_construction = false;
            Building armoryBuilding = armories.getFirst();
            playerInterface.buildRockWeapons(armoryBuilding, BuildProductionContainer.INFINITE_LIMIT, true);
            playerInterface.buildIronWeapons(armoryBuilding, BuildProductionContainer.INFINITE_LIMIT, true);
            playerInterface.buildRubberWeapons(armoryBuilding, BuildProductionContainer.INFINITE_LIMIT, true);
        }
        if (!quarters.isEmpty()) {
            quarters_under_construction = false;
        }
        if (constructionSites.isEmpty() && placeBuildingPeons.isEmpty()) {
            armory_under_construction = false;
            quarters_under_construction = false;
            tower_under_construction = false;
        }
    }

    protected final List<Selectable<?>> getIdlePeons() {
        return idlePeons;
    }

    protected final List<Selectable<?>> getIdleChieftains() {
        return idleChieftains;
    }

    protected final List<Selectable<?>> getIdleWarriors() {
        return idleWarriors;
    }

    protected final List<Selectable<?>> getGatherTreePeons() {
        return gatherTreePeons;
    }

    protected final List<Selectable<?>> getGatherRockPeons() {
        return gatherRockPeons;
    }

    protected final List<Selectable<?>> getGatherIronPeons() {
        return gatherIronPeons;
    }

    protected final List<Selectable<?>> getGatherRubberPeons() {
        return gatherRubberPeons;
    }

    protected final List<Building> getArmory() {
        return armories;
    }

    protected final List<Building> getQuarters() {
        return quarters;
    }

    protected final List<Building> getTowers() {
        return towers;
    }

    protected final List<Building> getConstructionSites() {
        return constructionSites;
    }

    protected final List<Selectable<?>> getPlaceBuildingPeons() {
        return placeBuildingPeons;
    }

    protected final List<Selectable<?>> getDefendingUnits() {
        return defendingUnits;
    }

    protected final boolean armoryUnderConstruction() {
        return armory_under_construction;
    }

    protected final void setArmoryUnderConstruction(boolean armory_under_construction) {
        this.armory_under_construction = armory_under_construction;
    }

    protected final boolean quartersUnderConstruction() {
        return quarters_under_construction;
    }

    protected final void setQuartersUnderConstruction(boolean quarters_under_construction) {
        this.quarters_under_construction = quarters_under_construction;
    }

    protected final boolean towerUnderConstruction() {
        return tower_under_construction;
    }

    protected final void setTowerUnderConstruction(boolean tower_under_construction) {
        this.tower_under_construction = tower_under_construction;
    }

    private void reset() {
        if (AppConfig.LEGACY_DETERMINISTIC) {
            sleep_time = owner.getWorld().getRandom().nextFloat() * SLEEP_SECONDS + MIN_SLEEP_SECONDS;
        } else {
            sleep_time = aiRandom.nextFloat(MIN_SLEEP_SECONDS,
                    MIN_SLEEP_SECONDS + SLEEP_SECONDS);
        }
    }

    protected final RandomGenerator getAIRandom() {
        return aiRandom;
    }

    /**
     * Executes one autonomous decision cycle for this artificial intelligence controller.
     */
    public abstract void tick();

    protected final boolean shouldDoAction() {
        sleep_time -= AnimationManager.ANIMATION_SECONDS_PER_TICK;
        if (!run_ai || sleep_time >= 0)
            return false;
        reset();
        return true;
    }

    public final void manTowers(int num_towers) {
        reclassify();
        if (towers.isEmpty() || idleWarriors.isEmpty())
            return;

        int length = Math.min(idleWarriors.size(), towers.size());
        for (int i = 0; i < length; i++) {
            playerInterface.setTarget(idleWarriors.get(i), towers.get(i), Action.DEFAULT, false);
        }
    }

    public static int attackLandscape(Player owner, PlayerInterface playerInterface, Target target, int num_warriors) {
        int ordered = 0;
        for (Selectable<?> s : owner.getUnits().getSet()) {
            if (s instanceof Unit unit && unit.getAbilities().hasAbilities(Abilities.THROW)) {
                if (!(unit.getPrimaryController() instanceof WalkController wc && wc.isAgressive())) {
                    playerInterface.setLandscapeTarget(unit, target.getGridX(), target.getGridY(),
                            Action.ATTACK, true);
                    ordered++;
                    if (ordered == num_warriors) {
                        break;
                    }
                }
            }
        }
        return ordered;
    }

    public static @Nullable Unit getWarrior(Player owner) {
        for (Selectable<?> s : owner.getUnits().getSet()) {
            if (s instanceof Unit unit && unit.getAbilities().hasAbilities(Abilities.THROW)) {
                return unit;
            }
        }
        return null;
    }

    protected final Target getTarget(RandomGenerator random) {
        float RADIUS = 30;
        float target_x = owner.getStartX() + random.nextFloat(-RADIUS, RADIUS);
        float target_y = owner.getStartY() + random.nextFloat(-RADIUS, RADIUS);
        Target target = getUnitGrid().findGridTarget(UnitGrid.toGridCoordinate(target_x),
                UnitGrid.toGridCoordinate(target_y), false);
        return target != null
                ? target
                : new LandscapeTarget(UnitGrid.toGridCoordinate(target_x), UnitGrid.toGridCoordinate(target_y));
    }
}
