package com.oddlabs.tt.ai;

import static com.oddlabs.tt.simulation.model.Ability.HARVEST;
import static com.oddlabs.tt.simulation.model.Ability.MAGIC;

import com.oddlabs.tt.simulation.landscape.LandscapeTarget;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.Difficulty;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponTier;
import com.oddlabs.tt.simulation.pathfinder.BuildingSiteScanFilter;
import com.oddlabs.tt.simulation.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.simulation.player.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/// Artificial intelligence player implementation managing economic production, base defense, and unit armies.
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
      int numWarriorsForChieftain) {}

  private static final EnumMap<Difficulty, DifficultyParams> PARAMS =
      new EnumMap<>(
          Map.of(
              Difficulty.EASY,
                  new DifficultyParams(
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
              Difficulty.NORMAL,
                  new DifficultyParams(
                      1.5f, // defenseFactor
                      3, // minUnitsBuildingWeapons
                      5, // minWeaponsInStock
                      5, // minUnitsReproducing
                      5, // maxUnitsGatheringTree
                      3, // maxUnitsGatheringRock
                      3, // maxUnitsGatheringIron
                      0, // maxUnitsGatheringRubber
                      1000, // unitsPerTower1
                      1000, // unitsPerTower2
                      7, // numWarriorsDefault
                      3, // numWarriorsIncrease
                      19, // numWarriorsMax
                      1000 // numWarriorsForChieftain
                      ),
              Difficulty.HARD,
                  new DifficultyParams(
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
                      20 // numWarriorsForChieftain
                      )));

  private final DifficultyParams params;

  private int numWarriors;

  private @Nullable LandscapeTarget defense_target = null;

  public AdvancedAI(Player owner, Difficulty difficulty) {
    super(owner);
    this.params = PARAMS.get(difficulty);
    this.numWarriors = params.numWarriorsDefault();
  }

  @Override
  public void tick() {
    if (!shouldDoAction()) return;
    reclassify();
    nodeDefendBase();
    reclassify();
    if (getOwner().getUnitCountContainer().getNumSupplies() > params.unitsPerTower2()) nodeGuardTowers(2);
    else if (getOwner().getUnitCountContainer().getNumSupplies() > params.unitsPerTower1()) nodeGuardTowers(1);

    reclassify();
    nodeAttackWithWarriorsAndChieftain(numWarriors, numWarriors >= params.numWarriorsForChieftain());
    nodeAssignIdlePeons();
    getOwner()
        .getChieftain()
        .ifPresent(chieftain ->
            ChieftainAI.forRace(getOwner().getPlayerInfo().race()).decide(chieftain, getPlayerInterface()));
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
      int num_units = armory.getUnitContainer().orElseThrow().getNumSupplies() - params.minUnitsBuildingWeapons();
      int num_weapons = numWeapons(armory) - params.minWeaponsInStock();
      if (num_units <= 0 || num_weapons <= 0) return;

      int num_warriors = Math.min(num_units, num_weapons);
      int num_rubber_units = Math.min(num_warriors, armory.getWeaponSupplyCount(WeaponTier.RUBBER));
      int num_iron_units = Math.min(num_warriors - num_rubber_units, armory.getWeaponSupplyCount(WeaponTier.IRON));
      int num_rock_units =
          Math.min(num_warriors - num_rubber_units - num_iron_units, armory.getWeaponSupplyCount(WeaponTier.ROCK));
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
      getPlayerInterface()
          .setLandscapeTarget(unit_list, defense_target.getGridX(), defense_target.getGridY(), Action.DEFEND, true);
    }
  }

  private int addFromList(List<Selectable<?>> list, List<Unit> new_list, int progress, int score) {
    int result = progress;
    for (Selectable<?> list1 : list) {
      Unit unit = (Unit) list1;
      new_list.add(unit);
      result += getUnitScore(unit);
      if (result > score) break;
    }
    return result;
  }

  private int scanForEnemies(Selectable<?> src) {
    FindOccupantFilter<Unit> filter =
        new FindOccupantFilter<>(src.getPositionX(), src.getPositionY(), 30f, src, Unit.class);
    getUnitGrid().scan(filter, src.getGridX(), src.getGridY());
    int score = 0;
    defense_target = null;
    for (Unit unit : filter.getResult()) {
      if (!unit.isDead() && getOwner().isEnemy(unit.getOwner())) {
        score += getUnitScore(unit);
        if (defense_target == null) defense_target = new LandscapeTarget(unit.getGridX(), unit.getGridY());
      }
    }
    return score;
  }

  private int getUnitScore(Unit unit) {
    if (unit.hasAbility(HARVEST)) {
      return SCORE_PEON;
    } else if (unit.hasAbility(MAGIC)) {
      return SCORE_CHIEFTAIN;
    } else {
      var tier = unit.getWeaponFactory().getTier();
      if (tier != null) {
        return switch (tier) {
          case ROCK -> SCORE_WARRIOR_ROCK;
          case IRON -> SCORE_WARRIOR_IRON;
          case RUBBER -> SCORE_WARRIOR_RUBBER;
        };
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
          getPlayerInterface().setTarget(getIdleWarriors().get(i), tower, Action.DEFAULT, false);
          nodeDeployUnitsInArmory(1);
        }
      }
    }
  }

  private void nodeBuildTower(int number) {
    if (!towerUnderConstruction()
        && ((getTowers().isEmpty() && number == 1) || getTowers().size() < number)
        && !getQuarters().isEmpty()
        && !getArmory().isEmpty()) {
      List<Selectable<?>> builders = getPeons(10);
      if (builders.isEmpty()) return;

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
      List<Selectable<?>> idle = getIdlePeons();
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
      List<Selectable<?>> warriors = new ArrayList<>(num_warriors + (idle_chieftain && use_chieftain ? 1 : 0));
      for (int i = 0; i < num_warriors; i++) {
        warriors.add(getIdleWarriors().get(i));
      }
      if (idle_chieftain && use_chieftain) {
        warriors.add(getIdleChieftains().getFirst());
      }

      Target target = findTarget(warriors.getFirst().getGridX(), warriors.getFirst().getGridY());
      if (target != null) {
        getPlayerInterface().setLandscapeTarget(warriors, target.getGridX(), target.getGridY(), Action.ATTACK, true);
        if (numWarriors < params.numWarriorsMax()) numWarriors += params.numWarriorsIncrease();
      }
    } else {
      nodeDeployUnitsInArmory(num_warriors - getIdleWarriors().size());
      if (use_chieftain) nodeTrainChieftain();
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
        int num_units = armory.getUnitContainer().orElseThrow().getNumSupplies() - params.minUnitsBuildingWeapons();
        int num_weapons = numWeapons(armory) - params.minWeaponsInStock();

        if (num_units >= num_warriors && num_weapons >= num_warriors) {
          int num_rubber_units = Math.min(num_warriors, armory.getWeaponSupplyCount(WeaponTier.RUBBER));
          int num_iron_units = Math.min(num_warriors - num_rubber_units, armory.getWeaponSupplyCount(WeaponTier.IRON));
          int num_rock_units =
              Math.min(num_warriors - num_rubber_units - num_iron_units, armory.getWeaponSupplyCount(WeaponTier.ROCK));
          if (num_rubber_units > 0)
            getPlayerInterface().deployUnits(armory, DeployType.RUBBER_WARRIOR, num_rubber_units);
          if (num_iron_units > 0) getPlayerInterface().deployUnits(armory, DeployType.IRON_WARRIOR, num_iron_units);
          if (num_rock_units > 0) getPlayerInterface().deployUnits(armory, DeployType.ROCK_WARRIOR, num_rock_units);
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

    if (tree >= params.maxUnitsGatheringTree()) tree = Integer.MAX_VALUE;
    if (rock >= params.maxUnitsGatheringRock()) rock = Integer.MAX_VALUE;
    if (iron >= params.maxUnitsGatheringIron()) iron = Integer.MAX_VALUE;
    if (rubber >= params.maxUnitsGatheringRubber()) rubber = Integer.MAX_VALUE;

    boolean deployed;
    do {
      deployed = false;
      if (num_units > 0 && tree < params.maxUnitsGatheringTree() && tree <= rock && tree <= iron && tree <= rubber) {
        getPlayerInterface().deployUnits(armory, DeployType.PEON_HARVEST_TREE, 1);
        deployed = true;
        tree++;
      } else if (num_units > 0
          && rock < params.maxUnitsGatheringRock()
          && rock <= tree
          && rock <= iron
          && rock <= rubber) {
        getPlayerInterface().deployUnits(armory, DeployType.PEON_HARVEST_ROCK, 1);
        deployed = true;
        rock++;
      } else if (num_units > 0
          && iron < params.maxUnitsGatheringIron()
          && iron <= tree
          && iron <= rock
          && iron <= rubber) {
        getPlayerInterface().deployUnits(armory, DeployType.PEON_HARVEST_IRON, 1);
        deployed = true;
        iron++;
      } else if (num_units > 0
          && rubber < params.maxUnitsGatheringRubber()
          && rubber <= tree
          && rubber <= rock
          && rubber <= iron) {
        getPlayerInterface().deployUnits(armory, DeployType.PEON_HARVEST_RUBBER, 1);
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
          int units =
              Math.min(
                  num_units, quarters.getUnitContainer().orElseThrow().getNumSupplies() - params.minUnitsReproducing());
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
    if (!armoryUnderConstruction() && getArmory().isEmpty() && quarters != null && quarters.canReproduce()) {
      List<Selectable<?>> builders = getPeons(20);
      if (builders.size() < 20) {
        if (!quarters.isDead() && quarters.getUnitContainer().orElseThrow().getNumSupplies() >= 20)
          getPlayerInterface().deployUnits(quarters, DeployType.PEON, 20);
      }
      if (builders.isEmpty()) return;

      // TODO: Should use Quarters as origin, if it exists
      setArmoryUnderConstruction(
          buildBuilding(BuildingType.ARMORY, builders, builders.getFirst().getGridX(), builders.getFirst().getGridY()));
      reclassify();
    }
  }

  private void nodeBuildQuarters() {
    if (!quartersUnderConstruction() && getQuarters().isEmpty()) {
      List<Selectable<?>> builders = getPeons(params.minUnitsReproducing());
      if (builders.isEmpty()) return;

      // TODO: Should use Armory as origin, if it exists
      setQuartersUnderConstruction(
          buildBuilding(
              BuildingType.QUARTERS, builders, builders.getFirst().getGridX(), builders.getFirst().getGridY()));
      reclassify();
    }
  }

  private List<Selectable<?>> getPeons(int min_num_peons) {
    int idleCount = getIdlePeons().size();
    int neededFromGatherers = Math.max(0, min_num_peons - idleCount);
    List<Selectable<?>> peons = new ArrayList<>(getIdlePeons());
    int taken = 0;
    List<List<Selectable<?>>> gathererLists =
        List.of(getGatherIronPeons(), getGatherRockPeons(), getGatherTreePeons(), getGatherRubberPeons());
    for (List<Selectable<?>> list : gathererLists) {
      for (Selectable<?> peon : list) {
        if (taken >= neededFromGatherers) {
          break;
        }
        peons.add(peon);
        taken++;
      }
      if (taken >= neededFromGatherers) {
        break;
      }
    }
    return peons.isEmpty() ? List.of() : Collections.unmodifiableList(peons);
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
    return armory.getWeaponSupplyCount(WeaponTier.ROCK)
        + armory.getWeaponSupplyCount(WeaponTier.IRON)
        + armory.getWeaponSupplyCount(WeaponTier.RUBBER);
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

    int squared_dist_building =
        (best_building.getGridX() - start_x) * (best_building.getGridX() - start_x)
            + (best_building.getGridY() - start_y) * (best_building.getGridY() - start_y);
    int squared_dist_target =
        (best_target.getGridX() - start_x) * (best_target.getGridX() - start_x)
            + (best_target.getGridY() - start_y) * (best_target.getGridY() - start_y);

    return squared_dist_target < squared_dist_building / 2 ? best_target : best_building;
  }

  private boolean buildBuilding(
      BuildingType building_type, List<? extends Selectable<?>> selection, int grid_x, int grid_y) {
    BuildingSiteScanFilter filter =
        new BuildingSiteScanFilter(getUnitGrid(), getOwner().getRaceInfo().getBuildingTemplate(building_type), 40);
    getUnitGrid().scan(filter, grid_x, grid_y);
    return filter
        .getSingleResult()
        .map(target -> {
          getPlayerInterface().placeBuilding(selection, building_type, target.getGridX(), target.getGridY());
          return true;
        })
        .orElse(false);
  }
}
