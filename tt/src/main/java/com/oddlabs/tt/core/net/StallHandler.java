package com.oddlabs.tt.core.net;

public interface StallHandler {
    void stopStall();

    void processStall(int tick);

    void peerhubFailed();
}
