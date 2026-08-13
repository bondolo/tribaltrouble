package com.oddlabs.tt.net;

import com.oddlabs.tt.core.animation.AnimationManager;
import com.oddlabs.tt.simulation.player.Player;
import org.jspecify.annotations.NonNull;

/**
 * Interface for placing beacon ping indicators in response to map ping network events.
 */
@FunctionalInterface
public interface BeaconListener {
    void newBeacon(@NonNull AnimationManager manager, @NonNull Player local_player, float x, float y);
}
