package com.oddlabs.tt.simulation.landscape;

import com.oddlabs.tt.core.animation.AnimationManager;
import com.oddlabs.tt.core.animation.SimulationClock;
import com.oddlabs.tt.core.util.ProgressListener;
import com.oddlabs.tt.simulation.model.AbstractElementNode;
import com.oddlabs.tt.simulation.model.Plants;
import com.oddlabs.tt.simulation.model.RacesResources;
import com.oddlabs.tt.simulation.model.SupplyManager;
import com.oddlabs.tt.simulation.model.SupplyManagers;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Terrain;
import com.oddlabs.tt.simulation.pathfinder.RegionBuilder;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
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

    private final @NonNull HeightMap world;
    private final @NonNull Random random;
    private final @NonNull AnimationManager animation_manager_game_time;
    private final @NonNull AnimationManager animation_manager_real_time;

    private final int max_unit_count;
    private final @NonNull NotificationListener notification_listener;

    private final @NonNull List<@NonNull Player> players;
    private final @NonNull SupplyManagers supply_managers;
    private final @NonNull UnitGrid unit_grid;
    private final @NonNull PatchGroup patch_root;
    private final @NonNull AbstractTreeGroup tree_root;
    private final @NonNull AbstractElementNode<?> element_root;
    private final @Nullable RacesResources races_resources;
    private final @NonNull LandscapeBoundsProvider landscape_resources;
    private final @NonNull Terrain terrain;
    private final float @NonNull [] @NonNull [] plantCoordinates;
    private final List<@NonNull Plants> activePlants = new ArrayList<>();

    private int global_checksum;
    private int gamespeed;

    public static @NonNull World newWorld(
            @NonNull LandscapeBoundsProvider landscape_resources, @Nullable RacesResources races_resources,
            @NonNull NotificationListener notification_listener, @NonNull WorldParameters world_params,
            @NonNull LandscapeData landscapeData, List<@NonNull PlayerInfo> player_infos,
            Color.@NonNull Linear @NonNull [] teamColors, boolean insertPlants) {
        return newWorld(landscape_resources, races_resources, notification_listener, world_params,
                landscapeData, player_infos, teamColors, insertPlants, ProgressListener.NONE);
    }

    public static @NonNull World newWorld(
            @NonNull LandscapeBoundsProvider landscape_resources, @Nullable RacesResources races_resources,
            @NonNull NotificationListener notification_listener, @NonNull WorldParameters world_params,
            @NonNull LandscapeData landscapeData, List<@NonNull PlayerInfo> player_infos,
            Color.@NonNull Linear @NonNull [] teamColors, boolean insertPlants,
            @NonNull ProgressListener progress_listener) {
        progress_listener.onProgress();
        World world = new World(landscape_resources, races_resources, notification_listener,
                world_params, landscapeData, player_infos, teamColors, insertPlants, progress_listener);
        progress_listener.onProgress();
        progress_listener.onProgress(1 / 5f);
        progress_listener.onProgress();

        return world;
    }

    public @NonNull Terrain getTerrainType() {
        return terrain;
    }

    public @NonNull LandscapeBoundsProvider getLandscapeResources() {
        return landscape_resources;
    }

    public @Nullable RacesResources getRacesResources() {
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

    private World(@NonNull LandscapeBoundsProvider landscape_resources,
            @Nullable RacesResources races_resources, @NonNull NotificationListener notification_listener,
            @NonNull WorldParameters world_params, @NonNull LandscapeData landscapeData,
            @NonNull List<@NonNull PlayerInfo> player_infos, Color.@NonNull Linear @NonNull [] teamColors,
            boolean insertPlants, @NonNull ProgressListener progress_listener) {
        IO.println("****************** Generating landscape ********************");
        this.terrain = landscapeData.terrain();
        this.plantCoordinates = landscapeData.plants();
        this.landscape_resources = landscape_resources;
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
                landscapeData.startingLocations()[0][1], progress_listener);
        this.patch_root = new PatchGroup(this);
        this.tree_root = AbstractTreeGroup.newRoot(this, landscapeData.trees(), landscapeData.palmTrees(), terrain);
        this.element_root = AbstractElementNode.newRoot(world);
        AbstractElementNode.buildSupplies(this, landscapeData.iron(), landscapeData.rocks(), landscapeData.plants(),
                terrain,
                insertPlants);
        activeWorlds.add(new WeakReference<>(this));
    }

    public @NonNull AbstractElementNode getElementRoot() {
        return element_root;
    }

    public @NonNull AbstractTreeGroup getTreeRoot() {
        return tree_root;
    }

    public @NonNull AbstractPatchGroup getPatchRoot() {
        return patch_root;
    }

    public @NonNull UnitGrid getUnitGrid() {
        return unit_grid;
    }

    public @Nullable SupplyManager getSupplyManager(@NonNull SupplyType type) {
        return supply_managers.getSupplyManager(type);
    }

    public @NonNull List<@NonNull Player> getPlayers() {
        return players;
    }

    public int getMaxUnitCount() {
        return max_unit_count;
    }

    public @NonNull NotificationListener getNotificationListener() {
        return notification_listener;
    }

    public @NonNull HeightMap getHeightMap() {
        return world;
    }

    public @NonNull LandscapeEnvironment getLandscapeEnvironment() {
        return world;
    }

    public @NonNull AnimationManager getAnimationManagerGameTime() {
        return animation_manager_game_time;
    }

    public @NonNull AnimationManager getAnimationManagerRealTime() {
        return animation_manager_real_time;
    }

    public @NonNull Random getRandom() {
        return random;
    }

    public void registerPlant(@NonNull Plants plant) {
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
}
