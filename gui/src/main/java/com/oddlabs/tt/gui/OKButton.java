package com.oddlabs.tt.gui;

import com.oddlabs.tt.base.util.Utils;

import java.util.ResourceBundle;

public class OKButton extends HorizButton {
    public OKButton(int width) {
        super(Utils.getBundleString(ResourceBundle.getBundle(OKButton.class.getName()), "ok"), width);
    }
}
