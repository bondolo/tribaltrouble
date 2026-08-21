package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.*;

import com.oddlabs.tt.simulation.model.SupplyModel;

abstract class SupplyModelVisitor<SM extends SupplyModel> extends WhiteModelVisitor<SM> {
    @Override
    public final void markDetailPoint(ElementSceneContext<SM> render_state) {
        markDetailPolygon(render_state, PolyDetail.LOW_POLY);
    }
}
