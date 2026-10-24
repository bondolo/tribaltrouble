package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.simulation.landscape.World;
import java.util.Random;

/**
 * Emulates legacy particle emitter PRNG consumption for deterministic validation.
 */
public final class LegacyOneShotEmitter implements Animated {
    private final World world;
    private final AnimationManager manager;
    private final int types;
    private final float angleBound;
    private final float angleMaxJump;
    private final float particlesPerSecond;
    private int numParticles;
    private float particleCounter = 0f;
    private float xAngle = 0f;
    private float yAngle = 0f;

    public LegacyOneShotEmitter(World world, AnimationManager manager, int numParticles,
            float particlesPerSecond, float angleBound, float angleMaxJump, int types) {
        this.world = world;
        this.manager = manager;
        this.numParticles = numParticles;
        this.particlesPerSecond = particlesPerSecond;
        this.angleBound = angleBound;
        this.angleMaxJump = angleMaxJump;
        this.types = types;
    }

    @Override
    public void animate(float t) {
        if (numParticles > 0) {
            particleCounter += particlesPerSecond * t;
        }
        while (particleCounter >= 1f && numParticles > 0) {
            initParticle();
            particleCounter -= 1f;
            numParticles--;
        }
        if (numParticles == 0) {
            manager.removeAnimation(this);
        }
    }

    private void initParticle() {
        randomizeVelocity();
        world.getRandom().nextInt(types);
    }

    private void randomizeVelocity() {
        Random random = world.getRandom();
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
