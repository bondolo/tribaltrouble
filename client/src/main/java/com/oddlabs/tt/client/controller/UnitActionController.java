package com.oddlabs.tt.client.controller;

import com.oddlabs.tt.client.delegate.PlacingDelegate;
import com.oddlabs.tt.client.delegate.TargetDelegate;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.simulation.model.Action;
import com.oddlabs.tt.simulation.model.BuildingType;
import com.oddlabs.tt.simulation.model.MagicType;
import com.oddlabs.tt.simulation.model.Unit;
import org.jspecify.annotations.Nullable;

/**
 * Controller for unit selection actions, including basic movement, combat,
 * peon construction orders, and chieftain spells.
 */
public final class UnitActionController implements ActionController {
    private final ActionContext context;
    private final boolean peon;
    private final @Nullable Unit chieftain;

    public UnitActionController(ActionContext context, boolean peon, @Nullable Unit chieftain) {
        this.context = context;
        this.peon = peon;
        this.chieftain = chieftain;
    }

    public void executeMove() {
        if (context.getViewer().getLocalPlayer().canMove()) {
            context.pushDelegate(new TargetDelegate(context.getViewer(), context.getCamera(), Action.MOVE));
        }
    }

    public void executeAttack() {
        if (context.getViewer().getLocalPlayer().canAttack()) {
            context.pushDelegate(new TargetDelegate(context.getViewer(), context.getCamera(), Action.ATTACK));
        }
    }

    public void executeGatherRepair() {
        if (peon && context.getViewer().getLocalPlayer().canRepair()) {
            context.pushDelegate(new TargetDelegate(context.getViewer(), context.getCamera(), Action.GATHER_REPAIR));
        }
    }

    public void executeBuild(BuildingType buildingType) {
        if (peon && context.getViewer().getLocalPlayer().canBuild(buildingType)) {
            context.pushDelegate(new PlacingDelegate(context.getViewer(), context.getCamera().getState(),
                    buildingType));
        }
    }

    public void executeMagic(int magicIndex) {
        if (chieftain != null && !chieftain.isDead() && context.getViewer().getLocalPlayer().canDoMagic(magicIndex)) {
            MagicType magicType = context.getViewer().getLocalPlayer().getRaceInfo().getMagicType(magicIndex);
            if (chieftain.canDoMagic(magicType)) {
                context.getViewer().getPeerHub().getPlayerInterface().doMagic(chieftain, magicType);
            }
        }
    }

    public boolean isPeon() {
        return peon;
    }

    public @Nullable Unit getChieftain() {
        return chieftain;
    }

    @Override
    public boolean handleInput(InputEvent event) {
        if (event.getPhase() != InputPhase.PRESSED) {
            return false;
        }

        if (event.consumeAction(GameAction.UNIT_MOVE)) {
            executeMove();
            event.consume();
            return true;
        }
        if (event.consumeAction(GameAction.UNIT_ATTACK)) {
            executeAttack();
            event.consume();
            return true;
        }
        if (peon && event.consumeAction(GameAction.UNIT_GATHER)) {
            executeGatherRepair();
            event.consume();
            return true;
        }
        if (peon && event.consumeAction(GameAction.UNIT_BUILD_QUARTERS)) {
            executeBuild(BuildingType.QUARTERS);
            event.consume();
            return true;
        }
        if (peon && event.consumeAction(GameAction.UNIT_BUILD_ARMORY)) {
            executeBuild(BuildingType.ARMORY);
            event.consume();
            return true;
        }
        if (peon && event.consumeAction(GameAction.UNIT_BUILD_TOWER)) {
            executeBuild(BuildingType.TOWER);
            event.consume();
            return true;
        }
        if (chieftain != null && event.consumeAction(GameAction.MAGIC_1)) {
            executeMagic(0);
            event.consume();
            return true;
        }
        if (chieftain != null && event.consumeAction(GameAction.MAGIC_2)) {
            executeMagic(1);
            event.consume();
            return true;
        }
        return false;
    }
}
