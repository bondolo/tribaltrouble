package com.oddlabs.tt.net;

import com.oddlabs.router.SessionID;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.simulation.landscape.NotificationListener;
import com.oddlabs.tt.simulation.landscape.LandscapeBoundsProvider;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.LandscapeGeometry;
import com.oddlabs.tt.simulation.landscape.SpawnLocation;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.RaceData;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.player.PlayerInitializer;
import com.oddlabs.tt.simulation.player.PlayerInterface;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import com.oddlabs.tt.simulation.model.Difficulty;
import com.oddlabs.tt.simulation.player.AI;
import com.oddlabs.tt.simulation.player.AdvancedAI;
import com.oddlabs.tt.simulation.player.PassiveAI;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Isolated headless simulation instance managing a local world and peer network endpoint.
 */
public final class HeadlessSimulationInstance implements AutoCloseable {
    public static final Color.Linear[] DEFAULT_PLAYER_COLORS = Arrays.stream(new Color.Standard[]{
            new Color.Standard(0xFFFFBF00), /* Orange */
            new Color.Standard(0xFF007FFF), /* Royal Blue */
            new Color.Standard(0xFFFF0040), /* Red */
            new Color.Standard(0xFF00FFBF), /* Teal */
            new Color.Standard(0xFFBF00FF), /* Purple */
            new Color.Standard(0xFFBFFF00) /* Lime */
    }).map(Color.Linear::new).toArray(Color.Linear[]::new);

    private final int playerIndex;
    private final World world;
    private final Player localPlayer;
    private final @Nullable AI ai;
    private final AnimationManager animationManager;
    private final PeerHub peerHub;

    public HeadlessSimulationInstance(
            int playerIndex,
            World world,
            @Nullable AI ai,
            AnimationManager animationManager,
            PeerHub peerHub) {
        this.playerIndex = playerIndex;
        this.world = world;
        this.localPlayer = world.getPlayers().get(playerIndex);
        this.ai = ai;
        this.animationManager = animationManager;
        this.peerHub = peerHub;
    }

    /**
     * Creates and initializes a headless simulation instance for the specified player slot.
     */
    public static HeadlessSimulationInstance create(
            int playerIndex,
            WorldParameters worldParams,
            Landscape landscape,
            List<PlayerSlot> playerSlots,
            List<UnitInfo> unitInfos,
            com.oddlabs.net.NetworkSelector network,
            SessionID sessionId,
            @Nullable String routerAddress,
            int routerPort) {
        List<PlayerInfo> playerInfos = playerSlots.stream()
                .map(slot -> (PlayerInfo) slot.getInfo())
                .toList();

        NotificationListener noopListener = new NotificationListener() {
        };

        LandscapeBoundsProvider boundsProvider = new LandscapeGeometry();
        RaceData raceData = new RaceData();
        World world = World.newWorld(
                raceData,
                boundsProvider,
                noopListener,
                worldParams,
                landscape,
                playerInfos,
                DEFAULT_PLAYER_COLORS
        );

        initPlayers(
                landscape.startingLocations(),
                playerSlots,
                world.getPlayers(),
                unitInfos,
                worldParams.initialGameSpeed()
        );

        Player localPlayer = world.getPlayers().get(playerIndex);
        AnimationManager animManager = new AnimationManager();
        StallHandler stallHandler = new StallHandler() {
            @Override
            public void stopStall() {
            }

            @Override
            public void processStall(int tick) {
            }

            @Override
            public void peerhubFailed() {
            }
        };

        PeerHub peerHub = new PeerHub(
                animManager,
                true,
                false,
                localPlayer,
                playerSlots,
                network,
                null,
                null,
                null,
                world.getDistributableTable(),
                sessionId,
                stallHandler,
                routerAddress,
                routerPort
        );

        PlayerSlot localSlot = playerSlots.get(playerIndex);
        if (!localSlot.isNetworkPeer()) {
            throw new IllegalArgumentException(
                    "Headless simulation instances require a network peer slot (got non-peer slot for player "
                            + playerIndex + ")");
        }

        AI ai = null;
        if (localSlot.getType() == PlayerSlot.AI) {
            ai = switch (localSlot.getAIDifficulty()) {
                case PlayerSlot.AI_NORMAL -> new AdvancedAI(localPlayer, peerHub.getPlayerInterface(),
                        Difficulty.NORMAL);
                case PlayerSlot.AI_HARD -> new AdvancedAI(localPlayer, peerHub.getPlayerInterface(), Difficulty.HARD);
                case PlayerSlot.AI_EASY -> new AdvancedAI(localPlayer, peerHub.getPlayerInterface(), Difficulty.EASY);
                case PlayerSlot.AI_BATTLE_TUTORIAL, PlayerSlot.AI_PASSIVE_CAMPAIGN ->
                    new PassiveAI(localPlayer, peerHub.getPlayerInterface(), true);
                case PlayerSlot.AI_NEUTRAL_CAMPAIGN ->
                    new PassiveAI(localPlayer, peerHub.getPlayerInterface(), false);
                case PlayerSlot.AI_TOWER_TUTORIAL, PlayerSlot.AI_CHIEFTAIN_TUTORIAL -> null;
                default -> throw new IllegalArgumentException("unexpected difficulty: " + localSlot.getAIDifficulty());
            };
            if (ai != null) {
                localPlayer.setAI(ai);
            }
        }

        for (int k = 0; k < playerSlots.size(); k++) {
            if (k == playerIndex) {
                continue;
            }
            PlayerSlot slotK = playerSlots.get(k);
            if (!slotK.isNetworkPeer() && slotK.getType() == PlayerSlot.AI) {
                Player otherPlayer = world.getPlayers().get(k);
                AI lockstepAi = switch (slotK.getAIDifficulty()) {
                    case PlayerSlot.AI_NORMAL -> new AdvancedAI(otherPlayer, otherPlayer, Difficulty.NORMAL);
                    case PlayerSlot.AI_HARD -> new AdvancedAI(otherPlayer, otherPlayer, Difficulty.HARD);
                    case PlayerSlot.AI_EASY -> new AdvancedAI(otherPlayer, otherPlayer, Difficulty.EASY);
                    case PlayerSlot.AI_BATTLE_TUTORIAL, PlayerSlot.AI_PASSIVE_CAMPAIGN ->
                        new PassiveAI(otherPlayer, otherPlayer, true);
                    case PlayerSlot.AI_NEUTRAL_CAMPAIGN ->
                        new PassiveAI(otherPlayer, otherPlayer, false);
                    case PlayerSlot.AI_TOWER_TUTORIAL, PlayerSlot.AI_CHIEFTAIN_TUTORIAL -> null;
                    default -> throw new IllegalArgumentException("unexpected difficulty: " + slotK.getAIDifficulty());
                };
                if (lockstepAi != null) {
                    otherPlayer.setAI(lockstepAi);
                }
            }
        }

        return new HeadlessSimulationInstance(playerIndex, world, ai, animManager, peerHub);
    }

    /**
     * Initializes initial unit armies and starting speed for players.
     */
    public static void initPlayers(
            List<List<SpawnLocation>> startingLocations,
            List<PlayerSlot> playerSlots,
            List<Player> players,
            List<UnitInfo> unitInfos,
            int initialGamespeed) {
        for (short i = 0; i < players.size(); i++) {
            PlayerInitializer.initPlayer(
                    players.get(i),
                    playerSlots.get(i),
                    unitInfos.get(i),
                    startingLocations.get(i),
                    initialGamespeed,
                    null
            );
        }
    }

    public int getPlayerIndex() {
        return playerIndex;
    }

    public World getWorld() {
        return world;
    }

    public Player getLocalPlayer() {
        return localPlayer;
    }

    public @Nullable AI getAI() {
        return ai;
    }

    public PeerHub getPeerHub() {
        return peerHub;
    }

    public PlayerInterface getPlayerInterface() {
        return peerHub.getPlayerInterface();
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public int getTick() {
        return world.getTick();
    }

    public int getChecksum() {
        return world.getChecksum();
    }

    public boolean isSynchronized() {
        return peerHub.isSynchronized();
    }

    public boolean isAlive() {
        return peerHub.isAlive(localPlayer);
    }

    public void animate(float dt) {
        if (PeerHub.CURRENT.isBound() && PeerHub.CURRENT.get() == peerHub) {
            animationManager.runAnimations(dt);
        } else {
            ScopedValue.where(PeerHub.CURRENT, peerHub).run(() -> animationManager.runAnimations(dt));
        }
    }

    /**
     * Executes an arbitrary action within the scope of this instance's active PeerHub.
     */
    public void run(Runnable action) {
        ScopedValue.where(PeerHub.CURRENT, peerHub).run(action);
    }

    @Override
    public void close() {
        peerHub.close();
    }
}
