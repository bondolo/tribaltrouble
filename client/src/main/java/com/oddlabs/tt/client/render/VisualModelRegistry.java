package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.IronSupply;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.RockSupply;
import com.oddlabs.tt.simulation.model.SupplyModel;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.weapon.DirectedThrowingWeapon;
import com.oddlabs.tt.simulation.model.weapon.LightningCloud;
import com.oddlabs.tt.simulation.model.weapon.PoisonFog;
import com.oddlabs.tt.simulation.model.weapon.RotatingThrowingWeapon;
import com.oddlabs.tt.simulation.model.weapon.SonicBlast;
import com.oddlabs.tt.simulation.model.weapon.Stun;
import java.util.IdentityHashMap;

/// Registry mapping simulation model types to their corresponding {@link VisualModelFactory}.
public final class VisualModelRegistry {
  private static final VisualModelRegistry DEFAULT_INSTANCE = createDefaultRegistry();

  private final IdentityHashMap<Class<? extends Model>, VisualModelFactory<?>> factories = new IdentityHashMap<>();

  /// {@return the default visual model registry instance pre-populated with standard game entity factories}
  public static VisualModelRegistry getDefault() {
    return DEFAULT_INSTANCE;
  }

  /// {@return a newly instantiated registry populated with standard visual model factories}
  public static VisualModelRegistry createDefaultRegistry() {
    VisualModelRegistry registry = new VisualModelRegistry();
    registry.register(Building.class, BuildingVisualModel::new);
    registry.register(Unit.class, (model, audio) -> new UnitVisualModel(model));
    registry.register(LightningCloud.class, LightningCloudVisualModel::new);
    registry.register(SonicBlast.class, SonicBlastVisualModel::new);
    registry.register(PoisonFog.class, PoisonFogVisualModel::new);
    registry.register(Stun.class, StunVisualModel::new);
    registry.register(IronSupply.class, IronSupplyVisualModel::new);
    registry.register(RockSupply.class, RockSupplyVisualModel::new);
    registry.register(SupplyModel.class, (model, audio) -> new GenericSupplyVisualModel(model));
    registry.register(DirectedThrowingWeapon.class, DirectedThrowingWeaponVisualModel::new);
    registry.register(RotatingThrowingWeapon.class, RotatingThrowingWeaponVisualModel::new);
    return registry;
  }

  /**
   * Registers a visual model factory for a specific model class.
   *
   * @param `modelClass` the simulation model class
   * @param `factory` the visual model factory
   * @param <M> the model type
   */
  public <M extends Model> void register(Class<M> modelClass, VisualModelFactory<M> factory) {
    factories.put(modelClass, factory);
  }

  /**
   * Creates and decorates a VisualModel instance for a simulation Model.
   *
   * @param `model` the simulation model
   * @param `audio` the active audio implementation
   * @return decorated VisualModel instance
   */
  @SuppressWarnings("unchecked")
  public VisualModel createVisualModel(Model model, AudioImplementation audio) {
    VisualModelFactory<Model> factory = (VisualModelFactory<Model>) factories.get(model.getClass());
    if (factory != null) {
      return factory.create(model, audio);
    }
    for (var entry : factories.entrySet()) {
      if (entry.getKey().isInstance(model)) {
        return ((VisualModelFactory<Model>) entry.getValue()).create(model, audio);
      }
    }
    return new DynamicVisualModel(model);
  }
}
