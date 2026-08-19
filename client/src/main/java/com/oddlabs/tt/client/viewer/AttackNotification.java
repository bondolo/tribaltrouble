package com.oddlabs.tt.client.viewer;

import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.base.animation.TimerAnimation;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.tt.simulation.model.Target;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

/**
 * Alerts the local player when one of their units or buildings is under attack.
 */
final class AttackNotification extends Notification {
    private static final float RADIUS = 30f;
    private static final float FADE_OUT = 5f;

    private boolean active = true;

    public AttackNotification(@NonNull Player local_player, @NonNull AudioImplementation audio,
            @NonNull GUIRoot gui_root, @NonNull Selectable<?> center,
            @NonNull NotificationManager manager, @NonNull AnimationManager animation_manager) {
        super(local_player.getWorld().getHeightMap(), audio, gui_root, center.getPositionX(), center.getPositionY(),
                manager,
                Color.Standard.RED,
                AssetRegistry.getInstance().getAttackNotificationAudio(local_player.getPlayerInfo().getRace()), false,
                animation_manager);
    }

    public boolean contains(@NonNull Target target) {
        float dx = getX() - target.getPositionX();
        float dy = getY() - target.getPositionY();
        float dist = dx * dx + dy * dy;
        return dist <= RADIUS * RADIUS;
    }

    public void restartTimer() {
        if (!active)
            getTimer().resetTime();
    }

    @Override
    public void update(@NonNull TimerAnimation anim) {
        if (active) {
            active = false;
            getArrow().remove();
            getTimer().setTimerInterval(FADE_OUT);
        } else {
            getTimer().stop();
            getManager().removeAttackNotification(this);
        }
    }
}
