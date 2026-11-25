package com.oddlabs.tt.client.render;

import static com.oddlabs.tt.simulation.landscape.AbstractTreeGroup.TreeType;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.client.viewer.Cheat;
import com.oddlabs.tt.engine.render.BoundingMode;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.InstancedSpriteRenderer;
import com.oddlabs.tt.engine.render.RenderConfig;
import com.oddlabs.tt.engine.render.RenderTools;
import com.oddlabs.tt.engine.render.Sprite;
import com.oddlabs.tt.engine.render.SpriteList;
import com.oddlabs.tt.engine.render.SubSceneRenderer;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.render.WaveAnimation;
import com.oddlabs.tt.engine.render.state.ScenePassContext;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.engine.resource.SpriteFile;
import com.oddlabs.tt.scenery.LandscapeRenderer;
import com.oddlabs.tt.simulation.landscape.AbstractTreeGroup;
import com.oddlabs.tt.simulation.landscape.LandscapeGeometry;
import com.oddlabs.tt.simulation.landscape.TreeGroup;
import com.oddlabs.tt.simulation.landscape.TreeLeaf;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import com.oddlabs.tt.simulation.model.Shadowable;
import com.oddlabs.util.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/// Specialized renderer for forest elements, coordinating the efficient drawing of crown and trunk sprite lists using
/// hardware instancing.
final class TreeRenderer implements SubSceneRenderer, AutoCloseable, Animated {
  private static final int CROWN_MIPMAP_CUTOFF = RenderConfig.NO_MIPMAP_CUTOFF;
  private static final float TREE_FALL_DURATION = 3f;
  private static final float TREE_SPAWN_DURATION = 3f;
  private static final float TREE_WAVE_SCALE = 0.035f;

  private final EnumMap<TreeType, List<TreeSupply>> render_lists = createTreeListMap();
  private final EnumMap<TreeType, List<TreeSupply>> respond_render_lists = createTreeListMap();
  private final Map<TreeType, Tree> trees = loadTrees();
  private final IdentityHashMap<TreeSupply, TreeRenderState> render_states = new IdentityHashMap<>();
  private final RespondManager respond_manager;
  private final WaveAnimation wave_animation = new WaveAnimation(TREE_WAVE_SCALE);
  private final @Nullable Cheat cheat;
  private final Matrix4f tempMatrix = new Matrix4f();
  private final AnimationManager animationManager;
  private final @Nullable LandscapeRenderer landscapeRenderer;
  private final Map<TreeType, TreeRenderBatches> treeBatches;
  private final Map<TreeSupply, Float> fallingTrees = new HashMap<>();
  private final Map<TreeSupply, Float> spawningTrees = new HashMap<>();
  private final TreeTraversal.Visitor treeVisitor = this::visitTree;

  private @Nullable CameraState camera;
  private int lastRenderedTreeCount;

  private record TreeRenderBatches(
      InstancedSpriteRenderer.RenderBatch crownBatch,
      InstancedSpriteRenderer.RenderBatch trunkBatch,
      InstancedSpriteRenderer.RenderBatch respondCrownBatch,
      InstancedSpriteRenderer.RenderBatch respondTrunkBatch) {}

  TreeRenderer(
      @Nullable Cheat cheat,
      RespondManager respond_manager,
      InstancedSpriteRenderer instancedSpriteRenderer,
      AnimationManager animationManager,
      @Nullable LandscapeRenderer landscapeRenderer) {
    this.cheat = cheat;
    this.respond_manager = respond_manager;
    this.animationManager = animationManager;
    this.landscapeRenderer = landscapeRenderer;
    this.treeBatches = createTreeBatches(instancedSpriteRenderer);
    animationManager.registerAnimation(this);
  }

  private static EnumMap<TreeType, List<TreeSupply>> createTreeListMap() {
    var map = new EnumMap<TreeType, List<TreeSupply>>(TreeType.class);
    for (TreeType type : TreeType.values()) {
      map.put(type, new ArrayList<>());
    }
    return map;
  }

  private static Tree loadTree(TreeType type, float shadowDiameter, float shadowOpacity, float shadowVerticalCenter) {
    SpriteList crown =
        Resources.findResource(
            new SpriteFile(LandscapeGeometry.crownPath(type), CROWN_MIPMAP_CUTOFF, false, false, true, false, true));
    SpriteList trunk =
        Resources.findResource(
            new SpriteFile(LandscapeGeometry.trunkPath(type), CROWN_MIPMAP_CUTOFF, true, true, true, false));
    return new Tree(trunk, crown, shadowDiameter, shadowOpacity, shadowVerticalCenter);
  }

  private static Map<TreeType, Tree> loadTrees() {
    var trees = new EnumMap<TreeType, Tree>(TreeType.class);
    trees.put(TreeType.JUNGLE, loadTree(TreeType.JUNGLE, 16.0f, 0.5f, 0.6f));
    trees.put(TreeType.PALM, loadTree(TreeType.PALM, 20.0f, 0.3f, 1.0f));
    trees.put(TreeType.OAK, loadTree(TreeType.OAK, 20.0f, 0.5f, 0.6f));
    trees.put(TreeType.PINE, loadTree(TreeType.PINE, 12.0f, 0.6f, 0.3f));
    return Collections.unmodifiableMap(trees);
  }

  private Map<TreeType, TreeRenderBatches> createTreeBatches(InstancedSpriteRenderer renderer) {
    var map = new EnumMap<TreeType, TreeRenderBatches>(TreeType.class);
    for (var entry : trees.entrySet()) {
      Tree tree = entry.getValue();
      SpriteList crownList = tree.crown();
      SpriteList trunkList = tree.trunk();

      Sprite crownSprite = crownList.getSprite();
      Texture crownTexture = crownSprite.textures[0][Sprite.TEXTURE_NORMAL];
      Texture crownTeam = crownSprite.textures[0][Sprite.TEXTURE_TEAM];

      Sprite trunkSprite = trunkList.getSprite();
      Texture trunkTexture = trunkSprite.textures[0][Sprite.TEXTURE_NORMAL];
      Texture trunkTeam = trunkSprite.textures[0][Sprite.TEXTURE_TEAM];
      Texture trunkBump = trunkSprite.hasBumpMap(0) ? trunkSprite.textures[0][Sprite.TEXTURE_BUMP] : null;

      var crownBatch = renderer.getBatch(crownList, crownTexture, crownTeam, null, false, false, true, true);
      var trunkBatch = renderer.getBatch(trunkList, trunkTexture, trunkTeam, trunkBump, false, false, true, true);
      var respondCrownBatch = renderer.getBatch(crownList, crownTexture, crownTeam, null, true, false, true, true);
      var respondTrunkBatch = renderer.getBatch(trunkList, trunkTexture, trunkTeam, trunkBump, true, false, true, true);

      map.put(entry.getKey(), new TreeRenderBatches(crownBatch, trunkBatch, respondCrownBatch, respondTrunkBatch));
    }
    return Collections.unmodifiableMap(map);
  }

  void initTreeBounds(AbstractTreeGroup root) {
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

  private TreeRenderState getOrCreateState(TreeSupply tree) {
    return render_states.computeIfAbsent(tree, this::createRenderState);
  }

  private TreeRenderState createRenderState(TreeSupply tree) {
    Tree visual = trees.get(tree.getTreeType());
    BoundingBox visualBounds = new BoundingBox();
    if (visual != null) {
      visualBounds.setTransformedBounds(visual.modelBounds(), tree.getMatrix());
    }
    return new TreeRenderState(new Matrix4f(tree.getMatrix()), visualBounds);
  }

  void setup(CameraState camera_state) {
    this.camera = camera_state;
  }

  void visit(AbstractTreeGroup node) {
    if (camera != null) {
      TreeTraversal.visit(node, camera, treeVisitor);
    }
  }

  private void visitTree(TreeSupply tree_supply, boolean fullyInside) {
    if (tree_supply.isEmpty() && !fallingTrees.containsKey(tree_supply)) {
      return;
    }

    TreeRenderState state = getOrCreateState(tree_supply);
    boolean in_view = fullyInside || (camera != null && state.visualBounds().testFrustum(camera.getFrustum()));
    if (in_view && camera != null) {
      (respond_manager.isResponding(tree_supply) ? respond_render_lists : render_lists)
          .get(tree_supply.getTreeType())
          .add(tree_supply);
    }
  }

  void onTreeFelled(TreeSupply tree) {
    spawningTrees.remove(tree);
    fallingTrees.put(tree, 0f);
    if (landscapeRenderer != null) {
      landscapeRenderer.onTreeFelled(tree);
    }
  }

  void onTreeSpawned(TreeSupply tree) {
    getOrCreateState(tree);
    fallingTrees.remove(tree);
    spawningTrees.put(tree, 0f);
  }

  @Override
  public void animate(float dt) {
    if (!fallingTrees.isEmpty()) {
      var eachFalling = fallingTrees.entrySet().iterator();
      while (eachFalling.hasNext()) {
        var entry = eachFalling.next();
        float progress = entry.getValue() + dt / TREE_FALL_DURATION;
        if (!entry.getKey().isEmpty() || progress >= 1.0f) {
          eachFalling.remove();
        } else {
          entry.setValue(progress);
        }
      }
    }
    if (!spawningTrees.isEmpty()) {
      var eachSpawning = spawningTrees.entrySet().iterator();
      while (eachSpawning.hasNext()) {
        var entry = eachSpawning.next();
        float progress = entry.getValue() + dt / TREE_SPAWN_DURATION;
        if (entry.getKey().isEmpty() || progress >= 1.0f) {
          if (progress >= 1.0f && !entry.getKey().isEmpty() && landscapeRenderer != null) {
            landscapeRenderer.onTreeSpawned(entry.getKey());
          }
          eachSpawning.remove();
        } else {
          entry.setValue(progress);
        }
      }
    }
  }

  void renderShadows(SelectableShadowRenderer shadowRenderer, float currentTime) {
    if (!DebugFlags.draw_trees || (cheat != null && !cheat.draw_trees)) {
      return;
    }
    float windX = 0.28f * (float) Math.cos(currentTime);
    float windY = 0.56f * (float) Math.sin(currentTime);
    for (var entry : fallingTrees.entrySet()) {
      TreeSupply tree = entry.getKey();
      float p = entry.getValue();
      Tree visual = trees.get(tree.getTreeType());
      float scale;
      float opacity;
      final float pHit = 0.65f;
      if (p <= pHit) {
        float u = p / pHit;
        float ramp = 1.0f - (1.0f - u) * (1.0f - u);
        opacity = 1.0f + 0.35f * ramp;
        scale = 1.0f - 0.15f * ramp;
      } else {
        float v = Math.min(1.0f, (p - pHit) / (1.0f - pHit));
        float fade = (1.0f - v) * (1.0f - v);
        opacity = 1.35f * fade;
        scale = 0.85f * fade;
      }
      if (scale > 0.001f && opacity > 0.001f) {
        shadowRenderer.addToShadowList(new TreeShadow(tree, visual, scale, opacity, windX, windY));
      }
    }
    for (var entry : spawningTrees.entrySet()) {
      TreeSupply tree = entry.getKey();
      if (!tree.isEmpty()) {
        float spawnProgress = entry.getValue();
        Tree visual = trees.get(tree.getTreeType());
        float inv = 1f - spawnProgress;
        float scale = 1f - inv * inv * inv * inv * inv * inv;
        shadowRenderer.addToShadowList(new TreeShadow(tree, visual, scale, 1.0f, windX, windY));
      }
    }
  }

  private record TreeShadow(
      TreeSupply tree, Tree visual, float scale, float opacityMultiplier, float windX, float windY)
      implements Shadowable {
    @Override
    public float getPositionX() {
      return tree.getPositionX() + windX;
    }

    @Override
    public float getPositionY() {
      return tree.getPositionY() + windY;
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

  @Override
  public void render(ScenePassContext pass) {
    if (!pass.inNoDetailMode()) {
      wave_animation.setTime(pass.time());
    }

    if (!DebugFlags.draw_trees || (cheat != null && !cheat.draw_trees)) {
      debugRender();
      // Just clear lists if not drawing
      clearLists();
      lastRenderedTreeCount = 0;
      return;
    }

    debugRender();

    int count = 0;
    for (var entry : render_lists.entrySet()) {
      List<TreeSupply> list = entry.getValue();
      if (!list.isEmpty()) {
        count += list.size();
        renderList(entry.getKey(), list, false);
      }
    }
    for (var entry : respond_render_lists.entrySet()) {
      List<TreeSupply> list = entry.getValue();
      if (!list.isEmpty()) {
        count += list.size();
        renderList(entry.getKey(), list, true);
      }
    }
    lastRenderedTreeCount = count;
  }

  int getLastRenderedTreeCount() {
    return lastRenderedTreeCount;
  }

  private void clearLists() {
    for (List<TreeSupply> list : render_lists.values()) {
      list.clear();
    }
    for (List<TreeSupply> list : respond_render_lists.values()) {
      list.clear();
    }
  }

  private void prepareMatrix(TreeSupply tree) {
    tempMatrix.set(getOrCreateState(tree).matrix());
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

  private void renderList(TreeType type, List<TreeSupply> render_list, boolean respond) {
    if (camera != null && render_list.size() > 1) {
      float camX = camera.getCurrentX();
      float camY = camera.getCurrentY();
      float camZ = camera.getCurrentZ();
      render_list.sort((a, b) ->
          Float.compare(
              RenderTools.getEyeDistanceSquared(a, camX, camY, camZ),
              RenderTools.getEyeDistanceSquared(b, camX, camY, camZ)));
    }

    TreeRenderBatches batches = treeBatches.get(type);
    var crownBatch = respond ? batches.respondCrownBatch() : batches.crownBatch();
    var trunkBatch = respond ? batches.respondTrunkBatch() : batches.trunkBatch();

    for (TreeSupply supply : render_list) {
      prepareMatrix(supply);
      // Render Crown. Blend = false, DepthWrite = true for opaque trees.
      crownBatch.addInstance(tempMatrix, Color.Standard.WHITE, Color.Standard.TRANSPARENT);
      // Render Trunk. Blend = false, DepthWrite = true.
      trunkBatch.addInstance(tempMatrix, Color.Standard.WHITE, Color.Standard.TRANSPARENT);
    }
    render_list.clear();
  }

  void debugRender() {
    if (DebugFlags.isBoundsEnabled(BoundingMode.TREES)) {
      for (List<TreeSupply> list : render_lists.values()) {
        for (TreeSupply tree : list) {
          RenderTools.draw(getOrCreateState(tree).visualBounds(), BoundingMode.TREES, Color.Linear.GREEN);
        }
      }
      for (List<TreeSupply> list : respond_render_lists.values()) {
        for (TreeSupply tree : list) {
          RenderTools.draw(getOrCreateState(tree).visualBounds(), BoundingMode.TREES, Color.Linear.GREEN);
        }
      }
    }
  }

  @Override
  public void close() {
    animationManager.removeAnimation(this);
    fallingTrees.clear();
    spawningTrees.clear();
  }
}
