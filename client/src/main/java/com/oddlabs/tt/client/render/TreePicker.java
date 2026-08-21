package com.oddlabs.tt.client.render;

import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.LODObject;
import com.oddlabs.tt.engine.render.PolyDetail;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.RenderTools;
import com.oddlabs.tt.engine.render.SpriteList;
import com.oddlabs.tt.engine.render.Tree;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.engine.resource.SpriteFile;
import com.oddlabs.tt.simulation.landscape.AbstractTreeGroup;
import com.oddlabs.tt.simulation.landscape.TreeGroup;
import com.oddlabs.tt.simulation.landscape.TreeLeaf;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import com.oddlabs.tt.simulation.model.BoundingBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.oddlabs.tt.simulation.landscape.AbstractTreeGroup.TreeType;


/**
 * Base class that manages the culling, level-of-detail selection, and picking of trees.
 */
class TreePicker {
    private static final int CROWN_MIPMAP_CUTOFF = RenderConfig.NO_MIPMAP_CUTOFF;
    private static final float SELECTION_RADIUS = 1.5f;

    @SuppressWarnings("unchecked")
    private final List<TreeSupply>[] render_lists = (List<TreeSupply>[]) new List[]{new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};
    @SuppressWarnings("unchecked")
    private final List<TreeSupply>[] respond_render_lists = (List<TreeSupply>[]) new List<?>[]{
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};

    private final BoundingBox picking_selection_box = new BoundingBox();
    private final SpriteSorter sprite_sorter;
    private final RenderStateCache<TreeRenderState> render_state_cache
            = new RenderStateCache<>(() -> new TreeRenderState(TreePicker.this));
    private final Map<TreeType, Tree> trees = loadTrees();
    private final RespondManager respond_manager;
    private CameraState camera;

    private boolean visible_override;

    TreePicker(SpriteSorter sprite_sorter, RespondManager respond_manager) {
        this.respond_manager = respond_manager;
        this.sprite_sorter = sprite_sorter;
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
        trees.put(TreeType.JUNGLE, new Tree(jungle_trunk, jungle_crown));
        trees.put(TreeType.PALM, new Tree(palm_trunk, palm_crown));
        trees.put(TreeType.OAK, new Tree(oak_trunk, oak_crown));
        trees.put(TreeType.PINE, new Tree(pine_trunk, pine_crown));
        return Collections.unmodifiableMap(trees);
    }

    final Map<TreeType, Tree> getTrees() {
        return trees;
    }

    public final List<TreeSupply>[] getRenderLists() {
        return render_lists;
    }

    public final List<TreeSupply>[] getRespondRenderLists() {
        return respond_render_lists;
    }

    public final void getAllPicks(List<TreeSupply> pick_list) {
        for (List<TreeSupply> render_list : render_lists) {
            pick_list.addAll(render_list);
            render_list.clear();
        }
        for (List<TreeSupply> respond_render_list : respond_render_lists) {
            pick_list.addAll(respond_render_list);
            respond_render_list.clear();
        }
    }

    private void addToHighDetailList(int index, TreeSupply tree, boolean respond) {
        if (respond) {
            respond_render_lists[index].add(tree);
        } else {
            render_lists[index].add(tree);
        }
    }

    final void markDetailPolygon(TreeSupply tree_supply, PolyDetail level) {
        // Always render high detail (Instanced Sprites)
        addToHighDetailList(tree_supply.getTreeType().ordinal(), tree_supply, respond_manager.isResponding(
                tree_supply));
    }

    public final void setup(CameraState camera_state) {
        this.camera = camera_state;
        render_state_cache.clear();
    }

    public final void visit(AbstractTreeGroup node) {
        RenderTools.FrustumIntersection frustum_state = camera.inNoDetailMode()
                ? RenderTools.FrustumIntersection.ALL_INSIDE
                : RenderTools.inFrustum(node, camera.getFrustum());

        if (visible_override || frustum_state != RenderTools.FrustumIntersection.ALL_OUTSIDE) {
            boolean old_override = visible_override;
            visible_override = visible_override || frustum_state == RenderTools.FrustumIntersection.ALL_INSIDE;

            switch (node) {
                case TreeGroup group -> {
                    for (AbstractTreeGroup child : group.children()) {
                        visit(child);
                    }
                }
                case TreeLeaf leaf -> {
                    for (TreeSupply tree : leaf.getTrees()) {
                        visitTree(tree);
                    }
                }
                case TreeSupply tree -> visitTree(tree);
            }

            visible_override = old_override;
        }
    }

    private boolean pickingInFrustum(TreeSupply tree_supply, float[][] frustum) {
        picking_selection_box.setBounds(-SELECTION_RADIUS + tree_supply.getPositionX(), SELECTION_RADIUS + tree_supply
                .getPositionX(), -SELECTION_RADIUS + tree_supply.getPositionY(), SELECTION_RADIUS + tree_supply
                        .getPositionY(), tree_supply.bmin_z, tree_supply.bmin_z + (tree_supply.bmax_z
                                - tree_supply.bmin_z) * tree_supply.getTreeType().heightScale);
        return RenderTools.inFrustum(picking_selection_box, frustum) != RenderTools.FrustumIntersection.ALL_OUTSIDE;
    }

    private void addToRenderList(TreeSupply tree, CameraState camera) {
        if (isPicking())
            markDetailPolygon(tree, PolyDetail.HIGH_POLY);
        else
            sprite_sorter.add(getRenderState(tree), camera, false);
    }

    private LODObject getRenderState(TreeSupply tree_supply) {
        TreeRenderState render_state = render_state_cache.get();
        render_state.setup(tree_supply);
        return render_state;
    }

    private void visitTree(TreeSupply tree_supply) {
        if (tree_supply.isHidden())
            return;

        boolean in_view;
        if (isPicking())
            in_view = !tree_supply.isDead() && (visible_override || pickingInFrustum(tree_supply, camera.getFrustum()));
        else
            in_view = visible_override || RenderTools.inFrustum(tree_supply, camera.getFrustum())
                    != RenderTools.FrustumIntersection.ALL_OUTSIDE;
        if (in_view) {
            addToRenderList(tree_supply, camera);
        }
    }

    boolean isPicking() {
        return true;
    }

    final CameraState getCamera() {
        return camera;
    }
}
