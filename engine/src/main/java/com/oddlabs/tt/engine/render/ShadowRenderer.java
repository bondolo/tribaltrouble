package com.oddlabs.tt.engine.render;


import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Shadowable;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.render.state.ScopedState;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

/**
 * Base class for rendering dynamic shadow decals for world objects.
 */
public abstract class ShadowRenderer {
    private Color.Linear color = Color.Linear.WHITE;
    private float patternVal = 0.0f;
    private @Nullable Texture currentTexture;
    private boolean radial = false;
    private @Nullable DecalRenderer sharedRenderer;

    public ScopedState setupShadows(RenderContext context, RenderQueues queues,
            float worldSize, Texture heightTexture, MatrixStack modelViewStack,
            MatrixStack projectionStack) {
        this.sharedRenderer = queues.getDecalRenderer();
        var decalState = sharedRenderer.setup(context, worldSize, heightTexture, modelViewStack, projectionStack);

        return () -> {
            decalState.close();
            this.sharedRenderer = null;
            this.currentTexture = null;
            this.patternVal = 0.0f;
        };
    }

    public void setShadowColor(Color color) {
        this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
    }

    public void setPattern(Selectable.VisualPattern pattern) {
        this.patternVal = (float) pattern.ordinal();
    }

    public void setPatternVal(float patternVal) {
        this.patternVal = patternVal;
    }

    public void bindShadowTexture(Texture texture) {
        this.currentTexture = texture;
    }

    public void setRadial(boolean radial) {
        this.radial = radial;
    }

    public final void renderShadow(RenderContext context, Shadowable model) {
        if (currentTexture != null && sharedRenderer != null) {
            renderShadow(context, model.getPositionX(), model.getPositionY(), model.getShadowDiameter(),
                    color, model.getShadowVerticalCenter(), model.getShadowOpacity());
        }
    }

    public final void renderShadow(RenderContext context, float f_x, float f_y, float shadow_size) {
        renderShadow(context, f_x, f_y, shadow_size, color, 0.6f, 1.0f);
    }

    public final void renderShadow(RenderContext context, float f_x, float f_y, float shadow_size,
            Color.Linear color, float shadowOffsetScale, float shadowOpacity) {
        if (currentTexture != null && sharedRenderer != null) {
            // Expand the quad for radial halos to provide room for the offset shadow blob and animation padding.
            float size = radial ? shadow_size * 2.5f : shadow_size;
            sharedRenderer.draw(context, currentTexture, f_x, f_y, size, color, patternVal, shadowOffsetScale, radial,
                    shadowOpacity);
        }
    }


    public void close() {
    }
}
