package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.gui.event.ItemChosenListener;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.engine.render.GUIRenderer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A dropdown menu group containing list items that can be chosen.
 */
public final class PulldownMenu<T> extends Group {
    private final Set<ItemChosenListener<T>> chosen_listeners = new CopyOnWriteArraySet<>();

    private final List<PulldownItem<T>> items = new ArrayList<>();
    private int chosen_item_index = -1;

    public PulldownMenu() {
        setCanFocus(true);
        setFocusCycle(true);
    }

    public Optional<PulldownItem<T>> getItem(int index) {
        return index >= 0 && index < items.size() ? Optional.of(items.get(index)) : Optional.empty();
    }

    public int getSize() {
        return items.size();
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        // Render bottom edge
        Horizontal bot = Skin.getSkin().getPulldownData().pulldownBottom();
        bot.render(renderer, 0, 0, getWidth(), ModeIconQuads.Mode.NORMAL);

        // Render top edge
        Horizontal top = Skin.getSkin().getPulldownData().pulldownTop();
        top.render(renderer, 0, getHeight() - top.getHeight(), getWidth(), ModeIconQuads.Mode.NORMAL);
    }

    public void addItem(PulldownItem<T> item) {
        items.add(item);
        addChild(item);
        item.setMenu(this);
        setDim(getWidth(), getHeight());
    }

    public void clear() {
        for (PulldownItem<T> item : items) {
            item.setMenu(null);
        }
        items.clear();
        clearChildren();
        chosen_item_index = -1;
        setDim(getWidth(), getHeight());
    }

    @Override
    public PulldownMenu<T> setDim(int width, int height) {
        int min_width = 0;
        Box item_box = Skin.getSkin().getPulldownData().pulldownItem();        // Adjust all items
        for (PulldownItem<T> item : items) {
            if (item.getTextWidth() > min_width)
                min_width = item.getTextWidth();
        }
        int item_pos_count = Skin.getSkin().getPulldownData().pulldownBottom().getHeight();
        min_width = Math.max(width, item_box.getLeftOffset() + min_width + item_box.getRightOffset());
        for (int i = 0; i < items.size(); i++) {
            PulldownItem<T> item = items.get(items.size() - 1 - i);
            int item_height = item_box.getBottomOffset() + item.getTextHeight() + item_box.getTopOffset();
            item.setDim(min_width, item_height);
            item.setPos(0, item_pos_count);
            item_pos_count += item_height;
        }
        int min_height = Math.max(height, item_pos_count + Skin.getSkin().getPulldownData().pulldownTop().getHeight());
        super.setDim(min_width, min_height);
        return this;
    }

    public Optional<PulldownItem<T>> getChosenItem() {
        return -1 != chosen_item_index ? Optional.of(items.get(chosen_item_index)) : Optional.empty();
    }

    /** {@return index of the chosen item, or -1 if no item is chosen} */
    public int getChosenItemIndex() {
        return chosen_item_index;
    }

    /**
     * Chooses an item by its index.
     *
     * @param index index of the item to choose or -1 to clear the chosen item
     */
    public void chooseItem(int index) {
        chosen_item_index = index;
        itemChosenAll();
    }

    /**
     * Chooses an item by its instance.
     *
     * @param item the item to choose, or null to clear the chosen item
     */
    public void chooseItem(@Nullable PulldownItem<T> item) {
        chooseItem(item != null ? items.indexOf(item) : -1);
    }

    @Override
    protected void focusNotify(boolean focus) {
        if (!focus) {
            remove();
        }
    }

    @Override
    protected void handleInput(InputEvent event) {
        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            if (event.consumeAction(GameAction.UI_NAV_UP)) {
                focusPrior();
                event.consume();
                return;
            }
            if (event.consumeAction(GameAction.UI_NAV_DOWN)) {
                focusNext();
                event.consume();
                return;
            }
        }
        super.handleInput(event);
    }

    // Sending click on to appropriate item when PulldownButton has been pressed and released on an item
    void clickItem(MouseButton button, int x, int y, int clicks) {
        for (PulldownItem<T> item : items) {
            if (item.isHovered())
                item.mouseClickedAll(button, x, y, clicks);
        }
    }

    public void itemChosenAll() {
        for (ItemChosenListener<T> listener : chosen_listeners) {
            listener.itemChosen(this, chosen_item_index);
        }
    }

    public void addItemChosenListener(ItemChosenListener<T> listener) {
        chosen_listeners.add(listener);
    }

    public void removeItemChosenListener(ItemChosenListener<T> listener) {
        chosen_listeners.remove(listener);
    }
}
