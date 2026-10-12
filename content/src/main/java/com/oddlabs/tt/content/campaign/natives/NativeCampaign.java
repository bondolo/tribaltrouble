package com.oddlabs.tt.content.campaign.natives;


import com.oddlabs.tt.content.campaign.Campaign;
import com.oddlabs.tt.content.campaign.CampaignDialogForm;
import com.oddlabs.tt.content.campaign.CampaignIcons;
import com.oddlabs.tt.content.campaign.CampaignState;
import com.oddlabs.tt.content.campaign.Island;
import com.oddlabs.tt.content.menu.Menu;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.client.Peer;
import com.oddlabs.tt.client.screen.ScreenManager;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Native faction single-player campaign sequence.
 */
public final class NativeCampaign extends Campaign {
    public static final int MAX_UNITS = 41;
    private static final int[] INITIAL_STATES = new int[]{
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

    private transient final NativeCampaignIcons icons;

    public NativeCampaign(Menu menu) {
        this(menu.getGUIRoot(), menu.getPeer(), menu.getScreenManager());
    }

    public NativeCampaign(CampaignState campaign_state, Menu menu) {
        this(menu.getGUIRoot(), campaign_state, menu.getPeer(), menu.getScreenManager());
    }

    public NativeCampaign(GUIRoot gui_root, Peer peer, ScreenManager screenManager) {
        this(gui_root, new CampaignState(INITIAL_STATES), peer, screenManager);
    }

    public NativeCampaign(GUIRoot gui_root, CampaignState campaign_state, Peer peer, ScreenManager screenManager) {
        super(campaign_state, peer, screenManager);
        icons = new NativeCampaignIcons("/gui/native_campaign.xml");

        if (getState().getCurrentIsland() == -1) {
            startIsland(gui_root, 0);
        }
        getState().setHasMagic0(true);
        getState().setHasRubberWeapons(true);
    }

    @Override
    public CampaignIcons getIcons() {
        return icons;
    }

    @Override
    protected void islandChosen(GUIRoot gui_root, int number) {
        Form dialog = new CampaignDialogForm(gui_root, islands[number].getHeader(),
                islands[number].getDescription(),
                null,
                Origin.AT_START,
                () -> startIsland(gui_root, number), true);
        gui_root.addModalForm(dialog);
    }

    @Override
    protected CharSequence getCurrentObjective() {
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
    protected void startIsland(GUIRoot gui_root, int number) {
        getState().setCurrentIsland(number);
        islands[number].chosen(gui_root);
    }
}
