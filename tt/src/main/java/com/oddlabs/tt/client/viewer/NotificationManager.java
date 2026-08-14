package com.oddlabs.tt.client.viewer;

import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.client.gui.GUIRoot;
import com.oddlabs.tt.engine.audio.AudioImplementation;
import com.oddlabs.tt.net.BeaconListener;
import com.oddlabs.tt.simulation.model.Selectable;
import com.oddlabs.tt.simulation.player.Player;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the creation and lifecycle of all in-game notifications, such as attack warnings and beacon alerts.
 */
public final class NotificationManager implements BeaconListener {
    private final List<@NonNull AttackNotification> attack_notifies = new ArrayList<>();
    private final List<@NonNull Notification> notifies = new ArrayList<>();
    private final GUIRoot gui_root;
    private final AudioImplementation audio;
    private @Nullable Notification latest_notification = null;

    public NotificationManager(GUIRoot gui_root, AudioImplementation audio) {
        this.gui_root = gui_root;
        this.audio = audio;
    }

    public @Nullable Notification getLatestNotification() {
        return latest_notification;
    }

    public void newAttackNotification(@NonNull AnimationManager manager, @NonNull Selectable<?> target,
            @NonNull Player local_player) {
        for (AttackNotification current : attack_notifies) {
            if (current.contains(target)) {
                current.restartTimer();
                return;
            }
        }
        addNotification(new AttackNotification(local_player, audio, gui_root, target, this, manager), attack_notifies);
    }

    public void newSelectableNotification(@NonNull Selectable<?> s, @NonNull AnimationManager manager,
            @NonNull Player local_player) {
        newNotification(manager, local_player, s.getPositionX(), s.getPositionY(), Color.Standard.GREEN, false);
    }

    @Override
    public void newBeacon(@NonNull AnimationManager manager, @NonNull Player local_player, float x, float y) {
        newNotification(manager, local_player, x, y, Color.Standard.BLUE, true);
    }

    private void newNotification(@NonNull AnimationManager manager, @NonNull Player local_player, float x, float y,
            @NonNull Color color, boolean show_always) {
        var params = AssetRegistry.getInstance().getBuildingNotificationAudio(local_player.getPlayerInfo().getRace());
        var notification = new Notification(local_player.getWorld().getHeightMap(), audio, gui_root, x, y, this,
                color, params, show_always, manager);
        addNotification(notification, notifies);
    }

    private <N extends @NonNull Notification> void addNotification(N notification, @NonNull List<N> list) {
        list.add(notification);
        latest_notification = notification;
    }

    void removeAttackNotification(@NonNull AttackNotification current) {
        attack_notifies.remove(current);
    }

    public void removeNotification(@NonNull Notification current) {
        notifies.remove(current);
    }
}
