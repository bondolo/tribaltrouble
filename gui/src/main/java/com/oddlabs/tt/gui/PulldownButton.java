package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

/**
 * Dropdown selection button managing opening and positioning of a {@link PulldownMenu}.
 */
public final class PulldownButton<T> extends GUIObject {
    private final PulldownMenu<T> menu;
    private final Label label;
    private final GUIRoot gui_root;
    private final Skin skin;
    private @Nullable PulldownItem<T> openedItem = null;

    public PulldownButton(GUIRoot gui_root, PulldownMenu<T> menu, int width) {
        this.menu = menu;
        this.gui_root = gui_root;
        this.skin = gui_root.getSkin();
        setCanFocus(true);
        menu.addItemChosenListener(this::itemChosen);
        label = new Label("", skin.getEditFont(), 0, Origin.AT_START);
        addChild(label);
        setDim(width, skin.getPulldownData().pulldownButton().getHeight());
    }

    public PulldownButton(GUIRoot gui_root, PulldownMenu<T> menu, int item_index, int width) {
        this(gui_root, menu, width);
        menu.chooseItem(item_index);
    }

    @Override
    protected Skin getSkin() {
        return skin;
    }

    @Override
    public PulldownButton<T> setDim(int width, int height) {
        super.setDim(width, height);
        PulldownData data = skin.getPulldownData();
        label.setDim(getWidth() - data.textOffsetLeft() - data.arrowOffsetRight() - data.arrow().quad(
                ModeIconQuads.Mode.NORMAL).getWidth(), label.getHeight());
        label.setPos(data.textOffsetLeft(), (getHeight() - label.getHeight()) / 2);
        if (menu.getWidth() < width)
            menu.setDim(width, menu.getHeight());
        return this;
    }

    @Override
    protected @Nullable GUIObject pick(float x, float y) {
        if (isMenuOpen()) {
            return null;
        }
        return super.pick(x, y);
    }

    @Override
    protected void render(GUIRenderer renderer, float clip_left, float clip_right, float clip_bottom,
            float clip_top) {
        if (isMenuOpen()) {
            return;
        }
        super.render(renderer, clip_left, clip_right, clip_bottom, clip_top);
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        PulldownData data = skin.getPulldownData();
        Horizontal pulldownButton = data.pulldownButton();

        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isActive()
                        ? ModeIconQuads.Mode.ACTIVE
                : ModeIconQuads.Mode.NORMAL;

        pulldownButton.render(renderer, 0, 0, getWidth(), skinMode);

        IconQuad arrowQuad = data.arrow().quad(skinMode);
        renderer.drawIcon(arrowQuad, getWidth() - data.arrowOffsetRight() - arrowQuad.getWidth(), 0);
    }

    @Override
    protected void mousePressed(MouseButton button, int x, int y) {
        if (isMenuOpen()) {
            deactivateMenu();
        } else {
            activateMenu();
        }
    }

    @Override
    protected void mouseReleased(MouseButton button, int x, int y) {
        if (isMenuOpen()) {
            menu.getChosenItem().ifPresent(GUIObject::setFocus);
            menu.clickItemExcept(button, x, y, 1, openedItem);
        }
        openedItem = null;
    }

    private boolean isMenuOpen() {
        return menu.getParent() != null;
    }

    private void activateMenu() {
        PulldownItem<T> currentItem = menu.getChosenItem()
                .orElse(menu.getSize() > 0 ? menu.getItem(0).orElse(null) : null);
        openedItem = currentItem;
        int item_y = currentItem != null ? currentItem.getY() : 0;
        int item_height = currentItem != null ? currentItem.getHeight() : getHeight();
        int menu_x = Math.round(getRootX());
        int menu_y = Math.round(getRootY() + (getHeight() - item_height) / 2f - item_y);
        int max_x = Math.max(0, gui_root.getWidth() - menu.getWidth());
        int max_y = Math.max(0, gui_root.getHeight() - menu.getHeight());
        menu.setPos(Math.clamp(menu_x, 0, max_x), Math.clamp(menu_y, 0, max_y));
        menu.setReturnFocusTarget(this);
        var modal_delegate = gui_root.getModalDelegate();
        if (modal_delegate != null) {
            modal_delegate.addChild(menu);
        } else {
            gui_root.addChild(menu);
        }
        menu.getChosenItem().ifPresentOrElse(GUIObject::setFocus, menu::setFocus);
    }

    private void deactivateMenu() {
        openedItem = null;
        setFocus();
        menu.remove();
    }

    public PulldownMenu<T> getMenu() {
        return menu;
    }

    @Override
    protected void doRemove() {
        super.doRemove();
        openedItem = null;
        if (isMenuOpen()) {
            menu.remove();
        }
    }

    public void setLabelColor(Color color) {
        label.setColor(color);
    }

    private void itemChosen(PulldownMenu<T> menu, int item_index) {
        menu.getItem(item_index).ifPresent(item -> {
            label.set(item.getLabelString());
            label.setColor(item.getLabelColor());
            if (isMenuOpen()) {
                deactivateMenu();
            }
        });
    }
}
