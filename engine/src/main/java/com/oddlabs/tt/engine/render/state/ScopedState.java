package com.oddlabs.tt.engine.render.state;

public interface ScopedState extends AutoCloseable {
    @Override
    void close();
}
