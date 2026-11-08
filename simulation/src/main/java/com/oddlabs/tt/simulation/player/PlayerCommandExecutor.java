package com.oddlabs.tt.simulation.player;

/// Execution contract for applying player commands to simulation state.
@FunctionalInterface
public interface PlayerCommandExecutor {

    /**
     * Executes the given command on this player context, validating entities and ownership.
     *
     * @param command the command to execute
     */
    void execute(PlayerCommand command);
}
