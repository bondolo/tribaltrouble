package com.oddlabs.tt.gui;

import com.oddlabs.tt.gui.event.MouseClickListener;

import static com.oddlabs.tt.gui.Placement.BOTTOM_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

/**
 * Modal confirmation dialog presenting a question with OK and Cancel buttons.
 */
public class QuestionForm extends Form {
    private final HorizButton yes_button;

    public QuestionForm(GUIRoot guiRoot, String message, MouseClickListener yes_action) {
        super(guiRoot);
        var font = skin.getEditFont();
        int message_width = font.getWidth(message);
        LabelBox info_label = new LabelBox(message, font, Math.min(400, message_width));
        addChild(info_label);
        Group button_group = new Group(getGUIRoot());
        yes_button = new OKButton(guiRoot, 80);
        yes_button.addMouseClickListener(new OKListener(this));
        yes_button.addMouseClickListener(yes_action);
        button_group.addChild(yes_button);
        HorizButton no_button = new CancelButton(guiRoot, 80);
        no_button.addMouseClickListener((_, _, _, _) -> this.cancel());
        button_group.addChild(no_button);
        yes_button.place();
        no_button.place(yes_button, RIGHT_MID);
        button_group.compileCanvas();
        addChild(button_group);

        // Place objects
        info_label.place();
        button_group.place(info_label, BOTTOM_MID);

        compileCanvas();
        centerPos();
    }


    @Override
    public void setFocus(FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            yes_button.setFocus(direction);
        }
    }

    public final void connectionLost() {
        remove();
    }
}
