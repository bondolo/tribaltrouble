package com.oddlabs.tt.simulation.model;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/// Collection of selectable units and structures representing an army or selection group.
public class Army implements Iterable<Selectable<?>> {

    private final Set<Selectable<?>> selection = new LinkedHashSet<>();

    public final List<Selectable<?>> filter(int ability_filter) {
        if (selection.isEmpty()) {
            return List.of();
        }
        List<Selectable<?>> result = new ArrayList<>();
        for (Selectable<?> s : selection) {
            if (s.getAbilities().hasAbilities(ability_filter)) {
                result.add(s);
            }
        }
        if (result.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(result);
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
        return Collections.unmodifiableSet(selection);
    }

    public final Stream<Selectable<?>> stream() {
        return selection.stream();
    }

    @Override
    public final @NonNull Iterator<Selectable<?>> iterator() {
        return Collections.unmodifiableSet(selection).iterator();
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

    public final boolean isEmpty() {
        return selection.isEmpty();
    }
}
