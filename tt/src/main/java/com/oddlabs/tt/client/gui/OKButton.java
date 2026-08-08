package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.core.util.Utils;

import java.util.ResourceBundle;

public class OKButton extends HorizButton {
    public OKButton(int width) {
        super(Utils.getBundleString(ResourceBundle.getBundle(OKButton.class.getName()), "ok"), width);
    }
}
