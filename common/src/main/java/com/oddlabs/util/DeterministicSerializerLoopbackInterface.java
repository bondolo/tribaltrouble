package com.oddlabs.util;


public interface DeterministicSerializerLoopbackInterface<T> {
    default void saveSucceeded() {
    }

    default void loadSucceeded(T object) {
    }

    void failed(Throwable e);
}
