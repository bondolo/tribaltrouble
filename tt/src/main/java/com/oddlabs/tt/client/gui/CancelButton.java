package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.util.Utils;

import java.util.ResourceBundle;

public final class CancelButton extends HorizButton {
    public CancelButton(int width) {
        super(Utils.getBundleString(ResourceBundle.getBundle(CancelButton.class.getName()), "cancel"), width);
    }
}
