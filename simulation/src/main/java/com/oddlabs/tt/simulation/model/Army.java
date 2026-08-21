package com.oddlabs.tt.simulation.model;


import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class Army {
    private final Set<Selectable<?>> selection = new LinkedHashSet<>();

    public final Selectable<?>[] filter(int ability_filter) {
        return (Selectable<?>[]) selection.stream()
                .filter(s -> s.getAbilities().hasAbilities(ability_filter))
                .toArray(Selectable[]::new);
    }

    public final boolean containsAbility(int ability_filter) {
        return selection.stream()
                .anyMatch(s -> s.getAbilities().hasAbilities(ability_filter));
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
