package com.oddlabs.tt.client.delegate;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.client.camera.Camera;
import com.oddlabs.tt.client.form.CampaignForm;
import com.oddlabs.tt.client.form.LoginForm;
import com.oddlabs.tt.client.form.SelectGameMenu;
import com.oddlabs.tt.client.form.TerrainMenuForm;
import com.oddlabs.tt.client.form.TutorialForm;
import com.oddlabs.tt.client.gui.GUIRoot;
import com.oddlabs.tt.client.gui.MenuButton;
import com.oddlabs.tt.render.Renderer;
import org.jspecify.annotations.NonNull;

/**
 * The game main menu
 */
public final class MainMenu extends Menu {
    public MainMenu(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, @NonNull Camera camera) {
        super(network, gui_root, camera);
        reload();
    }

    private void addGameTypeButtons() {
        MenuButton tutorial = new MenuButton(Menu.i18n("tutorial"), COLOR_NORMAL, COLOR_ACTIVE);
        tutorial.addMouseClickListener((_, _, _, _) -> setMenu(new TutorialForm(getNetwork(), getGUIRoot())));
        addChild(tutorial);

        MenuButton campaign_menu = new MenuButton(Menu.i18n("campaign"), COLOR_NORMAL, COLOR_ACTIVE);
        campaign_menu.addMouseClickListener((_, _, _, _) -> setMenu(new CampaignForm(getNetwork(), getGUIRoot(),
                MainMenu.this)));
        addChild(campaign_menu);

        MenuButton single_player = new MenuButton(Menu.i18n("skirmish"), COLOR_NORMAL, COLOR_ACTIVE);
        single_player.addMouseClickListener((_, _, _, _) -> setMenu(new TerrainMenuForm(getNetwork(), getGUIRoot(),
                MainMenu.this)));
        addChild(single_player);

        if (!Renderer.getRenderer().getSettings().hide_multiplayer) {
            MenuButton multi_player = new MenuButton(Menu.i18n("multiplayer"), COLOR_NORMAL, COLOR_ACTIVE);
            multi_player.addMouseClickListener((_, _, _, _) -> {
                if (Renderer.getRenderer().getNetwork().getMatchmakingClient().isConnected()) {
                    new SelectGameMenu(getNetwork(), getGUIRoot(), MainMenu.this);
                } else {
                    Renderer.getRenderer().getNetwork().getMatchmakingClient().close();
                    new LoginForm(getNetwork(), getGUIRoot(), MainMenu.this);
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

        if (Renderer.getRenderer().getNetwork().getMatchmakingClient().isConnected()) {
            new SelectGameMenu(getNetwork(), getGUIRoot(), this);
        }
    }
}
