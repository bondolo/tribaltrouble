package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.gui.event.CheckBoxListener;
import com.oddlabs.tt.engine.render.GUIRenderer;

import java.util.List;

public final class CheckBox extends GUIObject {
    private final List<CheckBoxListener> event_listeners = new java.util.ArrayList<>();

    private boolean checked;
    private boolean pressed = false;

    public CheckBox(boolean checked, String text) {
        this(checked, text, "");
    }

    public CheckBox(boolean checked, String text, String tool_tip) {
        super(!tool_tip.isEmpty() ? () -> tool_tip : null);
        this.checked = checked;
        Label label = new Label(text, Skin.getSkin().getEditFont());
        addChild(label);
        label.setPos(Skin.getSkin().getCheckBoxChecked().quad(ModeIconQuads.Mode.NORMAL).getWidth(), (Skin.getSkin()
                .getCheckBoxChecked().quad(ModeIconQuads.Mode.NORMAL).getHeight() - label.getHeight()) / 2);
        setDim(Skin.getSkin().getCheckBoxChecked().quad(ModeIconQuads.Mode.NORMAL).getWidth() + label.getWidth(), Skin
                .getSkin().getCheckBoxChecked().quad(ModeIconQuads.Mode.NORMAL).getHeight());
        setCanFocus(true);
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        if (checked != this.checked) {
            this.checked = checked;
            checkedAll(checked);
        }
    }

    private void toggleChecked() {
        checked = !checked;
        checkedAll(checked);
    }

    @Override
    protected void mouseClicked(MouseButton button, int x, int y, int clicks) {
        toggleChecked();
    }

    @Override
    protected void mouseReleased(MouseButton button, int x, int y) {
        pressed = false;
    }

    @Override
    protected void mousePressed(MouseButton button, int x, int y) {
        pressed = true;
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isActive()
                        ? ModeIconQuads.Mode.ACTIVE
                : ModeIconQuads.Mode.NORMAL;

        // When checked, active, pressed, and hovered, it should show the unchecked state
        // When unchecked, active, pressed, and hovered, it should show the checked state
        ModeIconQuads quad_to_render = isChecked()
                ? (skinMode == ModeIconQuads.Mode.ACTIVE && pressed && isHovered()
                        ? Skin.getSkin().getCheckBoxUnchecked()
                        : Skin.getSkin().getCheckBoxChecked())
                : (skinMode == ModeIconQuads.Mode.ACTIVE && pressed && isHovered()
                        ? Skin.getSkin().getCheckBoxChecked()
                        : Skin.getSkin().getCheckBoxUnchecked());

        renderer.drawModeIcon(quad_to_render, skinMode, 0, 0);
    }

    public void checkedAll(boolean checked) {
        checked(checked);
        for (var listener : event_listeners) {
            listener.checked(checked);
        }
    }

    void checked(boolean checked) {
        /*
        		GUIObject parent = (GUIObject)getParent();
        		if (parent != null)
        			parent.checkedAll(checked);
        */
    }

    public void addCheckBoxListener(CheckBoxListener listener) {
        event_listeners.add(listener);
    }

    public void removeCheckBoxListener(CheckBoxListener listener) {
        event_listeners.remove(listener);
    }
}
