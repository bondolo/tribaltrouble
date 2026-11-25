package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.*;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitSupplyContainer;
import java.util.Map;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/// An accessory representing resources (wood, rock, iron, rubber) currently being carried by a unit.
final class CarriedResourceAccessory implements StaticAccessory {
  private final Unit unit;
  private final Map<SupplyType, SpriteKey> carriedSupplySprites;

  CarriedResourceAccessory(Unit unit, Map<SupplyType, SpriteKey> carriedSupplySprites) {
    assert null != unit.getSupplyContainer() : "Unit must have a supply container";
    this.unit = unit;
    this.carriedSupplySprites = carriedSupplySprites;
  }

  @Override
  public @Nullable SpriteKey getSpriteRenderer() {
    UnitSupplyContainer supply_container = unit.getSupplyContainer();
    if (supply_container != null) {
      return supply_container.getSupplyType().map(carriedSupplySprites::get).orElse(null);
    }
    return null;
  }

  @Override
  public boolean isVisible(Model parent, CameraState camera) {
    UnitSupplyContainer supply_container = unit.getSupplyContainer();
    return unit.getAbilities().hasAbilities(Abilities.BUILD)
        && supply_container != null
        && supply_container.getSupplyType().isPresent()
        && supply_container.getNumSupplies() > 0;
  }

  @Override
  public void getRelativeTransform(Matrix4f dest, Model parent) {
    // Carried resources currently use the parent's exact transform in the old code.
    // We can add offsets here later if needed.
  }

  @Override
  public int getAnimation() {
    return unit.getAnimation();
  }
}
