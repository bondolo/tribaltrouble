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
 * Top-level renderer for non-instanced 3D sprites, handling simple world elements.
 */
public final class SpriteRenderer {
    private final @NonNull SpriteList sprite_list;
    private final @NonNull SpriteListRenderer sprite_list_renderer;
    private final @NonNull Texture @NonNull [] textures;
    private final @Nullable Texture @NonNull [] team_textures;
    private final @Nullable Texture @NonNull [] bump_textures;
    private final List<@NonNull ModelState<?>> no_detail_render_list = new ArrayList<>();
    private final @NonNull InstancedSpriteRenderer instancedSpriteRenderer;
    private final Matrix4f tempMatrix = new Matrix4f();

    public SpriteRenderer(@NonNull SpriteList sprite_list, @NonNull Texture @NonNull [] textures,
            @Nullable Texture @NonNull [] team_textures, @Nullable Texture @NonNull [] bump_textures,
            @NonNull InstancedSpriteRenderer spriteRenderer) {
        this.sprite_list = sprite_list;
        this.textures = textures;
        this.team_textures = team_textures;
        this.bump_textures = bump_textures;
        this.instancedSpriteRenderer = spriteRenderer;
        sprite_list_renderer = new SpriteListRenderer(sprite_list, spriteRenderer);
    }

    public @NonNull SpriteList getSpriteList() {
        return sprite_list;
    }

    void addToNoDetailList(@NonNull ModelState<?> model) {
        no_detail_render_list.add(model);
    }

    void addToRenderList(@NonNull PolyDetail detail, ModelState<?> model, boolean respond) {
        int index = detail.ordinal();
        index = Math.min(sprite_list.getNumSprites() - 1, index);
        if (respond) {
            sprite_list_renderer.addToRespondRenderList(model, index);
        } else {
            sprite_list_renderer.addToRenderList(model, index);
        }
    }

    int getTriangleCount(@NonNull PolyDetail detail) {
        int index = detail.ordinal();
        index = Math.min(sprite_list.getNumSprites() - 1, index);
        return sprite_list.getSprite(index).getTriangleCount();
    }

    private void clearRenderLists() {
        no_detail_render_list.clear();
    }

    public void getAllPicks(@NonNull Consumer<@NonNull Target> picks) {
        for (int i = 0; i < sprite_list.getNumSprites(); i++) {
            sprite_list_renderer.getAllPicks(picks, i);
        }
        for (ModelState<?> model : no_detail_render_list) {
            picks.accept((Target) model.getModel());
        }
        clearRenderLists();
    }

    public void renderAll() {
        for (int i = 0; i < sprite_list.getNumSprites(); i++) {
            sprite_list_renderer.renderAll(i, textures[i], team_textures[i], bump_textures[i]);
        }
    }

    public void renderNoDetail() {
        if (Globals.draw_misc && !no_detail_render_list.isEmpty()) {
            SpriteList quadList = SpriteList.getQuadInstance();
            for (var model : no_detail_render_list) {
                if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
                    RenderTools.draw(model.getModel());
                }
                float x = model.getModel().getPositionX();
                float y = model.getModel().getPositionY();
                float z = model.getModel().getPositionZ();
                float r = model.getModel().getNoDetailSize();
                tempMatrix.identity().translation(x, y, z + 0.1f).scale(r * 2);
                // Quads don't have animation, so pass 0, 0f
                // Disable depth test for no-detail sprites (overlays). Enable blend. No Depth Write.
                instancedSpriteRenderer.add(quadList, 0, 0, 0f, instancedSpriteRenderer.getWhiteTexture(), null,
                        null, false, true, false, false, tempMatrix, model.getTeamColor(), Color.Linear.TRANSPARENT);
            }
        }
        clearRenderLists();
    }
}
