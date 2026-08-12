package com.oddlabs.tt.core.net;

import org.jspecify.annotations.NonNull;
import java.io.Serializable;

/**
 * Functional interface for constructing player info instances without coupling core.net
 * to specific simulation player implementations.
 */
@FunctionalInterface
public interface PlayerInfoFactory {
    @NonNull
    Serializable createInfo(int team, int race, @NonNull String name);
}
