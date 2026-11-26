package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.WeaponTier;
import java.util.List;

/// Domain interface defining actions a player can take, such as deploying units, building, and casting magic.
public interface PlayerInterface {
  void deployUnits(Building building, DeployType type, int num_units);

  void buildWeapons(Building building, WeaponTier tier, int num_weapons, boolean infinite);

  default void doMagic(Unit chieftain, MagicType magic) {
    doMagic(chieftain, magic, true);
  }

  void doMagic(Unit chieftain, MagicType magic, boolean clearStack);

  void exitTower(Building building);

  void trainChieftain(Building building, boolean start);

  void placeBuilding(
      List<? extends Selectable<?>> selection, BuildingType template_type, int placing_grid_x, int placing_grid_y);

  void setRallyPoint(Building building, Target target);

  void setTarget(List<? extends Selectable<?>> selection, Target target, Action action, boolean aggressive);

  void setTarget(Selectable<?> unit, Target target, Action action, boolean aggressive);

  void setRallyPoint(Building building, int grid_x, int grid_y);

  void setLandscapeTarget(
      List<? extends Selectable<?>> selection, int grid_x, int grid_y, Action action, boolean aggressive);

  void setLandscapeTarget(Selectable<?> unit, int grid_x, int grid_y, Action action, boolean aggressive);

  void setPreferredGamespeed(int speed);

  void changePreferredGamespeed(int delta);
}
