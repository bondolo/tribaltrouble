package com.oddlabs.tt.engine.render;

import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the geometry and textures for a single sprite type.
 */
public final class SpriteRenderer {
    private final SpriteList sprite_list;
    private final List<ModelState<?>> render_list = new ArrayList<>();
    private final List<ModelState<?>> respond_render_list = new ArrayList<>();
    private final Texture texture;
    private final @Nullable Texture team_texture;
    private final @Nullable Texture bump_texture;
    private final InstancedSpriteRenderer instancedSpriteRenderer;
    private final Matrix4f tempMatrix = new Matrix4f();
    private final InstancedSpriteRenderer.RenderBatch opaqueBatch;
    private final InstancedSpriteRenderer.RenderBatch respondBatch;

    SpriteRenderer(SpriteList sprite_list, int tex_index,
            InstancedSpriteRenderer instancedSpriteRenderer) {
        this.sprite_list = sprite_list;
        this.instancedSpriteRenderer = instancedSpriteRenderer;

        Sprite sprite = sprite_list.getSprite();
        this.texture = sprite.textures[tex_index][Sprite.TEXTURE_NORMAL];
        this.team_texture = sprite.textures[tex_index][Sprite.TEXTURE_TEAM];
        this.bump_texture = sprite.hasBumpMap(tex_index)
                ? sprite.textures[tex_index][Sprite.TEXTURE_BUMP]
                : null;
        boolean modulate = sprite.modulateColor();
        this.opaqueBatch = instancedSpriteRenderer.getBatch(sprite_list, texture, team_texture, bump_texture, false,
                modulate, !modulate, true);
        this.respondBatch = instancedSpriteRenderer.getBatch(sprite_list, texture, team_texture, bump_texture, true,
                true, false, true);
    }

    SpriteRenderer(SpriteList sprite_list, Texture texture,
            InstancedSpriteRenderer instancedSpriteRenderer) {
        this.sprite_list = sprite_list;
        this.instancedSpriteRenderer = instancedSpriteRenderer;
        this.texture = texture;
        this.team_texture = null;
        this.bump_texture = null;
        boolean modulate = sprite_list.getSprite().modulateColor();
        this.opaqueBatch = instancedSpriteRenderer.getBatch(sprite_list, texture, team_texture, bump_texture, false,
                modulate, !modulate, true);
        this.respondBatch = instancedSpriteRenderer.getBatch(sprite_list, texture, team_texture, bump_texture, true,
                true, false, true);
    }

    public SpriteList getSpriteList() {
        return sprite_list;
    }

    public void addToRenderList(ModelState<?> model, boolean respond) {
        if (respond) {
            respond_render_list.add(model);
        } else {
            render_list.add(model);
        }
    }

    void renderAll() {
        if (!render_list.isEmpty()) {
            boolean skeletal = sprite_list.isSkeletal();
            for (ModelState<?> modelState : render_list) {
                if (DebugFlags.isBoundsEnabled(BoundingMode.PLAYERS)) {
                    RenderTools.draw(modelState.getModel());
                }
                int boneBaseOffset = skeletal
                        ? instancedSpriteRenderer.getOrEvaluateBoneOffset(sprite_list, modelState.getAnimation(),
                                modelState.getAnimationTicks())
                        : -1;
                opaqueBatch.addInstance(boneBaseOffset, modelState.getTransform(tempMatrix),
                        modelState.getColorR(), modelState.getColorG(), modelState.getColorB(), modelState.getColorA(),
                        modelState.getTeamColorR(), modelState.getTeamColorG(), modelState.getTeamColorB(), modelState
                                .getTeamColorA());
            }
            render_list.clear();
        }

        if (!respond_render_list.isEmpty()) {
            boolean skeletal = sprite_list.isSkeletal();
            for (ModelState<?> model : respond_render_list) {
                if (DebugFlags.isBoundsEnabled(BoundingMode.PLAYERS)) {
                    RenderTools.draw(model.getModel());
                }
                int boneBaseOffset = skeletal
                        ? instancedSpriteRenderer.getOrEvaluateBoneOffset(sprite_list, model.getAnimation(),
                                model.getAnimationTicks())
                        : -1;
                respondBatch.addInstance(boneBaseOffset, model.getTransform(tempMatrix),
                        1f, 1f, 1f, 1f,
                        model.getTeamColorR(), model.getTeamColorG(), model.getTeamColorB(), model.getTeamColorA());
            }
            respond_render_list.clear();
        }
    }

    public void addInstance(int animation, float animTicks,
            boolean respond, boolean blend, boolean depthWrite, boolean depthTest,
            Matrix4f modelMatrix, Color color, Color decalColor) {
        instancedSpriteRenderer.add(sprite_list, animation, animTicks,
                texture, team_texture, bump_texture,
                respond, blend, depthWrite, depthTest, modelMatrix, color, decalColor);
    }
}
