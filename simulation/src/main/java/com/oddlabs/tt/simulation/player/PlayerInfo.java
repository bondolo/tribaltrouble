package com.oddlabs.tt.simulation.player;

import com.oddlabs.tt.simulation.model.Race;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;

/**
 * Player identification and configuration data.
 */
public record PlayerInfo(int team, Race race, String name) implements Serializable {
    @Serial
    private static final long serialVersionUID = 3;

    public static final int TEAM_NEUTRAL = -1;

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof PlayerInfo player &&
                team == player.team &&
                race == player.race;
    }

    @Override
    public int hashCode() {
        return 31 * team + race.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    @Serial
    private Object writeReplace() {
        return new SerializationProxy(team, race.getValue(), name);
    }

    private static final class SerializationProxy implements Serializable {
        @Serial
        private static final long serialVersionUID = 3;

        private final int team;
        private final int race;
        private final String name;

        SerializationProxy(int team, int race, String name) {
            this.team = team;
            this.race = race;
            this.name = name;
        }

        @Serial
        private Object readResolve() {
            return new PlayerInfo(team, Race.fromValue(race), name);
        }
    }
}
