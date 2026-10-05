package com.oddlabs.tt.gui;

import com.oddlabs.tt.base.util.Utils;
import org.jspecify.annotations.Nullable;

import java.util.ResourceBundle;
import java.util.function.Consumer;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.BOTTOM_MID;

/**
 * Modal warning dialog with an optional "Do not show this again" checkbox.
 */
public final class WarningForm extends Form {
    private static final int MAX_WIDTH = 500;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(WarningForm.class.getName());

    private String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final CheckBox show_next_time;
    private final HorizButton ok_button;

    public WarningForm(GUIRoot guiRoot, String head, String message, @Nullable Consumer<Boolean> onDismiss) {
        super(guiRoot);
        var headlineFont = skin.getHeadlineFont();
        var editFont = skin.getEditFont();
        int head_width = Math.min(MAX_WIDTH, headlineFont.getWidth(head));
        int message_width = Math.min(MAX_WIDTH, editFont.getWidth(message));
        int width = Math.max(head_width, message_width);

        Group group = new Group(getGUIRoot());
        addChild(group);
        LabelBox head_label = new LabelBox(head, headlineFont, width);
        group.addChild(head_label);
        LabelBox info_label = new LabelBox(message, editFont, width);
        group.addChild(info_label);
        show_next_time = new CheckBox(guiRoot, false, i18n("dont_show"));
        group.addChild(show_next_time);

        head_label.place();
        info_label.place(head_label, BOTTOM_LEFT);
        show_next_time.place(info_label, BOTTOM_LEFT);
        group.compileCanvas();

        ok_button = new OKButton(guiRoot, 70);
        addChild(ok_button);
        ok_button.addMouseClickListener((_, _, _, _) -> {
            if (onDismiss != null) {
                onDismiss.accept(show_next_time.isChecked());
            }
            remove();
        });
        // Place objects
        group.place();
        ok_button.place(group, BOTTOM_MID);

        // headline
        compileCanvas();
        centerPos();
    }

    public WarningForm(GUIRoot guiRoot, String head, String message) {
        this(guiRoot, head, message, null);
    }

    @Override
    public void setFocus(FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            ok_button.setFocus(direction);
        }
    }


}
