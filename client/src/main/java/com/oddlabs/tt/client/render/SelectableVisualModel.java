package com.oddlabs.tt.client.render;

import com.oddlabs.tt.simulation.model.Selectable;

/// Base visual model for selectable entities providing visit dispatch.
abstract sealed class SelectableVisualModel<S extends Selectable<?>> extends AbstractVisualModel
    permits UnitVisualModel, BuildingVisualModel {

  protected final S selectable;

  SelectableVisualModel(S selectable) {
    super(selectable);
    this.selectable = selectable;
  }

  @Override
  public void visit(RenderState rs) {
    float alpha = rs.getInterpolationAlpha();
    float x = selectable.getInterpolatedPositionX(alpha);
    float y = selectable.getInterpolatedPositionY(alpha);
    float z_offset = rs.getVisuallyCorrectHeight(x, y) + selectable.getOffsetZ();
    rs.visitSelectable(selectable, z_offset);
  }
}
