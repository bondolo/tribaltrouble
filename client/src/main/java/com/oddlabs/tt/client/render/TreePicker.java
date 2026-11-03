package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.SpriteList;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.engine.resource.SpriteFile;
import com.oddlabs.tt.simulation.landscape.AbstractTreeGroup;
import com.oddlabs.tt.simulation.landscape.TreeGroup;
import com.oddlabs.tt.simulation.landscape.TreeLeaf;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static com.oddlabs.tt.simulation.landscape.AbstractTreeGroup.TreeType;

/**
 * Manages the culling, level-of-detail selection, and picking of trees.
 */
sealed class TreePicker permits TreeRenderer {
    private static final int CROWN_MIPMAP_CUTOFF = RenderConfig.NO_MIPMAP_CUTOFF;
    private static final float SELECTION_RADIUS = 1.5f;

    private final EnumMap<TreeType, List<TreeSupply>> render_lists = createTreeListMap();
    private final EnumMap<TreeType, List<TreeSupply>> respond_render_lists = createTreeListMap();
    private final Map<TreeType, Tree> trees = loadTrees();
    private final IdentityHashMap<TreeSupply, TreeRenderState> render_states = new IdentityHashMap<>();
    private final RespondManager respond_manager;
    private @Nullable CameraState camera;

    TreePicker(RespondManager respond_manager) {
        this.respond_manager = respond_manager;
    }

    private static EnumMap<TreeType, List<TreeSupply>> createTreeListMap() {
        var map = new EnumMap<TreeType, List<TreeSupply>>(TreeType.class);
        for (TreeType type : TreeType.values()) {
            map.put(type, new ArrayList<>());
        }
        return map;
    }

    private static Map<TreeType, Tree> loadTrees() {
        SpriteList jungle_crown = Resources.findResource(new SpriteFile("/geometry/misc/jungle_tree_crown.binsprite",
                CROWN_MIPMAP_CUTOFF, false, false, true, false, true));
        SpriteList jungle_trunk = Resources.findResource(new SpriteFile("/geometry/misc/jungle_tree_trunk.binsprite",
                CROWN_MIPMAP_CUTOFF, true, true, true, false));

        SpriteList palm_crown = Resources.findResource(new SpriteFile("/geometry/misc/palm_crown.binsprite",
                CROWN_MIPMAP_CUTOFF, false, false, true, false, true));
        SpriteList palm_trunk = Resources.findResource(new SpriteFile("/geometry/misc/palm_trunk.binsprite",
                CROWN_MIPMAP_CUTOFF, true, true, true, false));

        SpriteList oak_crown = Resources.findResource(new SpriteFile("/geometry/misc/oak_tree_crown.binsprite",
                CROWN_MIPMAP_CUTOFF, false, false, true, false, true));
        SpriteList oak_trunk = Resources.findResource(new SpriteFile("/geometry/misc/oak_tree_trunk.binsprite",
                CROWN_MIPMAP_CUTOFF, true, true, true, false));

        SpriteList pine_crown = Resources.findResource(new SpriteFile("/geometry/misc/pine_tree_crown.binsprite",
                CROWN_MIPMAP_CUTOFF, false, false, true, false, true));
        SpriteList pine_trunk = Resources.findResource(new SpriteFile("/geometry/misc/pine_tree_trunk.binsprite",
                CROWN_MIPMAP_CUTOFF, true, true, true, false));

        var trees = new EnumMap<TreeType, Tree>(TreeType.class);
        trees.put(TreeType.JUNGLE, new Tree(jungle_trunk, jungle_crown, 16.0f, 0.5f, 0.6f, 0.9f));
        trees.put(TreeType.PALM, new Tree(palm_trunk, palm_crown, 20.0f, 0.3f, 1.0f, 0.95f));
        trees.put(TreeType.OAK, new Tree(oak_trunk, oak_crown, 20.0f, 0.5f, 0.6f, 0.7f));
        trees.put(TreeType.PINE, new Tree(pine_trunk, pine_crown, 12.0f, 0.6f, 0.3f, 0.65f));
        return Collections.unmodifiableMap(trees);
    }

    final void initTreeBounds(AbstractTreeGroup root) {
        populateStatesRecursive(root);
    }

    private void populateStatesRecursive(AbstractTreeGroup node) {
        switch (node) {
            case TreeGroup group -> {
                for (AbstractTreeGroup child : group.children()) {
                    populateStatesRecursive(child);
                }
            }
            case TreeLeaf leaf -> {
                for (TreeSupply tree : leaf.getTrees()) {
                    getOrCreateState(tree);
                }
            }
            case TreeSupply tree -> getOrCreateState(tree);
        }
    }

    final TreeRenderState getOrCreateState(TreeSupply tree) {
        return render_states.computeIfAbsent(tree, this::createRenderState);
    }

    private TreeRenderState createRenderState(TreeSupply tree) {
        Tree visual = trees.get(tree.getTreeType());
        BoundingBox visualBounds = new BoundingBox();
        if (visual != null) {
            visualBounds.setTransformedBounds(visual.modelBounds(), tree.getMatrix());
        }
        return new TreeRenderState(tree, new Matrix4f(tree.getMatrix()), visualBounds);
    }

    final Map<TreeType, Tree> getTrees() {
        return trees;
    }

    final EnumMap<TreeType, List<TreeSupply>> getRenderLists() {
        return render_lists;
    }

    final EnumMap<TreeType, List<TreeSupply>> getRespondRenderLists() {
        return respond_render_lists;
    }

    final void getAllPicks(List<TreeSupply> pick_list) {
        for (List<TreeSupply> list : render_lists.values()) {
            pick_list.addAll(list);
            list.clear();
        }
        for (List<TreeSupply> list : respond_render_lists.values()) {
            pick_list.addAll(list);
            list.clear();
        }
    }

    private void addToRenderList(TreeType type, TreeSupply tree, boolean respond) {
        (respond ? respond_render_lists : render_lists).get(type).add(tree);
    }

    final void setup(CameraState camera_state) {
        this.camera = camera_state;
    }

    final void visit(AbstractTreeGroup node) {
        visit(node, camera != null && camera.inNoDetailMode());
    }

    private void visit(AbstractTreeGroup node, boolean fullyInside) {
        boolean childInside = fullyInside;
        if (!fullyInside && camera != null) {
            int res = node.intersectFrustum(camera.getFrustum());
            if (res >= 0) {
                return;
            }
            childInside = (res == FrustumIntersection.INSIDE);
        }

        switch (node) {
            case TreeGroup group -> {
                for (AbstractTreeGroup child : group.children()) {
                    visit(child, childInside);
                }
            }
            case TreeLeaf leaf -> {
                for (TreeSupply tree : leaf.getTrees()) {
                    visitTree(tree, childInside);
                }
            }
            case TreeSupply tree -> visitTree(tree, childInside);
        }
    }

    private boolean pickingInFrustum(TreeRenderState state, FrustumIntersection frustum) {
        TreeSupply tree_supply = state.tree();
        float heightScale = trees.get(tree_supply.getTreeType()).heightScale();
        float posX = tree_supply.getPositionX();
        float posY = tree_supply.getPositionY();
        BoundingBox bounds = state.visualBounds();
        return frustum.testAab(
                posX - SELECTION_RADIUS,
                posY - SELECTION_RADIUS,
                bounds.bmin_z,
                posX + SELECTION_RADIUS,
                posY + SELECTION_RADIUS,
                bounds.bmin_z + (bounds.bmax_z - bounds.bmin_z) * heightScale);
    }

    private void visitTree(TreeSupply tree_supply, boolean fullyInside) {
        if (tree_supply.isEmpty() && !isFalling(tree_supply))
            return;

        TreeRenderState state = getOrCreateState(tree_supply);
        boolean in_view;
        if (fullyInside) {
            in_view = !isPicking() || !tree_supply.isDead();
        } else if (camera == null) {
            in_view = false;
        } else if (isPicking()) {
            in_view = !tree_supply.isDead() && pickingInFrustum(state, camera.getFrustum());
        } else {
            in_view = state.visualBounds().testFrustum(camera.getFrustum());
        }
        if (in_view && camera != null) {
            addToRenderList(tree_supply.getTreeType(), tree_supply, respond_manager.isResponding(tree_supply));
        }
    }

    protected boolean isFalling(TreeSupply tree_supply) {
        return false;
    }

    boolean isPicking() {
        return true;
    }

    final @Nullable CameraState getCamera() {
        return camera;
    }
}
