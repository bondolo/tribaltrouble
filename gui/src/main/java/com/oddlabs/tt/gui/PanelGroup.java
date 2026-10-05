package com.oddlabs.tt.gui;

import com.oddlabs.tt.gui.event.MouseButtonListener;
import com.oddlabs.tt.engine.render.GUIRenderer;

import java.util.Objects;

/**
 * Container coordinating multiple tabbed panels with switching and focus management.
 */
public final class PanelGroup extends GUIObject {
    private final Skin skin;
    private final PanelData panelData;
    private final Group focus_group;
    private final PanelBox box;
    private final Panel[] panels;

    private int selected;

    public PanelGroup(GUIRoot guiRoot, Panel... panels) {
        this(guiRoot, 0, panels);
    }

    public PanelGroup(GUIRoot guiRoot, int selected, Panel... panels) {
        this.skin = Objects.requireNonNull(guiRoot.getSkin(), "Skin cannot be null");
        this.panelData = skin.getPanelData();
        this.focus_group = new Group(guiRoot);
        assert selected < panels.length && panels.length > 0 : "Invalid index selected.";
        this.panels = panels;

        int tab_height = panels[0].getTab().getHeight();
        int width = 0;
        int height = 0;
        for (Panel panel : panels) {
            if (width < panel.getWidth()) {
                width = panel.getWidth();
            }
            if (height < panel.getHeight()) {
                height = panel.getHeight();
            }
        }
        int total_height = height + tab_height;
        setDim(width, total_height);
        int x = panelData.leftTabOffset();
        int y = height;
        for (int i = 0; i < panels.length; i++) {
            panels[i].setPos((width - panels[i].getWidth()) / 2, panelData.bottomTabOffset()
                    + (height - panels[i].getHeight()) / 2);
            panels[i].getTab().setPos(x, y);
            x += panels[i].getTab().getWidth();
            panels[i].getTab().addMouseButtonListener(new TabListener(i));
        }
        box = new PanelBox(width, total_height - panels[0].getTab().getHeight() + panelData
                .bottomTabOffset());

        focus_group.setDim(width, total_height);
        focus_group.setPos(0, 0);
        addChild(focus_group);
        setCanFocus(true);
        selectPanel(selected);
    }

    @Override
    protected Skin getSkin() {
        return skin;
    }

    @Override
    public void setFocus(FocusDirection direction) {
        focus_group.setGroupFocus(direction);
    }

    public void cyclePanel(FocusDirection dir) {
        int intDir = (dir == FocusDirection.BACKWARD) ? -1 : 1;
        int next = (selected + intDir + panels.length) % panels.length;
        selectPanel(next);
    }

    private void selectPanel(int index) {
        focus_group.clearChildren();
        for (int i = 0; i < panels.length; i++) {
            if (i != index) {
                focus_group.addChild(panels[i].getTab());
                panels[i].getTab().select(false);
            }
        }
        focus_group.addChild(box);
        focus_group.addChild(panels[index].getTab());
        panels[index].getTab().select(true);
        focus_group.addChild(panels[index]);
        selected = index;
        panels[index].setFocus();
    }

    private final class PanelBox extends GUIObject {
        PanelBox(int width, int height) {
            setDim(width, height);
            setPos(0, 0);
        }

        @Override
        protected void renderGeometry(GUIRenderer renderer) {
            Box panelBox = panelData.box();
            panelBox.render(renderer, 0f, 0f, getWidth(), getHeight(), panels[selected].getTab().getRenderState());
        }
    }

    private final class TabListener implements MouseButtonListener {
        private final int index;

        TabListener(int index) {
            this.index = index;
        }

        @Override
        public void mousePressed(MouseButton button, int x, int y) {
            selectPanel(index);
        }

        @Override
        public void mouseReleased(MouseButton button, int x, int y) {
        }

        @Override
        public void mouseHeld(MouseButton button, int x, int y) {
        }

        @Override
        public void mouseClicked(MouseButton button, int x, int y, int clicks) {
        }
    }
}
