package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.*;

import com.oddlabs.tt.engine.render.*;

import com.oddlabs.tt.effects.render.*;


import java.util.function.Supplier;

@FunctionalInterface
interface RenderStateFactory<RS extends LODObject> extends Supplier<RS> {
    RS create();

    @Override
    default RS get() {
        return create();
    }
}
