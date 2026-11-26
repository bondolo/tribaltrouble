package com.oddlabs.tt.client.render;

import static com.oddlabs.tt.simulation.model.Ability.BUILD;
import static com.oddlabs.tt.simulation.model.Ability.THROW;

import com.oddlabs.tt.effects.particle.BalancedParametricEmitter;
import com.oddlabs.tt.effects.particle.StunFunction;
import com.oddlabs.tt.effects.render.EmitterAttachedAccessory;
import com.oddlabs.tt.engine.render.Accessory;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.render.SpriteList;
import com.oddlabs.tt.engine.render.TextureKey;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.SupplyType;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.util.Color;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

/// {@link VisualModel} implementation for units managing carried resources, stun particle effects, and dynamic status
/// accessories.
final class UnitVisualModel extends SelectableVisualModel<Unit> {
  private final Unit unit;
  private final TextureKey[] starTextures;
  private Matrix4f @Nullable [] evaluatedBones;
  private int evaluatedAnimation = -1;
  private float evaluatedTicks = -1f;

  UnitVisualModel(
      Unit unit,
      @Nullable Map<SupplyType, SpriteKey> carriedSupplySprites,
      @Nullable SpriteKey weaponSprite,
      TextureKey[] starTextures) {
    super(unit);
    this.unit = unit;
    this.starTextures = starTextures;
    if (unit.hasAbility(BUILD) && carriedSupplySprites != null) {
      addAccessory(new CarriedResourceAccessory(unit, carriedSupplySprites));
    }
    if (unit.hasAbility(THROW) && weaponSprite != null) {
      addAccessory(new WeaponAccessory(unit, this, weaponSprite));
    }
  }

  /// Evaluates and caches bone skinning matrices for the current animation and tick, or returns null if non-skeletal.
  public Matrix4fc @Nullable [] getEvaluatedBones(SpriteList spriteList, int animation, float ticks) {
    int boneCount = spriteList.getBoneCount();
    if (boneCount == 0) {
      return null;
    }
    if (evaluatedBones == null || evaluatedBones.length != boneCount) {
      evaluatedBones = new Matrix4f[boneCount];
      for (int i = 0; i < boneCount; i++) {
        evaluatedBones[i] = new Matrix4f();
      }
    }
    if (animation != evaluatedAnimation || ticks != evaluatedTicks) {
      if (spriteList.evaluateSkeleton(animation, ticks, evaluatedBones)) {
        evaluatedAnimation = animation;
        evaluatedTicks = ticks;
      } else {
        return null;
      }
    }
    return evaluatedBones;
  }

  public void updateStunStars(World world) {
    float timeLeft = unit.getStunTimeLeft();
    if (timeLeft > 0f) {
      boolean hasStunStar = false;
      for (Accessory acc : getAccessories()) {
        if (acc instanceof EmitterAttachedAccessory) {
          hasStunStar = true;
          break;
        }
      }
      if (!hasStunStar) {
        float velocity = (float) Math.PI / 2;
        BalancedParametricEmitter emitter =
            new BalancedParametricEmitter(
                world,
                new StunFunction(.4f, .15f),
                new Vector3f(0f, 0f, 0f),
                velocity,
                5f,
                (float) Math.PI * 2,
                (float) Math.PI * 2,
                5,
                0f,
                2f,
                Color.Linear.WHITE,
                Color.LinearDelta.ZERO,
                new Vector3f(.1f, .1f, .1f),
                new Vector3f(0f, 0f, 0f),
                timeLeft,
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                starTextures);

        float mountOffset = unit.getMountOffset();
        var offset =
            new Vector3f(
                unit.getTemplate().getStunX(),
                unit.getTemplate().getStunY(),
                unit.getTemplate().getStunZ() + mountOffset);
        addAccessory(new EmitterAttachedAccessory(emitter, offset));
      }
    } else {
      removeAccessoriesIf(acc -> acc instanceof EmitterAttachedAccessory);
    }
  }
}
