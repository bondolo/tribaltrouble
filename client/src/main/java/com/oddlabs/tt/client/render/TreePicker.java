package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.LODObject;
import com.oddlabs.tt.engine.render.PolyDetail;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.RenderTools;
import com.oddlabs.tt.engine.render.SpriteList;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.engine.resource.SpriteFile;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.simulation.landscape.AbstractTreeGroup;
import com.oddlabs.tt.simulation.landscape.TreeGroup;
import com.oddlabs.tt.simulation.landscape.TreeLeaf;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
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
    private final BoundingBox picking_selection_box = new BoundingBox();
    private final RenderStateCache<TreeRenderState> render_state_cache
            = new RenderStateCache<>(() -> new TreeRenderState(TreePicker.this));
    private final Map<TreeType, Tree> trees = loadTrees();
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

    static void initTreeBounds(AbstractTreeGroup root, Map<TreeType, Tree> visuals) {
        updateSuppliesRecursive(root, visuals);
        root.initBounds();
    }

    private static void updateSuppliesRecursive(AbstractTreeGroup node, Map<TreeType, Tree> visuals) {
        switch (node) {
            case TreeGroup group -> {
                for (AbstractTreeGroup child : group.children()) {
                    updateSuppliesRecursive(child, visuals);
                }
            }
            case TreeLeaf leaf -> {
                for (TreeSupply tree : leaf.getTrees()) {
                    updateTreeBounds(tree, visuals);
                }
            }
            case TreeSupply tree -> updateTreeBounds(tree, visuals);
        }
    }

    private static void updateTreeBounds(TreeSupply tree, Map<TreeType, Tree> visuals) {
        Tree visual = visuals.get(tree.getTreeType());
        if (visual != null) {
            tree.updateBounds(visual.modelBounds());
        }
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

    private void addToHighDetailList(TreeType type, TreeSupply tree, boolean respond) {
        (respond ? respond_render_lists : render_lists).get(type).add(tree);
    }

    final void markDetailPolygon(TreeSupply tree_supply, PolyDetail level) {
        // Always render high detail (Instanced Sprites)
        addToHighDetailList(tree_supply.getTreeType(), tree_supply, respond_manager.isResponding(
                tree_supply));
    }

    final void setup(CameraState camera_state) {
        this.camera = camera_state;
        render_state_cache.clear();
    }

    final void visit(AbstractTreeGroup node) {
        visit(node, camera != null && camera.inNoDetailMode() ? RenderTools.FRUSTUM_INSIDE
                : RenderTools.ALL_PLANES_MASK);
    }

    private void visit(AbstractTreeGroup node, int planeMask) {
        int nextPlaneMask = planeMask;
        if (planeMask != RenderTools.FRUSTUM_INSIDE && camera != null) {
            nextPlaneMask = RenderTools.testFrustum(node, camera.getFrustum(), planeMask);
            if (nextPlaneMask == RenderTools.FRUSTUM_OUTSIDE) {
                return;
            }
        }

        switch (node) {
            case TreeGroup group -> {
                for (AbstractTreeGroup child : group.children()) {
                    visit(child, nextPlaneMask);
                }
            }
            case TreeLeaf leaf -> {
                for (TreeSupply tree : leaf.getTrees()) {
                    visitTree(tree, nextPlaneMask);
                }
            }
            case TreeSupply tree -> visitTree(tree, nextPlaneMask);
        }
    }

    private boolean pickingInFrustum(TreeSupply tree_supply, float[][] frustum, int planeMask) {
        float heightScale = trees.get(tree_supply.getTreeType()).heightScale();
        picking_selection_box.setBounds(-SELECTION_RADIUS + tree_supply.getPositionX(),
                SELECTION_RADIUS + tree_supply.getPositionX(),
                -SELECTION_RADIUS + tree_supply.getPositionY(),
                SELECTION_RADIUS + tree_supply.getPositionY(),
                tree_supply.bmin_z,
                tree_supply.bmin_z + (tree_supply.bmax_z - tree_supply.bmin_z) * heightScale);
        return RenderTools.testFrustum(picking_selection_box, frustum, planeMask) != RenderTools.FRUSTUM_OUTSIDE;
    }

    private void addToRenderList(TreeSupply tree, CameraState camera) {
        markDetailPolygon(tree, PolyDetail.HIGH_POLY);
    }

    private LODObject getRenderState(TreeSupply tree_supply) {
        TreeRenderState render_state = render_state_cache.get();
        render_state.setup(tree_supply);
        return render_state;
    }

    private void visitTree(TreeSupply tree_supply, int planeMask) {
        if (tree_supply.isEmpty() && !isFalling(tree_supply))
            return;

        boolean in_view;
        if (planeMask == RenderTools.FRUSTUM_INSIDE) {
            in_view = !isPicking() || !tree_supply.isDead();
        } else if (camera == null) {
            in_view = false;
        } else if (isPicking()) {
            in_view = !tree_supply.isDead() && pickingInFrustum(tree_supply, camera.getFrustum(), planeMask);
        } else {
            in_view = RenderTools.testFrustum(tree_supply, camera.getFrustum(), planeMask)
                    != RenderTools.FRUSTUM_OUTSIDE;
        }
        if (in_view && camera != null) {
            addToRenderList(tree_supply, camera);
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
