package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.base.animation.SimulationClock;
import com.oddlabs.tt.base.util.ProgressListener;
import com.oddlabs.tt.simulation.model.AbstractElementNode;
import com.oddlabs.tt.simulation.model.Distributable;
import com.oddlabs.tt.simulation.model.DistributableTable;
import com.oddlabs.tt.simulation.model.Plants;
import com.oddlabs.tt.simulation.model.RaceData;
import com.oddlabs.tt.simulation.model.SupplyManager;
import com.oddlabs.tt.simulation.model.SupplyManagers;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.pathfinder.RegionBuilder;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

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
    public static final int GAMESPEED_DONTCARE = -2;

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
    private final NotificationListener notification_listener;

    private final List<Player> players;
    private final SupplyManagers supply_managers;
    private final UnitGrid unit_grid;
    private final PatchGroup patch_root;
    private final AbstractTreeGroup tree_root;
    private final AbstractElementNode<?> element_root;
    private final @Nullable RaceData races_resources;
    private final LandscapeBoundsProvider landscape_resources;
    private final Terrain terrain;
    private final float[][] plantCoordinates;
    private final List<Plants> activePlants = new ArrayList<>();
    private final DistributableTable distributable_table = new DistributableTable();

    private int global_checksum;
    private int gamespeed;

    public static World newWorld(
            @Nullable RaceData races_resources,
            @Nullable LandscapeBoundsProvider landscape_resources,
            NotificationListener notification_listener, WorldParameters world_params,
            LandscapeData landscapeData, List<PlayerInfo> player_infos,
            Color.Linear[] teamColors, boolean insertPlants) {
        ProgressListener.progress();
        World world = new World(races_resources, landscape_resources, notification_listener,
                world_params, landscapeData, player_infos, teamColors, insertPlants);
        ProgressListener.progress();
        ProgressListener.progress(1 / 5f);
        ProgressListener.progress();

        return world;
    }

    public Terrain getTerrainType() {
        return terrain;
    }

    public LandscapeBoundsProvider getLandscapeResources() {
        return landscape_resources;
    }

    public @Nullable RaceData getRaceData() {
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

    public void tick(float t) {
        getAnimationManagerGameTime().runAnimations(getSecondsPerTick() * t
                / AnimationManager.ANIMATION_SECONDS_PER_TICK);
        getAnimationManagerRealTime().runAnimations(t/*AnimationManager.ANIMATION_SECONDS_PER_TICK*/);
    }

    @Override
    public int getTick() {
        return getAnimationManagerRealTime().getTick();
    }

    private World(@Nullable RaceData races_resources,
            @Nullable LandscapeBoundsProvider landscape_resources,
            NotificationListener notification_listener,
            WorldParameters world_params, LandscapeData landscapeData,
            List<PlayerInfo> player_infos, Color.Linear[] teamColors,
            boolean insertPlants) {
        IO.println("****************** Generating landscape ********************");
        this.terrain = landscapeData.terrain();
        this.plantCoordinates = landscapeData.plants();
        this.landscape_resources = landscape_resources != null ? landscape_resources : new LandscapeGeometry();
        this.races_resources = races_resources;
        this.max_unit_count = world_params.maxUnitCount();
        this.notification_listener = notification_listener;
        this.gamespeed = world_params.initialGameSpeed();
        long time_start = System.currentTimeMillis();

        world = new HeightMap(this, landscapeData);
        animation_manager_game_time = new AnimationManager();
        animation_manager_real_time = new AnimationManager();
        random = new Random(42);

        players = List.of(IntStream.range(0, player_infos.size())
                .mapToObj(i -> new Player(this, player_infos.get(i), teamColors[i % teamColors.length])
                        .init(landscapeData.startingLocations()[i])
                ).toArray(Player[]::new));

        long time_stop = System.currentTimeMillis();
        IO.println("****************** Finished landscape in " + ((time_stop - time_start) / 1000f)
                + " sec ********************");
        this.supply_managers = new SupplyManagers(this);
        this.unit_grid = new UnitGrid(world);
        RegionBuilder.buildRegions(unit_grid, landscapeData.startingLocations()[0][0],
                landscapeData.startingLocations()[0][1]);
        this.patch_root = new PatchGroup(this);
        this.tree_root = AbstractTreeGroup.newRoot(this, landscapeData.trees(), landscapeData.palmTrees(), terrain);
        this.element_root = AbstractElementNode.newRoot(world);
        AbstractElementNode.buildSupplies(this, landscapeData.iron(), landscapeData.rocks(), landscapeData.plants(),
                terrain,
                insertPlants);
        activeWorlds.add(new WeakReference<>(this));
    }

    public AbstractElementNode getElementRoot() {
        return element_root;
    }

    public AbstractTreeGroup getTreeRoot() {
        return tree_root;
    }

    public AbstractPatchGroup getPatchRoot() {
        return patch_root;
    }

    public UnitGrid getUnitGrid() {
        return unit_grid;
    }

    public @Nullable SupplyManager getSupplyManager(SupplyType type) {
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

    public void registerPlant(Plants plant) {
        synchronized (activePlants) {
            activePlants.add(plant);
        }
    }

    private void removeAllPlants() {
        synchronized (activePlants) {
            activePlants.forEach(Plants::remove);
            activePlants.clear();
        }
    }

    public void setPlantsDetail(boolean insertPlants) {
        synchronized (activePlants) {
            if (insertPlants) {
                if (activePlants.isEmpty()) {
                    AbstractElementNode.addPlants(this, plantCoordinates, terrain);
                }
            } else {
                removeAllPlants();
            }
        }
    }

    private static final Set<WeakReference<World>> activeWorlds = new CopyOnWriteArraySet<>();

    public static void updatePlantsDetail(boolean insertPlants) {
        for (var ref : activeWorlds) {
            World w = ref.get();
            if (w != null) {
                w.setPlantsDetail(insertPlants);
            } else {
                activeWorlds.remove(ref);
            }
        }
    }

    public DistributableTable getDistributableTable() {
        return distributable_table;
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
