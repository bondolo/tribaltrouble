package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.Difficulty;

import java.util.EnumMap;
import java.util.Map;
import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.simulation.model.weapon.IronSpearWeapon;
import com.oddlabs.tt.simulation.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.simulation.model.weapon.RockSpearWeapon;
import com.oddlabs.tt.simulation.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.simulation.model.weapon.RubberSpearWeapon;
import com.oddlabs.tt.simulation.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.simulation.model.Target;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Artificial intelligence player implementation managing economic production, base defense, and unit armies.
 */
public final class AdvancedAI extends AI {

    private static final int SCORE_PEON = 1;
    private static final int SCORE_WARRIOR_ROCK = 4;
    private static final int SCORE_WARRIOR_IRON = 5;
    private static final int SCORE_WARRIOR_RUBBER = 10;
    private static final int SCORE_CHIEFTAIN = 25;

    private record DifficultyParams(
                                    float defenseFactor,
                                    int minUnitsBuildingWeapons,
                                    int minWeaponsInStock,
                                    int minUnitsReproducing,
                                    int maxUnitsGatheringTree,
                                    int maxUnitsGatheringRock,
                                    int maxUnitsGatheringIron,
                                    int maxUnitsGatheringRubber,
                                    int unitsPerTower1,
                                    int unitsPerTower2,
                                    int numWarriorsDefault,
                                    int numWarriorsIncrease,
                                    int numWarriorsMax,
                                    int numWarriorsForChieftain) {
    }

    private static final EnumMap<Difficulty, DifficultyParams> PARAMS = new EnumMap<>(Map.of(
            Difficulty.EASY, new DifficultyParams(
                    1f, // defenseFactor
                    0, // minUnitsBuildingWeapons
                    10, // minWeaponsInStock
                    0, // minUnitsReproducing
                    2, // maxUnitsGatheringTree
                    1, // maxUnitsGatheringRock
                    1, // maxUnitsGatheringIron
                    0, // maxUnitsGatheringRubber
                    1000, // unitsPerTower1
                    1000, // unitsPerTower2
                    3, // numWarriorsDefault
                    1, // numWarriorsIncrease
                    10, // numWarriorsMax
                    1000 // numWarriorsForChieftain
            ),
            Difficulty.NORMAL, new DifficultyParams(
                    1.5f, // defenseFactor
                    3, // minUnitsBuildingWeapons
                    5, // minWeaponsInStock
                    5, // minUnitsReproducing
                    5, // maxUnitsGatheringTree
                    3, // maxUnitsGatheringRock
                    3, // maxUnitsGatheringIron
                    0, // maxUnitsGatheringRubber
                    100, // unitsPerTower1
                    130, // unitsPerTower2
                    7, // numWarriorsDefault
                    3, // numWarriorsIncrease
                    25, // numWarriorsMax
                    20 // numWarriorsForChieftain
            ),
            Difficulty.HARD, new DifficultyParams(
                    2f, // defenseFactor
                    8, // minUnitsBuildingWeapons
                    0, // minWeaponsInStock
                    20, // minUnitsReproducing
                    15, // maxUnitsGatheringTree
                    9, // maxUnitsGatheringRock
                    10, // maxUnitsGatheringIron
                    3, // maxUnitsGatheringRubber
                    90, // unitsPerTower1
                    120, // unitsPerTower2
                    10, // numWarriorsDefault
                    5, // numWarriorsIncrease
                    40, // numWarriorsMax
                    15 // numWarriorsForChieftain
            )));

    private final DifficultyParams params;

    private int numWarriors;

    private @Nullable LandscapeTarget defense_target = null;

    public AdvancedAI(Player owner, PlayerInterface playerInterface, @Nullable UnitInfo unit_info,
            Difficulty difficulty) {
        super(owner, playerInterface, unit_info);
        this.params = PARAMS.get(difficulty);
        this.numWarriors = params.numWarriorsDefault();
    }

    @Override
    public void animate(float dt) {
        if (!shouldDoAction(dt))
            return;
        reclassify();
        nodeDefendBase();
        reclassify();
        if (getOwner().getUnitCountContainer().getNumSupplies() > params.unitsPerTower2())
            nodeGuardTowers(2);
        else if (getOwner().getUnitCountContainer().getNumSupplies() > params.unitsPerTower1())
            nodeGuardTowers(1);

        reclassify();
        nodeAttackWithWarriorsAndChieftain(numWarriors, numWarriors
                >= params.numWarriorsForChieftain());
        nodeAssignIdlePeons();
        getOwner().getChieftain().ifPresent(chieftain -> getOwner().getRaceInfo().getChieftainAI().decide(chieftain,
                getPlayerInterface()));
    }

    private void nodeDefendBase() {
        int enemy_score = 0;
        if (!getQuarters().isEmpty()) {
            enemy_score = scanForEnemies(getQuarters().getFirst());
        }
        if (!getArmory().isEmpty() && enemy_score == 0) {
            enemy_score = scanForEnemies(getArmory().getFirst());
        }
        enemy_score = (int) (params.defenseFactor() * enemy_score);
        for (Selectable<?> defendingUnit : getDefendingUnits()) {
            enemy_score -= getUnitScore((Unit) defendingUnit);
        }
        if (enemy_score > 0) {
            nodeDeployArmy();
            nodeDefend(enemy_score);
        }
    }

    private void nodeDeployArmy() {
        if (!getArmory().isEmpty()) {
            Building armory = getArmory().getFirst();
            int num_units = armory.getUnitContainer().orElseThrow().getNumSupplies()
                    - params.minUnitsBuildingWeapons();
            int num_weapons = numWeapons(armory) - params.minWeaponsInStock();
            if (num_units <= 0 || num_weapons <= 0)
                return;

            int num_warriors = Math.min(num_units, num_weapons);
            int num_rubber_units = Math.min(num_warriors, armory.getSupplyContainer(RubberAxeWeapon.class)
                    .orElseThrow().getNumSupplies());
            int num_iron_units = Math.min(num_warriors - num_rubber_units, armory.getSupplyContainer(
                    IronAxeWeapon.class).orElseThrow().getNumSupplies());
            int num_rock_units = Math.min(num_warriors - num_rubber_units - num_iron_units, armory.getSupplyContainer(
                    RockAxeWeapon.class).orElseThrow().getNumSupplies());
            if (num_rubber_units > 0) {
                getPlayerInterface().deployUnits(armory, DeployType.RUBBER_WARRIOR, num_rubber_units);
            }
            if (num_iron_units > 0) {
                getPlayerInterface().deployUnits(armory, DeployType.IRON_WARRIOR, num_iron_units);
            }
            if (num_rock_units > 0) {
                getPlayerInterface().deployUnits(armory, DeployType.ROCK_WARRIOR, num_rock_units);
            }
            num_units = armory.getUnitContainer().orElseThrow().getNumSupplies();
            if (num_units > 0) {
                getPlayerInterface().deployUnits(armory, DeployType.PEON, num_units);
            }
        }
    }

    private void nodeDefend(int score) {
        List<Unit> unit_list = new ArrayList<>();

        int result = 0;
        if (result < score) {
            result = addFromList(getIdleWarriors(), unit_list, result, score);
        }
        if (result < score) {
            result = addFromList(getIdlePeons(), unit_list, result, score);
        }
        if (result < score) {
            result = addFromList(getGatherTreePeons(), unit_list, result, score);
        }
        if (result < score) {
            result = addFromList(getGatherRockPeons(), unit_list, result, score);
        }
        if (result < score) {
            result = addFromList(getGatherIronPeons(), unit_list, result, score);
        }
        if (result < score) {
            result = addFromList(getGatherRubberPeons(), unit_list, result, score);
        }

        if (result > 0) {
            Unit[] units = new Unit[unit_list.size()];
            unit_list.toArray(units);
            getPlayerInterface().setLandscapeTarget(units, defense_target.getGridX(), defense_target.getGridY(),
                    Action.DEFEND, true);
        }
    }

    private int addFromList(List<Selectable<?>> list, List<Unit> new_list, int progress, int score) {
        int result = progress;
        for (Selectable<?> list1 : list) {
            Unit unit = (Unit) list1;
            new_list.add(unit);
            result += getUnitScore(unit);
            if (result > score)
                break;
        }
        return result;
    }

    private int scanForEnemies(Selectable<?> src) {
        FindOccupantFilter<Unit> filter = new FindOccupantFilter<>(src.getPositionX(), src.getPositionY(), 30f, src,
                Unit.class);
        getUnitGrid().scan(filter, src.getGridX(), src.getGridY());
        int score = 0;
        defense_target = null;
        for (Unit unit : filter.getResult()) {
            if (!unit.isDead() && getOwner().isEnemy(unit.getOwner())) {
                score += getUnitScore(unit);
                if (defense_target == null)
                    defense_target = new LandscapeTarget(unit.getGridX(), unit.getGridY());
            }
        }
        return score;
    }

    private int getUnitScore(Unit unit) {
        if (unit.getAbilities().hasAbilities(Abilities.HARVEST)) {
            return SCORE_PEON;
        } else if (unit.getAbilities().hasAbilities(Abilities.MAGIC)) {
            return SCORE_CHIEFTAIN;
        } else {
            var type = unit.getWeaponFactory().getType().orElse(null);
            if (type == RockAxeWeapon.class || type == RockSpearWeapon.class) {
                return SCORE_WARRIOR_ROCK;
            } else if (type == IronAxeWeapon.class || type == IronSpearWeapon.class) {
                return SCORE_WARRIOR_IRON;
            } else if (type == RubberAxeWeapon.class || type == RubberSpearWeapon.class) {
                return SCORE_WARRIOR_RUBBER;
            }
        }
        throw new IllegalArgumentException("Unit has no valid weapon or magic abilities");
    }

    private void nodeGuardTowers(int num_towers) {
        if ((getTowers().isEmpty() && num_towers > 0) || num_towers > getTowers().size()) {
            nodeBuildTower(num_towers);
        } else if (num_towers > 0) {
            for (int i = 0; i < getTowers().size(); i++) {
                Building tower = getTowers().get(i);
                if (!tower.getUnitContainer().orElseThrow().isSupplyFull() && getIdleWarriors().size() > i) {
                    getPlayerInterface().setTarget(Selectable.newArray(getIdleWarriors().get(i)), tower,
                            Action.DEFAULT, false);
                    nodeDeployUnitsInArmory(1);
                }
            }
        }
    }

    private void nodeBuildTower(int number) {
        if (!towerUnderConstruction() && ((getTowers().isEmpty() && number == 1) || getTowers().size() < number)
                && !getQuarters().isEmpty() && !getArmory().isEmpty()) {
            Selectable<?>[] builders = getPeons(10);
            if (builders.length == 0)
                return;

            Building origin = number % 2 == 1 ? getQuarters().getFirst() : getArmory().getFirst();
            int ox = origin.getGridX();
            int oy = origin.getGridY();
            int center = getOwner().getWorld().getHeightMap().getGridUnitsPerWorld() / 2;
            int dx = center - ox;
            int dy = center - oy;
            float inv_dist = 1f / (float) Math.sqrt(dx * dx + dy * dy);
            int tx = (int) (ox + 10f * dx * inv_dist);
            int ty = (int) (oy + 10f * dy * inv_dist);
            setTowerUnderConstruction(buildBuilding(BuildingType.TOWER, builders, tx, ty));
        }
    }

    private void nodeAssignIdlePeons() {
        if (!getIdlePeons().isEmpty()) {
            Selectable<?>[] idle = getIdlePeons().toArray(Selectable[]::new);
            if (quartersUnderConstruction() && !getConstructionSites().isEmpty()) {
                getPlayerInterface().setTarget(idle, getConstructionSites().getFirst(), Action.DEFAULT, false);
            } else if (armoryUnderConstruction() && !getConstructionSites().isEmpty()) {
                getPlayerInterface().setTarget(idle, getConstructionSites().getFirst(), Action.DEFAULT, false);
            } else if (towerUnderConstruction() && !getConstructionSites().isEmpty()) {
                getPlayerInterface().setTarget(idle, getConstructionSites().getFirst(), Action.DEFAULT, false);
            } else if (!getQuarters().isEmpty() && !getQuarters().getFirst().isDead()) {
                getPlayerInterface().setTarget(idle, getQuarters().getFirst(), Action.DEFAULT, false);
            }
        }
    }

    private void nodeAttackWithWarriorsAndChieftain(int num_warriors, boolean use_chieftain) {
        if (getIdleWarriors().size() >= num_warriors && (!use_chieftain || getOwner().hasActiveChieftain())) {
            boolean idle_chieftain = !getIdleChieftains().isEmpty();
            Selectable<?>[] warriors;
            if (idle_chieftain && use_chieftain) {
                warriors = Selectable.newArray(num_warriors + 1);
                warriors[num_warriors] = getIdleChieftains().getFirst();
            } else {
                warriors = Selectable.newArray(num_warriors);
            }

            for (int i = 0; i < num_warriors; i++) {
                warriors[i] = getIdleWarriors().get(i);
            }
            Target target = findTarget(warriors[0].getGridX(), warriors[0].getGridY());
            if (target != null) {
                getPlayerInterface().setLandscapeTarget(warriors, target.getGridX(), target.getGridY(), Action.ATTACK,
                        true);
                if (numWarriors < params.numWarriorsMax())
                    numWarriors += params.numWarriorsIncrease();
            }
        } else {
            nodeDeployUnitsInArmory(num_warriors - getIdleWarriors().size());
            if (use_chieftain)
                nodeTrainChieftain();
        }
    }

    private void nodeTrainChieftain() {
        if (!getOwner().hasActiveChieftain() && !getOwner().isTrainingChieftain()) {
            if (!getQuarters().isEmpty()) {
                getPlayerInterface().trainChieftain(getQuarters().getFirst(), true);
            }
        }
    }

    private void nodeDeployUnitsInArmory(int num_warriors) {
        Building armory = !getArmory().isEmpty() ? getArmory().getFirst() : null;
        if (armory != null) {
            if (!armory.isDead()) {
                int num_units = armory.getUnitContainer().orElseThrow().getNumSupplies()
                        - params.minUnitsBuildingWeapons();
                int num_weapons = numWeapons(armory) - params.minWeaponsInStock();

                if (num_units >= num_warriors && num_weapons >= num_warriors) {
                    int num_rubber_units = Math.min(num_warriors, armory.getSupplyContainer(RubberAxeWeapon.class)
                            .orElseThrow().getNumSupplies());
                    int num_iron_units = Math.min(num_warriors - num_rubber_units, armory.getSupplyContainer(
                            IronAxeWeapon.class).orElseThrow().getNumSupplies());
                    int num_rock_units = Math.min(num_warriors - num_rubber_units - num_iron_units, armory
                            .getSupplyContainer(RockAxeWeapon.class).orElseThrow().getNumSupplies());
                    if (num_rubber_units > 0)
                        getPlayerInterface().deployUnits(armory, DeployType.RUBBER_WARRIOR, num_rubber_units);
                    if (num_iron_units > 0)
                        getPlayerInterface().deployUnits(armory, DeployType.IRON_WARRIOR, num_iron_units);
                    if (num_rock_units > 0)
                        getPlayerInterface().deployUnits(armory, DeployType.ROCK_WARRIOR, num_rock_units);
                } else {
                    if (num_units < num_warriors) {
                        nodeTransferUnits(num_warriors - num_units, armory);
                    }
                    if (num_weapons < num_warriors) {
                        nodeGather(armory, num_units);
                    }
                }
            }
        } else {
            nodeBuildArmory();
        }
    }

    private void nodeGather(Building armory, int num_units) {
        int tree = getGatherTreePeons().size();
        int rock = getGatherRockPeons().size();
        int iron = getGatherIronPeons().size();
        int rubber = getGatherRubberPeons().size();

        if (tree >= params.maxUnitsGatheringTree())
            tree = Integer.MAX_VALUE;
        if (rock >= params.maxUnitsGatheringRock())
            rock = Integer.MAX_VALUE;
        if (iron >= params.maxUnitsGatheringIron())
            iron = Integer.MAX_VALUE;
        if (rubber >= params.maxUnitsGatheringRubber())
            rubber = Integer.MAX_VALUE;

        boolean deployed;
        do {
            deployed = false;
            if (num_units > 0 && tree < params.maxUnitsGatheringTree() && tree <= rock && tree <= iron
                    && tree
                            <= rubber) {
                getPlayerInterface().deployUnits(armory, DeployType.PEON_HARVEST_TREE, 1);
                deployed = true;
                tree++;
            } else if (num_units > 0 && rock < params.maxUnitsGatheringRock() && rock <= tree && rock
                    <= iron
                    && rock <= rubber) {
                        getPlayerInterface().deployUnits(armory, DeployType.PEON_HARVEST_ROCK, 1);
                        deployed = true;
                        rock++;
                    } else if (num_units > 0 && iron < params.maxUnitsGatheringIron() && iron <= tree
                            && iron
                                    <= rock && iron <= rubber) {
                                        getPlayerInterface().deployUnits(armory, DeployType.PEON_HARVEST_IRON, 1);
                                        deployed = true;
                                        iron++;
                                    } else if (num_units > 0 && rubber < params.maxUnitsGatheringRubber()
                                            && rubber
                                                    <= tree && rubber <= rock && rubber <= iron) {
                                                        getPlayerInterface().deployUnits(armory,
                                                                DeployType.PEON_HARVEST_RUBBER,
                                                                1);
                                                        deployed = true;
                                                        rubber++;
                                                    }
            num_units--;
        } while (deployed);
    }

    private void nodeTransferUnits(int num_units, Building armory) {
        Building quarters = !getQuarters().isEmpty() ? getQuarters().getFirst() : null;
        if (quarters != null) {
            if (!quarters.isDead()) {
                getPlayerInterface().setRallyPoint(quarters, armory);
                if (quarters.getUnitContainer().orElseThrow().getNumSupplies() > params.minUnitsReproducing()) {
                    int units = Math.min(num_units, quarters.getUnitContainer().orElseThrow().getNumSupplies()
                            - params.minUnitsReproducing());
                    getPlayerInterface().deployUnits(quarters, DeployType.PEON, units);
                }
            }
        } else {
            nodeBuildQuarters();
        }
    }

    private void nodeBuildArmory() {
        if (!quartersUnderConstruction() && getQuarters().isEmpty()) {
            nodeBuildQuarters();
        }
        Building quarters = !getQuarters().isEmpty() ? getQuarters().getFirst() : null;
        if (!armoryUnderConstruction() && getArmory().isEmpty()
                && quarters != null && quarters.getAbilities().hasAbilities(Abilities.REPRODUCE)) {
            Selectable<?>[] builders = getPeons(20);
            if (builders.length < 20) {
                if (!quarters.isDead() && quarters.getUnitContainer().orElseThrow().getNumSupplies() >= 20)
                    getPlayerInterface().deployUnits(quarters, DeployType.PEON, 20);
            }
            if (builders.length == 0)
                return;

            // TODO: Should use Quarters as origin, if it exists
            setArmoryUnderConstruction(buildBuilding(
                    BuildingType.ARMORY, builders, builders[0].getGridX(), builders[0]
                            .getGridY()));
            reclassify();
        }
    }

    private void nodeBuildQuarters() {
        if (!quartersUnderConstruction() && getQuarters().isEmpty()) {
            Selectable<?>[] builders = getPeons(params.minUnitsReproducing());
            if (builders.length == 0)
                return;

            // TODO: Should use Armory as origin, if it exists
            setQuartersUnderConstruction(buildBuilding(
                    BuildingType.QUARTERS, builders, builders[0].getGridX(),
                    builders[0].getGridY()));
            reclassify();
        }
    }

    private Selectable<?>[] getPeons(int min_num_peons) {
        int idleCount = getIdlePeons().size();
        int ironCount = getGatherIronPeons().size();
        int rockCount = getGatherRockPeons().size();
        int treeCount = getGatherTreePeons().size();
        int rubberCount = getGatherRubberPeons().size();
        int totalAvailable = idleCount + ironCount + rockCount + treeCount + rubberCount;
        int count = Math.min(min_num_peons, totalAvailable);
        if (count == 0) {
            return Selectable.newArray(0);
        }
        Selectable<?>[] peons = Selectable.newArray(count);
        int idx = 0;
        for (int i = 0; i < idleCount && idx < count; i++) {
            peons[idx++] = getIdlePeons().get(i);
        }
        for (int i = 0; i < ironCount && idx < count; i++) {
            peons[idx++] = getGatherIronPeons().get(i);
        }
        for (int i = 0; i < rockCount && idx < count; i++) {
            peons[idx++] = getGatherRockPeons().get(i);
        }
        for (int i = 0; i < treeCount && idx < count; i++) {
            peons[idx++] = getGatherTreePeons().get(i);
        }
        for (int i = 0; i < rubberCount && idx < count; i++) {
            peons[idx++] = getGatherRubberPeons().get(i);
        }
        return peons;
    }

    /*	private final int getNumUnitsDeploying() {
            int result = 0;
            if (getArmory() != null) {
                Building armory = (Building)getArmory()[0];
                result += armory.getDeployContainer(DeployType.ROCK_WARRIOR).getNumSupplies();
                result += armory.getDeployContainer(DeployType.IRON_WARRIOR).getNumSupplies();
                result += armory.getDeployContainer(DeployType.RUBBER_WARRIOR).getNumSupplies();
                result += armory.getDeployContainer(DeployType.PEON).getNumSupplies();
            }
            return result;
        }
    */
    private int numWeapons(Building armory) {
        return armory.getSupplyContainer(RockAxeWeapon.class).orElseThrow().getNumSupplies()
                + armory.getSupplyContainer(IronAxeWeapon.class).orElseThrow().getNumSupplies()
                + armory.getSupplyContainer(RubberAxeWeapon.class).orElseThrow().getNumSupplies();
    }

    private @Nullable Target findTarget(int start_x, int start_y) {
        Target best_building = getOwner().findNearestEnemyBuilding(start_x, start_y).orElse(null);
        Target best_target = getOwner().findNearestEnemy(start_x, start_y).orElse(null);
        if (best_building == null) {
            return best_target;
        }
        if (best_target == null) {
            return null;
        }

        int squared_dist_building = (best_building.getGridX() - start_x) * (best_building.getGridX() - start_x)
                + (best_building.getGridY() - start_y) * (best_building.getGridY() - start_y);
        int squared_dist_target = (best_target.getGridX() - start_x) * (best_target.getGridX() - start_x)
                + (best_target.getGridY() - start_y) * (best_target.getGridY() - start_y);

        return squared_dist_target < squared_dist_building / 2 ? best_target : best_building;
    }

    private boolean buildBuilding(BuildingType building_type, Selectable<?>[] selection, int grid_x,
            int grid_y) {
        BuildingSiteScanFilter filter = new BuildingSiteScanFilter(getUnitGrid(), getOwner().getRaceInfo()
                .getBuildingTemplate(building_type), 40);
        getUnitGrid().scan(filter, grid_x, grid_y);
        return filter.getSingleResult().map(target -> {
            getPlayerInterface().placeBuilding(selection, building_type, target.getGridX(), target.getGridY());
            return true;
        }).orElse(false);
    }
}
