package com.oddlabs.tt.simulation.model;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// Capability providing per-resource supply storage containers for a building or structure.
public interface SupplyStorage {
  Optional<SupplyContainer> getSupplyContainer(SupplyType type);

  default @Nullable SupplyContainer getSupplyContainerDirect(SupplyType type) {
    return getSupplyContainer(type).orElse(null);
  }
}
