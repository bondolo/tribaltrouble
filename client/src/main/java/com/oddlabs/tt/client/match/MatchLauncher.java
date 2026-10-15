package com.oddlabs.tt.client.match;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.client.Peer;
import com.oddlabs.tt.client.resource.AssetRegistry;
import com.oddlabs.tt.client.screen.ScreenManager;
import com.oddlabs.tt.client.trigger.GameOverTrigger;
import com.oddlabs.tt.client.viewer.InGameInfo;
import com.oddlabs.tt.client.viewer.WorldInitAction;
import com.oddlabs.tt.client.viewer.WorldStarter;
import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.base.util.LoadCallback;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.MessageForm;
import com.oddlabs.tt.net.Client;
import com.oddlabs.tt.net.ConfigurationListener;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.LoadCallbackFactory;
import com.oddlabs.tt.net.Server;
import com.oddlabs.tt.procedural.landscape.IslandGenerator;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.player.DefaultPlayerSlotHandler;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.Nullable;

import java.net.InetAddress;
import java.util.List;

/**
 * Orchestrates game startup, network server and client creation, and world session bootstrapping.
 */
public final class MatchLauncher {

    private MatchLauncher() {
    }

    /**
     * Completes post-initialization delegate wiring and background audio for a running world.
     *
     * @param worldViewer the world viewer instance being initialized
     */
    public static void completeGameSetup(WorldViewer worldViewer) {
        worldViewer.getGUIRoot().pushDelegate(worldViewer.getDelegate());
        worldViewer.getAudioManager().setMusic(AssetRegistry.getInstance().getMusic(worldViewer.getLocalPlayer()
                .getPlayerInfo().race()), 10f);
    }

    /**
     * Default world initialization action attaching standard game-over triggers and setting up presentation.
     */
    public static final class DefaultWorldInitAction implements WorldInitAction {
        @Override
        public void run(WorldViewer viewer) {
            new GameOverTrigger(viewer);
            completeGameSetup(viewer);
        }
    }

    /**
     * Connects to a remote or existing multiplayer match and displays the connecting progress form.
     *
     * @param peer the active client peer
     * @param guiRoot the root GUI container
     * @param screenManager the active screen manager
     * @param hostId the remote host ID
     * @param gameSpeed the game simulation speed
     * @param mapCode the terrain map code
     * @param owner the parent lobby form
     * @param inGameInfo match metadata
     * @param maxUnitCount maximum unit population limit
     * @return the initialized game network coordinator
     */
    public static GameNetwork joinGame(
            Peer peer,
            GUIRoot guiRoot,
            ScreenManager screenManager,
            int hostId,
            int gameSpeed,
            String mapCode,
            MultiplayerLobby owner,
            InGameInfo inGameInfo,
            int maxUnitCount) {
        WorldParameters worldParams = new WorldParameters(gameSpeed, mapCode, Player.INITIAL_UNIT_COUNT, maxUnitCount);
        var matchmakingClient = peer.getNetwork().getMatchmakingClient();
        var chatHub = peer.getNetwork().getChatHub();
        var networkSelector = peer.getNetwork().getSelector();
        @SuppressWarnings("unchecked") LoadCallbackFactory starterFactory = (session_id,
                generator,
                player_slots,
                unit_infos, player_slot) -> new WorldStarter(session_id,
                        (WorldGenerator<Landscape>) generator, worldParams, player_slots, unit_infos,
                        player_slot, inGameInfo,
                        new DefaultWorldInitAction(), peer, screenManager);
        Client client = new Client(null, networkSelector, matchmakingClient, chatHub,
                hostId,
                starterFactory,
                new DefaultPlayerSlotHandler());
        GameNetwork gameNetwork = new GameNetwork(null, client);
        ConnectingForm connectingForm = new ConnectingForm(gameNetwork, guiRoot, owner, screenManager, peer
                .getAudioManager());
        client.setConfigurationListener(connectingForm);
        guiRoot.addModalForm(connectingForm);
        return gameNetwork;
    }

    /**
     * Starts a new single-player or multiplayer match, instantiating simulation server, client, and world loaders.
     *
     * @param peer the active client peer
     * @param guiRoot the root GUI container
     * @param screenManager the active screen manager
     * @param owner the parent multiplayer lobby if applicable
     * @param worldParams world simulation parameters
     * @param inGameInfo match metadata
     * @param initAction initial trigger and post-load hook
     * @param game existing matchmaking game registration if applicable
     * @param islandConfig procedural island parameters
     * @param aiNames bot player display names
     * @return the initialized game network coordinator
     */
    public static GameNetwork startNewGame(
            Peer peer,
            GUIRoot guiRoot,
            ScreenManager screenManager,
            @Nullable MultiplayerLobby owner,
            WorldParameters worldParams,
            InGameInfo inGameInfo,
            WorldInitAction initAction,
            @Nullable Game game,
            IslandConfig islandConfig,
            String[] aiNames) {
        boolean multiplayer = inGameInfo.isMultiplayer();
        WorldGenerator<Landscape> generator = new IslandGenerator(islandConfig);
        InetAddress address = multiplayer ? null : Utils.getLoopbackAddress();
        var matchmakingClient = peer.getNetwork().getMatchmakingClient();
        var chatHub = peer.getNetwork().getChatHub();
        var networkSelector = peer.getNetwork().getSelector();
        final long deterministicSeed = peer.getEventQueue().getHighPrecisionManager().getTick();
        final Server server = new Server(networkSelector, matchmakingClient, game,
                address, generator, multiplayer, aiNames,
                (team, raceVal, name) -> new PlayerInfo(team, Race.fromValue(raceVal), name),
                new DefaultPlayerSlotHandler(), deterministicSeed);
        @SuppressWarnings("unchecked") LoadCallbackFactory starterFactory = (session_id,
                gen,
                player_slots, unit_infos,
                player_slot) -> new WorldStarter(
                        session_id, (WorldGenerator<Landscape>) gen,
                        worldParams, player_slots, unit_infos, player_slot, inGameInfo, initAction, peer,
                        screenManager);
        Client client = new Client(server::close, networkSelector, matchmakingClient,
                chatHub, -1, starterFactory, new DefaultPlayerSlotHandler());
        GameNetwork gameNetwork = new GameNetwork(server, client);
        if (multiplayer) {
            ConnectingForm connectingForm = new ConnectingForm(gameNetwork, guiRoot, owner, screenManager, peer
                    .getAudioManager());
            client.setConfigurationListener(connectingForm);
            guiRoot.addModalForm(connectingForm);
        } else {
            client.setConfigurationListener(new ConfigurationListener() {
                @Override
                public void connected(Client client, Game game,
                        WorldGenerator<?> generator, int player_slot) {
                    assert player_slot == 0 : "player_slot must be 0";
                }

                @Override
                public void setPlayers(List<PlayerSlot> players) {
                }

                @Override
                public void connectionLost() {
                    guiRoot.addModalForm(new MessageForm(guiRoot, ConnectingForm.i18n("connection_lost")));
                }

                @Override
                public void gameStarted(LoadCallback<?, ?> loadCallback) {
                    ProgressForm.Mode mode = inGameInfo.showLoadingHints()
                            ? ProgressForm.Mode.GAME_LOAD
                            : ProgressForm.Mode.TUTORIAL;
                    ProgressForm.setProgressForm(gameNetwork.getClient().getNetwork(), screenManager,
                            peer.getAudioManager(), loadCallback, mode);
                }
            });
        }
        return gameNetwork;
    }
}
