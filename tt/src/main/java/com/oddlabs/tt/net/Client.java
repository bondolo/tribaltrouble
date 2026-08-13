package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.ARMIEventBroker;
import com.oddlabs.net.ARMIInterfaceMethods;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.Connection;
import com.oddlabs.net.ConnectionInterface;
import com.oddlabs.net.IllegalARMIEventException;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.core.global.Globals;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;

import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import com.oddlabs.tt.core.util.LoadCallback;
import com.oddlabs.tt.engine.resource.WorldGenerator;

/** Network client endpoint managing connection negotiation and game startup. */
public final class Client implements ARMIEventBroker, GameClientInterface, ConnectionInterface {
    private static final int CONNECTING = 1;
    private static final int NEGOTIATING = 2;
    private static final int CLOSED = 5;

    private final @NonNull AbstractConnection connection;

    private final ARMIInterfaceMethods interface_methods = new ARMIInterfaceMethods(GameClientInterface.class);
    private final Serializable world_params;
    private final @NonNull GameServerInterface gameserver_interface;
    private final UnitInfo @NonNull [] unit_infos;
    private final @Nullable LoadCallbackFactory starter_factory;
    private final @NonNull PlayerSlotHandler slot_handler;
    private final @Nullable ChatHub chat_hub;
    private final @NonNull NetworkSelector network;
    private final Runnable cleanup_action;
    private int state = CONNECTING;
    private int session_id;

    private @Nullable WorldGenerator generator = null;

    private PlayerSlot[] player_slots;
    private short player_slot = -1;
    private boolean error_while_fading;
    private ConfigurationListener configuration_listener;

    public Client(Runnable cleanup_action, @NonNull NetworkSelector network,
            @Nullable MatchmakingClient matchmaking_client, @Nullable ChatHub chat_hub, int host_id,
            Serializable world_params, @Nullable LoadCallbackFactory starter_factory,
            @NonNull PlayerSlotHandler slot_handler) {
        this.slot_handler = slot_handler;
        this.cleanup_action = cleanup_action;
        this.network = network;
        this.chat_hub = chat_hub;
        this.world_params = world_params;
        this.starter_factory = starter_factory;
        if (host_id != -1)
            this.connection = new TunnelledConnection(matchmaking_client, host_id, this);
        else
            this.connection = new Connection(network, new InetSocketAddress(Utils.getLoopbackAddress(),
                    Globals.NET_PORT), this);
        gameserver_interface = (GameServerInterface) ARMIEvent.createProxy(connection, GameServerInterface.class);

        this.unit_infos = new UnitInfo[MatchmakingServerInterface.MAX_PLAYERS];
        for (int i = 0; i < unit_infos.length; i++) {
            unit_infos[i] = new UnitInfo(false, false, 0, false, Player.INITIAL_UNIT_COUNT, 0, 0, 0);
        }
    }

    private ConfigurationListener getConfigurationListener() {
        return configuration_listener;
    }

    public void setConfigurationListener(ConfigurationListener listener) {
        configuration_listener = listener;
    }

    public void setUnitInfo(int slot, @NonNull UnitInfo unit_info) {
        this.unit_infos[slot] = unit_info;
    }

    public @NonNull GameServerInterface getServerInterface() {
        return gameserver_interface;
    }

    @Override
    public void chat(int player_slot, @Nullable String chat) {
        if (chat != null && player_slot >= 0 && player_slot < player_slots.length && chat_hub != null) {
            String name = slot_handler.getPlayerName(player_slots[player_slot]);
            chat_hub.chat(new ChatMessage(name, chat, ChatMessage.Type.GAME_MENU));
        }
    }

    @Override
    public void setWorldGeneratorAndPlayerSlot(@NonNull Game game, @NonNull WorldGenerator generator,
            short player_slot) {
        if (state != CONNECTING)
            return;
        state = NEGOTIATING;
        this.generator = generator;
        this.player_slot = player_slot;
        getConfigurationListener().connected(this, game, generator, player_slot);
    }

    @Override
    public void writeBufferDrained(AbstractConnection conn) {
    }

    public void close() {
        connection.close();
        state = CLOSED;
        if (cleanup_action != null)
            cleanup_action.run();
    }

    public PlayerSlot[] getPlayers() {
        return player_slots;
    }

    public @NonNull NetworkSelector getNetwork() {
        return network;
    }

    @Override
    public void startGame(int session_id) {
        if (state != NEGOTIATING)
            return;
        close();
        this.session_id = session_id;
        LoadCallback<?, ?> starter = starter_factory != null
                ? starter_factory.createCallback(session_id, generator, player_slots, unit_infos, player_slot)
                : null;
        if (starter != null)
            getConfigurationListener().gameStarted(starter);
    }

    @Override
    public void setPlayers(PlayerSlot @NonNull [] player_slots) {
        this.player_slots = player_slots;
        getConfigurationListener().setPlayers(player_slots);
    }

    @Override
    public void handle(Object sender, @NonNull ARMIEvent armi_event) {
        try {
            armi_event.execute(interface_methods, this);
        } catch (IllegalARMIEventException _) {
            error();
        }
    }

    @Override
    public void connected(AbstractConnection conn) {
    }

    @Override
    public void error(AbstractConnection conn, IOException e) {
        error();
    }

    private void error() {
        getConfigurationListener().connectionLost();
        close();
    }
}
