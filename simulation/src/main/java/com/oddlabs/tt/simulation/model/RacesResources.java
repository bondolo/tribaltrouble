package com.oddlabs.tt.simulation.model;

import java.util.EnumMap;
import java.util.stream.IntStream;

/**
 * Central model-side registry for game race statistics and template configs.
 * Completely decoupled from graphics/rendering systems.
 */
public final class RacesResources {
    public static final int QUARTERS_SIZE = 5;
    public static final int ARMORY_SIZE = 5;
    public static final int TOWER_SIZE = 3;
    public static final int MAX_BUILDING_SIZE = IntStream.of(QUARTERS_SIZE, ARMORY_SIZE, TOWER_SIZE).max()
            .orElseThrow();
    public static final int QUARTERS_HIT_POINTS = 200;
    public static final int ARMORY_HIT_POINTS = 200;
    public static final int TOWER_HIT_POINTS = 100;
    public static final int VIKING_CHIEFTAIN_HIT_POINTS = 60;
    public static final int NATIVE_CHIEFTAIN_HIT_POINTS = 40;

    public static final float THROW_RANGE = 6f;
    public static final float BUILDING_RING_PHYSICAL_THICKNESS = 0.2f;

    private final EnumMap<Race, RaceInfo> raceInfos = new EnumMap<>(Race.class);

    public static boolean isValidRace(int race) {
        return race >= 0 && race < Race.values().length;
    }

    public RacesResources(EnumMap<Race, RaceInfo> raceInfos) {
        this.raceInfos.putAll(raceInfos);
    }

    public RaceInfo getRaceInfo(Race race) {
        return raceInfos.get(race);
    }
}
