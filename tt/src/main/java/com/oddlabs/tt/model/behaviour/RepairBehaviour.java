package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.audio.Assets;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.VisualSoundAccessory;
import org.jspecify.annotations.NonNull;

/**
 * Logic for unit repairing behavior.
 * Manages units moving to damaged buildings and restoring their hit points.
 */
public final class RepairBehaviour implements Behaviour {
    private static final int REPAIRS_PER_SUPPLY = 5;
    private static final float SECONDS_PER_ANIMATION_CYCLE = 1f;
    private final @NonNull Building building;
    private final @NonNull Unit unit;

    private float anim_time;
    private int repairs;
    private boolean sound;

    public RepairBehaviour(@NonNull Unit unit, @NonNull Building building) {
        this.unit = unit;
        this.building = building;
        unit.aimAtTarget(building);
        restartAnimation();
        unit.getSupplyContainer().increaseSupply(-1, TreeSupply.class);
        repairs = 0;
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public @NonNull State animate(float t) {
        anim_time += t;
        if (anim_time > unit.getWeaponFactory().getSecondsPerRelease(1f / SECONDS_PER_ANIMATION_CYCLE) && !sound) {
            sound = true;
            var random = unit.getOwner().getWorld().getRandom();
            unit.getOwner().getWorld().getAudio().newAudio(unit.getPositionX(), unit.getPositionY(), unit
                    .getPositionZ(), Assets.getHarvestSound(TreeSupply.class, random));

            RacesResources racesResources = unit.getOwner().getWorld().getRacesResources();
            if (racesResources != null) {
                var selectedSprite = random.nextBoolean()
                        ? racesResources.getSawEmojiSprite()
                        : racesResources.getHammerEmojiSprite();
                unit.addAccessory(new VisualSoundAccessory(selectedSprite,
                        VisualSoundAccessory.DURATION_REPAIR,
                        Assets.AUDIO_DISTANCE_HARVEST));
            }
        }

        if (anim_time > SECONDS_PER_ANIMATION_CYCLE) {
            restartAnimation();
            repairs++;
            if (building.isDead() || !building.isDamaged()) {
                return State.DONE;
            } else
                building.repair(1);
        }

        return repairs == REPAIRS_PER_SUPPLY ? State.DONE : State.INTERRUPTIBLE;
    }

    private void restartAnimation() {
        anim_time = 0;
        sound = false;
        unit.switchAnimation(1f / SECONDS_PER_ANIMATION_CYCLE, Unit.Animation.THROWING);
    }

    @Override
    public void forceInterrupted() {
    }
}
