package com.oddlabs.tt.client.controller;

import com.oddlabs.tt.client.delegate.TargetDelegate;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.Building;

/**
 * Controller for defensive tower actions, managing tower attack orders and garrison evacuation.
 */
public final class TowerActionController implements ActionController {
    private final ActionContext context;
    private final Building building;

    public TowerActionController(ActionContext context, Building building) {
        this.context = context;
        this.building = building;
    }

    public void executeTowerAttack() {
        if (!building.isDead() && building.getAbilities().hasAbilities(Abilities.ATTACK)) {
            context.pushDelegate(new TargetDelegate(context.getViewer(), context.getCamera(), Action.ATTACK));
        }
    }

    public void executeExitTower() {
        if (!building.isDead() && building.canExitTower()) {
            context.getViewer().getPeerHub().getPlayerInterface().exitTower(building);
            context.markNeedsUpdate();
        }
    }

    public Building getBuilding() {
        return building;
    }

    @Override
    public boolean handleInput(InputEvent event) {
        if (event.getPhase() != InputPhase.PRESSED) {
            return false;
        }

        if (event.consumeAction(GameAction.UNIT_ATTACK)) {
            executeTowerAttack();
            event.consume();
            return true;
        }
        if (event.consumeAction(GameAction.UNIT_EXIT_TOWER)) {
            executeExitTower();
            event.consume();
            return true;
        }
        return false;
    }
}
