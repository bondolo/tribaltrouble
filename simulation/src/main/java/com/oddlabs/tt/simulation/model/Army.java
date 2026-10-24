package com.oddlabs.tt.simulation.model;


import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Collection of selectable units and structures representing an army or selection group.
 */
public class Army {
    private static final Selectable<?>[] EMPTY_SELECTION = new Selectable<?>[0];

    private final Set<Selectable<?>> selection = new LinkedHashSet<>();

    public final Selectable<?>[] filter(int ability_filter) {
        if (selection.isEmpty()) {
            return EMPTY_SELECTION;
        }
        int count = 0;
        for (Selectable<?> s : selection) {
            if (s.getAbilities().hasAbilities(ability_filter)) {
                count++;
            }
        }
        if (count == 0) {
            return EMPTY_SELECTION;
        }
        Selectable<?>[] result = new Selectable<?>[count];
        int idx = 0;
        for (Selectable<?> s : selection) {
            if (s.getAbilities().hasAbilities(ability_filter)) {
                result[idx++] = s;
            }
        }
        return result;
    }

    public final boolean containsAbility(int ability_filter) {
        for (Selectable<?> s : selection) {
            if (s.getAbilities().hasAbilities(ability_filter)) {
                return true;
            }
        }
        return false;
    }

    public void clear() {
        selection.clear();
    }

    public void remove(Selectable<?> selectable) {
        selection.remove(selectable);
    }

    public final boolean contains(Selectable<?> selectable) {
        return selection.contains(selectable);
    }

    public final Set<Selectable<?>> getSet() {
        return selection;
    }

    public void add(Selectable<?> selectable) {
        selection.add(selectable);
    }

    public void addAll(Collection<? extends Selectable<?>> selectable) {
        selection.addAll(selectable);
    }

    public final int size() {
        return selection.size();
    }
}
