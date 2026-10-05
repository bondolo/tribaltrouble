package com.oddlabs.tt.gui;

import com.oddlabs.tt.gui.event.MouseClickListener;
import org.jspecify.annotations.Nullable;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.BOTTOM_MID;

/**
 * Modal message dialog displaying information or error text with an OK button.
 */
public class MessageForm extends Form {
    private static final int MAX_WIDTH = 500;
    private final HorizButton ok_button;

    public MessageForm(GUIRoot guiRoot, String head, String message, @Nullable String button,
            @Nullable MouseClickListener listener) {
        super(guiRoot);
        var headlineFont = skin.getHeadlineFont();
        var editFont = skin.getEditFont();
        int head_width = Math.min(MAX_WIDTH, headlineFont.getWidth(head));
        int message_width = Math.min(MAX_WIDTH, editFont.getWidth(message));
        int width = Math.max(head_width, message_width);

        LabelBox head_label = new LabelBox(head, headlineFont, width);
        addChild(head_label);
        LabelBox info_label = new LabelBox(message, editFont, width);
        addChild(info_label);
        if (button == null) {
            ok_button = new OKButton(guiRoot, 70);
            ok_button.addMouseClickListener(new OKListener(this));
        } else {
            ok_button = new HorizButton(guiRoot, button, 70);
            if (listener != null) {
                ok_button.addMouseClickListener(listener);
            }
        }
        addChild(ok_button);
        // Place objects
        head_label.place();
        info_label.place(head_label, BOTTOM_LEFT);
        ok_button.place(info_label, BOTTOM_MID);

        // headline
        compileCanvas();
        centerPos();
    }

    public MessageForm(GUIRoot guiRoot, String head, String message) {
        this(guiRoot, head, message, null, null);
    }

    public MessageForm(GUIRoot guiRoot, String message) {
        super(guiRoot);
        var editFont = skin.getEditFont();
        int width = Math.min(MAX_WIDTH, editFont.getWidth(message));
        LabelBox info_label = new LabelBox(message, editFont, width);
        addChild(info_label);
        ok_button = new OKButton(guiRoot, 70);
        addChild(ok_button);
        ok_button.place(info_label, BOTTOM_MID);
        ok_button.addMouseClickListener(new OKListener(this));
        // Place objects
        info_label.place();

        // headline
        compileCanvas();
        centerPos();
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
