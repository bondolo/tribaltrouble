package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.engine.render.ModeIconQuads;

import java.util.Objects;

/**
 * Header button for a sortable column within a MultiColumnComboBox.
 */
public final class ColumnButton<T> extends RadioButtonGroupElement {
    private final Skin skin;
    private final MultiColumnComboBoxData data;
    private final RowCollection<T> rows;
    private final int arrow_offset;
    private final int column_index;

    private boolean sorted_descending;
    private boolean pressed = false;

    ColumnButton(Skin skin, RadioButtonGroup group, RowCollection<T> rows, ColumnInfo info,
            int column_index, boolean sorted_descending) {
        super(column_index == 0, group);
        this.skin = Objects.requireNonNull(skin, "Skin cannot be null");
        this.data = skin.getMultiColumnComboBoxData();
        this.rows = rows;
        this.column_index = column_index;
        this.sorted_descending = sorted_descending;
        setDim(info.width(), data.buttonUnpressed().getHeight());

        Font font = data.font();
        Label label = new Label(info.caption(), font);
        label.setPos(data.captionOffset(), (getHeight() - font.getHeight()) / 2 + 1);
        addChild(label);

        IconQuad arrow = data.descending().quad(ModeIconQuads.Mode.NORMAL);
        arrow_offset = info.width() - arrow.getWidth();
        setCanFocus(true);
    }

    @Override
    protected Skin getSkin() {
        return skin;
    }

    @Override
    protected void mouseReleased(MouseButton button, int x, int y) {
        pressed = false;
    }

    @Override
    protected void mousePressed(MouseButton button, int x, int y) {
        pressed = true;
    }

    @Override
    protected void mouseClicked(MouseButton button, int x, int y, int clicks) {
        sorted_descending = !isMarked() || !sorted_descending;
        super.mouseClicked(button, x, y, clicks);
        rows.markChanged(column_index, sorted_descending);
    }

    int getColumnIndex() {
        return column_index;
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isHovered() && pressed
                        ? ModeIconQuads.Mode.ACTIVE
                : isActive()
                        ? ModeIconQuads.Mode.ACTIVE
                : ModeIconQuads.Mode.NORMAL;

        Horizontal buttonHorizontal = skinMode == ModeIconQuads.Mode.ACTIVE && isHovered() && pressed
                ? data.buttonPressed()
                : data.buttonUnpressed();

        buttonHorizontal.render(renderer, 0, 0, getWidth(), skinMode);
        if (isMarked())
            renderMark(renderer, skinMode);
    }

    private void renderMark(GUIRenderer renderer, ModeIconQuads.Mode skinMode) {
        ModeIconQuads arrow = sorted_descending
                ? data.descending()
                : data.ascending();

        IconQuad arrowQuad = arrow.quad(skinMode);
        renderer.drawIcon(arrowQuad, arrow_offset, (getHeight() - arrowQuad.getHeight()) / 2f);
    }
}
