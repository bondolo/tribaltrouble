package com.oddlabs.tt.simulation.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Maps distributable simulation objects to integer identifiers for ARMI serialization.
 */
@NullMarked
public final class DistributableTable {
    private final Map<Integer, Distributable> distributables = new HashMap<>();
    private final IdentityHashMap<Distributable, Integer> names = new IdentityHashMap<>();
    private int current_name = 1;

    public int register(Distributable distributable) {
        int name = current_name++;
        Distributable o = distributables.put(name, distributable);
        assert o == null : "Error registering distributable.";
        Integer p = names.put(distributable, name);
        assert p == null : "Error registering name.";
        return name;
    }

    public void unregister(Distributable distributable) {
        Integer name = names.remove(distributable);
        assert name != null : "Error unregistering name.";

        Distributable o = distributables.remove(name);
        assert o == distributable : "Error unregistering distributable.";
    }

    public int getName(Distributable distributable) {
        Integer val = names.get(distributable);
        assert val != null : distributable + " is not registered.";
        return val;
    }

    public @Nullable Distributable getDistributable(int name) {
        return distributables.get(name);
    }
}
