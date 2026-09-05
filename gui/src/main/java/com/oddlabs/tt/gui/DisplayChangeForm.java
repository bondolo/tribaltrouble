package com.oddlabs.tt.gui;

import com.oddlabs.tt.base.util.Utils;

import java.util.ResourceBundle;
import java.util.function.Consumer;

import static com.oddlabs.tt.gui.Origin.AT_END;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;

/**
 * Modal dialog confirming immediate vs. delayed display/resolution changes.
 */
public final class DisplayChangeForm extends Form {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(DisplayChangeForm.class.getName());

    private String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final Consumer<Boolean> changeHandler;
    private final HorizButton later_button;

    public DisplayChangeForm(Consumer<Boolean> changeHandler) {
        this.changeHandler = changeHandler;
        LabelBox info_label = new LabelBox(i18n("warning_message"), Skin.getSkin().getEditFont(), 500);
        addChild(info_label);
        HorizButton now_button = new HorizButton(i18n("now"), 120);
        addChild(now_button);
        now_button.addMouseClickListener((_, _, _, _) -> {
            remove();
            changeHandler.accept(true);
        });
        later_button = new HorizButton(i18n("later"), 120);
        addChild(later_button);
        later_button.addMouseClickListener((_, _, _, _) -> this.cancel());

        // Place objects
        info_label.place();
        now_button.place(AT_END);
        later_button.place(now_button, LEFT_MID);

        compileCanvas();
        centerPos();
    }

    @Override
    public void setFocus(FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            later_button.setFocus(direction);
        }
    }

    @Override
    protected void doCancel() {
        changeHandler.accept(false);
    }
}
