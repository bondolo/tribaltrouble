package com.oddlabs.tt.client.render;

import com.oddlabs.tt.effects.particle.BalancedParametricEmitter;
import com.oddlabs.tt.effects.particle.StunFunction;
import com.oddlabs.tt.effects.render.EmitterAttachedAccessory;
import com.oddlabs.tt.engine.render.Accessory;
import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.simulation.behaviour.StunController;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Abilities;
import com.oddlabs.tt.simulation.model.Unit;
import com.oddlabs.util.Color;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * {@link VisualModel} implementation for units managing carried resources, stun particle effects, and dynamic status
 * accessories.
 */
public final class UnitVisualModel extends AbstractVisualModel {
    private final Unit unit;

    public UnitVisualModel(Unit unit) {
        super(unit);
        this.unit = unit;
        if (unit.getAbilities().hasAbilities(Abilities.BUILD)) {
            addAccessory(new CarriedResourceAccessory(unit));
        }
    }

    /**
     * Updates stun star emitter attachment based on the unit's active stun state.
     *
     * @param world the active game world
     */
    public void updateStunStars(World world) {
        if (unit.getCurrentController() instanceof StunController stunController) {
            boolean hasStunStar = false;
            for (Accessory acc : getAccessories()) {
                if (acc instanceof EmitterAttachedAccessory) {
                    hasStunStar = true;
                    break;
                }
            }
            if (!hasStunStar) {
                float timeLeft = stunController.getTime();
                float velocity = (float) Math.PI / 2;
                BalancedParametricEmitter emitter = new BalancedParametricEmitter(
                        world,
                        new StunFunction(.4f, .15f), new Vector3f(0f, 0f, 0f),
                        velocity, 5f, (float) Math.PI * 2, (float) Math.PI * 2,
                        5, 0f, 2f,
                        Color.Linear.WHITE, Color.LinearDelta.ZERO,
                        new Vector3f(.1f, .1f, .1f), new Vector3f(0f, 0f, 0f), timeLeft,
                        GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                        AssetRegistry.getInstance().getStarTextures());

                float mountOffset = unit.getMountOffset();
                var offset = new Vector3f(
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
