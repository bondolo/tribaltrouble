package com.oddlabs.tt.content.campaign;

import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.client.trigger.GameOverDelayTrigger;
import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.client.Peer;
import com.oddlabs.tt.client.screen.ScreenManager;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.MessageForm;
import com.oddlabs.tt.base.util.Utils;
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
    private final Peer peer;
    private final ScreenManager screenManager;

    public Campaign(CampaignState state, Peer peer, ScreenManager screenManager) {
        this.state = state;
        this.peer = peer;
        this.screenManager = screenManager;
        this.audioManager = peer.getAudioManager();
    }

    public final CampaignState getState() {
        return state;
    }

    final AudioManager getAudioManager() {
        return audioManager;
    }

    final Peer getPeer() {
        return peer;
    }

    final ScreenManager getScreenManager() {
        return screenManager;
    }

    final void pushDelegate() {
        final GUIRoot gui_root = screenManager.createRoot();
        screenManager.fadeToRoot(gui_root);
        gui_root.pushDelegate(new CampaignMapForm(gui_root, Campaign.this));
    }

    public void defeated(WorldViewer viewer, String game_over_message) {
        new GameOverDelayTrigger(viewer, viewer.getDelegate().getCamera(), game_over_message);
        doDefeated();
    }

    final void doDefeated() {
        state.setCurrentIsland(state.getPrevIsland());
    }

    public final void victory(final WorldViewer viewer) {
        new GameOverDelayTrigger(viewer, viewer.getDelegate().getCamera(), i18n("island_complete"));
        LoadCampaignBox.loadSavegames(viewer.getPeer(),
                new DeterministicSerializerLoopbackInterface<CampaignState[]>() {
                    @Override
                    public void loadSucceeded(CampaignState[] campaign_states) {
                        doSave(viewer, campaign_states);
                    }

                    @Override
                    public void failed(Throwable e) {
                        doFailed(e, viewer);
                    }
                });
    }

    private void doSave(final WorldViewer viewer, CampaignState[] campaign_states) {
        updateCampaigns(campaign_states, getState());

        LoadCampaignBox.saveSavegames(viewer.getPeer(), campaign_states,
                (DeterministicSerializerLoopbackInterface<CampaignState[]>) e -> doFailed(e, viewer));
    }

    private boolean updateCampaigns(CampaignState[] campaigns, CampaignState campaign) {
        for (int i = 0; i < campaigns.length; i++) {
            if (campaigns[i].getName().equals(campaign.getName())) {
                campaigns[i] = campaign;
                return true;
            }
        }
        return false;
    }

    private void doFailed(Throwable e, WorldViewer viewer) {
        String failed_message = i18n("failed_message", LoadCampaignBox.SAVEGAMES_FILE_NAME, e.getMessage());
        viewer.getGUIRoot().addModalForm(new MessageForm(viewer.getGUIRoot(), failed_message));
    }

    public abstract CampaignIcons getIcons();

    protected abstract void islandChosen(GUIRoot gui_root, int number);

    protected abstract CharSequence getCurrentObjective();

    protected abstract void startIsland(GUIRoot gui_root, int number);
}
