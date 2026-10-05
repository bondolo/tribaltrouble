package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Selectable item in a {@link PulldownMenu}.
 */
public final class PulldownItem<T> extends ButtonObject {
    private final Skin skin;
    private final Box itemBox;
    private final Label label;
    private final @Nullable T attachment;
    private @Nullable PulldownMenu<T> menu;

    public PulldownItem(GUIRoot guiRoot, String label_str) {
        this(guiRoot, label_str, null);
    }

    public PulldownItem(GUIRoot guiRoot, String label_str, @Nullable T attachment) {
        super(guiRoot.getSkin().getPulldownData().font());
        this.skin = Objects.requireNonNull(guiRoot.getSkin(), "Skin cannot be null");
        this.itemBox = skin.getPulldownData().pulldownItem();
        this.attachment = attachment;
        label = new Label(label_str, getFont(), 0, Origin.AT_START);
        addChild(label);
        setDim(0, label.getHeight());
    }

    @Override
    protected Skin getSkin() {
        return skin;
    }

    public @Nullable T getAttachment() {
        return attachment;
    }

    public int getTextHeight() {
        return label.getHeight();
    }

    public int getTextWidth() {
        return label.getTextWidth();
    }

    @Override
    public PulldownItem<T> setDim(int width, int height) {
        super.setDim(width, height);
        label.setDim(getWidth() - itemBox.getLeftOffset() - itemBox.getRightOffset(), label.getHeight());
        label.setPos(itemBox.getLeftOffset(), (getHeight() - label.getHeight()) / 2);
        return this;
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.NORMAL
                : isActive() || isHovered()
                        ? ModeIconQuads.Mode.ACTIVE
                : ModeIconQuads.Mode.NORMAL;
        itemBox.render(renderer, 0f, 0f, getWidth(), getHeight(), skinMode);
    }

    public void setLabelString(CharSequence label_str) {
        label.set(label_str);
    }

    public CharSequence getLabelString() {
        return label;
    }

    public Color getLabelColor() {
        return label.getColor();
    }

    public void setLabelColor(Color color) {
        label.setColor(color);
    }

    void setMenu(@Nullable PulldownMenu<T> menu) {
        this.menu = menu;
    }

    @Override
    protected void mouseClicked(MouseButton button, int x, int y, int clicks) {
        if (menu != null) {
            menu.chooseItem(this);
        }
    }
}
