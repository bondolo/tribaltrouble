package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.Accessory;
import com.oddlabs.tt.engine.render.AnimatedAccessory;
import com.oddlabs.tt.engine.render.ElementSceneContext;
import com.oddlabs.tt.engine.render.WhiteModelVisitor;
import com.oddlabs.tt.simulation.model.Model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;
import java.util.function.Predicate;

/// Abstract base implementation of {@link VisualModel} providing common accessory management and lifecycle operations.
abstract class AbstractVisualModel implements VisualModel {
  private final Model model;
  private final SequencedCollection<Accessory> accessories = new ArrayList<>();

  protected AbstractVisualModel(Model model) {
    this.model = model;
  }

  @Override
  public final void addAccessory(Accessory accessory) {
    accessories.add(accessory);
  }

  protected final void removeAccessoriesIf(Predicate<Accessory> filter) {
    accessories.removeIf(filter);
  }

  @Override
  public SequencedCollection<Accessory> getAccessories() {
    if (this instanceof Accessory self) {
      if (accessories.isEmpty()) {
        return List.of(self);
      }
      List<Accessory> all = new ArrayList<>(1 + accessories.size());
      all.add(self);
      all.addAll(accessories);
      return Collections.unmodifiableList(all);
    }
    return Collections.unmodifiableSequencedCollection(accessories);
  }

  @Override
  public Model getModel() {
    return model;
  }

  /// Returns whether the primary model or visual effect represented directly by this visual model has expired.
  protected boolean isSelfExpired() {
    return model.isDead();
  }

  @Override
  public boolean isExpired() {
    if (!isSelfExpired()) {
      return false;
    }
    for (Accessory acc : accessories) {
      if (!acc.isExpired()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void update(float dt) {
    if (this instanceof AnimatedAccessory animated) {
      animated.animate(dt);
    }
    boolean hasExpired = false;
    for (Accessory acc : accessories) {
      if (acc instanceof AnimatedAccessory animated) {
        animated.animate(dt);
      }
      if (acc.isExpired()) {
        hasExpired = true;
      }
    }
    if (hasExpired) {
      for (Accessory acc : accessories) {
        if (acc.isExpired()) {
          acc.close();
        }
      }
      accessories.removeIf(Accessory::isExpired);
    }
  }

  @Override
  public void close() {
    for (Accessory acc : accessories) {
      acc.close();
    }
    accessories.clear();
  }

  @Override
  public void visit(RenderState rs) {
    float z_offset = rs.getVisuallyCorrectHeight(model.getPositionX(), model.getPositionY());
    ElementSceneContext<Model> state =
        (ElementSceneContext<Model>) rs.getCachedState(WhiteModelVisitor.getInstance(), model, z_offset);
    rs.visitAccessories(model, state);
  }
}
