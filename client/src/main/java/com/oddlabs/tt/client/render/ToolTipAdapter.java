package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.*;

import com.oddlabs.tt.engine.render.*;

import com.oddlabs.tt.client.gui.GUIIcons;
import com.oddlabs.tt.gui.ToolTipBox;
import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.ModelToolTip;
import com.oddlabs.tt.simulation.model.SceneryModel;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Supply;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.behaviour.Controller;
import com.oddlabs.tt.simulation.behaviour.GatherController;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.gui.ToolTip;

import java.util.List;
import java.util.Objects;

/**
 * Adapter that maps simulation-side ModelToolTip entities to the UI ToolTip representation.
 */
public final class ToolTipAdapter implements ToolTip {
    private final ModelToolTip model;
    private final Player local_player;

    public ToolTipAdapter(ModelToolTip model, Player local_player) {
        this.local_player = local_player;
        this.model = model;
    }

    private void visitPlayer(ToolTipBox tool_tip, Player player) {
        tool_tip.append(player.getPlayerInfo().getName());
        tool_tip.append(" - ");
        //      tool_tip.append(team_tip);
        //      tool_tip.append(" ");
        //      if (Renderer.getRenderer().getSettings().inDeveloperMode()) {
        //          tool_tip.append("total_units=");
        //          tool_tip.append(unit_count.getNumSupplies());
        //          tool_tip.append(" ");
        //      }
    }

    private void visitSelectable(ToolTipBox tool_tip, Selectable<?> selectable) {
        assert !selectable.isDead();
        visitPlayer(tool_tip, selectable.getOwner());
        /*      if (Renderer.getRenderer().getSettings().developer_mode) {
        		if (getCurrentBehaviour() instanceof WalkBehaviour)
        		((WalkBehaviour)getCurrentBehaviour()).appendToolTip(tool_tip);
        		else*/
        //tool_tip.append(getPrimaryController().getClass().getName());
        //}
    }

    @Override
    public void appendToolTip(ToolTipBox tool_tip) {
        switch (model) {
            case Unit unit -> visitUnit(tool_tip, unit);
            case Building building -> visitBuilding(tool_tip, building);
            case Supply supply -> visitSupply(tool_tip, supply);
            case SceneryModel scenery -> visitSceneryModel(tool_tip, scenery);
            default -> {
            }
        }
    }

    private void visitSceneryModel(ToolTipBox tool_tip, SceneryModel model) {
        String name = model.getName();
        if (name != null)
            tool_tip.append(name);
    }

    private void visitSupply(ToolTipBox tool_tip, Supply supply) {
        tool_tip.append(supply.getName());
        tool_tip.append(GUIIcons.getIcons().getToolTipIcon(supply.getSupplyType()));
    }

    private void visitBuilding(ToolTipBox tool_tip, Building building) {
        visitSelectable(tool_tip, building);
        tool_tip.append(building.getTemplate().getName());
        var health = (float) building.getHitPoints() / building.getTemplate().getMaxHitPoints();
        var watch = List.of(GUIIcons.getIcons().getWatch(health));
        tool_tip.append(watch);
        //      if (getUnitContainer() != null && Renderer.getRenderer().getSettings().developer_mode) {
        //          tool_tip.append(" units_in_building ");
        //          tool_tip.append(getUnitContainer().getNumSupplies());
        //      }

    }

    private void visitUnit(ToolTipBox tool_tip, Unit unit) {
        visitSelectable(tool_tip, unit);
        String name = unit.getName();
        tool_tip.append(Objects.requireNonNullElseGet(name, () -> unit.getTemplate().getName()));
        Controller c = unit.getPrimaryController();
        if (unit.getAbilities().hasAbilities(Abilities.MAGIC)) {
            var health = (float) unit.getHitPoints() / unit.getTemplate().getMaxHitPoints();
            tool_tip.append(List.of(GUIIcons.getIcons().getWatch(health)));
        } else if (unit.getOwner() == local_player && c instanceof GatherController<?> gc) {
            tool_tip.append(GUIIcons.getIcons().getToolTipIcon(gc.getSupplyType()));
        }
        /*      if (getCurrentBehaviour() instanceof WalkBehaviour)
        		((WalkBehaviour)getCurrentBehaviour()).appendToolTip(tool_tip);*/

    }
}
