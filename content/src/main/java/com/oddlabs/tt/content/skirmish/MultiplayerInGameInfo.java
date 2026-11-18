package com.oddlabs.tt.content.skirmish;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.client.delegate.GameStatsDelegate;
import com.oddlabs.tt.client.viewer.MultiplayerGameInfo;
import com.oddlabs.tt.content.form.FreeQuitLabel;
import com.oddlabs.tt.content.menu.InGameMainMenu;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.client.viewer.WorldViewer;

/**
 * In-game session metadata for multiplayer matches backed by a matchmaking {@link Game}.
 */
public final class MultiplayerInGameInfo extends DefaultInGameInfo implements MultiplayerGameInfo {

    private final Game game;

    public MultiplayerInGameInfo(Game game) {
        super(game.getGamespeed());
        this.game = game;
    }

    @Override
    public Game getGame() {
        return game;
    }

    @Override
    public void addGUI(WorldViewer viewer, InGameMainMenu menu, Group game_infos) {
        super.addGUI(viewer, menu, game_infos);
        FreeQuitLabel free_quit_label = new FreeQuitLabel(viewer.getGUIRoot(), viewer.getWorld(), viewer
                .getAnimationManagerLocal());
        menu.addChild(free_quit_label);
        free_quit_label.setPos(0, 0);
    }

    @Override
    public void addGameOverGUI(WorldViewer viewer, GameStatsDelegate delegate, int header_y,
            Group buttons) {
        addGameOverGUI(viewer, delegate, header_y, buttons, false);
    }

    @Override
    public boolean isRated() {
        return game.isRated();
    }

    @Override
    public boolean isMultiplayer() {
        return true;
    }

    @Override
    public float getRandomStartPosition() {
        return game.getRandomStartPos();
    }
}
