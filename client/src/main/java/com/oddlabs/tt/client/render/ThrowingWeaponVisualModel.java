package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioFile;
import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.client.resource.AudioRegistry;
import com.oddlabs.tt.engine.render.AnimatedAccessory;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.ElementSceneContext;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.weapon.ThrowingWeapon;
import java.util.concurrent.ThreadLocalRandom;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/// Abstract visual model for throwing weapons managing spatial flight audio and visual ballistic loft.
abstract sealed class ThrowingWeaponVisualModel<T extends ThrowingWeapon> extends AbstractVisualModel
    implements AnimatedAccessory permits DirectedThrowingWeaponVisualModel, RotatingThrowingWeaponVisualModel {
  protected static final float GRAVITY_MAGNITUDE = 3.0f * 9.82f;

  protected final T weapon;
  private final @Nullable AudioPlayer audioPlayer;
  private final boolean stopSoundOnImpact;
  protected final float effectiveGravity;

  ThrowingWeaponVisualModel(
      T weapon, AudioImplementation audio, float loftFactor, AudioFile sound, boolean stopSoundOnImpact) {
    super(weapon);
    this.weapon = weapon;
    this.stopSoundOnImpact = stopSoundOnImpact;
    this.effectiveGravity = GRAVITY_MAGNITUDE * loftFactor * loftFactor;

    var params =
        new AudioParameters(
            sound,
            AudioRegistry.AUDIO_RANK_WEAPON_ATTACK,
            AudioRegistry.AUDIO_DISTANCE_WEAPON_ATTACK,
            AudioRegistry.AUDIO_GAIN_WEAPON_ATTACK,
            AudioRegistry.AUDIO_RADIUS_WEAPON_ATTACK,
            ThreadLocalRandom.current().nextFloat(0.9f, 1.1f));
    this.audioPlayer = audio.newAudio(weapon.getPositionX(), weapon.getPositionY(), weapon.getPositionZ(), params);
  }

  protected final float computeVisualZ(float alpha) {
    float t = weapon.getTime();
    float totalTime = weapon.getTimeLimit();
    float baseZ = weapon.getInterpolatedPositionZ(alpha);
    if (totalTime > 0f && t < totalTime && effectiveGravity > 0f) {
      return baseZ + 0.5f * effectiveGravity * t * (totalTime - t);
    }
    return baseZ;
  }

  protected final float computeVisualZ() {
    return computeVisualZ(1.0f);
  }

  @Override
  public final void animate(float dt) {
    if (audioPlayer != null) {
      audioPlayer.setPosition(weapon.getPositionX(), weapon.getPositionY(), computeVisualZ());
    }
  }

  public abstract void getTransform(Matrix4f dest);

  public abstract void getTransform(Matrix4f dest, float alpha);

  @Override
  public void visit(RenderState rs) {
    ElementSceneContext<T> state = (ElementSceneContext<T>) rs.getCachedState(rs.getWeaponModelVisitor(), weapon);
    state.addToRenderList();
    rs.visitAccessories(weapon, state);
  }

  @Override
  protected final boolean isSelfExpired() {
    return weapon.isDead();
  }

  @Override
  public final boolean isVisible(Model parent, CameraState camera) {
    return !weapon.isDead();
  }

  @Override
  public final void close() {
    super.close();
    if (stopSoundOnImpact && audioPlayer != null) {
      audioPlayer.stop();
    }
  }
}
