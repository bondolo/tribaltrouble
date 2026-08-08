package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.client.render.*;
import com.oddlabs.tt.effects.render.*;

import com.oddlabs.tt.simulation.model.SupplyModel;
import org.jspecify.annotations.NonNull;

abstract class SupplyModelVisitor<SM extends SupplyModel> extends WhiteModelVisitor<SM> {
    @Override
    public final void markDetailPoint(@NonNull ElementRenderState<SM> render_state) {
        markDetailPolygon(render_state, PolyDetail.LOW_POLY);
    }
}
