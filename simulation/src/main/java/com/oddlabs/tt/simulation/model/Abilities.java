package com.oddlabs.tt.simulation.model;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Mutable set of {@link Ability} flags possessed by an entity.
final class Abilities {
  private final EnumSet<Ability> abilities;

  Abilities() {
    this(EnumSet.noneOf(Ability.class));
  }

  private Abilities(EnumSet<Ability> abilities) {
    this.abilities = abilities;
  }

  static Abilities of(Ability first, Ability... rest) {
    return new Abilities(EnumSet.of(first, rest));
  }

  boolean has(Ability ability) {
    return abilities.contains(ability);
  }

  boolean hasAll(Abilities other) {
    return abilities.containsAll(other.abilities);
  }

  boolean hasAll(Collection<Ability> required) {
    return abilities.containsAll(required);
  }

  void add(Ability ability) {
    abilities.add(ability);
  }

  void addAll(Abilities other) {
    abilities.addAll(other.abilities);
  }

  void addAll(Collection<Ability> abilities) {
    this.abilities.addAll(abilities);
  }

  void remove(Ability ability) {
    abilities.remove(ability);
  }

  void removeAll(Abilities other) {
    abilities.removeAll(other.abilities);
  }

  Set<Ability> asSet() {
    return Collections.unmodifiableSet(abilities);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return this == obj || (obj instanceof Abilities other && abilities.equals(other.abilities));
  }

  @Override
  public int hashCode() {
    return abilities.hashCode();
  }

  @Override
  public String toString() {
    return abilities.toString();
  }
}
