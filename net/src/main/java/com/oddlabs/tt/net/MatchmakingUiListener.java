package com.oddlabs.tt.net;


/**
 * Interface for receiving matchmaking status, error, and profile display events.
 */
public interface MatchmakingUiListener {
    void onProfileReceived(String profileInfo);

    void onErrorReceived(int errorCode);
}
