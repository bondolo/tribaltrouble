package com.oddlabs.tt.simulation.network;

/** ARMI interface for peer-to-peer chat and beacon messages. */
public interface PeerHubInterface {
    void chat(String text, boolean team);

    void beacon(float x, float y);
}
