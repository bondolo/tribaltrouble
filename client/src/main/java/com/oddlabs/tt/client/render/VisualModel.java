package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.Accessory;
import com.oddlabs.tt.simulation.model.ClientModel;
import com.oddlabs.tt.simulation.model.Model;
import java.util.SequencedCollection;

/// Manages the client-side visual accessories for a simulation model.
interface VisualModel extends ClientModel {
  Model getModel();

  SequencedCollection<Accessory> getAccessories();

  boolean isExpired();

  void update(float dt);

  @Override
  void close();

  void addAccessory(Accessory accessory);

  void visit(RenderState renderState);
}
