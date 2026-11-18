package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.base.animation.SimulationClock;
import com.oddlabs.tt.simulation.model.AbstractElementNode;
import com.oddlabs.tt.simulation.model.Distributable;
import com.oddlabs.tt.simulation.model.DistributableTable;
import com.oddlabs.tt.simulation.model.RaceData;
import com.oddlabs.tt.simulation.model.SupplyManager;
import com.oddlabs.tt.simulation.model.SupplyManagers;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.TickListener;
import com.oddlabs.tt.simulation.pathfinder.RegionBuilder;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.TurnCommandBuffer;
import com.oddlabs.util.Color;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.IntStream;

/**
 * Represents the game world, orchestrating the height map, resources, dynamic entities,
 * and players within a simulation environment.
 */
public final class World implements SimulationClock {
    private static final float[] GAMESPEEDS = new float[]{
            0f,
            AnimationManager.ANIMATION_SECONDS_PER_TICK / 2,
            AnimationManager.ANIMATION_SECONDS_PER_TICK,
            AnimationManager.ANIMATION_SECONDS_PER_TICK * 1.75f,
            AnimationManager.ANIMATION_SECONDS_PER_TICK * 4
    };

    private final HeightMap world;
    private final Random random;
    private final AnimationManager animation_manager_game_time;
    private final AnimationManager animation_manager_real_time;

    private final int max_unit_count;
    private NotificationListener notification_listener;

    private final List<Player> players;
    private final SupplyManagers supply_managers;
    private final UnitGrid unit_grid;
    private final PatchGroup patch_root;
    private final AbstractTreeGroup tree_root;
    private final AbstractElementNode<?> element_root;
    private final WorldParameters worldParameters;
    private final RaceData races_resources = new RaceData();
    private final LandscapeBoundsProvider landscape_resources = new LandscapeGeometry();
    private final Terrain terrain;
    private final DistributableTable distributable_table = new DistributableTable();
    private final TurnCommandBuffer turnCommandBuffer = new TurnCommandBuffer();
    private final List<TickListener> tickListeners = new ArrayList<>();

    private int global_checksum;
    private int gamespeed;
    private long lastTickNanos = System.nanoTime();

    public Terrain getTerrainType() {
        return terrain;
    }

    public LandscapeBoundsProvider getLandscapeResources() {
        return landscape_resources;
    }

    public RaceData getRaceData() {
        return races_resources;
    }

    public int getChecksum() {
        return global_checksum;
    }

    public void updateGlobalChecksum(int value) {
        global_checksum += value;
    }

    public int getGamespeed() {
        return gamespeed;
    }

    @Override
    public float getSecondsPerTick() {
        return GAMESPEEDS[gamespeed];
    }

    public static boolean isValidPreferredGamespeed(int speed) {
        return speed == GAMESPEED_DONTCARE || isValidGamespeed(speed);
    }

    public static boolean isValidGamespeed(int speed) {
        return speed >= 0 && speed < GAMESPEEDS.length;
    }

    public void gamespeedChanged() {
        int new_gamespeed = GAMESPEED_DONTCARE;
        for (Player player : players) {
            int gamespeed = player.getPreferredGamespeed();
            if (gamespeed != GAMESPEED_DONTCARE) {
                if (new_gamespeed != GAMESPEED_DONTCARE && gamespeed != new_gamespeed)
                    return;
                new_gamespeed = gamespeed;
            }
        }
        if (new_gamespeed != GAMESPEED_DONTCARE && new_gamespeed != gamespeed) {
            gamespeed = new_gamespeed;
            getNotificationListener().gamespeedChanged(gamespeed);
        }
    }

    public void addTickListener(TickListener listener) {
        tickListeners.add(listener);
    }

    public void removeTickListener(TickListener listener) {
        tickListeners.remove(listener);
    }

    public void tick() {
        lastTickNanos = System.nanoTime();
        List<TurnCommandBuffer.ScheduledCommand> scheduled = turnCommandBuffer.drainCommandsForTick(getTick());
        for (int i = 0; i < scheduled.size(); i++) {
            var cmd = scheduled.get(i);
            if (cmd.playerSlot() >= 0 && cmd.playerSlot() < players.size()) {
                players.get(cmd.playerSlot()).execute(cmd.command());
            }
        }
        getAnimationManagerGameTime().runAnimations(getSecondsPerTick());
        for (int i = 0; i < tickListeners.size(); i++) {
            tickListeners.get(i).onTick();
        }
        getAnimationManagerRealTime().runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);
    }

    @Override
    public float getInterpolationAlpha() {
        float secondsPerTick = getSecondsPerTick();
        if (secondsPerTick <= 0f) {
            return 1.0f;
        }
        long tickDurationNanos = (long) (secondsPerTick * 1_000_000_000L);
        if (tickDurationNanos <= 0L) {
            return 1.0f;
        }
        long elapsed = System.nanoTime() - lastTickNanos;
        if (elapsed <= 0L) {
            return 0.0f;
        }
        if (elapsed >= tickDurationNanos) {
            return 1.0f;
        }
        return (float) elapsed / (float) tickDurationNanos;
    }

    @Override
    public int getTick() {
        return getAnimationManagerRealTime().getTick();
    }

    /// Constructs a simulation world with default geometry bounds, normal game speed, and no-op notification listener.
    public World(WorldParameters worldParams, Landscape landscape, List<PlayerConfig> playerConfigs) {
        this(worldParams, landscape, playerConfigs, NotificationListener.EMPTY,
                Game.GAMESPEED_NORMAL);
    }

    /// Constructs a simulation world with the given initial game speed and no-op notification listener.
    public World(WorldParameters worldParams, Landscape landscape, List<PlayerConfig> playerConfigs,
            int initialGameSpeed) {
        this(worldParams, landscape, playerConfigs, NotificationListener.EMPTY, initialGameSpeed);
    }

    /// Constructs a simulation world with default normal game speed.
    public World(WorldParameters world_params, Landscape landscape,
            List<PlayerConfig> playerConfigs, NotificationListener notification_listener) {
        this(world_params, landscape, playerConfigs, notification_listener,
                Game.GAMESPEED_NORMAL);
    }

    /// Constructs a simulation world with the given parameters, terrain, player configurations, notification listener,
    /// and initial game speed.
    public World(WorldParameters world_params, Landscape landscape,
            List<PlayerConfig> playerConfigs, NotificationListener notification_listener, int initialGameSpeed) {
        if (!isValidGamespeed(initialGameSpeed)) {
            throw new IllegalArgumentException("Invalid initial game speed: " + initialGameSpeed);
        }
        IO.println("****************** Generating landscape ********************");
        this.terrain = landscape.terrain();
        this.max_unit_count = world_params.maxUnitCount();
        this.notification_listener = notification_listener;
        this.gamespeed = initialGameSpeed;
        this.worldParameters = world_params;
        long time_start = System.currentTimeMillis();

        world = new HeightMap(this, landscape);
        animation_manager_game_time = new AnimationManager();
        animation_manager_real_time = new AnimationManager();
        random = new Random(42);

        List<List<SpawnLocation>> startingLocations = landscape.startingLocations();
        players = List.of(IntStream.range(0, playerConfigs.size())
                .mapToObj(i -> {
                    SpawnLocation startLoc = startingLocations.size() > i && !startingLocations.get(i).isEmpty()
                            ? startingLocations.get(i).getFirst()
                            : new SpawnLocation(0f, 0f);
                    return new Player(this, playerConfigs.get(i), startLoc);
                }).toArray(Player[]::new));

        long time_stop = System.currentTimeMillis();
        IO.println("****************** Finished landscape in " + ((time_stop - time_start) / 1000f)
                + " sec ********************");
        this.supply_managers = new SupplyManagers(this);
        this.unit_grid = new UnitGrid(world);
        SpawnLocation firstSpawn = startingLocations.get(0).get(0);
        RegionBuilder.buildRegions(unit_grid, firstSpawn.x(), firstSpawn.y());
        this.patch_root = new PatchGroup(this);
        this.tree_root = AbstractTreeGroup.newRoot(this, landscape.trees(), landscape.palmTrees(), terrain);
        this.element_root = AbstractElementNode.newRoot(world);
        AbstractElementNode.buildSupplies(this, landscape.iron(), landscape.rocks(), landscape.plants(),
                terrain);
        for (int i = 0; i < players.size(); i++) {
            List<SpawnLocation> locations = startingLocations.size() > i ? startingLocations.get(i) : List.of();
            players.get(i).initializeStartingEntities(locations, gamespeed);
        }
        activeWorlds.add(new WeakReference<>(this));
    }

    public AbstractElementNode getElementRoot() {
        return element_root;
    }

    public AbstractTreeGroup getTreeRoot() {
        return tree_root;
    }

    public TreeSupply spawnTree(float x, float y) {
        AbstractTreeGroup.TreeType tree_type = (terrain == Terrain.VIKING)
                ? (random.nextBoolean() ? AbstractTreeGroup.TreeType.OAK : AbstractTreeGroup.TreeType.PINE)
                : (random.nextBoolean() ? AbstractTreeGroup.TreeType.JUNGLE : AbstractTreeGroup.TreeType.PALM);
        return tree_root.spawnTree(this, tree_type, x, y);
    }

    public AbstractPatchGroup getPatchRoot() {
        return patch_root;
    }

    public UnitGrid getUnitGrid() {
        return unit_grid;
    }

    public SupplyManager getSupplyManager(SupplyType type) {
        return supply_managers.getSupplyManager(type);
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getMaxUnitCount() {
        return max_unit_count;
    }

    public NotificationListener getNotificationListener() {
        return notification_listener;
    }

    public void setNotificationListener(NotificationListener notificationListener) {
        this.notification_listener = notificationListener;
    }

    public WorldParameters getWorldParameters() {
        return worldParameters;
    }

    public HeightMap getHeightMap() {
        return world;
    }

    public LandscapeEnvironment getLandscapeEnvironment() {
        return world;
    }

    public AnimationManager getAnimationManagerGameTime() {
        return animation_manager_game_time;
    }

    public AnimationManager getAnimationManagerRealTime() {
        return animation_manager_real_time;
    }

    public Random getRandom() {
        return random;
    }

    /**
     * Updates the player colours of all players in this world.
     *
     * @param playerColors the array of linear player colors
     */
    public void updatePlayerColors(Color.Linear[] playerColors) {
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setColor(playerColors[i % playerColors.length]);
        }
    }

    private static final Set<WeakReference<World>> activeWorlds = new CopyOnWriteArraySet<>();

    /**
     * Updates the player colors across all active world instances.
     *
     * @param playerColors the array of linear player colors
     */
    public static void updateAllPlayerColors(Color.Linear[] playerColors) {
        for (var ref : activeWorlds) {
            World w = ref.get();
            if (w != null) {
                w.updatePlayerColors(playerColors);
            } else {
                activeWorlds.remove(ref);
            }
        }
    }

    public DistributableTable getDistributableTable() {
        return distributable_table;
    }

    /// Returns the buffer for commands scheduled by tick.
    public TurnCommandBuffer getTurnCommandBuffer() {
        return turnCommandBuffer;
    }

    public int registerTarget(Distributable target) {
        int id = distributable_table.register(target);
        if (target instanceof Target t) {
            notification_listener.registerTarget(t);
        }
        return id;
    }

    public void unregisterTarget(Distributable target) {
        distributable_table.unregister(target);
        if (target instanceof Target t) {
            notification_listener.unregisterTarget(t);
        }
    }
}
