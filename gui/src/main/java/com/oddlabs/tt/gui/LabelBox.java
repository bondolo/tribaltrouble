package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.font.TextLayout;
import com.oddlabs.tt.gui.render.TextLineRenderer;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.util.Color;

/** A GUI Label that provides a text label drawn in a box. */
public class LabelBox extends TextField implements Comparable<LabelBox>, Clipped {
    private TextLayout textLayout;

    private Color.Linear color = Color.Linear.WHITE;

    public LabelBox(CharSequence text, Font font, int width) {
        super(text, font, Integer.MAX_VALUE);
        textLayout = new TextLayout(font, text, width);
        setDim(width, textLayout.getTextHeight());
    }

    private void updateLayout() {
        textLayout = new TextLayout(getFont(), getText(), getWidth());
        setDim(getWidth(), textLayout.getTextHeight());
    }

    @Override
    public LabelBox setText(CharSequence text) {
        super.setText(text);
        updateLayout();
        return this;
    }

    @Override
    public final LabelBox setDim(int width, int height) {
        super.setDim(width, height);
        return this;
    }

    public final LabelBox setColor(Color color) {
        this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
        return this;
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        var c = isDisabled() ? color.desaturate(0.3f).mul(0.5f).alpha(color.a() * 0.8f) : color;
        TextLineRenderer.render(renderer, textLayout, 0, getHeight() - getFont().getHeight(), c);
    }

    @Override
    public int compareTo(LabelBox o) {
        return getText().toString().compareToIgnoreCase(o.getText().toString());
    }
}
