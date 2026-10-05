package com.oddlabs.tt.gui;

import com.oddlabs.tt.base.util.Utils;

import java.util.ResourceBundle;

/**
 * Standard cancellation push button labelled with localized "Cancel".
 */
public final class CancelButton extends HorizButton {
    public CancelButton(GUIRoot guiRoot, int width) {
        super(guiRoot, Utils.getBundleString(ResourceBundle.getBundle(CancelButton.class.getName()), "cancel"), width);
    }
}
