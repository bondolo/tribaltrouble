package com.oddlabs.tt.net;

import com.oddlabs.event.NotDeterministic;
import com.oddlabs.net.JitterConfig;
import com.oddlabs.net.JitterTimeManager;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.net.TickTimeManager;
import com.oddlabs.net.TimeManager;
import com.oddlabs.router.Router;
import com.oddlabs.router.SessionID;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.simulation.landscape.LandscapeData;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.player.UnitInfo;
import com.oddlabs.util.Utils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Orchestrator harness running multiple headless simulation instances in the same JVM process.
 */
public final class HeadlessMultiplayerHarness implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(HeadlessMultiplayerHarness.class.getName());

    private final NetworkSelector network;
    private final Router router;
    private final SessionID sessionId;
    private final List<HeadlessSimulationInstance> instances;

    public HeadlessMultiplayerHarness(
            NetworkSelector network,
            Router router,
            SessionID sessionId,
            List<HeadlessSimulationInstance> instances) {
        this.network = network;
        this.router = router;
        this.sessionId = sessionId;
        this.instances = List.copyOf(instances);
    }

    /**
     * Creates a new multiplayer harness with an ephemeral local router and instances for each player.
     * Uses {@link TimeManager#DEFAULT} for real-time wall-clock pacing.
     */
    public static HeadlessMultiplayerHarness create(
            LandscapeData landscapeData,
            WorldParameters worldParams,
            PlayerSlot[] playerSlots,
            UnitInfo[] unitInfos) {
        return create(landscapeData, worldParams, playerSlots, unitInfos, TimeManager.DEFAULT, true);
    }

    /**
     * Creates a new multiplayer harness running on a deterministic virtual tick clock
     * for accelerated headless simulation and automated testing.
     */
    public static HeadlessMultiplayerHarness createVirtual(
            LandscapeData landscapeData,
            WorldParameters worldParams,
            PlayerSlot[] playerSlots,
            UnitInfo[] unitInfos) {
        return create(landscapeData, worldParams, playerSlots, unitInfos, new TickTimeManager(), true);
    }

    /**
     * Creates a new multiplayer harness running on a jittered clock source
     * for testing network synchronization under synthetic delay and burst conditions.
     */
    public static HeadlessMultiplayerHarness createJittered(
            LandscapeData landscapeData,
            WorldParameters worldParams,
            PlayerSlot[] playerSlots,
            UnitInfo[] unitInfos,
            JitterConfig jitterConfig) {
        return create(landscapeData, worldParams, playerSlots, unitInfos, new JitterTimeManager(jitterConfig), true);
    }

    /**
     * Creates a new multiplayer harness with custom time manager and AI peer support.
     */
    public static HeadlessMultiplayerHarness create(
            LandscapeData landscapeData,
            WorldParameters worldParams,
            PlayerSlot[] playerSlots,
            UnitInfo[] unitInfos,
            TimeManager timeManager,
            boolean allowAiPeers) {
        NetworkSelector network = new NetworkSelector(new NotDeterministic(), timeManager);
        Router router = new Router(
                network,
                Utils.getLoopbackAddress(),
                0,
                Logger.getAnonymousLogger(),
                (IOException e) -> logger.warning("Harness router failed: " + e)
        );

        int routerPort = router.getPort();
        SessionID sessionId = new SessionID(1);
        List<HeadlessSimulationInstance> instanceList = new ArrayList<>();

        for (int i = 0; i < playerSlots.length; i++) {
            boolean isParticipant = allowAiPeers && playerSlots[i].getType() == PlayerSlot.AI;
            if (!isParticipant) {
                continue;
            }
            HeadlessSimulationInstance instance = HeadlessSimulationInstance.create(
                    i,
                    worldParams,
                    landscapeData,
                    playerSlots,
                    unitInfos,
                    network,
                    sessionId,
                    Utils.getLoopbackAddress().getHostAddress(),
                    routerPort,
                    allowAiPeers
            );
            instanceList.add(instance);
        }

        return new HeadlessMultiplayerHarness(network, router, sessionId, instanceList);
    }

    public List<HeadlessSimulationInstance> getInstances() {
        return instances;
    }

    public HeadlessSimulationInstance getInstance(int index) {
        return instances.get(index);
    }

    public Router getRouter() {
        return router;
    }

    public NetworkSelector getNetwork() {
        return network;
    }

    public SessionID getSessionId() {
        return sessionId;
    }

    /**
     * Checks whether all simulation instances have synchronized with the router.
     */
    public boolean allSynchronized() {
        for (HeadlessSimulationInstance instance : instances) {
            if (!instance.isSynchronized()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Pumps network socket I/O and router message processing.
     */
    public void pumpNetwork() {
        network.tick();
        router.process();
    }

    /**
     * Waits until all instances have completed the network session handshake.
     */
    public void awaitSynchronized(Duration timeout) throws TimeoutException, InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (!allSynchronized()) {
            pumpNetwork();
            if (allSynchronized()) {
                return;
            }
            if (Instant.now().isAfter(deadline)) {
                throw new TimeoutException("Timed out waiting for instances to synchronize with router");
            }
            Thread.sleep(2);
        }
    }

    /**
     * Steps the simulation forward by the standard tick duration.
     */
    public void step() {
        step(AnimationManager.ANIMATION_SECONDS_PER_TICK);
    }

    /**
     * Steps the simulation forward by the given delta time.
     */
    public void step(float dt) {
        if (network.getTimeManager() instanceof TickTimeManager tickTimeManager) {
            tickTimeManager.advance();
        } else if (network.getTimeManager() instanceof JitterTimeManager jitterTimeManager) {
            jitterTimeManager.advance();
        }
        pumpNetwork();
        for (HeadlessSimulationInstance instance : instances) {
            instance.animate(dt);
        }
        pumpNetwork();
    }

    /**
     * Runs the harness until all instances reach at least the target tick count.
     */
    public void runUntilTick(int targetTick, Duration timeout) throws TimeoutException, InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        boolean isVirtualClock = network.getTimeManager() instanceof TickTimeManager
                || (network.getTimeManager() instanceof JitterTimeManager jtm
                        && jtm.getConfig().baseClock() instanceof TickTimeManager);
        while (true) {
            boolean allReached = true;
            for (HeadlessSimulationInstance instance : instances) {
                if (instance.getTick() < targetTick) {
                    allReached = false;
                    break;
                }
            }
            if (allReached) {
                int firstTick = instances.getFirst().getTick();
                boolean allSameTick = true;
                for (int i = 1; i < instances.size(); i++) {
                    if (instances.get(i).getTick() != firstTick) {
                        allSameTick = false;
                        break;
                    }
                }
                if (allSameTick) {
                    return;
                }
            }
            if (Instant.now().isAfter(deadline)) {
                throw new TimeoutException("Timed out waiting for instances to reach tick " + targetTick);
            }
            step();
            if (!isVirtualClock) {
                Thread.sleep(2);
            }
        }
    }

    /**
     * Returns true if all instances are currently synchronized at the exact same tick.
     */
    public boolean areTicksAligned() {
        if (instances.isEmpty()) {
            return true;
        }
        int firstTick = instances.getFirst().getTick();
        for (int i = 1; i < instances.size(); i++) {
            if (instances.get(i).getTick() != firstTick) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifies that all instances have identical state checksums at the current tick.
     */
    public void verifyChecksums() {
        if (instances.isEmpty()) {
            return;
        }
        int expectedChecksum = instances.getFirst().getChecksum();
        int expectedTick = instances.getFirst().getTick();
        for (int i = 1; i < instances.size(); i++) {
            HeadlessSimulationInstance instance = instances.get(i);
            int checksum = instance.getChecksum();
            int tick = instance.getTick();
            if (checksum != expectedChecksum || tick != expectedTick) {
                throw new AssertionError(String.format(
                        "Checksum mismatch: instance 0 has checksum %d at tick %d, but instance %d has checksum %d at tick %d",
                        expectedChecksum, expectedTick, i, checksum, tick
                ));
            }
        }
    }

    @Override
    public void close() {
        for (HeadlessSimulationInstance instance : instances) {
            try {
                instance.close();
            } catch (Exception e) {
                logger.warning("Error closing instance: " + e);
            }
        }
        try {
            router.close();
        } catch (Exception e) {
            logger.warning("Error closing router: " + e);
        }
    }
}
