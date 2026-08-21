package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.model.Race;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

public final class PlayerInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 3;

    public static final int TEAM_NEUTRAL = -1;

    private final int race;
    private final String name;
    private final int team;

    public PlayerInfo(int team, Race race, String name) {
        this.team = team;
        this.race = race.getValue();
        this.name = name;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof PlayerInfo player &&
                team == player.team &&
                race == player.race;
    }

    public Race getRace() {
        return Race.fromValue(race);
    }

    public String getName() {
        return name;
    }

    public int getTeam() {
        return team;
    }

    @Override
    public String toString() {
        return name;
    }
}
