package com.oddlabs.tt.core.net;

public interface ProfileListener {
    void success();

    void error(int error_code);
}
