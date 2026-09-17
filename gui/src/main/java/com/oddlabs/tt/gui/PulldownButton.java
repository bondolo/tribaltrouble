package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.util.Color;

/**
 * A button representing a dropdown selection that opens a PulldownMenu.
 */
public final class PulldownButton<T> extends GUIObject {
    private final PulldownMenu<T> menu;
    private final Label label;
    private final GUIRoot gui_root;

    public PulldownButton(GUIRoot gui_root, PulldownMenu<T> menu, int width) {
        this.menu = menu;
        this.gui_root = gui_root;
        setCanFocus(true);
        menu.addItemChosenListener(this::itemChosen);
        label = new Label("", Skin.getSkin().getEditFont(), 0, Origin.AT_START);
        addChild(label);
        setDim(width, Skin.getSkin().getPulldownData().pulldownButton().getHeight());
    }

    public PulldownButton(GUIRoot gui_root, PulldownMenu<T> menu, int item_index, int width) {
        this(gui_root, menu, width);
        menu.chooseItem(item_index);
    }

    @Override
    public PulldownButton<T> setDim(int width, int height) {
        super.setDim(width, height);
        PulldownData data = Skin.getSkin().getPulldownData();
        label.setDim(getWidth() - data.textOffsetLeft() - data.arrowOffsetRight() - data.arrow().quad(
                ModeIconQuads.Mode.NORMAL).getWidth(), label.getHeight());
        label.setPos(data.textOffsetLeft(), (getHeight() - label.getHeight()) / 2);
        if (menu.getWidth() < width)
            menu.setDim(width, menu.getHeight());
        return this;
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        PulldownData data = Skin.getSkin().getPulldownData();
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
        }
        menu.clickItem(button, x, y, 1);
    }

    private boolean isMenuOpen() {
        return menu.getParent() != null;
    }

    private void activateMenu() {
        int menu_x = (int) (getRootX() + getWidth() - menu.getWidth());
        int menu_y = (int) (getRootY() - menu.getHeight());
        if (menu_y < 0) {
            menu_y = (int) (getRootY() + getHeight());
        }
        menu.setPos(menu_x, menu_y);
        var modal_delegate = gui_root.getModalDelegate();
        if (modal_delegate != null) {
            modal_delegate.addChild(menu);
        } else {
            gui_root.addChild(menu);
        }
        menu.getChosenItem().ifPresentOrElse(GUIObject::setFocus, menu::setFocus);
    }

    private void deactivateMenu() {
        setFocus();
        menu.remove();
    }

    public PulldownMenu<T> getMenu() {
        return menu;
    }

    @Override
    protected void doRemove() {
        super.doRemove();
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
