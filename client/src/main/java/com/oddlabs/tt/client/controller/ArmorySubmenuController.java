package com.oddlabs.tt.client.controller;

import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.SupplyType;

import java.util.EnumSet;
import java.util.Set;

/**
 * Modal controller for active Armory production submenus (Weapons, Army, Transport, Harvest).
 * Handles spinner adjustments, direct submenu switching, and stack exit on cancel or back.
 */
public final class ArmorySubmenuController implements ActionController {

    private static final Set<GameAction> SUBMENU_ACTIONS = EnumSet.of(
            GameAction.RES_TREE,
            GameAction.RES_TREE_DEC, GameAction.RES_TREE_BATCH, GameAction.RES_TREE_BATCH_DEC, GameAction.RES_ROCK,
            GameAction.RES_ROCK_DEC, GameAction.RES_ROCK_BATCH, GameAction.RES_ROCK_BATCH_DEC, GameAction.RES_IRON,
            GameAction.RES_IRON_DEC, GameAction.RES_IRON_BATCH, GameAction.RES_IRON_BATCH_DEC, GameAction.RES_CHICKEN,
            GameAction.RES_CHICKEN_DEC, GameAction.RES_CHICKEN_BATCH, GameAction.RES_CHICKEN_BATCH_DEC,
            GameAction.TRAIN_PEON, GameAction.TRAIN_PEON_DEC, GameAction.TRAIN_PEON_BATCH,
            GameAction.TRAIN_PEON_BATCH_DEC, GameAction.GAMEPLAY_BACK
    );

    private final ActionContext context;
    private final SubmenuType type;
    private final Building building;
    private final ActionControllerStack stack;

    public ArmorySubmenuController(ActionContext context, SubmenuType type, Building building,
            ActionControllerStack stack) {
        this.context = context;
        this.type = type;
        this.building = building;
        this.stack = stack;
    }

    public SubmenuType type() {
        return type;
    }

    public Building getBuilding() {
        return building;
    }

    @Override
    public void onEnter() {
        context.markNeedsUpdate();
    }

    @Override
    public void onExit() {
        context.markNeedsUpdate();
    }

    @Override
    public boolean handleInput(InputEvent event) {
        InputPhase phase = event.getPhase();
        boolean pressed = phase == InputPhase.PRESSED || phase == InputPhase.REPEAT;
        boolean released = phase == InputPhase.RELEASED;
        boolean repeat = phase == InputPhase.REPEAT;

        if (pressed) {
            if (!repeat) {
                if (handleSubmenuSwitch(event)) {
                    return true;
                }
                if (event.consumeAction(GameAction.GAMEPLAY_BACK)) {
                    event.consume();
                    stack.pop();
                    return true;
                }
            }
            return handleSpinners(event, true);
        } else if (released) {
            return handleSpinners(event, false);
        }
        return false;
    }

    private boolean canSwitchSubmenu(InputEvent event) {
        for (GameAction action : SUBMENU_ACTIONS) {
            if (event.hasAction(action)) {
                return false;
            }
        }
        return true;
    }

    private boolean handleSubmenuSwitch(InputEvent event) {
        if (!canSwitchSubmenu(event)) {
            return false;
        }

        SubmenuType target = null;
        if (event.consumeAction(GameAction.PROD_WEAPONS)) {
            target = SubmenuType.WEAPONS;
        } else if (event.consumeAction(GameAction.PROD_ARMY)) {
            target = SubmenuType.ARMY;
        } else if (event.consumeAction(GameAction.PROD_TRANSPORT)) {
            target = SubmenuType.TRANSPORT;
        } else if (event.consumeAction(GameAction.PROD_HARVEST)) {
            target = SubmenuType.HARVEST;
        }

        if (target != null) {
            event.consume();
            if (target != type) {
                stack.pop();
                stack.push(new ArmorySubmenuController(context, target, building, stack));
            }
            return true;
        }
        return false;
    }

    private boolean handleSpinners(InputEvent event, boolean pressed) {
        if (type == SubmenuType.ARMY) {
            SpinnerAction peon = checkSpinnerAction(event, GameAction.TRAIN_PEON, GameAction.TRAIN_PEON_DEC,
                    GameAction.TRAIN_PEON_BATCH, GameAction.TRAIN_PEON_BATCH_DEC);
            if (peon.active()) {
                context.adjustArmoryPeon(pressed, peon.decrement(), peon.batch());
                event.consume();
                return true;
            }
        }

        if (handleSupplySpinner(event, pressed, SupplyType.RUBBER, GameAction.RES_CHICKEN,
                GameAction.RES_CHICKEN_DEC, GameAction.RES_CHICKEN_BATCH, GameAction.RES_CHICKEN_BATCH_DEC)) {
            return true;
        }

        if (handleSupplySpinner(event, pressed, SupplyType.IRON, GameAction.RES_IRON,
                GameAction.RES_IRON_DEC, GameAction.RES_IRON_BATCH, GameAction.RES_IRON_BATCH_DEC)) {
            return true;
        }

        if (handleSupplySpinner(event, pressed, SupplyType.WOOD, GameAction.RES_TREE,
                GameAction.RES_TREE_DEC, GameAction.RES_TREE_BATCH, GameAction.RES_TREE_BATCH_DEC)) {
            return true;
        }

        return handleSupplySpinner(event, pressed, SupplyType.ROCK, GameAction.RES_ROCK,
                GameAction.RES_ROCK_DEC, GameAction.RES_ROCK_BATCH, GameAction.RES_ROCK_BATCH_DEC);
    }

    private boolean handleSupplySpinner(
            InputEvent event, boolean pressed, SupplyType supply,
            GameAction base, GameAction dec, GameAction batch, GameAction batchDec
    ) {
        SpinnerAction action = checkSpinnerAction(event, base, dec, batch, batchDec);
        if (action.active()) {
            context.adjustArmorySupply(type, supply, pressed, action.decrement(), action.batch());
            event.consume();
            return true;
        }
        return false;
    }

    private static SpinnerAction checkSpinnerAction(
            InputEvent event, GameAction base,
            GameAction dec, GameAction batch, GameAction batchDec
    ) {
        if (event.consumeAction(base)) return new SpinnerAction(true, false, false);
        if (event.consumeAction(dec)) return new SpinnerAction(true, true, false);
        if (event.consumeAction(batch)) return new SpinnerAction(true, false, true);
        if (event.consumeAction(batchDec)) return new SpinnerAction(true, true, true);
        return new SpinnerAction(false, false, false);
    }

    private record SpinnerAction(boolean active, boolean decrement, boolean batch) {
    }
}
