package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.GameSession;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.matchmaking.Profile;
import com.oddlabs.matchmaking.TunnelAddress;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.ConnectionListener;
import com.oddlabs.net.ConnectionListenerInterface;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.net.SocketConnectionListener;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.player.AIDifficulty;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.SlotType;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

/// Network server host managing client connections and slot allocation.
public final class Server implements ConnectionListenerInterface<Object> {
  private static final int NEGOTIATING = 1;
  private static final int SYNCHRONIZING = 2;
  private static final int CLOSED = 3;

  private final PlayerSlot[] players;
  private final String[] ai_names;
  private final WorldGenerator<?> generator;
  private final @Nullable Game game;
  private final ConnectionListener local_listener;
  private final Map<AbstractConnection, ClientConnection> connection_to_client = new LinkedHashMap<>();
  private final Random random;
  private final @Nullable MatchmakingClient matchmaking_client;
  private @Nullable ConnectionListener tunnelled_listener;

  private int state = NEGOTIATING;
  private final boolean register_server;

  public Server(
      NetworkSelector network,
      @Nullable MatchmakingClient matchmaking_client,
      @Nullable Game game,
      InetAddress ip,
      WorldGenerator<?> generator,
      boolean register_server,
      String[] ai_names,
      long deterministicSeed) {
    this.local_listener =
        new SocketConnectionListener(network, new InetSocketAddress(ip, NetConfig.DEFAULT_NET_PORT), this);
    this.matchmaking_client = matchmaking_client;
    this.game = game;
    this.generator = generator;
    this.register_server = register_server;
    this.ai_names = ai_names;
    this.random = new Random(deterministicSeed);
    players =
        IntStream.range(0, MatchmakingServerInterface.MAX_PLAYERS)
            .mapToObj(i -> {
              PlayerSlot slot = new PlayerSlot(i);
              slot.setReady(i != 0);
              return slot;
            })
            .toArray(PlayerSlot[]::new);
  }

  private int getNumClients() {
    return connection_to_client.size();
  }

  private @Nullable ClientConnection getClientFromConnection(AbstractConnection conn) {
    return connection_to_client.get(conn);
  }

  private void unregisterGame() {
    local_listener.close();
    if (tunnelled_listener != null) tunnelled_listener.close();
    if (register_server && matchmaking_client != null && matchmaking_client.isConnected()) {
      matchmaking_client.getInterface().unregisterGame();
    }
  }

  private void unregister() {
    state = CLOSED;
  }

  private void closeConnections() {
    for (AbstractConnection conn : connection_to_client.keySet()) {
      conn.close();
    }
    connection_to_client.clear();
    unregister();
  }

  public void close() {
    unregisterGame();
    closeConnections();
  }

  private int getNumReady() {
    return (int)
        connection_to_client.values().stream().filter(client -> client.getClient().getPlayerSlot().isReady()).count();
  }

  @Override
  public void error(ConnectionListener listener, IOException e) {
    IO.println("Listener failed: " + e);
    close();
  }

  public void handleError(AbstractConnection conn, Exception e) {
    IO.println("Disconnecting client because of exception: " + e);
    ClientConnection client = getClientFromConnection(conn);
    if (client != null) {
      disconnectClient(client);
      if (state == NEGOTIATING) {
        resetSlotState(client.getClient().getPlayerSlot(), true);
      }
    }
  }

  private void disconnectClient(ClientConnection client) {
    assert client != null;
    client.getConnection().close();
    connection_to_client.remove(client.getConnection());
  }

  private @Nullable ClientConnection locateClientForSlot(PlayerSlot player_slot) {
    return connection_to_client.values().stream()
        .filter(client -> client.getClient().getPlayerSlot() == player_slot)
        .findFirst()
        .orElse(null);
  }

  public void resetSlotState(PlayerSlot client_slot, int slot, boolean open) {
    if (!canControlSlot(client_slot, slot)) return;
    resetSlotState(players[slot], open);
  }

  private void resetSlotState(PlayerSlot client_slot, boolean open) {
    client_slot.setType(open ? SlotType.OPEN : SlotType.CLOSED);
    client_slot.setInfo(null);
    client_slot.setAddress(null);
    client_slot.setReady(true);
    client_slot.setAIDifficulty(AIDifficulty.NONE);
    ClientConnection player_client = locateClientForSlot(client_slot);
    if (player_client != null) disconnectClient(player_client);
    broadcastPlayers(true);
  }

  private boolean canControlSlot(PlayerSlot client_slot, int slot) {
    int client_slot_idx = client_slot.getSlot();
    return slot >= 0
        && slot < players.length
        && state == NEGOTIATING
        && ((client_slot_idx == 0 || client_slot_idx == slot));
  }

  public void startServer(PlayerSlot slot) {
    if (!canControlSlot(slot, 0) || getNumReady() != getNumClients()) return;
    state = SYNCHRONIZING;
    unregisterGame();
    broadcastInits();
  }

  public void setPlayerSlot(
      PlayerSlot client_slot,
      int slot,
      SlotType slotType,
      Race race,
      int team,
      boolean ready,
      AIDifficulty aiDifficulty) {
    if (slot < 0 || slot >= players.length || !PlayerSlot.isValidType(slotType)) return;
    int client_slot_idx = client_slot.getSlot();
    if (!canControlSlot(client_slot, slot) || (client_slot_idx == slot && slotType != SlotType.HUMAN)) return;
    PlayerSlot player_slot = players[slot];
    ClientConnection player_client = locateClientForSlot(player_slot);
    if (player_client != null && slotType != SlotType.HUMAN) disconnectClient(player_client);
    String name;
    if (slotType == SlotType.AI) {
      name = ai_names[slot];
    } else {
      PlayerInfo info = player_slot.getInfo();
      name = info != null ? info.name() : "Player";
    }
    PlayerInfo player_info = new PlayerInfo(team, race, name);
    boolean reset_ready =
        player_slot.getInfo() == null
            || slotType != player_slot.getType()
            || aiDifficulty != player_slot.getAIDifficulty()
            || (!player_info.equals(player_slot.getInfo()));
    player_slot.setType(slotType);
    player_slot.setAIDifficulty(aiDifficulty);
    player_slot.setInfo(player_info);
    player_slot.setReady(slotType != SlotType.HUMAN || ready);
    broadcastPlayers(reset_ready);
  }

  private void resetReady() {
    int num_humans = 0;
    for (PlayerSlot player_slot : players) {
      if (player_slot.getType() == SlotType.HUMAN) num_humans++;
    }
    if (num_humans > 1) {
      for (PlayerSlot player_slot : players) {
        if (player_slot.getType() == SlotType.HUMAN) player_slot.setReady(false);
      }
    }
  }

  private void broadcastPlayers(boolean reset_ready) {
    if (reset_ready) resetReady();
    connection_to_client.values().forEach(client -> client.getClientInterface().setPlayers(players));
  }

  public void chat(PlayerSlot player_slot, String chat) {
    connection_to_client.values().forEach(client -> client.getClientInterface().chat(player_slot.getSlot(), chat));
  }

  private void broadcastInits() {
    connection_to_client
        .values()
        .forEach(client -> {
          int session_id = random.nextInt();
          client.getClientInterface().startGame(session_id);
        });
  }

  private short locateAvailableSlot() {
    return (short)
        IntStream.range(0, players.length).filter(i -> players[i].getType() == SlotType.OPEN).findFirst().orElse(-1);
  }

  @Override
  public void incomingConnection(ConnectionListener connection_listener, Object remote_address) {
    IO.println("Incoming host connection from " + remote_address);
    var available_slot = locateAvailableSlot();
    if (state != NEGOTIATING
        || available_slot == -1
        || (remote_address instanceof InetAddress address && !address.isLoopbackAddress())
        || (remote_address instanceof TunnelIdentifier identifier
            && game != null
            && game.isRated()
            && identifier.profile().getWins() < GameSession.MIN_WINS_FOR_RANKING)) {
      IO.println(
          "rejecting incoming connection since state = "
              + state
              + " | locateAvailableSlot() = "
              + available_slot
              + " remote_address = "
              + remote_address);
      connection_listener.rejectConnection();
      return;
    }
    PlayerSlot player_slot = players[available_slot];
    int rating = 0;
    String name;
    TunnelAddress address;
    if (remote_address instanceof InetAddress) {
      address = matchmaking_client != null ? matchmaking_client.getLocalAddress() : null;
      if (register_server && matchmaking_client != null) {
        tunnelled_listener = new TunnelledConnectionListener(matchmaking_client, this);
        matchmaking_client.getInterface().registerGame(game);
      }
      Profile profile = matchmaking_client != null ? matchmaking_client.getProfile() : null;
      if (profile != null) {
        name = profile.getNick();
        rating = profile.getRating();
      } else name = Utils.getBundleString(ResourceBundle.getBundle(MatchmakingClient.class.getName()), "player");
    } else {
      TunnelIdentifier tunnel_id = (TunnelIdentifier) remote_address;
      name = tunnel_id.profile().getNick();
      rating = tunnel_id.profile().getRating();
      address = tunnel_id.address();
    }
    int max_teams = game != null && game.isRated() ? 2 : MatchmakingServerInterface.MAX_PLAYERS;
    PlayerInfo player_info = new PlayerInfo(available_slot % max_teams, Race.fromValue(random.nextInt(2)), name);
    player_slot.setType(SlotType.HUMAN);
    player_slot.setAIDifficulty(AIDifficulty.NONE);
    player_slot.setInfo(player_info);
    player_slot.setReady(false);
    ClientInfo client = new ClientInfo(this, player_slot);
    AbstractConnection conn = connection_listener.acceptConnection(client);
    ClientConnection client_conn = new ClientConnection(conn, client);
    connection_to_client.put(conn, client_conn);
    client_conn.getClientInterface().setWorldGeneratorAndPlayerSlot(game, generator, available_slot);
    broadcastPlayers(true);
  }
}
