package com.oddlabs.tt.content.skirmish;

import com.oddlabs.procedural.MapCode;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.event.MouseClickListener;
import com.oddlabs.tt.base.util.Utils;

import java.math.BigInteger;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;

import static com.oddlabs.tt.gui.Placement.BOTTOM_RIGHT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

/** Form dialog allowing the user to view, enter, or randomize a procedural map code. */
public final class MapcodeForm extends Form {
    private static final int BUTTON_WIDTH = 100;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(MapcodeForm.class.getName());

    private String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final TerrainMenu menu;
    private final GUIRoot gui_root;
    private final EditLine editline_seed;

    public MapcodeForm(GUIRoot gui_root, TerrainMenu menu) {
        super(gui_root);
        this.gui_root = gui_root;
        this.menu = menu;
        Label label_seed = new Label(i18n("map_code"), getSkin().getEditFont());
        editline_seed = new EditLine(gui_root, 200, 12, MapCode.CHAR_TO_WORD + MapCode.LOWER_CASE_CHARS,
                Origin.AT_START) {
            @Override
            protected boolean insert(int index, int codepoint) {
                return super.insert(index, Character.toUpperCase(codepoint));
            }

            @Override
            public boolean append(CharSequence text) {
                var shifted = text.toString().toUpperCase();
                return super.append(shifted);
            }
        };
        editline_seed.addEnterListener(_ -> done());

        HorizButton button_ok = new OKButton(gui_root, BUTTON_WIDTH);
        button_ok.addMouseClickListener((_, _, _, _) -> done());
        HorizButton button_cancel = new CancelButton(gui_root, BUTTON_WIDTH);
        button_cancel.addMouseClickListener((_, _, _, _) -> this.cancel());
        HorizButton button_rand = new HorizButton(gui_root, i18n("randomize"), BUTTON_WIDTH);
        button_rand.addMouseClickListener(new RandButtonListener());

        addChild(label_seed);
        addChild(editline_seed);
        addChild(button_ok);
        addChild(button_cancel);
        addChild(button_rand);
        label_seed.place();
        editline_seed.place(label_seed, RIGHT_MID);
        button_cancel.place(editline_seed, BOTTOM_RIGHT);
        button_ok.place(button_cancel, LEFT_MID);
        button_rand.place(button_ok, LEFT_MID);
        compileCanvas();
        centerPos();
    }

    @Override
    public void setFocus(FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            editline_seed.setFocus(direction);
        }
    }

    private void done() {
        remove();
        menu.parseMapcode(editline_seed.getContents());
        menu.setFocus();
    }

    private final class RandButtonListener implements MouseClickListener {
        @Override
        public void mouseClicked(MouseButton button, int x, int y, int clicks) {
            BigInteger rand_int = new BigInteger(60, ThreadLocalRandom.current());
            String rand_string = MapCode.createString(rand_int);
            editline_seed.clear();
            editline_seed.append(rand_string);
        }
    }
}
