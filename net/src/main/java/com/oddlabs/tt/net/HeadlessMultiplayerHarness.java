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

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
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
    private final @Nullable CyclicBarrier startBarrier;
    private final @Nullable CyclicBarrier doneBarrier;
    private final List<InstanceWorker> workers;
    private final List<Thread> workerThreads;

    public HeadlessMultiplayerHarness(
            NetworkSelector network,
            Router router,
            SessionID sessionId,
            List<HeadlessSimulationInstance> instances) {
        this.network = network;
        this.router = router;
        this.sessionId = sessionId;
        this.instances = List.copyOf(instances);
        if (this.instances.size() > 1) {
            int count = this.instances.size();
            this.startBarrier = new CyclicBarrier(count + 1);
            this.doneBarrier = new CyclicBarrier(count + 1);
            List<InstanceWorker> workerList = new ArrayList<>(count);
            List<Thread> threadList = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                InstanceWorker worker = new InstanceWorker(this.instances.get(i), startBarrier, doneBarrier);
                workerList.add(worker);
                Thread thread = Thread.ofPlatform()
                        .name("headless-sim-worker-" + i)
                        .daemon(true)
                        .start(worker);
                threadList.add(thread);
            }
            this.workers = List.copyOf(workerList);
            this.workerThreads = List.copyOf(threadList);
        } else {
            this.startBarrier = null;
            this.doneBarrier = null;
            this.workers = List.of();
            this.workerThreads = List.of();
        }
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
     * Steps the simulation forward concurrently across virtual threads by the standard tick duration.
     */
    public void stepConcurrent() {
        stepConcurrent(AnimationManager.ANIMATION_SECONDS_PER_TICK);
    }

    /**
     * Steps the simulation forward by the given delta time sequentially.
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
     * Steps the simulation forward by the given delta time concurrently across virtual threads.
     * Lockstep phases are coordinated using a {@link Phaser}.
     */
    public void stepConcurrent(float dt) {
        if (network.getTimeManager() instanceof TickTimeManager tickTimeManager) {
            tickTimeManager.advance();
        } else if (network.getTimeManager() instanceof JitterTimeManager jitterTimeManager) {
            jitterTimeManager.advance();
        }
        pumpNetwork();
        if (instances.size() <= 1) {
            for (HeadlessSimulationInstance instance : instances) {
                instance.animate(dt);
            }
        } else {
            dispatchConcurrent(dt);
        }
        pumpNetwork();
    }

    private void dispatchConcurrent(float dt) {
        if (startBarrier == null || doneBarrier == null) {
            return;
        }
        for (InstanceWorker worker : workers) {
            worker.currentDt = dt;
        }
        try {
            startBarrier.await();
            doneBarrier.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Simulation tick interrupted", e);
        } catch (BrokenBarrierException e) {
            throw new RuntimeException("Simulation barrier broken", e);
        }
        for (InstanceWorker worker : workers) {
            Throwable t = worker.failure;
            if (t != null) {
                worker.failure = null;
                if (t instanceof RuntimeException re) {
                    throw re;
                }
                if (t instanceof Error err) {
                    throw err;
                }
                throw new RuntimeException(t);
            }
        }
    }

    /**
     * Runs the harness until all instances reach at least the target tick count using sequential stepping.
     */
    public void runUntilTick(int targetTick, Duration timeout) throws TimeoutException, InterruptedException {
        runUntilTick(targetTick, timeout, false);
    }

    /**
     * Runs the harness until all instances reach at least the target tick count using concurrent virtual thread
     * stepping.
     */
    public void runUntilTickConcurrent(int targetTick, Duration timeout) throws TimeoutException, InterruptedException {
        runUntilTick(targetTick, timeout, true);
    }

    private void runUntilTick(int targetTick, Duration timeout, boolean concurrent)
            throws TimeoutException, InterruptedException {
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
            if (concurrent) {
                stepConcurrent();
            } else {
                step();
            }
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
        try {
            network.close();
        } catch (Exception e) {
            logger.warning("Error closing network selector: " + e);
        }
        for (InstanceWorker worker : workers) {
            worker.stop();
        }
        if (startBarrier != null) {
            startBarrier.reset();
        }
        for (Thread thread : workerThreads) {
            thread.interrupt();
            try {
                thread.join(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Dedicated worker executing simulation animation ticks for a single instance.
     */
    private static final class InstanceWorker implements Runnable {
        private final HeadlessSimulationInstance instance;
        private final CyclicBarrier startBarrier;
        private final CyclicBarrier doneBarrier;
        private volatile float currentDt;
        private volatile boolean running = true;
        private volatile @Nullable Throwable failure;

        InstanceWorker(HeadlessSimulationInstance instance, CyclicBarrier startBarrier, CyclicBarrier doneBarrier) {
            this.instance = instance;
            this.startBarrier = startBarrier;
            this.doneBarrier = doneBarrier;
        }

        void stop() {
            running = false;
        }

        @Override
        public void run() {
            ScopedValue.where(PeerHub.CURRENT, instance.getPeerHub()).run(() -> {
                while (running) {
                    try {
                        startBarrier.await();
                        if (!running) {
                            break;
                        }
                        try {
                            instance.animate(currentDt);
                        } catch (Throwable t) {
                            failure = t;
                        }
                        doneBarrier.await();
                    } catch (InterruptedException | BrokenBarrierException e) {
                        break;
                    }
                }
            });
        }
    }
}
