package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.DeployType;
import com.oddlabs.tt.simulation.model.MagicType;

/**
 * Exploded command interface exposing individual simulation actions with default reified command dispatch.
 */
public interface PlayerActions extends PlayerCommandExecutor {

    /**
     * Deploys units from a building.
     *
     * @param buildingId the building entity ID
     * @param deployType the unit deployment type
     * @param numUnits the number of units to deploy
     */
    void deployUnits(int buildingId, DeployType deployType, int numUnits);

    /**
     * Orders a quarters building to create harvesters.
     *
     * @param buildingId the quarters building ID
     * @param numTree wood harvesters to create
     * @param numRock rock harvesters to create
     * @param numIron iron harvesters to create
     * @param numRubber rubber harvesters to create
     */
    void createHarvesters(int buildingId, int numTree, int numRock, int numIron, int numRubber);

    /**
     * Orders an armory to produce rock weapons.
     *
     * @param buildingId the armory building ID
     * @param numWeapons number of weapons
     * @param infinite whether to continuously produce
     */
    void buildRockWeapons(int buildingId, int numWeapons, boolean infinite);

    /**
     * Orders an armory to produce iron weapons.
     *
     * @param buildingId the armory building ID
     * @param numWeapons number of weapons
     * @param infinite whether to continuously produce
     */
    void buildIronWeapons(int buildingId, int numWeapons, boolean infinite);

    /**
     * Orders an armory to produce rubber weapons.
     *
     * @param buildingId the armory building ID
     * @param numWeapons number of weapons
     * @param infinite whether to continuously produce
     */
    void buildRubberWeapons(int buildingId, int numWeapons, boolean infinite);

    /**
     * Orders a chieftain to cast a spell.
     *
     * @param chieftainId the chieftain entity ID
     * @param magic the magic spell type
     */
    void doMagic(int chieftainId, MagicType magic);

    /**
     * Orders units inside a tower to exit.
     *
     * @param buildingId the tower building ID
     */
    void exitTower(int buildingId);

    /**
     * Starts or stops training a chieftain at quarters.
     *
     * @param buildingId the quarters building ID
     * @param start whether to start or stop training
     */
    void trainChieftain(int buildingId, boolean start);

    /**
     * Places a building blueprint and orders peons to construct it.
     *
     * @param selectionIds array of selected builder entity IDs
     * @param templateType the type of building to place
     * @param gridX landscape grid X coordinate
     * @param gridY landscape grid Y coordinate
     */
    void placeBuilding(int[] selectionIds, BuildingType templateType, int gridX, int gridY);

    /**
     * Places a building blueprint and orders a single peon to construct it.
     *
     * @param unitId the selected builder entity ID
     * @param templateType the type of building to place
     * @param gridX landscape grid X coordinate
     * @param gridY landscape grid Y coordinate
     */
    default void placeBuilding(int unitId, BuildingType templateType, int gridX, int gridY) {
        placeBuilding(new int[]{unitId}, templateType, gridX, gridY);
    }

    /**
     * Sets the rally point of a building to a target entity.
     *
     * @param buildingId the building entity ID
     * @param targetEntityId the target entity ID
     */
    void setRallyPoint(int buildingId, int targetEntityId);

    /**
     * Sets the rally point of a building to landscape grid coordinates.
     *
     * @param buildingId the building entity ID
     * @param gridX landscape grid X coordinate
     * @param gridY landscape grid Y coordinate
     */
    void setRallyPoint(int buildingId, int gridX, int gridY);

    /**
     * Directs selected units to target an entity with an action.
     *
     * @param selectionIds array of selected unit entity IDs
     * @param targetEntityId the target entity ID
     * @param action the action type
     * @param aggressive whether the action is aggressive
     */
    void setTarget(int[] selectionIds, int targetEntityId, Action action, boolean aggressive);

    /**
     * Directs a single selected unit to target an entity with an action.
     *
     * @param unitId the selected unit entity ID
     * @param targetEntityId the target entity ID
     * @param action the action type
     * @param aggressive whether the action is aggressive
     */
    default void setTarget(int unitId, int targetEntityId, Action action, boolean aggressive) {
        setTarget(new int[]{unitId}, targetEntityId, action, aggressive);
    }

    /**
     * Directs selected units to move to or target landscape grid coordinates.
     *
     * @param selectionIds array of selected unit entity IDs
     * @param gridX landscape grid X coordinate
     * @param gridY landscape grid Y coordinate
     * @param action the action type
     * @param aggressive whether the action is aggressive
     */
    void setLandscapeTarget(int[] selectionIds, int gridX, int gridY, Action action, boolean aggressive);

    /**
     * Directs a single selected unit to move to or target landscape grid coordinates.
     *
     * @param unitId the selected unit entity ID
     * @param gridX landscape grid X coordinate
     * @param gridY landscape grid Y coordinate
     * @param action the action type
     * @param aggressive whether the action is aggressive
     */
    default void setLandscapeTarget(int unitId, int gridX, int gridY, Action action, boolean aggressive) {
        setLandscapeTarget(new int[]{unitId}, gridX, gridY, action, aggressive);
    }

    /**
     * Sets the preferred simulation game speed for this player.
     *
     * @param speed the desired speed index
     */
    void setPreferredGamespeed(int speed);

    /**
     * Adjusts the preferred simulation game speed by a delta.
     *
     * @param delta the speed delta
     */
    void changePreferredGamespeed(int delta);

    @Override
    default void execute(PlayerCommand command) {
        switch (command) {
            case PlayerCommand.DeployUnits cmd ->
                deployUnits(cmd.buildingId(), cmd.deployType(), cmd.numUnits());
            case PlayerCommand.CreateHarvesters cmd ->
                createHarvesters(cmd.buildingId(), cmd.numTree(), cmd.numRock(), cmd.numIron(), cmd.numRubber());
            case PlayerCommand.BuildRockWeapons cmd ->
                buildRockWeapons(cmd.buildingId(), cmd.numWeapons(), cmd.infinite());
            case PlayerCommand.BuildIronWeapons cmd ->
                buildIronWeapons(cmd.buildingId(), cmd.numWeapons(), cmd.infinite());
            case PlayerCommand.BuildRubberWeapons cmd ->
                buildRubberWeapons(cmd.buildingId(), cmd.numWeapons(), cmd.infinite());
            case PlayerCommand.DoMagic cmd ->
                doMagic(cmd.chieftainId(), cmd.magic());
            case PlayerCommand.ExitTower cmd ->
                exitTower(cmd.buildingId());
            case PlayerCommand.TrainChieftain cmd ->
                trainChieftain(cmd.buildingId(), cmd.start());
            case PlayerCommand.PlaceBuilding cmd ->
                placeBuilding(cmd.selectionIds(), cmd.templateType(), cmd.gridX(), cmd.gridY());
            case PlayerCommand.SetRallyPointTarget cmd ->
                setRallyPoint(cmd.buildingId(), cmd.targetEntityId());
            case PlayerCommand.SetRallyPointCoords cmd ->
                setRallyPoint(cmd.buildingId(), cmd.gridX(), cmd.gridY());
            case PlayerCommand.SetTarget cmd ->
                setTarget(cmd.selectionIds(), cmd.targetEntityId(), cmd.action(), cmd.aggressive());
            case PlayerCommand.SetLandscapeTarget cmd ->
                setLandscapeTarget(cmd.selectionIds(), cmd.gridX(), cmd.gridY(), cmd.action(), cmd.aggressive());
            case PlayerCommand.SetPreferredGamespeed cmd ->
                setPreferredGamespeed(cmd.speed());
            case PlayerCommand.ChangePreferredGamespeed cmd ->
                changePreferredGamespeed(cmd.delta());
        }
    }
}
