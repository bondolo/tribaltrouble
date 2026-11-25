package com.oddlabs.tt.client.match;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.base.util.LoadCallback;
import com.oddlabs.tt.client.Peer;
import com.oddlabs.tt.client.resource.AudioRegistry;
import com.oddlabs.tt.client.screen.ScreenManager;
import com.oddlabs.tt.client.trigger.GameOverTrigger;
import com.oddlabs.tt.client.viewer.InGameInfo;
import com.oddlabs.tt.client.viewer.MultiplayerGameInfo;
import com.oddlabs.tt.client.viewer.WorldInitAction;
import com.oddlabs.tt.client.viewer.WorldStarter;
import com.oddlabs.tt.client.viewer.WorldViewer;
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
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.util.Utils;
import java.net.InetAddress;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Orchestrates game startup, network server and client creation, and world session bootstrapping.
public final class MatchLauncher {

  private MatchLauncher() {}

  public static void completeGameSetup(WorldViewer worldViewer) {
    worldViewer.getGUIRoot().pushDelegate(worldViewer.getDelegate());
    worldViewer
        .getAudioManager()
        .setMusic(AudioRegistry.getMusic(worldViewer.getLocalPlayer().getPlayerInfo().race()), 10f);
  }

  /// Default world initialization action attaching standard game-over triggers and setting up presentation.
  public static final class DefaultWorldInitAction implements WorldInitAction {
    @Override
    public void run(WorldViewer viewer) {
      new GameOverTrigger(viewer);
      completeGameSetup(viewer);
    }
  }

  public static GameNetwork joinGame(
      Peer peer,
      GUIRoot guiRoot,
      ScreenManager screenManager,
      int hostId,
      MultiplayerLobby owner,
      MultiplayerGameInfo inGameInfo) {
    Game game = inGameInfo.getGame();
    WorldParameters worldParams =
        new WorldParameters(game.getMapcode(), Player.INITIAL_UNIT_COUNT, game.getMaxUnitCount());
    var matchmakingClient = peer.getNetwork().getMatchmakingClient();
    var chatHub = peer.getNetwork().getChatHub();
    var networkSelector = peer.getNetwork().getSelector();
    @SuppressWarnings("unchecked")
    LoadCallbackFactory starterFactory =
        (session_id, generator, player_slots, unit_infos, player_slot) ->
            new WorldStarter(
                session_id,
                (WorldGenerator<Landscape>) generator,
                worldParams,
                player_slots,
                unit_infos,
                player_slot,
                inGameInfo,
                new DefaultWorldInitAction(),
                peer,
                screenManager);
    Client client = new Client(null, networkSelector, matchmakingClient, chatHub, hostId, starterFactory);
    GameNetwork gameNetwork = new GameNetwork(null, client);
    ConnectingForm connectingForm =
        new ConnectingForm(gameNetwork, guiRoot, owner, screenManager, peer.getAudioManager());
    client.setConfigurationListener(connectingForm);
    guiRoot.addModalForm(connectingForm);
    return gameNetwork;
  }

  public static GameNetwork startNewGame(
      Peer peer,
      GUIRoot guiRoot,
      ScreenManager screenManager,
      @Nullable MultiplayerLobby owner,
      WorldParameters worldParams,
      InGameInfo inGameInfo,
      WorldInitAction initAction,
      IslandConfig islandConfig,
      String[] aiNames) {
    boolean multiplayer = inGameInfo.isMultiplayer();
    Game game = inGameInfo instanceof MultiplayerGameInfo mpInfo ? mpInfo.getGame() : null;
    WorldGenerator<Landscape> generator = new IslandGenerator(islandConfig);
    InetAddress address = multiplayer ? null : Utils.getLoopbackAddress();
    var matchmakingClient = peer.getNetwork().getMatchmakingClient();
    var chatHub = peer.getNetwork().getChatHub();
    var networkSelector = peer.getNetwork().getSelector();
    final long deterministicSeed = peer.getEventQueue().getHighPrecisionManager().getTick();
    final Server server =
        new Server(
            networkSelector, matchmakingClient, game, address, generator, multiplayer, aiNames, deterministicSeed);
    @SuppressWarnings("unchecked")
    LoadCallbackFactory starterFactory =
        (session_id, gen, player_slots, unit_infos, player_slot) ->
            new WorldStarter(
                session_id,
                (WorldGenerator<Landscape>) gen,
                worldParams,
                player_slots,
                unit_infos,
                player_slot,
                inGameInfo,
                initAction,
                peer,
                screenManager);
    Client client = new Client(server::close, networkSelector, matchmakingClient, chatHub, -1, starterFactory);
    GameNetwork gameNetwork = new GameNetwork(server, client);
    if (multiplayer) {
      ConnectingForm connectingForm =
          new ConnectingForm(gameNetwork, guiRoot, owner, screenManager, peer.getAudioManager());
      client.setConfigurationListener(connectingForm);
      guiRoot.addModalForm(connectingForm);
    } else {
      client.setConfigurationListener(
          new ConfigurationListener() {
            @Override
            public void connected(Client client, Game game, WorldGenerator<?> generator, int player_slot) {
              assert player_slot == 0 : "player_slot must be 0";
            }

            @Override
            public void setPlayers(List<PlayerSlot> players) {}

            @Override
            public void connectionLost() {
              guiRoot.addModalForm(new MessageForm(guiRoot, ConnectingForm.i18n("connection_lost")));
            }

            @Override
            public void gameStarted(LoadCallback<?, ?> loadCallback) {
              ProgressForm.Mode mode =
                  inGameInfo.showLoadingHints() ? ProgressForm.Mode.GAME_LOAD : ProgressForm.Mode.TUTORIAL;
              ProgressForm.setProgressForm(
                  gameNetwork.getClient().getNetwork(), screenManager, peer.getAudioManager(), loadCallback, mode);
            }
          });
    }
    return gameNetwork;
  }
}
