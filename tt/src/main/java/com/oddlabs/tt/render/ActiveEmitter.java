package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.render.particle.Emitter;
import org.jspecify.annotations.NonNull;

/**
 * Client-side animation wrapper that updates a standalone emitter on the render thread
 * and removes it from the render queues once completed.
 */
public class ActiveEmitter implements Animated {
    private final @NonNull Emitter<?> emitter;
    private final @NonNull RenderQueues queues;
    private final @NonNull AnimationManager manager;

    public ActiveEmitter(@NonNull Emitter<?> emitter, @NonNull RenderQueues queues, @NonNull AnimationManager manager) {
        this.emitter = emitter;
        this.queues = queues;
        this.manager = manager;
        queues.addEmitter(emitter);
        manager.registerAnimation(this);
    }

    @Override
    public void animate(float t) {
        emitter.animate(t);
        if (emitter.isFinished()) {
            queues.removeEmitter(emitter);
            manager.removeAnimation(this);
        }
    }
}
