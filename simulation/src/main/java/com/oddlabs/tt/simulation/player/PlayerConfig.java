package com.oddlabs.tt.simulation.player;

import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/// Configuration for creating and initializing a player within a simulation world.
///
/// @param info the player identification and race info
/// @param color the linear rendering and team color
/// @param playerInterface optional custom interface for domain action dispatch, or null for default local execution
/// @param controllerType controller type determining starting unit placement and control mode
/// @param unitInfo optional starting base and army specification, or null if no units should be spawned
/// @param chieftainName optional custom name for the starting chieftain, or null
public record PlayerConfig(
                           PlayerInfo info,
                           Color.Linear color,
                           @Nullable PlayerInterface playerInterface,
                           PlayerControllerType controllerType,
                           @Nullable UnitInfo unitInfo,
                           @Nullable String chieftainName
) {
    public PlayerConfig {
        Objects.requireNonNull(info, "info must not be null");
        Objects.requireNonNull(color, "color must not be null");
        controllerType = controllerType != null ? controllerType : PlayerControllerType.HUMAN;
    }

    /// Convenience constructor creating a player config with default local action dispatch and no starting units.
    public PlayerConfig(PlayerInfo info, Color.Linear color) {
        this(info, color, null, PlayerControllerType.HUMAN, null, null);
    }

    /// Convenience constructor creating a player config with custom interface and no starting units.
    public PlayerConfig(PlayerInfo info, Color.Linear color, @Nullable PlayerInterface playerInterface) {
        this(info, color, playerInterface, PlayerControllerType.HUMAN, null, null);
    }

    /// Convenience constructor creating a player config with controller type, starting units, and default
    /// local interface.
    public PlayerConfig(PlayerInfo info, Color.Linear color, PlayerControllerType controllerType,
            @Nullable UnitInfo unitInfo) {
        this(info, color, null, controllerType, unitInfo, null);
    }

    /// Creates a list of configs from parallel player info and color collections with default local interfaces and no
    /// starting units.
    public static List<PlayerConfig> from(List<PlayerInfo> playerInfos, Color.Linear[] colors) {
        return IntStream.range(0, playerInfos.size())
                .mapToObj(i -> new PlayerConfig(playerInfos.get(i), colors[i % colors.length]))
                .toList();
    }
}
