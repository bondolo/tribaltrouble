package com.oddlabs.tt.content.form;

import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.gui.event.*;
import com.oddlabs.tt.client.gui.*;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.base.util.Utils;

import java.util.ResourceBundle;

/** UI label indicating remaining time for a free match quit. */
public final class FreeQuitLabel extends Label implements Animated {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(FreeQuitLabel.class.getName());

    private String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final World world;
    private final AnimationManager manager;

    public FreeQuitLabel(World world, AnimationManager manager) {
        super("", Skin.getSkin().getEditFont(), 300);
        this.world = world;
        this.manager = manager;
    }

    @Override
    protected void doAdd() {
        super.doAdd();
        manager.registerAnimation(this);
    }

    @Override
    protected void doRemove() {
        super.doRemove();
        manager.removeAnimation(this);
    }

    @Override
    public void animate(float t) {
        int time_left = (int) PeerHub.getFreeQuitTimeLeft(world);
        if (time_left > 0) {
            clear();
            append(i18n("quit_time_left", time_left));
        }
    }
}
