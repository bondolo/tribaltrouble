package com.oddlabs.tt.effects.render;


import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.LandscapeRenderer;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.ShadowListRenderer;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Shadowable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Renders ground-crack decals for supply impact events using the decal instancing pipeline.
 * Crack texture and per-instance colour are driven by the owning {@link com.oddlabs.tt.simulation.model.SupplyModel}.
 */
public final class CrackDecalRenderer extends ShadowListRenderer {
    private final @NonNull Texture crackTexture;
    private final Deque<@NonNull Shadowable> crack_list = new ArrayDeque<>();

    public CrackDecalRenderer(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        crackTexture = Resources.findResource(desc)[0];
        setRadial(false);
    }

    public void addToCrackList(@NonNull Shadowable shadowable) {
        if (DebugFlags.process_shadows) {
            crack_list.add(shadowable);
        }
    }

    @Override
    public void renderShadows(@NonNull RenderContext context, @NonNull RenderQueues queues,
            @NonNull LandscapeRenderer renderer, @NonNull MatrixStack modelViewStack,
            @NonNull MatrixStack projectionStack) {
        if (crack_list.isEmpty()) {
            return;
        }

        try (var _ = setupShadows(context, queues, renderer, modelViewStack, projectionStack)) {
            setPattern(Selectable.VisualPattern.NONE);
            bindShadowTexture(crackTexture);
            while (!crack_list.isEmpty()) {
                var model = crack_list.pop();
                setShadowColor(model.getShadowColor());
                setPatternVal(model.getShadowPattern());
                renderShadow(context, renderer, model);
            }
        }
    }
}
