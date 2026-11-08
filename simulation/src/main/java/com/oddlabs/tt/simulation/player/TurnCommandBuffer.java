package com.oddlabs.tt.simulation.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// Buffers and schedules player commands indexed by simulation tick.
public final class TurnCommandBuffer {

    /// An immutable scheduled command targeting a specific player slot on a given tick.
    ///
    /// @param tick the simulation tick on which the command executes
    /// @param playerSlot the player slot index executing the command
    /// @param command the player command payload
    public record ScheduledCommand(int tick, int playerSlot, PlayerCommand command) {
    }

    private final Map<Integer, List<ScheduledCommand>> commandsByTick = new TreeMap<>();

    /// Enqueues a command for execution at a specified target simulation tick.
    ///
    /// @param targetTick the tick on which to execute
    /// @param playerSlot the index of the player slot executing the command
    /// @param command the command payload
    public void schedule(int targetTick, int playerSlot, PlayerCommand command) {
        commandsByTick.computeIfAbsent(targetTick, _ -> new ArrayList<>())
                .add(new ScheduledCommand(targetTick, playerSlot, command));
    }

    /// Removes and returns all commands scheduled for the specified tick.
    ///
    /// @param tick the simulation tick
    /// @return unmodifiable list of commands scheduled for the tick, or an empty list if none
    public List<ScheduledCommand> drainCommandsForTick(int tick) {
        if (commandsByTick.isEmpty()) {
            return List.of();
        }
        List<ScheduledCommand> commands = commandsByTick.remove(tick);
        return commands != null ? Collections.unmodifiableList(commands) : List.of();
    }

    /// Returns the count of distinct future ticks with scheduled commands.
    ///
    /// @return count of pending scheduled ticks
    public int getScheduledTickCount() {
        return commandsByTick.size();
    }

    /// Returns whether no commands are currently buffered.
    ///
    /// @return true if empty
    public boolean isEmpty() {
        return commandsByTick.isEmpty();
    }

    /// Clears all buffered commands across all ticks.
    public void clear() {
        commandsByTick.clear();
    }
}
