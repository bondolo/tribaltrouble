package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.IconButton;
import com.oddlabs.tt.gui.IconDisabler;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.TextField;
import com.oddlabs.tt.gui.ToolTipBox;
import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.gui.event.MouseButtonListener;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.base.util.Utils;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.ResourceBundle;

/** A spinner control with an associated icon. */
public abstract class IconSpinner extends GUIObject {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(IconSpinner.class.getName());

    private String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final ModeIconQuads icon_quad;
    private final String tool_tip;
    private final @Nullable List<IconQuad> tool_tip_icons;
    private final TextField label;
    private final IconButton button_plus;
    private final IconButton button_minus;
    private final WorldViewer viewer;
    private @Nullable IconDisabler icon_disabler = null;

    private int text_count = 0;

    public IconSpinner(WorldViewer viewer, ModeIconQuads icon_quad, String tool_tip,
            @Nullable List<IconQuad> tool_tip_icons,
            GameAction action, GameAction dec_action) {
        this.icon_quad = icon_quad;
        this.tool_tip = tool_tip;
        this.tool_tip_icons = tool_tip_icons;
        this.viewer = viewer;
        setCanFocus(true);
        setDim(icon_quad.quad(ModeIconQuads.Mode.NORMAL).getWidth(), icon_quad.quad(ModeIconQuads.Mode.NORMAL)
                .getHeight());

        button_plus = new IconSpinnerButton(Skin.getSkin().getPlusButton(), action,
                () -> i18n("increase", viewer.getInputManager().getBindingString(action)),
                this);
        button_plus.setPos(0, 0);
        button_plus.addMouseButtonListener(new IncreaseListener());
        addChild(button_plus);

        button_minus = new IconSpinnerButton(Skin.getSkin().getMinusButton(), dec_action,
                () -> i18n("decrease", viewer.getInputManager().getBindingString(dec_action)),
                this);
        button_minus.setPos(button_plus.getWidth(), 0);
        button_minus.addMouseButtonListener(new DecreaseListener());
        addChild(button_minus);

        label = new Label("", Skin.getSkin().getHeadlineFont(), icon_quad.quad(ModeIconQuads.Mode.NORMAL).getWidth(),
                Origin.AT_MIDDLE);
        label.setPos(0, (getHeight() - label.getHeight()) / 2);
        addChild(label);
    }

    @Override
    public final void setFocus() {
        if (viewer.getGUIRoot().getDelegate() instanceof GUIObject obj) {
            obj.setFocus();
        }
    }

    public final void setIconDisabler(IconDisabler icon_disabler) {
        this.icon_disabler = icon_disabler;
    }

    public final void doUpdate() {
        setCount();
        if (icon_disabler != null) {
            setDisabled(computeCount() == 0 && getOrderSize() == 0 && icon_disabler.isDisabled());
        }
    }

    public abstract int computeCount();

    protected abstract void increase(int amount);

    protected abstract void decrease(int amount);

    protected abstract void release();

    protected abstract int getOrderSize();

    public abstract boolean renderInfinite();

    protected abstract float getProgress();

    private void setCount() {
        int count = computeCount();
        if (count != text_count) {
            text_count = count;
            label.clear();
            if (text_count != 0) {
                label.append(renderInfinite() ? "∞" : Integer.toString(text_count));
            }
        }
    }

    @Override
    public void appendToolTip(ToolTipBox tool_tip_box) {
        tool_tip_box.append(tool_tip);
        tool_tip_box.append(tool_tip_icons);
    }

    public final void shortcutPressed(boolean decrement, boolean batch) {
        if (!isDisabled()) {
            MouseButton mouse_button = batch ? MouseButton.RIGHT : MouseButton.LEFT;

            (decrement ? button_minus : button_plus).mousePressedAll(mouse_button, 0, 0);
        }
    }

    public final void shortcutReleased(boolean decrement, boolean batch) {
        if (!isDisabled()) {
            release();
        }
    }

    @Override
    protected final void renderGeometry(GUIRenderer renderer) {
        int x = (getWidth() - icon_quad.quad(ModeIconQuads.Mode.NORMAL).getWidth()) / 2;
        int y = (getHeight() - icon_quad.quad(ModeIconQuads.Mode.NORMAL).getHeight()) / 2;

        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isHovered()
                        ? ModeIconQuads.Mode.ACTIVE
                : ModeIconQuads.Mode.NORMAL;

        renderer.drawIcon(icon_quad.quad(skinMode), x, y);

        if (text_count > 0) {
            var watchQuad = GUIIcons.getIcons().getWatch(getProgress());
            renderer.drawIcon(watchQuad, getWidth() - watchQuad.getWidth(), getHeight() - watchQuad.getHeight());
        }
    }

    @Override
    protected final void mouseReleased(MouseButton button, int x, int y) {
    }

    @Override
    protected final void mousePressed(MouseButton button, int x, int y) {
    }

    @Override
    protected final void mouseClicked(MouseButton button, int x, int y, int clicks) {
    }

    @Override
    protected final void mouseHeld(MouseButton button, int x, int y) {
    }

    private final class IncreaseListener implements MouseButtonListener {
        @Override
        public void mouseClicked(MouseButton button, int x, int y, int clicks) {
        }

        @Override
        public void mouseHeld(MouseButton button, int x, int y) {
            mousePressed(button, x, y);
        }

        @Override
        public void mousePressed(MouseButton button, int x, int y) {
            increase(button == MouseButton.RIGHT ? 10 : 1);
        }

        @Override
        public void mouseReleased(MouseButton button, int x, int y) {
            release();
        }
    }

    private final class DecreaseListener implements MouseButtonListener {
        @Override
        public void mouseClicked(MouseButton button, int x, int y, int clicks) {
        }

        @Override
        public void mouseHeld(MouseButton button, int x, int y) {
            mousePressed(button, x, y);
        }

        @Override
        public void mousePressed(MouseButton button, int x, int y) {
            decrease(button == MouseButton.RIGHT ? 10 : 1);
        }

        @Override
        public void mouseReleased(MouseButton button, int x, int y) {
            release();
        }
    }
}
