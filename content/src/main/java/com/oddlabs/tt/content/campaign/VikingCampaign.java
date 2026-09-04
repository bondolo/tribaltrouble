package com.oddlabs.tt.content.campaign;


import com.oddlabs.tt.client.gui.CampaignIcons;
import com.oddlabs.tt.client.gui.VikingCampaignIcons;
import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.engine.ClientEngine;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Viking faction single-player campaign sequence.
 */
public final class VikingCampaign extends Campaign {
    public static final int MAX_UNITS = 46;
    private static final int[] INITIAL_STATES = new int[]{
            /*
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE,
            		CampaignState.ISLAND_AVAILABLE};
            */
            CampaignState.ISLAND_AVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_HIDDEN,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE,
            CampaignState.ISLAND_UNAVAILABLE};

    private final Island[] islands = Stream.<Function<VikingCampaign, Island>>of(
            VikingIsland0::new, VikingIsland1::new, VikingIsland2::new, VikingIsland3::new, VikingIsland4::new,
            VikingIsland5::new, VikingIsland6::new, VikingIsland7::new, VikingIsland8::new, VikingIsland9::new,
            VikingIsland10::new, VikingIsland11::new, VikingIsland12::new, VikingIsland13::new, VikingIsland14::new)
            .map(c -> c.apply(this))
            .toArray(Island[]::new);

    public VikingCampaign(GUIRoot gui_root, ClientEngine engine) {
        this(gui_root, new CampaignState(INITIAL_STATES), engine);
    }

    public VikingCampaign(GUIRoot gui_root,
            CampaignState campaign_state,
            ClientEngine engine) {
        super(campaign_state, engine);
        if (getState().getCurrentIsland() == -1) {
            startIsland(gui_root, 0);
        }
    }

    @Override
    public CampaignIcons getIcons() {
        return VikingCampaignIcons.getIcons();
    }

    @Override
    public void islandChosen(GUIRoot gui_root, int number) {
        Form dialog = new CampaignDialogForm(islands[number].getHeader(),
                islands[number].getDescription(),
                null,
                Origin.AT_START,
                () -> startIsland(gui_root, number), true);
        gui_root.addModalForm(dialog);
    }

    @Override
    public CharSequence getCurrentObjective() {
        if (getState().getCurrentIsland() != -1) {
            return islands[getState().getCurrentIsland()].getCurrentObjective();
        }
        throw new IllegalStateException("No current island");
    }

    @Override
    public void defeated(WorldViewer viewer, String game_over_message) {
        if (getState().getCurrentIsland() == 13)
            ((VikingIsland13) islands[13]).removeCounter();
        super.defeated(viewer, game_over_message);
    }

    @Override
    public void startIsland(GUIRoot gui_root, int number) {
        getState().setCurrentIsland(number);
        islands[number].chosen(gui_root);
    }
}
