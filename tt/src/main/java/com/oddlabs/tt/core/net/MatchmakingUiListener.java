package com.oddlabs.tt.core.net;

import org.jspecify.annotations.NonNull;

/**
 * Interface for receiving matchmaking status, error, and profile display events.
 */
public interface MatchmakingUiListener {
    void onProfileReceived(@NonNull String profileInfo);

    void onErrorReceived(int errorCode);
}
