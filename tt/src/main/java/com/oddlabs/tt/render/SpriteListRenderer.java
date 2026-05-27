package com.oddlabs.tt.render;


import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.util.Target;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Internal renderer that manages sprite list batches for a specific entity type.
 */
final class SpriteListRenderer {
    private final @NonNull SpriteList sprite_list;
    private final @NonNull List<@NonNull ModelState<?>> @NonNull [] render_lists;
    private final @NonNull List<@NonNull ModelState<?>> @NonNull [] respond_render_lists;
    private final @NonNull InstancedSpriteRenderer instancedSpriteRenderer;
    private final Matrix4f tempMatrix = new Matrix4f();

    @SuppressWarnings("unchecked")
    SpriteListRenderer(@NonNull SpriteList sprite_list, @NonNull InstancedSpriteRenderer instancedSpriteRenderer) {
        this.sprite_list = sprite_list;
        this.instancedSpriteRenderer = instancedSpriteRenderer;
        int num_sprites = sprite_list.getNumSprites();
        render_lists = (List<ModelState<?>>[]) new ArrayList<?>[num_sprites];
        respond_render_lists = (List<ModelState<?>>[]) new ArrayList[num_sprites];
        for (int i = 0; i < num_sprites; i++) {
            render_lists[i] = new ArrayList<>();
            respond_render_lists[i] = new ArrayList<>();
        }
    }

    public void addToRenderList(ModelState<?> model, int sprite_index) {
        render_lists[sprite_index].add(model);
    }

    public void addToRespondRenderList(ModelState<?> model, int sprite_index) {
        respond_render_lists[sprite_index].add(model);
    }

    public void getAllPicks(@NonNull Consumer<Target> picks, int sprite_index) {
        List<ModelState<?>> render_list = render_lists[sprite_index];
        pickFromList(render_list, picks);
        render_list.clear();

        render_list = respond_render_lists[sprite_index];
        pickFromList(render_list, picks);
        render_list.clear();
    }

    private void pickFromList(@NonNull List<@Nullable ModelState<?>> render_list, @NonNull Consumer<
            @NonNull Target> picks) {
        for (int i = 0; i < render_list.size(); i++) {
            ModelState<?> model = render_list.get(i);
            render_list.set(i, null);
            if (model.getModel() instanceof Target target) {
                picks.accept(target);
            }
        }
    }

    public void renderAll(int index, @NonNull Texture texture, @Nullable Texture teamTexture,
            @Nullable Texture bumpTexture) {
        List<ModelState<?>> render_list = render_lists[index];
        boolean modulate = sprite_list.getSprite(index).modulateColor();

        for (ModelState<?> modelState : render_list) {
            if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
                RenderTools.draw(modelState.getModel());
            }
            // Standard sprites: If modulate, use Blend. If opaque/alpha, use A2C (Blend=False).
            // Depth Write = !modulate (Opaque writes depth, Effects don't).
            instancedSpriteRenderer.add(sprite_list, index, modelState.getAnimation(),
                    modelState.getAnimationTicks(), texture, teamTexture, bumpTexture, false, modulate,
                    !modulate, true, modelState.getTransform(tempMatrix), modelState.getColor(), modelState
                            .getTeamColor());
        }
        render_list.clear();

        render_list = respond_render_lists[index];
        if (!render_list.isEmpty()) {
            for (ModelState<?> model : render_list) {
                // Respond color is usually white or specific, here using white as placeholder or model color if needed
                // Respond rendering usually highlights the unit.
                // Using white for color and decal color for now as per original logic which seemed to use default colors.

                if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
                    RenderTools.draw(model.getModel());
                }
                // Respond (Overlays) usually shouldn't write depth to avoid z-fighting with the unit itself
                // Let's assume No Depth Write for overlays is safer.
                instancedSpriteRenderer.add(sprite_list, index, model.getAnimation(),
                        model.getAnimationTicks(), texture, teamTexture, bumpTexture, true, true, false,
                        true, model.getTransform(tempMatrix), Color.Linear.WHITE, Color.Linear.WHITE);
            }
            render_list.clear();
        }
    }
}
