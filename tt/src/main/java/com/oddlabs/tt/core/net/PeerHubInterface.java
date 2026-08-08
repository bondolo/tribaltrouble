package com.oddlabs.tt.core.net;

public interface PeerHubInterface {
    void chat(String text, boolean team);

    void beacon(float x, float y);
}
