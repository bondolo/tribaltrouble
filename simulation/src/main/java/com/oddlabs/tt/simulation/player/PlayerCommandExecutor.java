package com.oddlabs.tt.simulation.player;

/**
 * Interface for executing player commands on the simulation timeline.
 */
public interface PlayerCommandExecutor {

    /**
     * Executes the given command on this player context, validating entities and ownership.
     *
     * @param command the command to execute
     */
    void execute(PlayerCommand command);
}
