package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.BuildingType;

import com.oddlabs.tt.simulation.model.MagicType;

/**
 * Interface defining actions a player can take, such as deploying units, building, and casting magic.
 */
public interface PlayerInterface {
    void deployUnits(Building building, DeployType type, int num_units);

    /*	void deployPeons(Building building, int num_units);
        void deployRockWarriors(Building building, int num_units);
        void deployIronWarriors(Building building, int num_units);
        void deployRubberWarriors(Building building, int num_units);*/
    void createHarvesters(Building building, int num_tree, int num_rock, int num_iron, int num_rubber);

    void buildRockWeapons(Building building, int num_weapons, boolean infinite);

    void buildIronWeapons(Building building, int num_weapons, boolean infinite);

    void buildRubberWeapons(Building building, int num_weapons, boolean infinite);

    void doMagic(Unit chieftain, MagicType magic);

    void exitTower(Building building);

    void trainChieftain(Building building, boolean start);

    void placeBuilding(Selectable<?>[] selection, BuildingType template_type, int placing_grid_x,
            int placing_grid_y);

    void setRallyPoint(Building building, Target target);

    void setTarget(Selectable<?>[] selection, Target target, Action action,
            boolean aggressive);

    void setRallyPoint(Building building, int grid_x, int grid_y);

    void setLandscapeTarget(Selectable<?>[] selection, int grid_x, int grid_y, Action action,
            boolean aggressive);

    void setPreferredGamespeed(int speed);

    void changePreferredGamespeed(int delta);
}
