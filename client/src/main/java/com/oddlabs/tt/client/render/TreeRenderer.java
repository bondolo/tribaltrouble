package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.client.viewer.Cheat;
import com.oddlabs.tt.engine.render.BoundingMode;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.InstancedSpriteRenderer;
import com.oddlabs.tt.engine.render.MatrixStack;
import com.oddlabs.tt.engine.render.RenderTools;
import com.oddlabs.tt.engine.render.SceneRenderer;
import com.oddlabs.tt.engine.render.Sprite;
import com.oddlabs.tt.engine.render.SpriteList;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.WaveAnimation;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.simulation.landscape.AbstractTreeGroup;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import com.oddlabs.tt.simulation.model.Shadowable;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Specialized renderer for forest elements, coordinating the efficient
 * drawing of crown and trunk sprite lists using hardware instancing.
 */
final class TreeRenderer extends TreePicker implements AutoCloseable, SceneRenderer, Animated {
    private static final Logger logger = Logger.getLogger(TreeRenderer.class.getName());
    private static final float TREE_FALL_DURATION = 3f;
    private static final float TREE_SPAWN_DURATION = 3f;

    private final InstancedSpriteRenderer instancedSpriteRenderer;
    private final WaveAnimation wave_animation = new WaveAnimation();
    private final @Nullable Cheat cheat;
    private final Matrix4f tempMatrix = new Matrix4f();
    private final AnimationManager animationManager;
    private final Map<TreeSupply, Float> fallingTrees = new ConcurrentHashMap<>();
    private final Map<TreeSupply, Float> spawningTrees = new ConcurrentHashMap<>();

    TreeRenderer(@Nullable Cheat cheat, SpriteSorter sprite_sorter,
            RespondManager respond_manager,
            InstancedSpriteRenderer instancedSpriteRenderer,
            AnimationManager animationManager
    ) {
        super(sprite_sorter, respond_manager);
        this.cheat = cheat;
        this.instancedSpriteRenderer = instancedSpriteRenderer;
        this.animationManager = animationManager;
        animationManager.registerAnimation(this);
    }

    void onTreeFelled(TreeSupply tree) {
        spawningTrees.remove(tree);
        fallingTrees.put(tree, 0f);
    }

    void onTreeSpawned(TreeSupply tree) {
        fallingTrees.remove(tree);
        spawningTrees.put(tree, 0f);
    }

    @Override
    protected boolean isFalling(TreeSupply tree_supply) {
        return fallingTrees.containsKey(tree_supply);
    }

    @Override
    public void animate(float dt) {
        if (!fallingTrees.isEmpty()) {
            for (var entry : fallingTrees.entrySet()) {
                float progress = entry.getValue() + dt / TREE_FALL_DURATION;
                if (!entry.getKey().isEmpty() || progress >= 1.0f) {
                    fallingTrees.remove(entry.getKey());
                } else {
                    entry.setValue(progress);
                }
            }
        }
        if (!spawningTrees.isEmpty()) {
            for (var entry : spawningTrees.entrySet()) {
                float progress = entry.getValue() + dt / TREE_SPAWN_DURATION;
                if (entry.getKey().isEmpty() || progress >= 1.0f) {
                    spawningTrees.remove(entry.getKey());
                } else {
                    entry.setValue(progress);
                }
            }
        }
    }

    public void renderShadows(SelectableShadowRenderer shadowRenderer) {
        for (List<TreeSupply> list : getRenderLists()) {
            for (TreeSupply tree : list) {
                Tree visual = getTrees().get(tree.getTreeType());
                Float fallProgress = fallingTrees.get(tree);
                if (fallProgress != null) {
                    float scale = Math.max(0f, 1f - fallProgress);
                    float opacity = 1.0f + 0.3f * fallProgress;
                    shadowRenderer.addToShadowList(new TreeShadow(tree, visual, scale, opacity));
                } else {
                    Float spawnProgress = spawningTrees.get(tree);
                    if (spawnProgress != null) {
                        float inv = 1f - spawnProgress;
                        float scale = 1f - inv * inv * inv * inv * inv * inv;
                        shadowRenderer.addToShadowList(new TreeShadow(tree, visual, scale, 1.0f));
                    } else {
                        shadowRenderer.addToShadowList(new TreeShadow(tree, visual, 1.0f, 1.0f));
                    }
                }
            }
        }
    }

    private record TreeShadow(TreeSupply tree, Tree visual, float scale, float opacityMultiplier) implements
            Shadowable {
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
            return visual.shadowDiameter() * scale;
        }

        @Override
        public float getShadowVerticalCenter() {
            return visual.shadowVerticalCenter();
        }

        @Override
        public float getShadowOpacity() {
            return visual.shadowOpacity() * opacityMultiplier;
        }
    }

    public void render(RenderContext context, CameraState state, MatrixStack modelViewStack,
            MatrixStack projectionStack, float currentTime) {
        if (!state.inNoDetailMode()) {
            wave_animation.setTime(currentTime);
        }

        if (!DebugFlags.draw_trees || (cheat != null && !cheat.draw_trees)) {
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

    private void prepareMatrix(TreeSupply tree) {
        tempMatrix.set(tree.getMatrix());
        Float fallProgress = fallingTrees.get(tree);
        if (fallProgress != null) {
            float time = fallProgress;
            tempMatrix.translate(0f, 0f, -13f * (time * time * time * time * time * time));
            tempMatrix.rotate((float) Math.toRadians(90f * time * time), 1f, 0f, 0f);
        } else {
            Float spawnProgress = spawningTrees.get(tree);
            if (spawnProgress != null) {
                float inv = 1f - spawnProgress;
                float scale = 1f - inv * inv * inv * inv * inv * inv;
                float zScale = (float) Math.log(scale * (Math.E - 1.0) + 1.0);
                tempMatrix.scale(scale, scale, zScale);
            }
            wave_animation.mulRotation(tempMatrix);
        }
    }

    private void renderList(Tree tree, List<TreeSupply> render_list, boolean respond) {
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

    public void debugRender(List<TreeSupply>[] render_lists, List<
            TreeSupply>[] respond_render_lists) {
        if (DebugFlags.isBoundsEnabled(BoundingMode.PLAYERS)) {
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
    public void render(RenderContext context, CameraState state, MatrixStack modelViewStack,
            MatrixStack projectionStack) {
        render(context, state, modelViewStack, projectionStack, 0f);
    }

    @Override
    boolean isPicking() {
        return false;
    }

    @Override
    public void close() {
        animationManager.removeAnimation(this);
        fallingTrees.clear();
        spawningTrees.clear();
    }
}
