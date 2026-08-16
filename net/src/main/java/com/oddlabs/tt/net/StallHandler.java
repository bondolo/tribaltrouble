package com.oddlabs.tt.net;

/** Handler interface for network synchronization stalls. */
public interface StallHandler {
    void stopStall();

    void processStall(int tick);

    void peerhubFailed();
}
