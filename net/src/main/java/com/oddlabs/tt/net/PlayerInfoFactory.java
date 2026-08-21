package com.oddlabs.tt.net;

import java.io.Serializable;

/**
 * Functional interface for constructing player info instances without coupling core.net
 * to specific simulation player implementations.
 */
@FunctionalInterface
public interface PlayerInfoFactory {
    Serializable createInfo(int team, int race, String name);
}
