package com.oddlabs.tt.net;

import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.ARMIEventWriter;
import com.oddlabs.net.ARMIInterfaceMethods;
import com.oddlabs.net.IllegalARMIEventException;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.router.Router;
import com.oddlabs.router.SessionID;
import com.oddlabs.router.SessionInfo;
import com.oddlabs.tt.core.animation.Animated;
import com.oddlabs.tt.core.animation.AnimationManager;
import com.oddlabs.tt.core.animation.SimulationClock;
import com.oddlabs.tt.core.global.Globals;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInterface;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.core.event.StateChecksum;
import com.oddlabs.tt.core.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Manages networking connections, peers, message routing, and synchronization
 * between players in a multiplayer session.
 */
public final class PeerHub implements Animated, RouterHandler {
    private static final String ROUTER_ADDRESS = "127.0.0.1";
    public static final ResourceBundle bundle = ResourceBundle.getBundle(PeerHub.class.getName());

    private static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public static final @NonNull String SYSTEM_NAME = i18n("system_name");

    private static final int MILLISECONDS_PER_HEARTBEAT = 60;
    private static final int CLIENT_MAX_DELAY_MILLIS = 60;
    private static final float FREE_QUIT_TIME = 120f;
    private static final int TICKS_PER_STATUS_UPDATE = (int) (20 / AnimationManager.ANIMATION_SECONDS_PER_TICK);
    private static final int TICKS_PER_CHECKSUM = (int) (10 / AnimationManager.ANIMATION_SECONDS_PER_TICK);

    private static boolean waiting_for_ack = false;

    private final ARMIInterfaceMethods interface_methods = new ARMIInterfaceMethods(PeerHubInterface.class);
    private final StateChecksum checksum = new StateChecksum();
    private final @NonNull PeerHubInterface peerhubs_interface;
    private final @NonNull PlayerInterface player_interface;
    private final int num_participants;
    private final Peer @NonNull [] peer_index_to_peer;
    private final Map<Player, Peer> player_to_peer = new LinkedHashMap<>();
    private final Map<Peer, Player> peer_to_player = new LinkedHashMap<>();
    private final Set<Player> nonhuman_players = new HashSet<>();
    private final @NonNull NetworkSelector network;
    private final @NonNull RouterClient router_client;
    private final @Nullable Router router;
    private final @Nullable BeaconListener beacon_listener;
    private final @Nullable MatchmakingClient matchmaking_client;
    private final @Nullable ChatHub chat_hub;
    private final @NonNull Player local_player;
    private final @NonNull AnimationManager manager;
    private final boolean is_multiplayer;
    private final boolean is_rated;
    private final StallHandler stall_handler;
    private @Nullable Predicate<String> ignore_filter;

    private int pause_ticks;
    private int server_millis;
    private int paused;
    private boolean is_synchronized;

    public PeerHub(@NonNull AnimationManager manager, boolean is_multiplayer, boolean is_rated,
            @NonNull Player local_player, PlayerSlot[] player_slots, @NonNull NetworkSelector network,
            @Nullable BeaconListener beacon_listener, @Nullable MatchmakingClient matchmaking_client,
            @Nullable ChatHub chat_hub, DistributableTable distributable_table, SessionID session_id,
            StallHandler stall_handler) {
        this.stall_handler = stall_handler;
        this.is_rated = is_rated;
        this.local_player = local_player;
        this.network = network;
        if (matchmaking_client != null) {
            matchmaking_client.setGameWonAckListener(PeerHub::receivedAck);
        }
        this.beacon_listener = beacon_listener;
        this.matchmaking_client = matchmaking_client;
        this.chat_hub = chat_hub;
        this.is_multiplayer = is_multiplayer;
        this.manager = manager;

        GameArgumentReader argument_reader = new GameArgumentReader(distributable_table);
        List<Peer> peer_index_to_peer_list = new ArrayList<>();
        List<@NonNull Player> players = local_player.getWorld().getPlayers();
        int local_peer_index = -1;
        if (!is_multiplayer) {
            this.router = new Router(network, com.oddlabs.util.Utils.getLoopbackAddress(), 0, Logger
                    .getAnonymousLogger(), (IOException e) -> {
                        throw new IllegalStateException("Local router failed", e);
                    });
            this.router_client = new RouterClient(network, this, router.getPort());
        } else {
            this.router = null;
            this.router_client = new RouterClient(network, ROUTER_ADDRESS, this);
        }
        for (short i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player_slots[i].getType() != PlayerSlot.HUMAN) {
                nonhuman_players.add(player);
                continue;
            }
            System.out.println("index " + i + " contains player " + player);
            final int peer_index = peer_index_to_peer_list.size();
            ARMIEventWriter router_handler = (ARMIEvent event) -> router_client.getInterface().relayEventTo(peer_index,
                    event);
            PeerHubInterface peer_interface = (PeerHubInterface) ARMIEvent.createProxy(router_handler,
                    PeerHubInterface.class);
            Peer peer = new Peer(this, peer_index, player, argument_reader, peer_interface);
            if (player == local_player) {
                local_peer_index = peer_index;
            }
            peer_index_to_peer_list.add(peer);
            peer_to_player.put(peer, player);
            player_to_peer.put(player, peer);
        }
        this.peer_index_to_peer = new Peer[peer_index_to_peer_list.size()];
        peer_index_to_peer_list.toArray(peer_index_to_peer);
        this.num_participants = peer_index_to_peer.length;
        ARMIEventWriter game_router_handler = router_client.getInterface()::relayGameStateEvent;
        this.player_interface = (PlayerInterface) ARMIEvent.createProxy(game_router_handler, new GameArgumentWriter(
                distributable_table), PlayerInterface.class);
        ARMIEventWriter hub_router_handler = router_client.getInterface()::relayEvent;
        this.peerhubs_interface = (PeerHubInterface) ARMIEvent.createProxy(hub_router_handler, PeerHubInterface.class);
        manager.registerAnimation(this);
        router_client.connect(session_id, new SessionInfo(num_participants, MILLISECONDS_PER_HEARTBEAT),
                local_peer_index);
    }

    @Override
    public void routerFailed(Exception e) {
        System.out.println("Router failed with exception: " + e);
        closeNetwork();
        stall_handler.peerhubFailed();
    }

    @Override
    public void heartbeat(int millis) {
        if (millis < server_millis) {
            routerFailed(new IOException("Invalid time received: " + millis + " (tick currently at " + getTick()
                    + ")"));
            return;
        }
        server_millis = millis;
    }

    @Override
    public void receiveEvent(int client_id, @NonNull ARMIEvent event) {
        Peer peer = getPeerFromClientID(client_id);
        if (peer == null) {
            routerFailed(new IOException("Invalid client_id received: " + client_id));
            return;
        }
        try {
            event.execute(interface_methods, peer);
        } catch (IllegalARMIEventException e) {
            peerDisconnected(peer, e.getMessage());
        }
    }

    @Override
    public void receiveGameStateEvent(int client_id, int millis, ARMIEvent event) {
        if (millis < server_millis) {
            routerFailed(new IOException("Invalid time received for event: " + millis + " (tick currently at "
                    + getTick() + ")"));
            return;
        }
        Peer peer = getPeerFromClientID(client_id);
        if (peer == null) {
            routerFailed(new IOException("Invalid client_id received: " + client_id));
            return;
        }
        server_millis = millis;
        peer.addEvent(millisToTickCeil(millis), event);
    }

    @Override
    public void playerDisconnected(int client_id, boolean checksum_error) {
        Peer peer = getPeerFromClientID(client_id);
        if (checksum_error)
            peerChecksumError(peer);
        else
            peerDisconnected(peer, "Peer disconnected");
    }

    private void peerChecksumError(@NonNull Peer peer) {
        System.out.println("Disconnecting peer because of checksum mismatch: " + peer.getPlayerInfo().getName());
        peerDisconnected(peer, "Checksum error");
        Globals.checksum_error_in_last_game = true;
    }

    private @Nullable Peer getPeerFromClientID(int client_id) {
        if (client_id >= 0 && client_id < peer_index_to_peer.length)
            return unsafeGetPeerFromClientID(client_id);
        else
            return null;
    }

    private Peer unsafeGetPeerFromClientID(int client_id) {
        return peer_index_to_peer[client_id];
    }

    @Override
    public void start() {
        is_synchronized = true;
    }

    private Peer locatePeerFromPlayer(Player player) {
        return player_to_peer.get(player);
    }

    private boolean isDisconnected(Peer peer) {
        return getPlayerFromPeer(peer) == null;
    }

    private Player getPlayerFromPeer(Peer peer) {
        return peer_to_player.get(peer);
    }

    public boolean isAlive(@NonNull Player player) {
        return (nonhuman_players.contains(player) || locatePeerFromPlayer(player) != null) && player.isAlive();
    }

    public @NonNull PlayerInterface getPlayerInterface() {
        return player_interface;
    }

    private int millisToTickCeil(int millis) {
        return millisToTick(millis + (int) AnimationManager.ANIMATION_MILLISECONDS_PER_TICK - 1);
    }

    private int millisToTick(int millis) {
        return (int) (millis / AnimationManager.ANIMATION_MILLISECONDS_PER_TICK) - pause_ticks;
    }

    @Override
    public void animate(float t) {
        if (router != null)
            router.process();
        int server_tick = millisToTick(server_millis);
        if (!is_synchronized || getTick() == server_tick) {
            processStall();
        } else {
            if (!isPaused()) {
                doTick(t);
                int min_tick = millisToTick(server_millis - MILLISECONDS_PER_HEARTBEAT - CLIENT_MAX_DELAY_MILLIS);
                while (getTick() < min_tick)
                    doTick(t);
            } else
                pause_ticks++;
        }
    }

    private int getTick() {
        return local_player.getWorld().getTick();
    }

    private void doTick(float t) {
        stall_handler.stopStall();
        if (getFreeQuitTicksLeft(local_player.getWorld()) == 0 && matchmaking_client != null && matchmaking_client
                .isConnected())
            matchmaking_client.getInterface().freeQuitStopNotify();

        if (getTick() % TICKS_PER_STATUS_UPDATE == 0 && matchmaking_client != null && matchmaking_client.isConnected())
            sendStatusUpdate();

        for (Peer peer : peer_index_to_peer) {
            if (peer != null) {
                try {
                    peer.executeEvents(getTick());
                } catch (IllegalARMIEventException e) {
                    peerDisconnected(peer, e.getMessage());
                }
            }
        }
        local_player.getWorld().tick(t);
        if (getTick() % TICKS_PER_CHECKSUM == 0)
            sendChecksum();
    }

    private void sendChecksum() {
        checksum.update(getTick());
        checksum.update(local_player.getWorld().getChecksum());
        local_player.getWorld().getAnimationManagerGameTime().updateChecksum(checksum);
        local_player.getWorld().getAnimationManagerRealTime().updateChecksum(checksum);
        router_client.getInterface().checksum(checksum.getValue());
    }

    public void setPaused(boolean p) {
        if (!is_multiplayer && p)
            paused++;
        else if (!is_multiplayer && !p)
            paused--;
    }

    public boolean isPaused() {
        return paused > 0;
    }

    private void sendStatusUpdate() {
        int[] status = new int[num_participants];
        for (int i = 0; i < status.length; i++) {
            Peer peer = getPeerFromClientID(i);
            if (peer != null)
                status[i] = peer.getPlayer().getStatus();
        }
        if (matchmaking_client != null)
            matchmaking_client.getInterface().updateGameStatus(getTick(), status);
    }

    private @NonNull Iterator<Peer> getPeerIterator() {
        return peer_to_player.keySet().iterator();
    }

    public @NonNull PeerHubInterface getInterface() {
        return peerhubs_interface;
    }

    private void processStall() {
        stall_handler.processStall(getTick());
    }

    private void removePeerFromActiveList(@NonNull Peer peer) {
        System.out.println("Removing from active list:" + peer);
        peer_index_to_peer[peer.getPeerIndex()] = null;
        peer.getPlayer().setPreferredGamespeed(SimulationClock.GAMESPEED_DONTCARE);
    }

    @Override
    public void updateChecksum(@NonNull StateChecksum sum) {
        sum.update(getTick());
        sum.update(checksum.getValue());
    }

    public void peerDisconnected(@NonNull Peer peer, String reason) {
        Player player = getPlayerFromPeer(peer);
        if (player == null)
            return;
        peer_to_player.remove(peer);
        player_to_peer.remove(player);
        int peer_index = peer.getPeerIndex();
        removePeerFromActiveList(peer);
        String left_game_message = i18n("left_game", peer.getPlayerInfo().getName(), reason);
        receiveChat(SYSTEM_NAME, left_game_message, false);
        if (getFreeQuitTicksLeft(local_player.getWorld()) >= 0 && matchmaking_client != null && matchmaking_client
                .isConnected())
            matchmaking_client.getInterface().gameQuitNotify(peer.getPlayerInfo().getName());
    }

    public void sendChat(String text, boolean team_only) {
        Iterator<Peer> it = getPeerIterator();
        int local_team = local_player.getPlayerInfo().getTeam();
        while (it.hasNext()) {
            Peer peer = it.next();
            int peer_team = peer.getPlayerInfo().getTeam();
            if (!team_only || local_team == peer_team)
                peer.getPeerHubInterface().chat(text, team_only);
        }
    }

    public void sendBeacon(float x, float y) {
        Iterator<Peer> it = getPeerIterator();
        int local_team = local_player.getPlayerInfo().getTeam();
        while (it.hasNext()) {
            Peer peer = it.next();
            int peer_team = peer.getPlayerInfo().getTeam();
            if (local_team == peer_team)
                peer.getPeerHubInterface().beacon(x, y);
        }
    }

    public void receiveChat(@NonNull String name, @NonNull String text, boolean team) {
        if (chat_hub != null) {
            if (team)
                chat_hub.chat(new ChatMessage(name, text, ChatMessage.Type.TEAM));
            else
                chat_hub.chat(new ChatMessage(name, text, ChatMessage.Type.NORMAL));
        }
    }

    public void setIgnoreFilter(@Nullable Predicate<String> ignore_filter) {
        this.ignore_filter = ignore_filter;
    }

    public void receiveBeacon(float x, float y, @NonNull String owner) {
        if ((ignore_filter == null || !ignore_filter.test(owner)) && beacon_listener != null)
            beacon_listener.newBeacon(manager, local_player, x, y);
    }

    private void closeNetwork() {
        router_client.close();
        if (router != null)
            router.close();
        Object[] peers = peer_to_player.keySet().toArray();
        for (Object peer1 : peers) {
            Peer peer = (Peer) peer1;
            if (peer.getPlayer() != local_player) {
                peerDisconnected(peer, "Synchronization failed");
            }
        }
        stall_handler.stopStall();
        leaveGame();
    }

    public void close() {
        closeNetwork();
        manager.removeAnimation(this);
        System.out.println("PeerHub closed");
    }

    private static int getFreeQuitTicksLeft(@NonNull SimulationClock clock) {
        return (int) (FREE_QUIT_TIME / clock.getSecondsPerTick()) - clock.getTick();
    }

    public static float getFreeQuitTimeLeft(@NonNull SimulationClock clock) {
        int left = getFreeQuitTicksLeft(clock);
        return left * AnimationManager.ANIMATION_SECONDS_PER_TICK;
    }

    public void leaveGame() {
        if (matchmaking_client != null && matchmaking_client.isConnected()) {
            if (getFreeQuitTicksLeft(local_player.getWorld()) >= 0) {
                matchmaking_client.getInterface().gameQuitNotify(local_player.getPlayerInfo().getName());
            } else {
                matchmaking_client.getInterface().gameLostNotify();
            }
        }
    }

    public void gameWon() {
        if (matchmaking_client != null && matchmaking_client.isConnected()) {
            matchmaking_client.getInterface().gameWonNotify();
            waiting_for_ack = true;
        }
    }

    public static void receivedAck() {
        waiting_for_ack = false;
    }

    public static boolean isWaitingForAck() {
        return waiting_for_ack;
    }
}
