package com.oddlabs.tt.simulation.model.weapon;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import org.jspecify.annotations.Nullable;

/// Logic controller for the Lightning Cloud magic effect.
public final class LightningCloud extends Model implements Magic {
  private static final int NUM_STRIKES = 6;
  private static final float SECONDS_BETWEEN_STRIKES = .125f;

  private final Player owner;
  private final float seconds_per_hit;
  private final float meters_per_second;
  private final float hit_chance;
  private final int damage;
  private final float height;
  private final float seconds_to_init;
  private final float offset_z;
  private final @Nullable LegacyCloudEmitter legacyCloudEmitter;

  private float seconds_to_live;
  private @Nullable Selectable<?> target = null;
  private @Nullable Selectable<?> prev_target = null;
  private float hit_timer = 0f;
  private int strike_counter = 0;

  public LightningCloud(
      World world,
      float offset_x,
      float offset_y,
      float offset_z,
      float seconds_to_live,
      float seconds_per_hit,
      float seconds_to_init,
      float meters_per_second,
      float hit_chance,
      int damage,
      float height,
      Unit src) {
    super(world);
    float start_x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
    float start_y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
    setPosition(start_x, start_y, world.getHeightMap().getNearestHeight(start_x, start_y) + height);
    register();

    this.seconds_to_live = seconds_to_live;
    this.seconds_to_init = seconds_to_init;
    this.offset_z = offset_z;
    this.seconds_per_hit = seconds_per_hit;
    this.meters_per_second = meters_per_second;
    this.hit_chance = hit_chance;
    this.damage = damage;
    this.height = height;
    owner = src.getOwner();

    if (AppConfig.LEGACY_DETERMINISTIC) {
      this.legacyCloudEmitter = new LegacyCloudEmitter(world);
      world.getAnimationManagerGameTime().registerAnimation(legacyCloudEmitter);
    } else {
      this.legacyCloudEmitter = null;
    }
  }

  public float getSecondsToLive() {
    return seconds_to_live;
  }

  public float getSecondsToInit() {
    return seconds_to_init;
  }

  public float getCloudHeight() {
    return height;
  }

  public float getCloudOffsetZ() {
    return offset_z;
  }

  @Override
  public void remove() {
    super.remove();
    if (legacyCloudEmitter != null) {
      legacyCloudEmitter.done();
      owner.getWorld().getAnimationManagerGameTime().removeAnimation(legacyCloudEmitter);
    }
    owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
    owner.getWorld().getNotificationListener().onModelRemoved(this);
  }

  @Override
  protected void onReinsert() {
    float x = getPositionX();
    float y = getPositionY();
    float z = getPositionZ();
    setBounds(x - 10f, x + 10f, y - 10f, y + 10f, z - 10f, z + 20f);
  }

  @Override
  protected BoundingBox @Nullable [] getLocalBounds() {
    return null;
  }

  @Override
  public void animate(float dt) {
    seconds_to_live -= dt;
    boolean finished = seconds_to_live <= 0f;

    hit_timer += dt;

    if (hit_timer > seconds_per_hit) {
      if (target == null) {
        target =
            owner
                .findNearestEnemy(
                    UnitGrid.toGridCoordinate(getPositionX()), UnitGrid.toGridCoordinate(getPositionY()), prev_target)
                .orElse(null);
        if (target == null) {
          target =
              owner
                  .findNearestEnemy(
                      UnitGrid.toGridCoordinate(getPositionX()), UnitGrid.toGridCoordinate(getPositionY()), null)
                  .orElse(null);
          if (target == null) {
            return;
          }
        }
      }

      float dx = target.getPositionX() - getPositionX();
      float dy = target.getPositionY() - getPositionY();
      float dist = (float) Math.sqrt(dx * dx + dy * dy);
      dx /= dist;
      dy /= dist;
      if (dist < meters_per_second * dt) {
        if (!target.isDead()
            && owner.getWorld().getRandom().nextFloat() < hit_chance * (1 - target.getDefenseChance())) {
          target.hit(damage, dx, dy, owner);
        }
        strike(target);
        strike(target);
        prev_target = target;
        target = null;
        hit_timer = 0f;
        strike_counter = 0;
      } else {
        float x = getPositionX() + dx * (meters_per_second * dt);
        float y = getPositionY() + dy * (meters_per_second * dt);
        float z = owner.getWorld().getHeightMap().getNearestHeight(x, y) + height;
        setPosition(x, y, z);
      }
    } else if (prev_target != null
        && strike_counter < NUM_STRIKES - 1
        && hit_timer > (strike_counter + 1) * SECONDS_BETWEEN_STRIKES) {
      strike(prev_target);
      strike(prev_target);
      strike_counter++;
    }
    if (finished) {
      remove();
    }
  }

  private void strike(Target target) {
    if (AppConfig.LEGACY_DETERMINISTIC) {
      var rand = owner.getWorld().getRandom();
      for (int i = 0; i < 31; i++) {
        rand.nextFloat();
      }
    }

    owner.getWorld().getNotificationListener().onLightningStrike(this, target);
  }

  @Override
  public void interrupt() {
    // DO NOT FIX (AppConfig.LEGACY_DETERMINISTIC): Preserved for legacy simulation fidelity; the original game only
    // silenced the sound here while the spell keeps running after the caster is interrupted.
  }

  private static final class LegacyCloudEmitter implements Animated {
    private final World world;
    private int numParticles = 25;
    private float particleCounter = 0f;
    private boolean started = true;

    LegacyCloudEmitter(World world) {
      this.world = world;
    }

    void done() {
      started = false;
    }

    @Override
    public void animate(float dt) {
      if (started) {
        particleCounter += 100f * dt;
      }
      while (particleCounter >= 1f && numParticles > 0 && started) {
        initParticle();
        particleCounter -= 1f;
        numParticles--;
        if (numParticles == 0) {
          world.getAnimationManagerGameTime().removeAnimation(this);
          break;
        }
      }
    }

    private void initParticle() {
      var rand = world.getRandom();
      rand.nextFloat();
      rand.nextFloat();
      rand.nextFloat();
      rand.nextFloat();
      rand.nextFloat();
      rand.nextFloat();
      rand.nextFloat();
      rand.nextFloat();
      rand.nextInt(1);
    }
  }
}
