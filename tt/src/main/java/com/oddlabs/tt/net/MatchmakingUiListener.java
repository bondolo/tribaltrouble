package com.oddlabs.tt.net;

import org.jspecify.annotations.NonNull;

/**
 * Interface for receiving matchmaking status, error, and profile display events.
 */
public interface MatchmakingUiListener {
    void onProfileReceived(@NonNull String profileInfo);

    void onErrorReceived(int errorCode);
}
