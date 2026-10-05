package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import org.jspecify.annotations.Nullable;

/**
 * Group container framed with bordered background quads and optional caption.
 */
public final class BorderGroup extends Group {
    private final @Nullable Label label;

    public BorderGroup(GUIRoot guiRoot, @Nullable String caption) {
        super(guiRoot);
        if (caption != null) {
            GroupData data = skin.getGroupData();
            this.label = new Label(caption, data.captionFont());
        } else {
            this.label = null;
        }
    }

    public BorderGroup(GUIRoot guiRoot) {
        this(guiRoot, null);
    }

    @Override
    public void compileCanvas() {
        GroupData data = skin.getGroupData();
        Box group = data.group();
        if (label != null) {
            super.compileCanvas(group.getLeftOffset(),
                    group.getBottomOffset(),
                    group.getRightOffset(),
                    group.getTopOffset() + data.captionOffset());
            label.setPos(data.captionLeft(), getHeight() - data.captionY());
            addChild(label);
        } else {
            super.compileCanvas(group.getLeftOffset(), group.getBottomOffset(), group.getRightOffset(), group
                    .getTopOffset());
        }
        setCanFocus(true);
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        skin.getGroupData().group().render(renderer, 0f, 0f, getWidth(), getHeight(),
                ModeIconQuads.Mode.NORMAL);
    }
}
