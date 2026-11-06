package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.MagicType;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Sealed hierarchy of stateless simulation commands submitted by human or automated player controllers.
 */
public sealed interface PlayerCommand permits
        PlayerCommand.DeployUnits,
        PlayerCommand.CreateHarvesters,
        PlayerCommand.BuildRockWeapons,
        PlayerCommand.BuildIronWeapons,
        PlayerCommand.BuildRubberWeapons,
        PlayerCommand.DoMagic,
        PlayerCommand.ExitTower,
        PlayerCommand.TrainChieftain,
        PlayerCommand.PlaceBuilding,
        PlayerCommand.SetRallyPointTarget,
        PlayerCommand.SetRallyPointCoords,
        PlayerCommand.SetTarget,
        PlayerCommand.SetLandscapeTarget,
        PlayerCommand.SetPreferredGamespeed,
        PlayerCommand.ChangePreferredGamespeed {

    record DeployUnits(int buildingId, DeployType deployType, int numUnits) implements PlayerCommand {
    }

    record CreateHarvesters(int buildingId, int numTree, int numRock, int numIron, int numRubber)
            implements PlayerCommand {
    }

    record BuildRockWeapons(int buildingId, int numWeapons, boolean infinite) implements PlayerCommand {
    }

    record BuildIronWeapons(int buildingId, int numWeapons, boolean infinite) implements PlayerCommand {
    }

    record BuildRubberWeapons(int buildingId, int numWeapons, boolean infinite) implements PlayerCommand {
    }

    record DoMagic(int chieftainId, MagicType magic) implements PlayerCommand {
    }

    record ExitTower(int buildingId) implements PlayerCommand {
    }

    record TrainChieftain(int buildingId, boolean start) implements PlayerCommand {
    }

    record PlaceBuilding(int[] selectionIds, BuildingType templateType, int gridX, int gridY)
            implements PlayerCommand {

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            return obj instanceof PlaceBuilding other
                    && gridX == other.gridX
                    && gridY == other.gridY
                    && templateType == other.templateType
                    && Arrays.equals(selectionIds, other.selectionIds);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(selectionIds);
            result = 31 * result + templateType.hashCode();
            result = 31 * result + Integer.hashCode(gridX);
            result = 31 * result + Integer.hashCode(gridY);
            return result;
        }

        @Override
        public String toString() {
            return "PlaceBuilding[selectionIds=" + Arrays.toString(selectionIds)
                    + ", templateType=" + templateType
                    + ", gridX=" + gridX
                    + ", gridY=" + gridY + "]";
        }
    }

    record SetRallyPointTarget(int buildingId, int targetEntityId) implements PlayerCommand {
    }

    record SetRallyPointCoords(int buildingId, int gridX, int gridY) implements PlayerCommand {
    }

    record SetTarget(int[] selectionIds, int targetEntityId, Action action, boolean aggressive)
            implements PlayerCommand {

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            return obj instanceof SetTarget other
                    && targetEntityId == other.targetEntityId
                    && aggressive == other.aggressive
                    && action == other.action
                    && Arrays.equals(selectionIds, other.selectionIds);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(selectionIds);
            result = 31 * result + Integer.hashCode(targetEntityId);
            result = 31 * result + action.hashCode();
            result = 31 * result + Boolean.hashCode(aggressive);
            return result;
        }

        @Override
        public String toString() {
            return "SetTarget[selectionIds=" + Arrays.toString(selectionIds)
                    + ", targetEntityId=" + targetEntityId
                    + ", action=" + action
                    + ", aggressive=" + aggressive + "]";
        }
    }

    record SetLandscapeTarget(int[] selectionIds, int gridX, int gridY, Action action, boolean aggressive)
            implements PlayerCommand {

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            return obj instanceof SetLandscapeTarget other
                    && gridX == other.gridX
                    && gridY == other.gridY
                    && aggressive == other.aggressive
                    && action == other.action
                    && Arrays.equals(selectionIds, other.selectionIds);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(selectionIds);
            result = 31 * result + Integer.hashCode(gridX);
            result = 31 * result + Integer.hashCode(gridY);
            result = 31 * result + action.hashCode();
            result = 31 * result + Boolean.hashCode(aggressive);
            return result;
        }

        @Override
        public String toString() {
            return "SetLandscapeTarget[selectionIds=" + Arrays.toString(selectionIds)
                    + ", gridX=" + gridX
                    + ", gridY=" + gridY
                    + ", action=" + action
                    + ", aggressive=" + aggressive + "]";
        }
    }

    record SetPreferredGamespeed(int speed) implements PlayerCommand {
    }

    record ChangePreferredGamespeed(int delta) implements PlayerCommand {
    }
}
