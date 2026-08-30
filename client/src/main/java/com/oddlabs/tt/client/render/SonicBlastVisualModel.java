package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.effects.particle.SonicBlastEffect;
import com.oddlabs.tt.engine.render.AnimatedAccessory;
import com.oddlabs.tt.engine.render.CameraState;
import com.oddlabs.tt.engine.render.SonicBlastAccessory;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.resource.AudioAssets;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.weapon.SonicBlast;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * {@link VisualModel} implementation for sonic blasts managing expanding shockwaves and sound effects.
 */
public final class SonicBlastVisualModel extends AbstractVisualModel implements AnimatedAccessory, SonicBlastAccessory {
    private final SonicBlast blast;
    private final AudioImplementation audio;
    private @Nullable SonicBlastEffect effect;
    private final AudioPlayer lur;
    private final AudioPlayer rumble;

    public SonicBlastVisualModel(SonicBlast blast, AudioImplementation audio) {
        super(blast);
        this.blast = blast;
        this.audio = audio;
        World world = blast.getWorld();
        this.lur = audio.newAudio(blast.getPositionX(), blast.getPositionY(), blast.getPositionZ(),
                AudioAssets.SONIC_BLAST_LUR[world.getRandom().nextInt(AudioAssets.SONIC_BLAST_LUR.length)]);
        this.rumble = audio.newAudio(blast.getPositionX(), blast.getPositionY(), blast.getPositionZ(),
                AudioAssets.SONIC_BLAST_RUMBLE);
        triggerBlast(blast.getPositionX(), blast.getPositionY(), blast.getPositionZ(), blast.getHitRadius(), blast
                .getSeconds());
    }

    @Override
    public void triggerBlast(float x, float y, float z, float radius, float duration) {
        if (effect != null) {
            effect.abort();
        }
        World world = blast.getWorld();
        this.effect = new SonicBlastEffect(world, new Vector3f(x, y, z), radius, duration);
        audio.newAudio(x, y, z, AudioAssets.SONIC_BLAST);
        lur.stop(10.0f);
        rumble.stop(15.0f);
    }

    @Override
    public void animate(float t) {
        if (effect != null && effect.isDead()) {
            effect = null;
        }
    }

    public @Nullable SonicBlastEffect getEffect() {
        return effect;
    }

    @Override
    protected boolean isSelfExpired() {
        return blast.isDead() && effect == null;
    }

    @Override
    public boolean isVisible(Model parent, CameraState camera) {
        return !blast.isDead() || effect != null;
    }

    @Override
    public void getRelativeTransform(Matrix4f dest, Model parent) {
    }

    @Override
    public @Nullable SpriteKey getSpriteRenderer() {
        return null;
    }

    @Override
    public void close() {
        super.close();
        if (effect != null) {
            effect.abort();
        }
        lur.stop(15.0f);
        rumble.stop(15.0f);
    }
}
