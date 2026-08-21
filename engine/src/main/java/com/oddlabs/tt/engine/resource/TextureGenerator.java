package com.oddlabs.tt.engine.resource;

import com.oddlabs.tt.engine.render.Texture;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public abstract class TextureGenerator implements Supplier<Texture[]> {
    protected abstract Texture[] generate();

    @Override
    public final Texture[] get() {
        return generate();
    }

    @Override
    public int hashCode() {
        return getClass().getSimpleName().hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return getClass().isInstance(o);
    }
}
