package com.oddlabs.tt.content.form;

import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.OKListener;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.base.util.Utils;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;

public final class WelcomeForm extends Form {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(WelcomeForm.class.getName());

    private String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public WelcomeForm() {
        Label label_headline = new Label(i18n("welcome_caption"), Skin.getSkin().getHeadlineFont());
        addChild(label_headline);

        LabelBox box = new LabelBox(i18n("welcome_message"), Skin.getSkin().getEditFont(), 400);
        addChild(box);

        HorizButton ok_button = new OKButton(100);
        ok_button.addMouseClickListener(new OKListener(this));
        addChild(ok_button);

        // Place objects
        label_headline.place();
        box.place(label_headline, BOTTOM_LEFT);
        ok_button.place(Origin.AT_END);

        // headline
        compileCanvas();
        centerPos();
    }

}
