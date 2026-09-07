package com.oddlabs.tt.client.controller;

import com.oddlabs.tt.client.delegate.RallyPointDelegate;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.simulation.model.Building;

/**
 * Controller for Quarters building actions, handling chieftain training,
 * rally point positioning, and peon queue spinner adjustments.
 */
public final class QuartersActionController implements ActionController {
    private final ActionContext context;
    private final Building building;

    public QuartersActionController(ActionContext context, Building building) {
        this.context = context;
        this.building = building;
    }

    public void executeTrainChieftain() {
        if (!building.isDead() && (building.canBuildChieftain() || building.canStopChieftain())) {
            boolean startTraining = !building.getChieftainContainer().map(c -> c.isTraining()).orElse(false);
            context.getViewer().getPeerHub().getPlayerInterface().trainChieftain(building, startTraining);
        }
    }

    public void executeSetRallyPoint() {
        if (!building.isDead()) {
            context.pushDelegate(new RallyPointDelegate(context.getViewer(), context.getCamera(), building));
            context.markNeedsUpdate();
        }
    }

    public Building getBuilding() {
        return building;
    }

    @Override
    public boolean handleInput(InputEvent event) {
        InputPhase phase = event.getPhase();
        boolean pressed = phase == InputPhase.PRESSED || phase == InputPhase.REPEAT;
        boolean released = phase == InputPhase.RELEASED;
        boolean repeat = phase == InputPhase.REPEAT;

        if (pressed) {
            if (!repeat) {
                if (event.consumeAction(GameAction.TRAIN_CHIEFTAIN)) {
                    executeTrainChieftain();
                    event.consume();
                    return true;
                }
                if (event.consumeAction(GameAction.UNIT_SET_RALLY)) {
                    executeSetRallyPoint();
                    event.consume();
                    return true;
                }
            }
            return handlePeonSpinner(event, true);
        } else if (released) {
            return handlePeonSpinner(event, false);
        }
        return false;
    }

    private boolean handlePeonSpinner(InputEvent event, boolean pressed) {
        boolean active = false;
        boolean dec = false;
        boolean batch = false;

        if (event.consumeAction(GameAction.TRAIN_PEON)) {
            active = true;
        } else if (event.consumeAction(GameAction.TRAIN_PEON_DEC)) {
            active = true;
            dec = true;
        } else if (event.consumeAction(GameAction.TRAIN_PEON_BATCH)) {
            active = true;
            batch = true;
        } else if (event.consumeAction(GameAction.TRAIN_PEON_BATCH_DEC)) {
            active = true;
            dec = true;
            batch = true;
        }

        if (active) {
            context.adjustQuartersPeon(pressed, dec, batch);
            event.consume();
            return true;
        }
        return false;
    }
}
