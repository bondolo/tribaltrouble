package com.oddlabs.tt.base.util;


import java.util.function.Function;

/**
 * Headless-compatible callback interface for asynchronous asset or world loading.
 */
@FunctionalInterface
public interface LoadCallback<T, X> extends Function<T, X> {
    X load(T context);

    @Override
    default X apply(T t) {
        return load(t);
    }
}
