package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.LandscapeRenderer;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.ShadowListRenderer;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Renders target click response indicators on the landscape.
 */
public final class TargetRespondRenderer extends ShadowListRenderer {
    private static final float SHADOW_SIZE = 1.6f;
    private final Texture ring;

    private final Deque<@NonNull LandscapeTargetRespond> target_list = new ArrayDeque<>();

    public TargetRespondRenderer(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        ring = Resources.findResource(desc)[0];
        setRadial(true);
    }

    public void addToTargetList(@NonNull LandscapeTargetRespond target) {
        if (DebugFlags.process_shadows)
            target_list.push(target);
    }

    @Override
    public void renderShadows(@NonNull RenderContext context, @NonNull RenderQueues queues,
            @NonNull LandscapeRenderer renderer, @NonNull MatrixStack modelViewStack,
            @NonNull MatrixStack projectionStack) {
        if (target_list.isEmpty()) return;

        try (var _ = setupShadows(context, queues, renderer, modelViewStack, projectionStack)) {
            setShadowColor(Color.Linear.GREEN);
            setPattern(Selectable.VisualPattern.FRIENDLY);
            bindShadowTexture(ring);
            while (!target_list.isEmpty()) {
                var target = target_list.pop();
                renderShadow(context, renderer, target.getPositionX(), target
                        .getPositionY(), SHADOW_SIZE * target.getProgress()
                );
            }
        }
    }
}
