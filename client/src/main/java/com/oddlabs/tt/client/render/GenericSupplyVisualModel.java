package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.simulation.model.SupplyModel;

/// Generic visual model for supply models that do not have custom particle accessories.
final class GenericSupplyVisualModel extends AbstractSupplyVisualModel<SupplyModel> {
  GenericSupplyVisualModel(SupplyModel supplyModel, SpriteKey spriteKey) {
    super(supplyModel, spriteKey);
  }
}
