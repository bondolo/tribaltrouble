package com.oddlabs.tt.client.viewer;

import com.oddlabs.matchmaking.GameSession;
import com.oddlabs.matchmaking.Participant;
import com.oddlabs.router.SessionID;
import com.oddlabs.tt.base.util.LoadCallback;
import com.oddlabs.tt.engine.ClientEngine;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.render.UIRenderer;
import com.oddlabs.tt.procedural.GeneratedLandscapeData;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Triggers world loading, initializes players and matchmaking session information,
 * and sets up the active gameplay session once loading is complete.
 */
public final class WorldStarter implements LoadCallback<GUIRoot, UIRenderer> {
    private final ClientEngine engine;
    private final UnitInfo[] unit_infos;
    private final PlayerSlot[] player_slots;
    private final short player_slot;
    private final InGameInfo ingame_info;
    private final WorldGenerator<GeneratedLandscapeData> generator;
    private final WorldParameters world_params;
    private final @Nullable WorldInitAction initial_action;
    private final int session_id;

    public WorldStarter(int session_id, WorldGenerator<GeneratedLandscapeData> generator,
            WorldParameters world_params,
            PlayerSlot[] player_slots, UnitInfo[] unit_infos, short player_slot, InGameInfo ingame_info,
            @Nullable WorldInitAction initial_action, ClientEngine engine) {
        this.engine = engine;
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
    public UIRenderer load(GUIRoot gui_root) {
        engine.getFramePacer().freezeTime();
        List<PlayerSlot> player_slot_list = new ArrayList<>();
        List<UnitInfo> unit_info_list = new ArrayList<>();
        short corrected_player_slot = -1;
        for (short i = 0; i < player_slots.length; i++) {
            if (player_slots[i].getInfo() != null) {
                if (player_slot == i)
                    corrected_player_slot = (short) player_slot_list.size();
                player_slot_list.add(player_slots[i]);
                unit_info_list.add(unit_infos[i]);
            }
        }
        assert corrected_player_slot != -1;
        PlayerSlot[] player_slots = player_slot_list.toArray(new PlayerSlot[0]);
        UnitInfo[] corrected_unit_infos = unit_info_list.toArray(new UnitInfo[0]);
        WorldViewer viewer = new WorldViewer(gui_root, engine, world_params, ingame_info, generator, player_slots,
                corrected_unit_infos, corrected_player_slot, new SessionID(session_id));
        if (initial_action != null)
            initial_action.run(viewer);
        Participant[] participants = getParticipants(viewer, player_slots);
        if (engine.getNetwork().getMatchmakingClient().isConnected()) {
            GameSession game_session = new GameSession(session_id, participants, ingame_info.isRated());
            engine.getNetwork().getMatchmakingClient().getInterface().gameStartedNotify(game_session);
        }
        System.out.println("PeerHub created (session_id = " + session_id + ") Player list:");
        return viewer.getRenderer();
    }

    private static Participant[] getParticipants(WorldViewer viewer,
            PlayerSlot[] player_slots) {
        List<Participant> participant_list = new ArrayList<>();
        List<Player> players = viewer.getWorld().getPlayers();
        for (short i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player_slots[i].getType() != PlayerSlot.HUMAN)
                continue;
            int host_id;
            if (player_slots[i].getAddress() != null)
                host_id = player_slots[i].getAddress().getHostID();
            else
                host_id = -1;
            Participant p = new Participant(host_id, player.getPlayerInfo().getName(), player.getPlayerInfo().getTeam(),
                    player.getPlayerInfo().getRace().getValue());
            participant_list.add(p);
        }
        Participant[] participants = new Participant[participant_list.size()];
        participant_list.toArray(participants);
        return participants;
    }
}
