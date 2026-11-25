package com.oddlabs.tt.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.effects.particle.Emitter;
import com.oddlabs.tt.engine.render.TextureKey;
import com.oddlabs.tt.simulation.landscape.FlatLandscape;
import com.oddlabs.tt.simulation.landscape.Landscape;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.model.UnitType;
import com.oddlabs.tt.simulation.model.weapon.Stun;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.player.PlayerConfig;
import com.oddlabs.tt.simulation.player.PlayerInfo;
import com.oddlabs.util.Color;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class StunVisualModelTest {

  private World world;
  private Player player;
  private Unit chieftain;
  private TextureKey[] noteTextures;

  @BeforeEach
  void setUp() {
    int gridSize = 32;
    Landscape landscape = FlatLandscape.create(gridSize);
    WorldParameters worldParams = new WorldParameters("1", 5, 20);
    List<PlayerInfo> playerInfos = List.of(new PlayerInfo(0, Race.VIKINGS, "Player"));
    Color.Linear[] playerColors = new Color.Linear[] {new Color.Linear(1f, 0f, 0f, 1f)};
    this.world = new World(worldParams, landscape, PlayerConfig.from(playerInfos, playerColors));
    this.player = world.getPlayers().getFirst();
    this.chieftain = new Unit(player, 16f, 16f, null, player.getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN));

    this.noteTextures = new TextureKey[8];
    for (int i = 0; i < 8; i++) {
      noteTextures[i] = new TextureKey(i);
    }
  }

  private static AudioImplementation createDummyAudio() {
    return (AudioImplementation)
        Proxy.newProxyInstance(
            AudioImplementation.class.getClassLoader(), new Class<?>[] {AudioImplementation.class}, (_, _, _) -> null);
  }

  @Test
  void stunVisualModelSpawnsParticlesAndAnimates() {
    Stun stun = new Stun(2.57f, 0f, 3.8f, 36f, 30f, 10f, chieftain);
    StunVisualModel visualModel = new StunVisualModel(stun, createDummyAudio(), noteTextures);

    Emitter<?> emitter = visualModel.getEmitter();
    assertNotNull(emitter);
    assertFalse(emitter.hasActiveParticles());

    // Update several frames (dt = 0.05s) to accumulate time and spawn particles
    for (int i = 0; i < 10; i++) {
      visualModel.update(0.05f);
    }

    assertTrue(emitter.hasActiveParticles(), "Emitter should have active note particles after updating");

    Queue<Emitter<?>> queue = new ArrayDeque<>();
    visualModel.addEmitters(queue);
    assertTrue(queue.contains(emitter), "addEmitters should add emitter to queue");
  }

  @Test
  void stunIsInsertedIntoElementRootAndCanBeFound() {
    Stun stun = new Stun(2.57f, 0f, 3.8f, 36f, 30f, 10f, chieftain);
    assertTrue(stun.isRegistered());

    boolean found = false;
    var queue = new ArrayDeque<com.oddlabs.tt.simulation.model.AbstractElementNode<?>>();
    queue.add(world.getElementRoot());
    while (!queue.isEmpty()) {
      var node = queue.poll();
      for (var elem = node.getModels().getFirst(); elem != null; elem = elem.getNext()) {
        if (elem == stun) {
          found = true;
          break;
        }
      }
      if (found) break;
      if (node instanceof com.oddlabs.tt.simulation.model.ElementNode<?> en) {
        for (var child : en.children()) {
          queue.add(child);
        }
      }
    }
    assertTrue(found, "Stun should be found in element quadtree");
  }

  @Test
  void testStunAnimationLifecycle() {
    Stun stun = new Stun(2.57f, 0f, 3.8f, 36f, 30f, 10f, chieftain);
    StunVisualModel visualModel = new StunVisualModel(stun, createDummyAudio(), noteTextures);
    stun.setClientModel(visualModel);

    assertFalse(stun.isDead(), "Stun should be alive before release animation executes");
    stun.animate(0.02f);
    assertTrue(stun.isDead(), "Stun should be removed and dead immediately after release animation executes");
  }

  @Test
  void testStunTransformInWhiteModelVisitor() {
    Stun stun = new Stun(2.57f, 0f, 3.8f, 36f, 30f, 10f, chieftain);
    org.joml.Matrix4f transform = new org.joml.Matrix4f();
    com.oddlabs.tt.engine.render.SceneContext sc =
        (com.oddlabs.tt.engine.render.SceneContext)
            Proxy.newProxyInstance(
                com.oddlabs.tt.engine.render.SceneContext.class.getClassLoader(),
                new Class<?>[] {com.oddlabs.tt.engine.render.SceneContext.class},
                (proxy, method, args) -> method.getName().equals("getInterpolationAlpha") ? 0.5f : null);
    com.oddlabs.tt.engine.render.ElementSceneContext<com.oddlabs.tt.simulation.model.Model> esc =
        new com.oddlabs.tt.engine.render.ElementSceneContext<>(sc);
    esc.setup(com.oddlabs.tt.engine.render.WhiteModelVisitor.getInstance(), stun);
    com.oddlabs.tt.engine.render.WhiteModelVisitor.getInstance().getTransform(esc, transform);
    System.out.println(
        "Stun transform translation: x=" + transform.m30() + " y=" + transform.m31() + " z=" + transform.m32());
    assertTrue(transform.m32() > 0f, "Z should be above ground");
  }

  @Test
  void testStunVisualModelDetachedLifecycle() {
    Stun stun = new Stun(2.57f, 0f, 3.8f, 36f, 30f, 10f, chieftain);
    StunVisualModel visualModel = new StunVisualModel(stun, createDummyAudio(), noteTextures);
    stun.setClientModel(visualModel);

    for (int i = 0; i < 10; i++) {
      visualModel.update(0.05f);
    }
    assertTrue(visualModel.getEmitter().hasActiveParticles(), "Emitter must have active particles");

    stun.remove();
    assertTrue(stun.isDead(), "Stun must be dead after remove");

    assertFalse(visualModel.isExpired(), "Visual model must not be expired while particles are active");
    assertTrue(visualModel.isVisible(stun, null), "Visual model must be visible while particles are active");

    for (int i = 0; i < 100; i++) {
      visualModel.update(0.1f);
    }
    assertTrue(visualModel.getEmitter().isFinished(), "Emitter should be finished after particles expire");
    assertTrue(visualModel.isExpired(), "Visual model should be expired when emitter is finished");
  }
}
