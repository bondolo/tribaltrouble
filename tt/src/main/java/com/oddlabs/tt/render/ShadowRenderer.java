package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.render.state.ScopedState;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

abstract class ShadowRenderer {
    private final DecalRenderer decalRenderer = new DecalRenderer();
    private Color color = new Color.Linear(Color.WHITE_LINEAR);
    private Selectable.@NonNull VisualPattern pattern = Selectable.VisualPattern.NONE;
    private @Nullable Texture currentTexture;

    protected @NonNull ScopedState setupShadows(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        var decalState = decalRenderer.setup(context, renderer, modelViewStack, projectionStack);

        return () -> {
            decalState.close();
            currentTexture = null;
            pattern = Selectable.VisualPattern.NONE;
        };
    }

    protected void setShadowColor(@NonNull Color color) {
        this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
    }

    protected void setPattern(Selectable.@NonNull VisualPattern pattern) {
        this.pattern = pattern;
    }

    protected void bindShadowTexture(@NonNull Texture texture) {
        this.currentTexture = texture;
    }

    protected void setRadial(boolean radial) {
        decalRenderer.setRadial(radial);
    }

    protected final void renderShadow(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer, float shadow_size, float f_x, float f_y) {
        if (currentTexture != null) {
            // Only increase quad size for radial (procedural) halos to provide padding for throb/animations.
            float size = decalRenderer.isRadial() ? shadow_size * 1.25f : shadow_size;
            decalRenderer.draw(context, currentTexture, f_x, f_y, size, color, pattern);
        }
    }

    public void close() {
        decalRenderer.close();
    }
}
