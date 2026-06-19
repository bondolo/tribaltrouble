package com.oddlabs.tt.render;

import com.oddlabs.tt.model.snapshot.EntitySnapshot;
import org.jspecify.annotations.NonNull;

abstract class SupplyModelVisitor<S extends EntitySnapshot> extends WhiteModelVisitor<S> {
    @Override
    public final void markDetailPoint(@NonNull ElementRenderState<S> render_state) {
        markDetailPolygon(render_state, PolyDetail.LOW_POLY);
    }
}
