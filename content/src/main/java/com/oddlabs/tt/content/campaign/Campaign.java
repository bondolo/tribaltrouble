package com.oddlabs.tt.content.campaign;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.gui.MessageForm;
import com.oddlabs.tt.client.gui.CampaignIcons;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.client.trigger.GameOverDelayTrigger;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.util.DeterministicSerializerLoopbackInterface;

import java.util.ResourceBundle;

/**
 * Base class managing campaign progression, state, and victory or defeat flow across islands.
 */
public abstract class Campaign {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(Campaign.class.getName());

    private static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final CampaignState state;
    private final AudioManager audioManager;
    private CampaignState[] campaign_states; // for saving

    public Campaign(CampaignState state, AudioManager audioManager) {
        this.state = state;
        this.audioManager = audioManager;
    }

    public final CampaignState getState() {
        return state;
    }

    public final AudioManager getAudioManager() {
        return audioManager;
    }

    public final void pushDelegate(NetworkSelector network, GUI gui) {
        final GUIRoot gui_root = gui.newFade(null, gui.getRenderer());
        gui_root.pushDelegate(new CampaignMapForm(network, gui_root, Campaign.this));
    }

    public void defeated(WorldViewer viewer, String game_over_message) {
        new GameOverDelayTrigger(viewer, viewer.getDelegate().getCamera(), game_over_message);
        doDefeated();
    }

    public final void doDefeated() {
        state.setCurrentIsland(state.getPrevIsland());
    }

    public final void victory(final WorldViewer viewer) {
        new GameOverDelayTrigger(viewer, viewer.getDelegate().getCamera(), i18n("island_complete"));
        LoadCampaignBox.loadSavegames(
                new DeterministicSerializerLoopbackInterface<CampaignState[]>() {
                    @Override
                    public void loadSucceeded(CampaignState[] campaign_states) {
                        Campaign.this.campaign_states = campaign_states;
                        doSave(viewer);
                    }

                    @Override
                    public void failed(Throwable e) {
                        doFailed(e, viewer);
                    }
                });
    }

    private void doSave(final WorldViewer viewer) {
        for (int i = 0; i < campaign_states.length; i++) {
            if (campaign_states[i].getName().equals(getState().getName())) {
                campaign_states[i] = getState();
            }
        }
        LoadCampaignBox.saveSavegames(campaign_states,
                (DeterministicSerializerLoopbackInterface<CampaignState[]>) e -> doFailed(e, viewer));
    }

    private void doFailed(Throwable e, WorldViewer viewer) {
        String failed_message = i18n("failed_message", LoadCampaignBox.SAVEGAMES_FILE_NAME, e.getMessage());
        viewer.getGUIRoot().addModalForm(new MessageForm(failed_message));
    }

    public abstract CampaignIcons getIcons();

    public abstract void islandChosen(NetworkSelector network, GUIRoot gui_root, int number);

    public abstract CharSequence getCurrentObjective();

    public abstract void startIsland(NetworkSelector network, GUIRoot gui_root, int number);
}
