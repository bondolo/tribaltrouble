package com.oddlabs.tt.net;

import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.simulation.player.Player;

/**
 * Interface for placing beacon ping indicators in response to map ping network events.
 */
@FunctionalInterface
public interface BeaconListener {
    void newBeacon(AnimationManager manager, Player local_player, float x, float y);
}
