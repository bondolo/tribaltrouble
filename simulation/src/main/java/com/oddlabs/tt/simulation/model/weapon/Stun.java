package com.oddlabs.tt.simulation.model.weapon;

import static com.oddlabs.tt.simulation.model.Ability.ATTACK;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.MountUnitContainer;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.tt.simulation.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.simulation.pathfinder.UnitGrid;
import com.oddlabs.tt.simulation.player.Player;
import java.util.Random;
import org.jspecify.annotations.Nullable;

/// Logic controller for the Stun magic effect.
public final class Stun extends Model implements Magic {

  private final Unit src;
  private final float offset_x;
  private final float offset_y;
  private final float offset_z;
  private final float hit_radius;
  private final float stun_time_closest;
  private final float stun_time_farthest;
  private final Player owner;

  private final float start_x;
  private final float start_y;
  private final Iterable<? extends Selectable<?>> target_list;
  private final @Nullable LegacyStunEmitter legacyStunEmitter;

  public Stun(
      float offset_x,
      float offset_y,
      float offset_z,
      float hit_radius,
      float stun_time_closest,
      float stun_time_farthest,
      Unit src) {
    super(src.getOwner().getWorld());
    this.src = src;
    this.offset_x = offset_x;
    this.offset_y = offset_y;
    this.offset_z = offset_z;
    this.hit_radius = hit_radius;
    this.stun_time_closest = stun_time_closest;
    this.stun_time_farthest = stun_time_farthest;
    this.owner = src.getOwner();

    start_x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
    start_y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
    float z = src.getPositionZ() + offset_z;
    setPosition(start_x, start_y, z);
    register();

    var filter =
        new FindOccupantFilter<>(src.getPositionX(), src.getPositionY(), hit_radius, src, Selectable.genericClass());
    UnitGrid unit_grid = owner.getWorld().getUnitGrid();
    unit_grid.scan(
        filter, UnitGrid.toGridCoordinate(src.getPositionX()), UnitGrid.toGridCoordinate(src.getPositionY()));
    target_list = filter.getResult();

    if (AppConfig.LEGACY_DETERMINISTIC) {
      owner.getWorld().getRandom().nextInt(3);
      this.legacyStunEmitter = new LegacyStunEmitter(owner.getWorld());
      owner.getWorld().getAnimationManagerGameTime().registerAnimation(legacyStunEmitter);
    } else {
      this.legacyStunEmitter = null;
    }
  }

  @Override
  public void remove() {
    super.remove();
    if (legacyStunEmitter != null) {
      legacyStunEmitter.done();
      owner.getWorld().getAnimationManagerGameTime().removeAnimation(legacyStunEmitter);
    }
    owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
    owner.getWorld().getNotificationListener().onModelRemoved(this);
  }

  @Override
  protected void onReinsert() {
    float x = getPositionX();
    float y = getPositionY();
    float z = getPositionZ();
    setBounds(x - hit_radius, x + hit_radius, y - hit_radius, y + hit_radius, z - 5f, z + 20f);
  }

  @Override
  protected BoundingBox @Nullable [] getLocalBounds() {
    return null;
  }

  @Override
  public void animate(float dt) {
    if (!src.isDead()) {
      float x = src.getPositionX() + offset_x * src.getDirectionX() - offset_y * (-src.getDirectionY());
      float y = src.getPositionY() + offset_x * src.getDirectionY() + offset_y * src.getDirectionX();
      float z = src.getPositionZ() + offset_z;
      setPosition(x, y, z);
    }

    for (Selectable<?> selectable : target_list) {
      Unit unit = null;
      if (selectable instanceof Unit unit1) {
        unit = unit1;
      } else if (selectable instanceof Building building) {
        if (!building.isDead() && building.hasAbility(ATTACK)) {
          if (building.getRawUnitContainer() instanceof MountUnitContainer muc && muc.getNumSupplies() > 0) {
            unit = muc.getUnit();
          }
        }
      }

      if (unit == null || unit.isDead()) continue;

      float dx = unit.getPositionX() - start_x;
      float dy = unit.getPositionY() - start_y;
      float squared_dist = dx * dx + dy * dy;
      if (owner.isEnemy(unit.getOwner()) && squared_dist < hit_radius * hit_radius) {
        float dist = (float) Math.sqrt(squared_dist);
        float time = calculateValueFromCurrentRadius(dist, stun_time_closest, stun_time_farthest);
        unit.stun(time);
      }
    }
    remove();
  }

  private float calculateValueFromCurrentRadius(float current_radius, float max, float min) {
    float base_factor = 6f / 7f;
    float error = (float) Math.pow(base_factor, hit_radius);
    float factor = (float) Math.pow(base_factor, current_radius);
    float result = (max - min + error) * factor + min - error;
    return result;
  }

  @Override
  public void interrupt() {
    remove();
  }

  private static final class LegacyStunEmitter implements Animated {
    private final World world;
    private float particleCounter = 0f;
    private boolean started = true;
    private float xAngle = 0f;
    private float yAngle = 0f;

    LegacyStunEmitter(World world) {
      this.world = world;
    }

    void done() {
      started = false;
    }

    @Override
    public void animate(float dt) {
      if (started) {
        particleCounter += 35f * dt;
      }
      while (particleCounter >= 1f && started) {
        initParticle();
        particleCounter -= 1f;
      }
    }

    private void initParticle() {
      randomizeVelocity();
      world.getRandom().nextInt(8);
    }

    private void randomizeVelocity() {
      Random random = world.getRandom();
      float angleMaxJump = (float) Math.PI;
      float angleBound = 0.5f;
      float dxAngle = random.nextFloat() * angleMaxJump - .5f * angleMaxJump;
      float dyAngle = random.nextFloat() * angleMaxJump - .5f * angleMaxJump;

      if ((xAngle + dxAngle < -angleBound) || (xAngle + dxAngle > angleBound)) {
        xAngle -= dxAngle;
      } else {
        xAngle += dxAngle;
      }

      if ((yAngle + dyAngle < -angleBound) || (yAngle + dyAngle > angleBound)) {
        yAngle -= dyAngle;
      } else {
        yAngle += dyAngle;
      }
    }
  }
}
