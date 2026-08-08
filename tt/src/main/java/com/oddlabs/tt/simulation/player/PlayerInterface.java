package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.BuildingType;
import org.jspecify.annotations.NonNull;

import com.oddlabs.tt.simulation.model.MagicType;

/**
 * Interface defining actions a player can take, such as deploying units, building, and casting magic.
 */
public interface PlayerInterface {
    void deployUnits(@NonNull Building building, @NonNull DeployType type, int num_units);

    /*	void deployPeons(Building building, int num_units);
        void deployRockWarriors(Building building, int num_units);
        void deployIronWarriors(Building building, int num_units);
        void deployRubberWarriors(Building building, int num_units);*/
    void createHarvesters(@NonNull Building building, int num_tree, int num_rock, int num_iron, int num_rubber);

    void buildRockWeapons(@NonNull Building building, int num_weapons, boolean infinite);

    void buildIronWeapons(@NonNull Building building, int num_weapons, boolean infinite);

    void buildRubberWeapons(@NonNull Building building, int num_weapons, boolean infinite);

    void doMagic(@NonNull Unit chieftain, @NonNull MagicType magic);

    void exitTower(@NonNull Building building);

    void trainChieftain(@NonNull Building building, boolean start);

    void placeBuilding(Selectable<?> @NonNull [] selection, @NonNull BuildingType template_type, int placing_grid_x,
            int placing_grid_y);

    void setRallyPoint(@NonNull Building building, @NonNull Target target);

    void setTarget(Selectable<?> @NonNull [] selection, @NonNull Target target, @NonNull Action action,
            boolean aggressive);

    void setRallyPoint(@NonNull Building building, int grid_x, int grid_y);

    void setLandscapeTarget(Selectable<?> @NonNull [] selection, int grid_x, int grid_y, @NonNull Action action,
            boolean aggressive);

    void setPreferredGamespeed(int speed);

    void changePreferredGamespeed(int delta);
}
