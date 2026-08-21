package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.gui.render.TextLineRenderer;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.util.Color;

/** A GUI object that is used to provide a text label another GUI object. */
public class Label extends TextField implements Comparable<Label> {
    public static final Color.Linear DEFAULT_COLOR = Color.Linear.WHITE;
    private static final int INSET = 2;

    private final Origin align;

    private Color.Linear color = DEFAULT_COLOR;

    public Label(CharSequence text, Font font) {
        this(text, font, font.getWidth(text), Origin.AT_START);
    }

    public Label(CharSequence text, Font font, int width) {
        this(text, font, width, Origin.AT_START);
    }

    public Label(CharSequence text, Font font, int width, Origin align) {
        super(text, font, Integer.MAX_VALUE);
        this.align = align;
        setDim(width, font.getHeight());
    }

    public final Label setColor(Color color) {
        this.color = color instanceof Color.Linear linear ? linear : new Color.Linear(color);
        return this;
    }

    public Color getColor() {
        return color;
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        var c = isDisabled() ? color.desaturate(0.3f).mul(0.5f).alpha(color.a() * 0.8f) : color;
        int textWidth = getFont().getWidth(getText());
        int x = switch (align) {
            case AT_START -> 0;
            case AT_MIDDLE -> (getWidth() - Math.min(getWidth(), textWidth)) / 2;
            case AT_END -> getWidth() - textWidth - INSET;
        };
        TextLineRenderer.render(renderer, getFont(), getText(), x, 0, 0, getWidth() - INSET, c);
    }

    @Override
    public int compareTo(Label o) {
        return getText().toString().compareToIgnoreCase(o.getText().toString());
    }
}
