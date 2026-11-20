package com.oddlabs.tt.client.viewer;

import com.oddlabs.matchmaking.Game;

/// Extension of {@link InGameInfo} for multiplayer game sessions backed by a matchmaking {@link Game}.
public interface MultiplayerGameInfo extends InGameInfo {
  /// {@return the matchmaking game definition for this session}
  Game getGame();
}
