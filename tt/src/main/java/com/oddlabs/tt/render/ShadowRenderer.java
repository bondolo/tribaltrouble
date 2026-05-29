package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.render.state.ScopedState;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Base class for rendering dynamic shadow decals for world objects.
 */
abstract class ShadowRenderer {
    private final DecalRenderer decalRenderer = new DecalRenderer();
    private Color.@NonNull Linear color = Color.Linear.WHITE;
    private Selectable.@NonNull VisualPattern pattern = Selectable.VisualPattern.NONE;
    private @Nullable Texture currentTexture;

    protected @NonNull ScopedState setupShadows(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
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

    protected final void renderShadow(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer,
            @NonNull Shadowable model) {
        if (currentTexture != null) {
            Color.Linear c = new Color.Linear(color.r(), color.g(), color.b(),
                    color.a() * model.getShadowOpacity());
            renderShadow(context, renderer, model.getPositionX(), model.getPositionY(), model.getShadowDiameter(), c,
                    model.getShadowVerticalCenter());
        }
    }

    protected final void renderShadow(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer,
            float f_x, float f_y, float shadow_size) {
        renderShadow(context, renderer, f_x, f_y, shadow_size, color, 0.6f);
    }

    protected final void renderShadow(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer,
            float f_x, float f_y, float shadow_size, Color.@NonNull Linear color, float shadowOffsetScale) {
        if (currentTexture != null) {
            // Expand the quad for radial halos to provide room for the offset shadow blob and animation padding.
            float size = decalRenderer.isRadial() ? shadow_size * 2.5f : shadow_size;
            decalRenderer.draw(context, currentTexture, f_x, f_y, size, color, pattern, shadowOffsetScale);
        }
    }

    public void close() {
        decalRenderer.close();
    }
}
