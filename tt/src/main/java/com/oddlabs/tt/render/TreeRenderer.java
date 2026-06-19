package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.model.AbstractTreeGroup;
import com.oddlabs.tt.model.TreeSupply;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.tt.viewer.Cheat;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Specialized renderer for forest elements, coordinating the efficient
 * drawing of crown and trunk sprite lists using hardware instancing.
 */
public final class TreeRenderer extends TreePicker implements AutoCloseable, SceneRenderer, Animated {
    private static final Logger logger = Logger.getLogger(TreeRenderer.class.getName());
    private final @NonNull InstancedSpriteRenderer instancedSpriteRenderer;
    private final WaveAnimation wave_animation = new WaveAnimation();
    private final @Nullable Cheat cheat;
    private final Matrix4f tempMatrix = new Matrix4f();
    private final @NonNull AnimationManager animation_manager;
    private final Map<TreeSupply, TreeVisualState> active_animations = new HashMap<>();

    TreeRenderer(@Nullable Cheat cheat, @NonNull SpriteSorter sprite_sorter, @NonNull RespondManager respond_manager,
            @NonNull InstancedSpriteRenderer instancedSpriteRenderer, @NonNull AnimationManager animation_manager
    ) {
        super(sprite_sorter, respond_manager);
        this.cheat = cheat;
        this.instancedSpriteRenderer = instancedSpriteRenderer;
        this.animation_manager = animation_manager;
        this.animation_manager.registerAnimation(this);
    }

    void renderShadows(@NonNull SelectableShadowRenderer shadowRenderer) {
        for (List<TreeSupply> list : getRenderLists()) {
            List<Shadowable> shadowables = list.stream().<Shadowable>map(tree -> new Shadowable() {
                @Override
                public float getPositionX() {
                    return tree.getPositionX();
                }

                @Override
                public float getPositionY() {
                    return tree.getPositionY();
                }

                @Override
                public float getShadowDiameter() {
                    return TreeRenderer.this.getShadowDiameter(tree);
                }

                @Override
                public float getShadowOpacity() {
                    return TreeRenderer.this.getShadowOpacity(tree);
                }

                @Override
                public float getShadowVerticalCenter() {
                    return tree.getTreeType().shadowVerticalCenter;
                }
            }).toList();
            shadowRenderer.addToShadowList(shadowables);
        }
    }

    @Override
    public void render(@NonNull RenderContext context, @NonNull CameraState state, @NonNull MatrixStack modelViewStack,
            @NonNull MatrixStack projectionStack) {
        if (!state.inNoDetailMode()) {
            wave_animation.setTime(Renderer.getRenderer().getEventQueue().getTime());
        }

        if (!Globals.draw_trees || (cheat != null && !cheat.draw_trees)) {
            // Just clear lists if not drawing
            clearLists();
            return;
        }

        List<TreeSupply>[] render_lists = getRenderLists();
        List<TreeSupply>[] respond_render_lists = getRespondRenderLists();

        AbstractTreeGroup.TreeType[] ordinals = AbstractTreeGroup.TreeType.values();

        for (int i = 0; i < render_lists.length; i++) {
            renderList(getTrees().get(ordinals[i]), render_lists[i], false);
        }
        for (int i = 0; i < respond_render_lists.length; i++) {
            if (!respond_render_lists[i].isEmpty())
                renderList(getTrees().get(ordinals[i]), respond_render_lists[i], true);
        }
    }

    private void clearLists() {
        for (List<TreeSupply> list : getRenderLists()) list.clear();
        for (List<TreeSupply> list : getRespondRenderLists()) list.clear();
    }

    private void prepareMatrix(@NonNull TreeSupply tree) {
        tempMatrix.set(tree.getMatrix());
        TreeVisualState state = active_animations.get(tree);
        if (state != null) {
            if (!state.spawning) {
                float time = state.getTreeFallProgress();
                tempMatrix.translate(0f, 0f, -13f * (time * time * time * time * time * time));
                tempMatrix.rotate((float) Math.toRadians(90f * time * time), 1f, 0f, 0f);
            } else {
                float scale = state.scale;
                float zScale = (float) Math.log(scale * (Math.E - 1.0) + 1.0);
                tempMatrix.scale(scale, scale, zScale);
                wave_animation.mulRotation(tempMatrix);
            }
        } else {
            if (tree.isEmpty()) {
                tempMatrix.translate(0f, 0f, -13f);
                tempMatrix.rotate((float) Math.toRadians(90f), 1f, 0f, 0f);
            } else {
                wave_animation.mulRotation(tempMatrix);
            }
        }
    }

    private void renderList(@NonNull Tree tree, @NonNull List<TreeSupply> render_list, boolean respond) {
        SpriteList crownList = tree.crown();
        SpriteList trunkList = tree.trunk();

        Sprite crownSprite = crownList.getSprite(0);
        Texture crownTexture = crownSprite.textures[0][Sprite.TEXTURE_NORMAL];
        Texture crownTeam = crownSprite.textures[0][Sprite.TEXTURE_TEAM];
        Texture crownBump = crownSprite.hasBumpMap(0) ? crownSprite.textures[0][Sprite.TEXTURE_BUMP] : null;

        Sprite trunkSprite = trunkList.getSprite(0);
        Texture trunkTexture = trunkSprite.textures[0][Sprite.TEXTURE_NORMAL];
        Texture trunkTeam = trunkSprite.textures[0][Sprite.TEXTURE_TEAM];
        Texture trunkBump = trunkSprite.hasBumpMap(0) ? trunkSprite.textures[0][Sprite.TEXTURE_BUMP] : null;

        for (TreeSupply supply : render_list) {
            prepareMatrix(supply);
            // Render Crown (Sprite 0). Blend = false, DepthWrite = true for opaque trees.
            instancedSpriteRenderer.add(crownList, 0, 0, 0f, crownTexture, crownTeam, crownBump, respond, false,
                    true, true, tempMatrix, Color.Standard.WHITE, Color.Standard.WHITE);
            // Render Trunk (Sprite 0). Blend = false, DepthWrite = true.
            instancedSpriteRenderer.add(trunkList, 0, 0, 0f, trunkTexture, trunkTeam, trunkBump, respond, false,
                    true, true, tempMatrix, Color.Standard.WHITE, Color.Standard.WHITE);
        }
        render_list.clear();
    }

    void debugRender(@NonNull List<TreeSupply> @NonNull [] render_lists, @NonNull List<
            TreeSupply> @NonNull [] respond_render_lists) {
        if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
            for (List<TreeSupply> render_list : render_lists) {
                for (TreeSupply group : render_list) {
                    RenderTools.draw(group);
                }
            }
            for (List<TreeSupply> respond_render_list : respond_render_lists) {
                for (TreeSupply group : respond_render_list) {
                    RenderTools.draw(group);
                }
            }
        }
    }

    @Override
    boolean isPicking() {
        return false;
    }

    @Override
    protected boolean isHidden(@NonNull TreeSupply tree_supply) {
        TreeVisualState state = active_animations.get(tree_supply);
        return state != null && state.hide;
    }

    @Override
    public void animate(float t) {
        Iterator<Map.Entry<TreeSupply, TreeVisualState>> it = active_animations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<TreeSupply, TreeVisualState> entry = it.next();
            TreeVisualState state = entry.getValue();
            state.animate(t);
            if (state.spawning && state.animation_time >= 3.0f) {
                it.remove();
            }
        }
    }

    public void onTreeCutDown(@NonNull TreeSupply tree) {
        active_animations.put(tree, new TreeVisualState(false, 0f));
        tree.getWorld().getAudio().newAudio(tree.getCX(), tree.getCY(), tree.getCZ(),
                AudioAssets.TREE_FALL[tree.getTreeType().ordinal() % 2]);
    }

    public void onTreeRespawned(@NonNull TreeSupply tree) {
        active_animations.put(tree, new TreeVisualState(true, 0f));
    }

    private float getShadowDiameter(@NonNull TreeSupply tree) {
        TreeVisualState state = active_animations.get(tree);
        boolean hide = state != null && state.hide;
        float scale = state != null ? state.scale : 1.0f;
        float progress = state != null ? state.getTreeFallProgress() : 0.0f;

        float base_diameter = hide ? 0f : (tree.getTreeType().shadowDiameter * scale);
        return tree.isEmpty() ? base_diameter * Math.max(0f, 1f - progress) : base_diameter;
    }

    private float getShadowOpacity(@NonNull TreeSupply tree) {
        TreeVisualState state = active_animations.get(tree);
        boolean hide = state != null && state.hide;
        float progress = state != null ? state.getTreeFallProgress() : 0.0f;

        float base_opacity = hide ? 0f : tree.getTreeType().shadowOpacity;
        return tree.isEmpty() ? base_opacity * (1.0f + 0.3f * progress) : base_opacity;
    }

    @Override
    public void close() {
        animation_manager.removeAnimation(this);
        active_animations.clear();
    }

    private static final class TreeVisualState {
        final boolean spawning;
        float animation_time;
        float scale;
        boolean hide = false;

        TreeVisualState(boolean spawning, float animation_time) {
            this.spawning = spawning;
            this.animation_time = animation_time;
            this.scale = spawning ? 0.0f : 1.0f;
        }

        void animate(float t) {
            if (spawning) {
                animation_time += t;
                float progress = Math.min(1.0f, animation_time / 3.0f);
                float inv = 1f - progress;
                scale = 1f - inv * inv * inv * inv * inv * inv;
                if (progress >= 1.0f) {
                    scale = 1f;
                }
            } else {
                animation_time += t;
                if (animation_time >= 3.0f) { // SECOND_PER_TREEFALL
                    hide = true;
                }
            }
        }

        float getTreeFallProgress() {
            return animation_time / 3.0f;
        }
    }
}
