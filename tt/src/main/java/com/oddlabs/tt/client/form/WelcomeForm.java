package com.oddlabs.tt.client.form;

import com.oddlabs.tt.client.delegate.MainMenu;
import com.oddlabs.tt.client.gui.Form;
import com.oddlabs.tt.client.gui.GUIRoot;
import com.oddlabs.tt.client.gui.HorizButton;
import com.oddlabs.tt.client.gui.Label;
import com.oddlabs.tt.client.gui.LabelBox;
import com.oddlabs.tt.client.gui.OKButton;
import com.oddlabs.tt.client.gui.OKListener;
import com.oddlabs.tt.client.gui.Origin;
import com.oddlabs.tt.client.gui.Skin;
import com.oddlabs.tt.core.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

import static com.oddlabs.tt.client.gui.Placement.BOTTOM_LEFT;

public final class WelcomeForm extends Form {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(WelcomeForm.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public WelcomeForm(GUIRoot gui_root, MainMenu main_menu) {
        Label label_headline = new Label(i18n("welcome_caption"), Skin.getSkin().getHeadlineFont());
        addChild(label_headline);

        LabelBox box = new LabelBox(i18n("welcome_message"), Skin.getSkin().getEditFont(), 400);
        addChild(box);

        HorizButton ok_button = new OKButton(100);
        addChild(ok_button);
        ok_button.addMouseClickListener(new OKListener(this));

        // Place objects
        label_headline.place();
        box.place(label_headline, BOTTOM_LEFT);
        ok_button.place(Origin.AT_END);

        // headline
        compileCanvas();
        centerPos();
    }

}
