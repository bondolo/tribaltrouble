package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.gui.ToolTipBox;
import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.simulation.model.BuildProductionContainer;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.WeaponTier;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.simulation.player.PlayerInterface;
import com.oddlabs.tt.client.viewer.WorldViewer;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A spinner GUI control for queuing weapon and item production orders in a building.
 */
public final class BuildSpinner extends IconSpinner {
    public static final int INFINITE_LIMIT = BuildProductionContainer.INFINITE_LIMIT;

    private final PlayerInterface player_interface;
    private final WeaponTier tier;

    private @Nullable Building current_building;
    private int num_orders;
    private int order_size;
    private boolean infinite;

    BuildSpinner(WorldViewer viewer, PlayerInterface player_interface,
            ModeIconQuads icon_quad, String tool_tip, List<IconQuad> tool_tip_icons,
            GameAction action, GameAction dec_action, WeaponTier tier) {
        super(viewer, icon_quad, tool_tip, tool_tip_icons, action, dec_action);
        this.player_interface = player_interface;
        this.tier = tier;
    }

    void setBuilding(@Nullable Building current_building) {
        this.current_building = current_building;
        if (current_building != null && !current_building.isDead()) {
            num_orders = current_building.getWeaponBuildContainer(tier).map(BuildProductionContainer::getNumOrders)
                    .orElse(0);
        } else {
            num_orders = 0;
        }
    }

    @Override
    public int computeCount() {
        if (current_building != null && !current_building.isDead()) {
            BuildProductionContainer build_container = current_building.getWeaponBuildContainer(tier).orElse(null);
            if (build_container == null) {
                return 0;
            }
            int count = Math.clamp(build_container.getNumSupplies() + getOrderDiff(),
                    0, build_container.getMaxSupplyCount());
            infinite = count >= INFINITE_LIMIT;
            return count;
        } else {
            return 0;
        }
    }

    @Override
    public boolean renderInfinite() {
        return infinite;
    }

    private void order(int num) {
        if (current_building != null && !current_building.isDead() && tier != null) {
            switch (tier) {
                case ROCK -> player_interface.buildRockWeapons(current_building, num, infinite);
                case IRON -> player_interface.buildIronWeapons(current_building, num, infinite);
                case RUBBER -> player_interface.buildRubberWeapons(current_building, num, infinite);
            }
        }
    }

    @Override
    public void appendToolTip(ToolTipBox tool_tip_box) {
        if (!isDisabled())
            super.appendToolTip(tool_tip_box);
    }

    @Override
    protected float getProgress() {
        return (current_building == null || current_building.isDead() || tier == null)
                ? 0
                : current_building.getWeaponBuildContainer(tier)
                        .map(BuildProductionContainer::getBuildProgress).orElse(0f);
    }

    @Nullable
    Building getBuilding() {
        return current_building;
    }

    private int getOrderDiff() {
        return (current_building == null || current_building.isDead() || tier == null)
                ? 0
                : num_orders - current_building.getWeaponBuildContainer(tier)
                        .map(BuildProductionContainer::getNumOrders).orElse(0);
    }

    @Override
    protected void increase(int amount) {
        order_size += amount;
        num_orders += amount;
    }

    @Override
    protected void decrease(int amount) {
        order_size -= amount;
        num_orders -= amount;
    }

    @Override
    protected void release() {
        order(order_size);
        order_size = 0;
    }

    @Override
    protected int getOrderSize() {
        return order_size;
    }
}
