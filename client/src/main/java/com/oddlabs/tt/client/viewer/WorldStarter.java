package com.oddlabs.tt.client.viewer;

import com.oddlabs.matchmaking.GameSession;
import com.oddlabs.matchmaking.Participant;
import com.oddlabs.router.SessionID;
import com.oddlabs.tt.base.util.LoadCallback;
import com.oddlabs.tt.client.Peer;
import com.oddlabs.tt.client.screen.DefaultScreen;
import com.oddlabs.tt.client.screen.Screen;
import com.oddlabs.tt.client.screen.ScreenManager;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Triggers world loading, initializes players and matchmaking session information,
 * and sets up the active gameplay session once loading is complete.
 */
public final class WorldStarter implements LoadCallback<GUIRoot, Screen> {
    private final Peer peer;
    private final ScreenManager screenManager;
    private final List<UnitInfo> unit_infos;
    private final List<PlayerSlot> player_slots;
    private final short player_slot;
    private final InGameInfo ingame_info;
    private final WorldGenerator<Landscape> generator;
    private final WorldParameters world_params;
    private final @Nullable WorldInitAction initial_action;
    private final int session_id;

    public WorldStarter(int session_id, WorldGenerator<Landscape> generator,
            WorldParameters world_params,
            List<PlayerSlot> player_slots, List<UnitInfo> unit_infos, short player_slot, InGameInfo ingame_info,
            @Nullable WorldInitAction initial_action, Peer peer, ScreenManager screenManager) {
        this.peer = peer;
        this.screenManager = screenManager;
        this.initial_action = initial_action;
        this.session_id = session_id;
        this.world_params = world_params;
        this.generator = generator;
        this.unit_infos = unit_infos;
        this.player_slots = player_slots;
        this.player_slot = player_slot;
        this.ingame_info = ingame_info;
    }

    @Override
    public Screen load(GUIRoot gui_root) {
        peer.getFramePacer().freezeTime();
        List<Integer> activeIndices = IntStream.range(0, player_slots.size())
                .filter(i -> player_slots.get(i).getInfo() != null)
                .boxed()
                .toList();
        List<PlayerSlot> playerSlots = activeIndices.stream()
                .map(player_slots::get)
                .toList();
        List<UnitInfo> correctedUnitInfos = activeIndices.stream()
                .map(unit_infos::get)
                .toList();
        short corrected_player_slot = (short) activeIndices.indexOf((int) player_slot);
        assert corrected_player_slot != -1;
        WorldViewer viewer = new WorldViewer(gui_root, peer, world_params, ingame_info, generator, playerSlots,
                correctedUnitInfos, corrected_player_slot, new SessionID(session_id), screenManager);
        if (initial_action != null)
            initial_action.run(viewer);
        Participant[] participants = getParticipants(viewer, playerSlots);
        if (peer.getNetwork().getMatchmakingClient().isConnected()) {
            GameSession game_session = new GameSession(session_id, participants, ingame_info.isRated());
            peer.getNetwork().getMatchmakingClient().getInterface().gameStartedNotify(game_session);
        }
        return new DefaultScreen(gui_root, viewer.getRenderer());
    }

    private static Participant[] getParticipants(WorldViewer viewer,
            List<PlayerSlot> playerSlots) {
        List<Player> players = viewer.getWorld().getPlayers();
        return IntStream.range(0, players.size())
                .filter(i -> playerSlots.get(i).getType() == PlayerSlot.HUMAN)
                .mapToObj(i -> {
                    Player player = players.get(i);
                    int hostId = playerSlots.get(i).getAddress() != null
                            ? playerSlots.get(i).getAddress().getHostID() : -1;
                    return new Participant(hostId, player.getPlayerInfo().name(),
                            player.getPlayerInfo().team(), player.getPlayerInfo().race().getValue());
                })
                .toArray(Participant[]::new);
    }
}
