package com.oddlabs.tt.simulation.landscape;

import java.util.Optional;

/**
 * Composite environment interface providing height querying, dimensional bounds, and visual client state.
 */
public interface LandscapeEnvironment extends HeightQuery, LandscapeBounds {
    /**
     * Optional accessor for the parent simulation world.
     */
    default Object getWorld() {
        return null;
    }

    default <C> Optional<C> getClientState(Class<C> type) {
        return Optional.empty();
    }
}
