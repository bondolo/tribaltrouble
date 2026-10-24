package com.oddlabs.tt.simulation.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps distributable simulation objects to integer identifiers for ARMI serialization.
 */
@NullMarked
public final class DistributableTable {
    private final Map<Integer, Distributable> distributables = new HashMap<>();
    private int current_name = 1;

    public int register(Distributable distributable) {
        int name = current_name++;
        distributable.setId(name);
        assert distributable.getId() == name : "Distributable did not retain assigned id: " + distributable;
        Distributable o = distributables.put(name, distributable);
        assert o == null : "Error registering distributable.";
        return name;
    }

    public void unregister(Distributable distributable) {
        int name = distributable.getId();
        assert name != Distributable.NO_ID : "Error unregistering unregistered distributable: " + distributable;
        distributable.setId(Distributable.NO_ID);

        Distributable o = distributables.remove(name);
        assert o == distributable : "Error unregistering distributable.";
    }

    public int getName(Distributable distributable) {
        int id = distributable.getId();
        assert id != Distributable.NO_ID : distributable + " is not registered.";
        return id;
    }

    public @Nullable Distributable getDistributable(int name) {
        return distributables.get(name);
    }

    public Collection<Distributable> getDistributables() {
        return Collections.unmodifiableCollection(distributables.values());
    }
}
