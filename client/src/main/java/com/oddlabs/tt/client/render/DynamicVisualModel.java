package com.oddlabs.tt.client.render;

import com.oddlabs.tt.simulation.model.Model;

/// General-purpose {@link VisualModel} implementation for models hosting dynamic and transient accessories.
final class DynamicVisualModel extends AbstractVisualModel {
  DynamicVisualModel(Model model) {
    super(model);
  }
}
