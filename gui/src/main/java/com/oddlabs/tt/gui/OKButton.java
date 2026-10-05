package com.oddlabs.tt.gui;

import com.oddlabs.tt.base.util.Utils;

import java.util.ResourceBundle;

/**
 * Standard confirmation push button labelled with localized "OK".
 */
public class OKButton extends HorizButton {
    public OKButton(GUIRoot guiRoot, int width) {
        super(guiRoot, Utils.getBundleString(ResourceBundle.getBundle(OKButton.class.getName()), "ok"), width);
    }
}
