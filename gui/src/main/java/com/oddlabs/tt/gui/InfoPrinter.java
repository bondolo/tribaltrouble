package com.oddlabs.tt.gui;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.util.Color;

import java.util.ArrayList;
import java.util.List;

public final class InfoPrinter extends GUIObject implements Animated, com.oddlabs.tt.base.util.InfoPrinter {
    public static final Color.Linear PRIVATE_COLOR = new Color.Standard(0xFF_33_66_FF).linear();
    public static final Color.Linear TEAM_COLOR = new Color.Standard(0xFF_4C_7F_FF).linear();
    private static final float SECONDS_PER_TIMEOUT = 8f;

    private final Font font;
    private final List<LabelBox> history = new ArrayList<>();
    private final List<Float> timers = new ArrayList<>();
    private final int lines;
    private final GUIRoot gui_root;

    private float time;

    public InfoPrinter(GUIRoot gui_root, int lines, Font font) {
        this.gui_root = gui_root;
        this.lines = lines;
        this.font = font;
        displayChangedNotify(gui_root.getWidth(), gui_root.getHeight());
        gui_root.getAnimationManager().registerAnimation(this);
        time = 0;
    }

    public GUIRoot getGUIRoot() {
        return gui_root;
    }

    @Override
    protected void doRemove() {
        super.doRemove();
        gui_root.getAnimationManager().removeAnimation(this);
    }

    @Override
    protected void displayChangedNotify(int width, int height) {
        setDim(width, height);
    }

    @Override
    public void print(String text) {
        print(text, Color.Linear.TRANSPARENT);
    }

    public void print(String text, Color color) {
        int width = Math.min(font.getWidth(text), getWidth());
        LabelBox label_box = new BackgroundLabelBox(text, font, width);
        if (color.a() > .2f)
            label_box.setColor(color);
        addChild(label_box);
        history.add(label_box);
        timers.add(time + SECONDS_PER_TIMEOUT);

        while (history.size() > lines) {
            removeLine(0);
        }
        setLabelsPos();
    }

    private void removeLine(int index) {
        LabelBox label_box = history.get(index);
        label_box.remove();
        history.remove(index);
        timers.remove(index);
        setLabelsPos();
    }

    @Override
    public void animate(float t) {
        time += t;
        for (int i = timers.size() - 1; i >= 0; i--) {
            float remove_time = timers.get(i);
            if (time > remove_time) {
                removeLine(i);
            }
        }
    }

    private void setLabelsPos() {
        int y = getHeight();
        for (LabelBox label_box : history) {
            y -= label_box.getHeight();
            label_box.setPos(0, y);
        }
    }
}
