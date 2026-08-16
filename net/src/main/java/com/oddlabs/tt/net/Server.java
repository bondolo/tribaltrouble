package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.GameSession;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.matchmaking.Profile;
import com.oddlabs.matchmaking.TunnelAddress;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.AbstractConnectionListener;
import com.oddlabs.net.ConnectionListener;
import com.oddlabs.net.ConnectionListenerInterface;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.PlayerSlotHandler;
import java.util.stream.IntStream;

/** Network server host managing client connections and slot allocation. */
public final class Server implements ConnectionListenerInterface {
    private static final int NEGOTIATING = 1;
    private static final int SYNCHRONIZING = 2;
    private static final int CLOSED = 3;

    private final PlayerSlot @NonNull [] players;
    private final String[] ai_names;
    private final @NonNull WorldGenerator generator;
    private final Game game;
    private final @NonNull AbstractConnectionListener local_listener;
    private final Map<AbstractConnection, ClientConnection> connection_to_client = new LinkedHashMap<>();
    private final @NonNull Random random;
    private final @Nullable MatchmakingClient matchmaking_client;
    private AbstractConnectionListener tunnelled_listener;

    private int state = NEGOTIATING;
    private final boolean register_server;

    private @Nullable PlayerInfoFactory player_info_factory;
    private final @NonNull PlayerSlotHandler slot_handler;

    public Server(@NonNull NetworkSelector network, @Nullable MatchmakingClient matchmaking_client, Game game,
            InetAddress ip, @NonNull WorldGenerator generator,
            boolean register_server, String[] ai_names, @NonNull PlayerSlotHandler slot_handler) {
        this(network, matchmaking_client, game, ip, generator, register_server, ai_names, null, slot_handler);
    }

    public Server(@NonNull NetworkSelector network, @Nullable MatchmakingClient matchmaking_client, Game game,
            InetAddress ip, @NonNull WorldGenerator generator,
            boolean register_server, String[] ai_names, @Nullable PlayerInfoFactory player_info_factory,
            @NonNull PlayerSlotHandler slot_handler) {
        this.slot_handler = slot_handler;
        this.player_info_factory = player_info_factory;
        this.local_listener = new ConnectionListener(network, ip, NetConfig.DEFAULT_NET_PORT, this);
        this.matchmaking_client = matchmaking_client;
        this.game = game;
        this.generator = generator;
        this.register_server = register_server;
        this.ai_names = ai_names;
        this.random = new Random(System.currentTimeMillis());
        players = IntStream.rangeClosed(0, MatchmakingServerInterface.MAX_PLAYERS)
                .mapToObj(slot_handler::createSlot)
                .map(PlayerSlot.class::cast)
                .toArray(PlayerSlot[]::new);
    }

    private @NonNull Iterator<ClientConnection> getClientIterator() {
        return connection_to_client.values().iterator();
    }

    private int getNumClients() {
        return connection_to_client.size();
    }

    private ClientConnection getClientFromConnection(AbstractConnection conn) {
        return connection_to_client.get(conn);
    }

    private void unregisterGame() {
        local_listener.close();
        if (tunnelled_listener != null)
            tunnelled_listener.close();
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
        int count = 0;
        Iterator<ClientConnection> it = getClientIterator();
        while (it.hasNext()) {
            ClientConnection client = it.next();
            if (slot_handler.isReady(client.getClient().getPlayerSlot()))
                count++;
        }
        return count;
    }

    @Override
    public void error(AbstractConnectionListener listener, IOException e) {
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

    private void disconnectClient(@NonNull ClientConnection client) {
        assert client != null;
        client.getConnection().close();
        connection_to_client.remove(client.getConnection());
    }

    private @Nullable ClientConnection locateClientForSlot(Serializable player_slot) {
        Iterator<ClientConnection> it = getClientIterator();
        while (it.hasNext()) {
            ClientConnection client = it.next();
            if (client.getClient().getPlayerSlot() == player_slot)
                return client;
        }
        return null;
    }

    public void resetSlotState(@NonNull Serializable client_slot, int slot, boolean open) {
        if (!canControlSlot(client_slot, slot))
            return;
        resetSlotState(players[slot], open);
    }

    private void resetSlotState(@NonNull Serializable client_slot, boolean open) {
        slot_handler.resetSlot(client_slot, open);
        ClientConnection player_client = locateClientForSlot(client_slot);
        if (player_client != null)
            disconnectClient(player_client);
        broadcastPlayers(true);
    }

    private boolean canControlSlot(@NonNull Serializable client_slot, int slot) {
        int client_slot_idx = slot_handler.getSlotIndex(client_slot);
        return slot >= 0 && slot < players.length && state == NEGOTIATING &&
                ((client_slot_idx == 0 || client_slot_idx == slot));
    }

    public void startServer(@NonNull Serializable slot) {
        if (!canControlSlot(slot, 0) || getNumReady() != getNumClients())
            return;
        state = SYNCHRONIZING;
        unregisterGame();
        broadcastInits();
    }

    public void setPlayerSlot(@NonNull Serializable client_slot, int slot, int type, int race, int team, boolean ready,
            int ai_difficulty) {
        if (!slot_handler.isValidType(type))
            return;
        int client_slot_idx = slot_handler.getSlotIndex(client_slot);
        if (!canControlSlot(client_slot, slot) || (client_slot_idx == slot && type != slot_handler.getHumanType()))
            return;
        Serializable player_slot = players[slot];
        ClientConnection player_client = locateClientForSlot(player_slot);
        if (player_client != null && type != slot_handler.getHumanType())
            disconnectClient(player_client);
        String name;
        if (type == slot_handler.getAiType()) {
            name = ai_names[slot];
        } else {
            Serializable info = slot_handler.getInfo(player_slot);
            name = info != null ? info.toString() : "Player";
        }
        Serializable player_info = player_info_factory != null ? player_info_factory.createInfo(team, race, name)
                : slot_handler.getInfo(player_slot);
        boolean reset_ready = slot_handler.getInfo(player_slot) == null || type != slot_handler.getType(player_slot)
                || ai_difficulty
                        != slot_handler.getAIDifficulty(player_slot) || (player_info != null && !player_info.equals(
                                slot_handler.getInfo(player_slot)));
        slot_handler.updateSlot(player_slot, type, ai_difficulty, player_info, ready);
        broadcastPlayers(reset_ready);
    }

    private void resetReady() {
        int num_humans = 0;
        for (Serializable player_slot : players) {
            if (slot_handler.getType(player_slot) == slot_handler.getHumanType())
                num_humans++;
        }
        if (num_humans > 1) {
            for (Serializable player_slot : players) {
                if (slot_handler.getType(player_slot) == slot_handler.getHumanType())
                    slot_handler.updateSlot(player_slot, slot_handler.getType(player_slot), slot_handler
                            .getAIDifficulty(player_slot), slot_handler.getInfo(player_slot), false);
            }
        }
    }

    private void broadcastPlayers(boolean reset_ready) {
        if (reset_ready)
            resetReady();
        Iterator<ClientConnection> it = getClientIterator();
        while (it.hasNext()) {
            ClientConnection client = it.next();
            client.getClientInterface().setPlayers(players);
        }
    }

    public void chat(@NonNull Serializable player_slot, String chat) {
        Iterator<ClientConnection> it = getClientIterator();
        while (it.hasNext()) {
            ClientConnection client = it.next();
            client.getClientInterface().chat(slot_handler.getSlotIndex(player_slot), chat);
        }
    }

    private void broadcastInits() {
        Iterator<ClientConnection> it = getClientIterator();
        while (it.hasNext()) {
            ClientConnection client = it.next();
            int session_id = random.nextInt();
            client.getClientInterface().startGame(session_id);
        }
    }

    private short locateAvailableSlot() {
        for (short i = 0; i < players.length; i++) {
            if (slot_handler.getType(players[i]) == slot_handler.getOpenType())
                return i;
        }
        return (short) -1;
    }

    @Override
    public void incomingConnection(@NonNull AbstractConnectionListener connection_listener, Object remote_address) {
        IO.println("Incoming host connection from " + remote_address);
        short available_slot = locateAvailableSlot();
        if (state != NEGOTIATING || available_slot == -1 ||
                (remote_address instanceof InetAddress address && !address.isLoopbackAddress()) ||
                (remote_address instanceof TunnelIdentifier identifier && game != null && game.isRated() &&
                        identifier.profile().getWins() < GameSession.MIN_WINS_FOR_RANKING)) {
            IO.println("rejecting incoming connection since state = " + state + " | locateAvailableSlot() = "
                    + available_slot + " remote_address = " + remote_address);
            connection_listener.rejectConnection();
            return;
        }
        Serializable player_slot = players[available_slot];
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
            } else
                name = Utils.getBundleString(ResourceBundle.getBundle(MatchmakingClient.class.getName()), "player");
        } else {
            TunnelIdentifier tunnel_id = (TunnelIdentifier) remote_address;
            name = tunnel_id.profile().getNick();
            rating = tunnel_id.profile().getRating();
            address = tunnel_id.address();
        }
        int max_teams = game != null && game.isRated() ? 2 : MatchmakingServerInterface.MAX_PLAYERS;
        Serializable player_info = player_info_factory != null ? player_info_factory.createInfo(available_slot
                % max_teams, random.nextInt(2), name) : null;
        slot_handler.updateSlot(player_slot, slot_handler.getHumanType(), slot_handler.getAiNone(), player_info, false);
        ClientInfo client = new ClientInfo(this, player_slot);
        AbstractConnection conn = connection_listener.acceptConnection(client);
        ClientConnection client_conn = new ClientConnection(conn, client);
        connection_to_client.put(conn, client_conn);
        client_conn.getClientInterface().setWorldGeneratorAndPlayerSlot(game, generator, available_slot);
        broadcastPlayers(true);
    }
}
