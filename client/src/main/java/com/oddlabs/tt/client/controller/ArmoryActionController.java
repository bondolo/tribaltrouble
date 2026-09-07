package com.oddlabs.tt.client.controller;

import com.oddlabs.tt.client.delegate.RallyPointDelegate;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.simulation.model.Building;

/**
 * Controller for top-level Armory building actions, dispatching into category submenus
 * and managing rally point placement.
 */
public final class ArmoryActionController implements ActionController {
    private final ActionContext context;
    private final Building building;
    private final ActionControllerStack stack;

    public ArmoryActionController(ActionContext context, Building building, ActionControllerStack stack) {
        this.context = context;
        this.building = building;
        this.stack = stack;
    }

    public void openSubmenu(SubmenuType type) {
        stack.push(new ArmorySubmenuController(context, type, building, stack));
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

        if (pressed && !repeat) {
            if (event.consumeAction(GameAction.PROD_WEAPONS)) {
                openSubmenu(SubmenuType.WEAPONS);
                event.consume();
                return true;
            }
            if (event.consumeAction(GameAction.PROD_ARMY) || event.consumeAction(GameAction.UNIT_ATTACK)) {
                openSubmenu(SubmenuType.ARMY);
                event.consume();
                return true;
            }
            if (event.consumeAction(GameAction.PROD_HARVEST) || event.consumeAction(GameAction.UNIT_GATHER)) {
                openSubmenu(SubmenuType.HARVEST);
                event.consume();
                return true;
            }
            if (event.consumeAction(GameAction.PROD_TRANSPORT)) {
                openSubmenu(SubmenuType.TRANSPORT);
                event.consume();
                return true;
            }
            if (event.consumeAction(GameAction.UNIT_SET_RALLY)) {
                executeSetRallyPoint();
                event.consume();
                return true;
            }
        } else if (released) {
            // Legacy transport alias (tower hotkey) from top-level Armory
            if (event.consumeAction(GameAction.UNIT_BUILD_TOWER) || event.consumeAction(GameAction.PROD_TRANSPORT)) {
                openSubmenu(SubmenuType.TRANSPORT);
                event.consume();
                return true;
            }
        }
        return false;
    }
}
