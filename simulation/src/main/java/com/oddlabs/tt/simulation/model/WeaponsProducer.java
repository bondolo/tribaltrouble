package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.simulation.landscape.World;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Manages the production of weapons in an armory building.
 */
public class WeaponsProducer {
    private static final float MAX_BREAK_TIME = .25f;
    private static final float BREAK_PROBABILITY = .2f;

    private final Building building;
    private final WorkerUnitContainer unit_container;
    private final BuildProductionContainer[] production_containers;
    private final @Nullable LegacySmokeEmitter legacySmokeEmitter;

    private float break_time = 0f;
    private boolean producing;

    public WeaponsProducer(Building building, WorkerUnitContainer unit_container,
            BuildProductionContainer[] production_containers) {
        this.building = building;
        this.unit_container = unit_container;
        this.production_containers = production_containers;
        if (AppConfig.LEGACY_DETERMINISTIC) {
            this.legacySmokeEmitter = new LegacySmokeEmitter(building.getOwner().getWorld());
            building.getOwner().getWorld().getAnimationManagerRealTime().registerAnimation(legacySmokeEmitter);
        } else {
            this.legacySmokeEmitter = null;
        }
    }

    public final boolean isProducing() {
        return producing;
    }

    public void remove() {
        if (legacySmokeEmitter != null) {
            building.getOwner().getWorld().getAnimationManagerRealTime().removeAnimation(legacySmokeEmitter);
        }
    }

    public final void animate(float t) {
        Deque<BuildProductionContainer> build_list = new ArrayDeque<>();
        for (var production_container : production_containers) {
            if (production_container.getNumSupplies() > 0 && production_container.hasEnoughSupplies()) {
                build_list.add(production_container);
            }
        }

        if (!build_list.isEmpty()) {
            if (break_time <= 0) {
                if (building.getOwner().getWorld().getRandom().nextFloat() < BREAK_PROBABILITY) {
                    break_time = building.getOwner().getWorld().getRandom().nextFloat() * MAX_BREAK_TIME;
                    producing = false;
                    if (legacySmokeEmitter != null) {
                        legacySmokeEmitter.stop();
                    }
                } else {
                    producing = true;
                    if (legacySmokeEmitter != null) {
                        legacySmokeEmitter.start();
                    }
                }
            }
            float man_seconds_per_container = unit_container.getNumSupplies() * t / build_list.size();
            while (!build_list.isEmpty()) {
                build_list.pop().build(man_seconds_per_container);
            }
        } else {
            producing = false;
            if (legacySmokeEmitter != null) {
                legacySmokeEmitter.stop();
            }
        }
        break_time -= t;
    }

    private static final class LegacySmokeEmitter implements Animated {
        private final World world;
        private float particleCounter = 0f;
        private boolean started = false;
        private float xAngle = 0f;
        private float yAngle = 0f;

        LegacySmokeEmitter(World world) {
            this.world = world;
        }

        void start() {
            started = true;
        }

        void stop() {
            started = false;
        }

        @Override
        public void animate(float t) {
            if (started) {
                particleCounter += 6f * t;
            }
            while (particleCounter >= 1f && started) {
                initParticle();
                particleCounter -= 1f;
            }
        }

        private void initParticle() {
            randomizeAcceleration();
            world.getRandom().nextInt(1);
        }

        private void randomizeAcceleration() {
            Random random = world.getRandom();
            float angleMaxJump = 0.1f;
            float angleBound = 1.5f;
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
