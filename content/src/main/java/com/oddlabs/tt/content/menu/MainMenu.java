package com.oddlabs.tt.content.menu;

import com.oddlabs.tt.client.camera.Camera;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.MenuButton;
import com.oddlabs.tt.content.campaign.CampaignForm;
import com.oddlabs.tt.content.skirmish.TerrainMenuForm;
import com.oddlabs.tt.content.tutorial.TutorialForm;

/**
 * The game main menu
 */
public final class MainMenu extends Menu {
    public MainMenu(GUIRoot gui_root, Camera camera) {
        super(gui_root, camera);
        reload();
    }

    private void addGameTypeButtons() {
        MenuButton tutorial = new MenuButton(Menu.i18n("tutorial"), COLOR_NORMAL, COLOR_ACTIVE);
        tutorial.addMouseClickListener((_, _, _, _) -> setMenu(new TutorialForm(getGUIRoot())));
        addChild(tutorial);

        MenuButton campaign_menu = new MenuButton(Menu.i18n("campaign"), COLOR_NORMAL, COLOR_ACTIVE);
        campaign_menu.addMouseClickListener((_, _, _, _) -> setMenu(new CampaignForm(MainMenu.this)));
        addChild(campaign_menu);

        MenuButton single_player = new MenuButton(Menu.i18n("skirmish"), COLOR_NORMAL, COLOR_ACTIVE);
        single_player.addMouseClickListener((_, _, _, _) -> setMenu(new TerrainMenuForm(MainMenu.this)));
        addChild(single_player);

        if (!engine.getSettings().hide_multiplayer) {
            MenuButton multi_player = new MenuButton(Menu.i18n("multiplayer"), COLOR_NORMAL, COLOR_ACTIVE);
            multi_player.addMouseClickListener((_, _, _, _) -> {
                if (engine.getNetwork().getMatchmakingClient().isConnected()) {
                    new SelectGameMenu(MainMenu.this);
                } else {
                    engine.getNetwork().getMatchmakingClient().close();
                    new LoginForm(MainMenu.this);
                }
            });
            addChild(multi_player);
        }
    }

    @Override
    protected void addButtons() {
        addGameTypeButtons();

        addDefaultOptionsButton();

        addExitButton();

        if (engine.getNetwork().getMatchmakingClient().isConnected()) {
            new SelectGameMenu(this);
        }
    }
}
