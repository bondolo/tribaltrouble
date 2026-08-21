package com.oddlabs.tt.content.campaign;

import com.oddlabs.tt.client.camera.Camera;
import com.oddlabs.tt.client.camera.StaticCamera;
import com.oddlabs.tt.client.delegate.GameStatsDelegate;
import com.oddlabs.tt.client.viewer.InGameInfo;
import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.content.menu.InGameMainMenu;
import com.oddlabs.tt.content.menu.InGameMenuHook;
import com.oddlabs.tt.content.menu.Menu;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Skin;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;

/**
 * In-game session handler and menu hook for campaign mode.
 */
final class CampaignInGameInfo implements InGameInfo, InGameMenuHook {
    private final Campaign campaign;

    public CampaignInGameInfo(Campaign campaign) {
        this.campaign = campaign;
    }

    @Override
    public void openInGameMenu(WorldViewer viewer, Camera camera) {
        viewer.getGUIRoot().pushDelegate(new InGameMainMenu(viewer, new StaticCamera(camera.getState())));
    }

    @Override
    public boolean isRated() {
        return false;
    }

    @Override
    public boolean isMultiplayer() {
        return false;
    }

    @Override
    public float getRandomStartPosition() {
        return 0f;
    }

    @Override
    public void addGUI(WorldViewer viewer, InGameMainMenu menu, Group game_infos) {
        menu.addAbortButton(Menu.i18n("end_game"));
        int screen_width = viewer.getGUIRoot().getWidth();
        LabelBox label_objective = new LabelBox(Menu.i18n("objective"), Skin.getSkin().getEditFont(), screen_width / 2);
        LabelBox label_description = new LabelBox(campaign.getCurrentObjective(), Skin.getSkin().getEditFont(),
                screen_width / 2);
        game_infos.addChild(label_objective);
        game_infos.addChild(label_description);
        label_objective.place();
        label_description.place(label_objective, BOTTOM_LEFT);
        game_infos.compileCanvas();
    }

    @Override
    public void addGameOverGUI(WorldViewer viewer, final GameStatsDelegate delegate, int header_y,
            Group group) {
        HorizButton button_ok = new OKButton(150);
        button_ok.addMouseClickListener((_, _, _, _) -> delegate.startMenu());

        group.addChild(button_ok);
        button_ok.place();
    }

    @Override
    public void close(WorldViewer viewer) {
        if (campaign.getState().getIslandState(0) != CampaignState.ISLAND_COMPLETED) {
            Menu.startMenu(viewer.getNetwork(), viewer.getGUIRoot().getGUI(),
                    viewer.getAudioManager());
        } else {
            campaign.pushDelegate(viewer.getNetwork(), viewer.getGUIRoot().getGUI());
        }
    }

    @Override
    public void abort(WorldViewer viewer) {
        viewer.getGUIRoot().pushDelegate(new GameStatsDelegate(viewer, viewer.getDelegate().getCamera(),
                Menu.i18n("game_aborted")));
        campaign.doDefeated();
    }
}
