package com.oddlabs.tt.client.trigger;

import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.client.viewer.WorldViewer;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Monitors the status of all players during gameplay to detect when the game
 * is over (e.g. all enemies defeated or the local player is defeated).
 */
public final class GameOverTrigger implements Animated {

    private final int[] teams;
    private final boolean[] dead_tribes;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(GameOverTrigger.class.getName());

    private String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final WorldViewer viewer;

    public GameOverTrigger(WorldViewer viewer) {
        this.viewer = viewer;
        viewer.getWorld().getAnimationManagerRealTime().registerAnimation(this);
        teams = new int[MatchmakingServerInterface.MAX_PLAYERS];
        dead_tribes = new boolean[viewer.getWorld().getPlayers().size()];
        Arrays.fill(dead_tribes, false);
    }

    @Override
    public void animate(float t) {
        List<Player> players = viewer.getWorld().getPlayers();
        Player local_player = viewer.getLocalPlayer();
        boolean enemy_alive = false;

        for (int i = 0; i < players.size(); i++) {
            Player current = players.get(i);
            if (!dead_tribes[i]) {
                if (!viewer.getPeerHub().isAlive(current)) {
                    if (current == local_player) {
                        doGameOver(countTeams(players));
                        return;
                    } else {
                        dead_tribes[i] = true;
                        String defeat_message = i18n("defeat_message", current.getPlayerInfo().getName());
                        viewer.getPeerHub().receiveChat(PeerHub.SYSTEM_NAME, defeat_message, false);
                    }
                } else if (local_player.isEnemy(current)) {
                    enemy_alive = true;
                }
            }
        }
        if (!enemy_alive) {
            doGameWon();
            return;
        }
        if (countTeams(players) < 2) {
            stop();
        }
    }

    private int countTeams(List<Player> players) {
        for (int i = 0; i < players.size(); i++) {
            teams[i] = 0;
        }

        for (Player current : players) {
            if (viewer.getPeerHub().isAlive(current) && current.getPlayerInfo().getTeam() != PlayerInfo.TEAM_NEUTRAL)
                teams[current.getPlayerInfo().getTeam()]++;
        }

        int team_count = 0;
        for (int team : teams) {
            if (team > 0)
                team_count++;
        }
        return team_count;
    }

    public void disable() {
        viewer.getWorld().getAnimationManagerRealTime().removeAnimation(this);
    }

    private void createDelayTrigger(String text) {
        new GameOverDelayTrigger(viewer, viewer.getDelegate().getCamera(), text);
    }

    private void doGameOver(int team_count) {
        viewer.getPeerHub().leaveGame();
        if (team_count < 2) {
            createDelayTrigger(i18n("you_defeated_game_over"));
        } else {
            createDelayTrigger(i18n("you_defeated"));
        }
        disable();
    }

    private void doGameWon() {
        viewer.getPeerHub().gameWon();
        createDelayTrigger(i18n("you_victorious"));
        disable();
    }

    private void stop() {
        createDelayTrigger(i18n("game_over"));
        disable();
    }
}
