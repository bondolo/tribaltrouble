package com.oddlabs.tt.content.campaign;


import com.oddlabs.tt.client.gui.CampaignIcons;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.client.gui.NativeCampaignIcons;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.engine.ClientEngine;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Native faction single-player campaign sequence.
 */
public final class NativeCampaign extends Campaign {
    public static final int MAX_UNITS = 41;
    private static final int[] INITIAL_STATES = new int[]{
            /*
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
            CampaignState.ISLAND_HIDDEN};

    private final Island[] islands = Stream.<Function<Campaign, Island>>of(
            NativeIsland0::new, NativeIsland1::new, NativeIsland2::new, NativeIsland3::new,
            NativeIsland4::new, NativeIsland5::new, NativeIsland6::new, NativeIsland7::new)
            .map(constructor -> constructor.apply(this))
            .toArray(Island[]::new);

    public NativeCampaign(GUIRoot gui_root,
            ClientEngine engine) {
        this(gui_root, new CampaignState(INITIAL_STATES), engine);
    }

    public NativeCampaign(GUIRoot gui_root,
            CampaignState campaign_state,
            ClientEngine engine) {
        super(campaign_state, engine);

        if (getState().getCurrentIsland() == -1) {
            startIsland(gui_root, 0);
        }
        getState().setHasMagic0(true);
        getState().setHasRubberWeapons(true);
    }

    @Override
    public CampaignIcons getIcons() {
        return NativeCampaignIcons.getIcons();
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
        throw new IllegalArgumentException("No current island");
    }

    @Override
    public void defeated(WorldViewer viewer, String game_over_message) {
        if (getState().getCurrentIsland() == 4)
            ((NativeIsland4) islands[4]).removeCounter();
        super.defeated(viewer, game_over_message);
    }

    @Override
    public void startIsland(GUIRoot gui_root, int number) {
        getState().setCurrentIsland(number);
        islands[number].chosen(gui_root);
    }
}
