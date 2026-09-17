package com.oddlabs.tt.net;

import com.oddlabs.router.SessionID;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.simulation.landscape.NotificationListener;
import com.oddlabs.tt.simulation.landscape.LandscapeBoundsProvider;
import com.oddlabs.tt.simulation.landscape.LandscapeData;
import com.oddlabs.tt.simulation.landscape.LandscapeGeometry;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.RaceData;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.tt.simulation.player.PlayerInterface;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import com.oddlabs.tt.simulation.model.Difficulty;
import com.oddlabs.tt.simulation.player.AI;
import com.oddlabs.tt.simulation.player.AdvancedAI;
import com.oddlabs.tt.simulation.player.PassiveAI;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
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
    private final AI ai;
    private final AnimationManager animationManager;
    private final PeerHub peerHub;

    public HeadlessSimulationInstance(
            int playerIndex,
            World world,
            AI ai,
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
            LandscapeData landscapeData,
            PlayerSlot[] playerSlots,
            UnitInfo[] unitInfos,
            com.oddlabs.net.NetworkSelector network,
            SessionID sessionId,
            @Nullable String routerAddress,
            int routerPort,
            boolean allowAiPeers) {
        List<PlayerInfo> playerInfos = new ArrayList<>();
        for (PlayerSlot slot : playerSlots) {
            if (slot.getInfo() instanceof PlayerInfo info) {
                playerInfos.add(info);
            }
        }

        NotificationListener noopListener = new NotificationListener() {
        };

        LandscapeBoundsProvider boundsProvider = new LandscapeGeometry();
        RaceData raceData = new RaceData();
        World world = World.newWorld(
                raceData,
                boundsProvider,
                noopListener,
                worldParams,
                landscapeData,
                playerInfos,
                DEFAULT_PLAYER_COLORS,
                false
        );

        initPlayers(
                landscapeData.startingLocations(),
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
                routerPort,
                allowAiPeers
        );

        PlayerSlot localSlot = playerSlots[playerIndex];
        if (localSlot.getType() != PlayerSlot.AI) {
            throw new IllegalArgumentException(
                    "Headless simulation instances require PlayerSlot.AI for the local slot (got "
                            + localSlot.getType() + " for player " + playerIndex + ")");
        }

        AI ai = switch (localSlot.getAIDifficulty()) {
            case PlayerSlot.AI_NORMAL -> new AdvancedAI(localPlayer, peerHub.getPlayerInterface(), null,
                    Difficulty.NORMAL);
            case PlayerSlot.AI_HARD -> new AdvancedAI(localPlayer, peerHub.getPlayerInterface(), null,
                    Difficulty.HARD);
            case PlayerSlot.AI_EASY -> new AdvancedAI(localPlayer, peerHub.getPlayerInterface(), null,
                    Difficulty.EASY);
            case PlayerSlot.AI_BATTLE_TUTORIAL, PlayerSlot.AI_PASSIVE_CAMPAIGN ->
                new PassiveAI(localPlayer, peerHub.getPlayerInterface(), null, true);
            case PlayerSlot.AI_NEUTRAL_CAMPAIGN ->
                new PassiveAI(localPlayer, peerHub.getPlayerInterface(), null, false);
            default -> throw new IllegalArgumentException("unexpected difficulty: " + localSlot.getAIDifficulty());
        };
        localPlayer.setAI(ai);
        animManager.registerAnimation(ai);

        return new HeadlessSimulationInstance(playerIndex, world, ai, animManager, peerHub);
    }

    /**
     * Initializes initial unit armies and starting speed for players.
     */
    public static void initPlayers(
            float[][] startingLocations,
            PlayerSlot[] playerSlots,
            List<Player> players,
            UnitInfo[] unitInfos,
            int initialGamespeed) {
        for (short i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            UnitInfo unitInfo = unitInfos[i];
            float[] startingLocation = startingLocations[i];

            player.setPreferredGamespeed(initialGamespeed);
            var raceInfo = player.getRaceInfo();
            if (raceInfo == null) {
                continue;
            }

            int idx = 0;
            for (int j = 0; j < unitInfo.numPeons(); j++, idx++) {
                new Unit(player, startingLocation[2 * idx], startingLocation[2 * idx + 1], null,
                        raceInfo.getUnitTemplate(UnitType.PEON));
            }
            for (int j = 0; j < unitInfo.numRockWarriors(); j++, idx++) {
                new Unit(player, startingLocation[2 * idx], startingLocation[2 * idx + 1], null,
                        raceInfo.getUnitTemplate(UnitType.WARRIOR_ROCK));
            }
            for (int j = 0; j < unitInfo.numIronWarriors(); j++, idx++) {
                new Unit(player, startingLocation[2 * idx], startingLocation[2 * idx + 1], null,
                        raceInfo.getUnitTemplate(UnitType.WARRIOR_IRON));
            }
            for (int j = 0; j < unitInfo.numRubberWarriors(); j++, idx++) {
                new Unit(player, startingLocation[2 * idx], startingLocation[2 * idx + 1], null,
                        raceInfo.getUnitTemplate(UnitType.WARRIOR_RUBBER));
            }
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

    public AI getAI() {
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
        ScopedValue.where(PeerHub.CURRENT, peerHub).run(() -> animationManager.runAnimations(dt));
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
