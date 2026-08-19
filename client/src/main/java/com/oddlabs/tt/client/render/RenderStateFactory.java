package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.*;

import com.oddlabs.tt.engine.render.*;

import com.oddlabs.tt.effects.render.*;

import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

@FunctionalInterface
interface RenderStateFactory<RS extends LODObject> extends Supplier<RS> {
    @NonNull
    RS create();

    @Override
    default @NonNull RS get() {
        return create();
    }
}
