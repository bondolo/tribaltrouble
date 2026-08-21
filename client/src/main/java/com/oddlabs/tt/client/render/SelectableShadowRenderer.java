package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.procedural.GeneratorHalos;
import com.oddlabs.tt.engine.render.LandscapeRenderer;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.ModelState;
import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.RenderQueues;
import com.oddlabs.tt.engine.render.ShadowListRenderer;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Shadowable;
import com.oddlabs.util.Color;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Specialized renderer that handles drawing halos for selected units and regular shadows for other entities.
 */
public final class SelectableShadowRenderer extends ShadowListRenderer {
    private final EnumMap<GeneratorHalos.HaloType, Texture> halos = new EnumMap<>(
            GeneratorHalos.HaloType.class);

    private final Deque<ModelState<?>> selection_list = new ArrayDeque<>();
    private final Deque<Shadowable> shadowed_list = new ArrayDeque<>();

    public SelectableShadowRenderer(Supplier<Texture[]> halos_desc) {
        Texture[] textures = Resources.findResource(halos_desc);
        for (int i = 0; i < textures.length; i++) {
            Texture halo = textures[i];
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, halo.getHandle());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            halos.put(GeneratorHalos.HaloType.values()[i], halo);
        }
        setRadial(true);
    }

    public void addToSelectionList(ModelState<?> modelState) {
        if (DebugFlags.process_shadows) {
            selection_list.add(modelState);
        }
    }

    public void addToShadowList(ModelState<?> modelState) {
        if (DebugFlags.process_shadows) {
            var model = modelState.getModel();
            if (null != model) {
                shadowed_list.add(model);
            }
        }
    }

    public void addToShadowList(Collection<? extends Shadowable> shadowable) {
        if (DebugFlags.process_shadows) {
            shadowed_list.addAll(shadowable);
        }
    }

    @Override
    public void renderShadows(RenderContext context, RenderQueues queues,
            LandscapeRenderer renderer, MatrixStack modelViewStack,
            MatrixStack projectionStack) {
        try (var _ = setupShadows(context, queues, renderer, modelViewStack, projectionStack)) {
            setShadowColor(Color.Linear.WHITE);
            setPattern(Selectable.VisualPattern.NONE);
            bindShadowTexture(halos.get(GeneratorHalos.HaloType.SHADOWED));
            while (!shadowed_list.isEmpty()) {
                var model = shadowed_list.pop();
                renderShadow(context, renderer, model);
            }

            bindShadowTexture(halos.get(GeneratorHalos.HaloType.SELECTED));
            while (!selection_list.isEmpty()) {
                var modelState = selection_list.pop();
                var model = Objects.requireNonNull(modelState.getModel());
                setShadowColor(modelState.getSelectionColor());
                setPattern(modelState.getPattern());

                renderShadow(context, renderer, model);
            }
        }
    }
}
