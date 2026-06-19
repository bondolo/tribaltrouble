package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Renders target click response indicators on the landscape.
 */
public final class TargetRespondRenderer extends ShadowListRenderer {
    private static final float SHADOW_SIZE = 1.6f;
    private final Texture ring;

    private final List<@NonNull ActiveTargetRespond> activeResponds = new CopyOnWriteArrayList<>();

    public TargetRespondRenderer(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        ring = Resources.findResource(desc)[0];
        setRadial(true);
    }

    public void addRespond(@NonNull ActiveTargetRespond respond) {
        activeResponds.add(respond);
    }

    public void removeRespond(@NonNull ActiveTargetRespond respond) {
        activeResponds.remove(respond);
    }

    @Override
    public void renderShadows(@NonNull RenderContext context, @NonNull RenderQueues queues,
            @NonNull LandscapeRenderer renderer, @NonNull MatrixStack modelViewStack,
            @NonNull MatrixStack projectionStack) {
        if (activeResponds.isEmpty()) return;

        try (var _ = setupShadows(context, queues, renderer, modelViewStack, projectionStack)) {
            setShadowColor(Color.Linear.BLUE);
            setPattern(Selectable.VisualPattern.FRIENDLY);
            bindShadowTexture(ring);
            for (ActiveTargetRespond target : activeResponds) {
                renderShadow(context, renderer, target.getX(), target.getY(), SHADOW_SIZE * target.getProgress());
            }
        }
    }
}
