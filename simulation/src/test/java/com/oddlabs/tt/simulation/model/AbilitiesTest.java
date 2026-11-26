package com.oddlabs.tt.simulation.model;

import static com.oddlabs.tt.simulation.model.Ability.ATTACK;
import static com.oddlabs.tt.simulation.model.Ability.BUILD;
import static com.oddlabs.tt.simulation.model.Ability.HARVEST;
import static com.oddlabs.tt.simulation.model.Ability.MAGIC;
import static com.oddlabs.tt.simulation.model.Ability.TARGET;
import static com.oddlabs.tt.simulation.model.Ability.THROW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/// Unit tests for {@link Abilities} enum-set operations, isolation, and value semantics.
@NullMarked
final class AbilitiesTest {

  @Test
  void emptyAndFactoryCreation() {
    Abilities empty = new Abilities();
    for (Ability ability : Ability.values()) {
      assertFalse(empty.has(ability));
    }
    assertTrue(empty.asSet().isEmpty());
    assertTrue(empty.hasAll(new Abilities()));
    assertTrue(empty.hasAll(Set.of()));

    Abilities peon = Abilities.of(BUILD, HARVEST, ATTACK, TARGET);
    assertTrue(peon.has(BUILD));
    assertTrue(peon.has(HARVEST));
    assertTrue(peon.has(ATTACK));
    assertTrue(peon.has(TARGET));
    assertFalse(peon.has(THROW));
    assertEquals(Set.of(BUILD, HARVEST, ATTACK, TARGET), peon.asSet());
  }

  @Test
  void hasAllChecks() {
    Abilities warrior = Abilities.of(ATTACK, TARGET, THROW);

    assertTrue(warrior.hasAll(Abilities.of(ATTACK, THROW)));
    assertTrue(warrior.hasAll(Set.of(ATTACK, TARGET)));
    assertFalse(warrior.hasAll(Abilities.of(ATTACK, MAGIC)));
    assertFalse(warrior.hasAll(EnumSet.of(ATTACK, BUILD)));
  }

  @Test
  void addAddAllRemoveRemoveAllAndIsolation() {
    Abilities source = Abilities.of(BUILD, HARVEST);
    Abilities target = new Abilities();
    target.add(ATTACK);
    target.addAll(source);

    assertTrue(target.has(ATTACK));
    assertTrue(target.has(BUILD));
    assertTrue(target.has(HARVEST));

    // Mutating source or target does not affect the other.
    source.add(MAGIC);
    assertFalse(target.has(MAGIC));

    target.remove(HARVEST);
    assertFalse(target.has(HARVEST));
    assertTrue(source.has(HARVEST));

    target.removeAll(Abilities.of(BUILD, THROW));
    assertFalse(target.has(BUILD));
    assertTrue(target.has(ATTACK));
    assertTrue(source.has(BUILD));

    Abilities fromCollection = new Abilities();
    fromCollection.addAll(Set.of(THROW, MAGIC));
    assertTrue(fromCollection.has(THROW));
    assertTrue(fromCollection.has(MAGIC));
  }

  @Test
  void templateAbilitiesAreImmutable() {
    Template peonTemplate = new RaceData().getRaceInfo(Race.VIKINGS).getUnitTemplate(UnitType.PEON);
    Set<Ability> abilities = peonTemplate.getAbilities();
    assertTrue(peonTemplate.hasAbility(BUILD));
    assertThrows(UnsupportedOperationException.class, () -> abilities.add(MAGIC));
  }

  @Test
  void unmodifiableViewEqualsHashCodeAndToString() {
    Abilities a = Abilities.of(ATTACK, TARGET);
    Abilities b = Abilities.of(TARGET, ATTACK);
    Abilities c = Abilities.of(ATTACK);

    Set<Ability> view = a.asSet();
    assertThrows(UnsupportedOperationException.class, () -> view.add(MAGIC));

    assertEquals(a, a);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c);
    assertNotEquals(a, null);
    assertNotEquals(a, "not abilities");
    assertEquals(EnumSet.of(ATTACK, TARGET).toString(), a.toString());
  }
}
