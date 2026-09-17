package com.oddlabs.tt.headless;

import java.util.List;

/**
 * Outcome metrics and state summary for a completed headless match.
 *
 * @param winningTeam identifier of the surviving team, or -1 if draw/timeout
 * @param victory true if a single team eliminated all opposing teams
 * @param finalTick tick number reached when match concluded
 * @param finalChecksum simulation state checksum across all instances at conclusion
 * @param survivingPlayerIndices list of player indices who survived to the end
 */
public record HeadlessMatchResult(
                                  int winningTeam,
                                  boolean victory,
                                  int finalTick,
                                  int finalChecksum,
                                  List<Integer> survivingPlayerIndices) {

    public HeadlessMatchResult {
        survivingPlayerIndices = List.copyOf(survivingPlayerIndices);
    }
}
