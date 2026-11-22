package com.oddlabs.tt.headless;

import java.util.List;

/// Outcome metrics and state summary for a completed headless match.
///
/// @param winningTeam identifier of the surviving team, or -1 if draw/timeout
/// @param victory true if a single team eliminated all opposing teams
/// @param finalTick tick number reached when match concluded
/// @param finalChecksum simulation state checksum across all instances at conclusion
/// @param finalMerkleRootHash semantic [com.oddlabs.tt.simulation.model.WorldMerkleTree] root hash at conclusion
/// @param survivingPlayerIndices list of player indices who survived to the end
/// @param totalMagicCasts total number of spells cast across all players during the match
public record HeadlessMatchResult(
    int winningTeam,
    boolean victory,
    int finalTick,
    int finalChecksum,
    long finalMerkleRootHash,
    List<Integer> survivingPlayerIndices,
    int totalMagicCasts) {

  public HeadlessMatchResult(
      int winningTeam,
      boolean victory,
      int finalTick,
      int finalChecksum,
      long finalMerkleRootHash,
      List<Integer> survivingPlayerIndices) {
    this(winningTeam, victory, finalTick, finalChecksum, finalMerkleRootHash, survivingPlayerIndices, 0);
  }

  public HeadlessMatchResult {
    survivingPlayerIndices = List.copyOf(survivingPlayerIndices);
  }
}
