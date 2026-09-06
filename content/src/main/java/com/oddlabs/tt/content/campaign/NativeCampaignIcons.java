package com.oddlabs.tt.content.campaign;

import com.oddlabs.tt.gui.GUIErrorHandler;
import com.oddlabs.tt.gui.GUIIcon;
import com.oddlabs.tt.gui.IconAtlas;
import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;

/**
 * Loads and provides icon resources for the Native campaign map.
 */
final class NativeCampaignIcons implements CampaignIcons {
    private static final int NUM_ISLANDS = 8;

    private static final NativeCampaignIcons ICONS = new NativeCampaignIcons("/gui/native_campaign.xml");

    private final IconQuad map;
    private final MapIslandData[] islands = new MapIslandData[NUM_ISLANDS];
    private final IconQuad[] flags = new IconQuad[3];
    private final IconQuad[] boats = new IconQuad[3];
    private final GUIIcon[] hidden = new GUIIcon[1];
    private final IconQuad[] faces = new IconQuad[9];

    static NativeCampaignIcons getIcons() {
        return ICONS;
    }

    private NativeCampaignIcons(String xml_file) {
        IconAtlas atlas = IconAtlas.load(xml_file, new GUIErrorHandler());

        flags[0] = atlas.getNamedIconQuad("flag0");
        flags[1] = atlas.getNamedIconQuad("flag1");
        flags[2] = atlas.getNamedIconQuad("flag2");
        boats[0] = atlas.getNamedIconQuad("boat0");
        boats[1] = atlas.getNamedIconQuad("boat1");
        boats[2] = atlas.getNamedIconQuad("boat2");
        hidden[0] = getNamedGUIIcon(atlas, "hidden0");
        faces[0] = atlas.getNamedIconQuad("face0");
        faces[1] = atlas.getNamedIconQuad("face1");
        faces[2] = atlas.getNamedIconQuad("face2");
        faces[3] = atlas.getNamedIconQuad("face3");
        faces[4] = atlas.getNamedIconQuad("face4");
        faces[5] = atlas.getNamedIconQuad("face5");
        faces[6] = atlas.getNamedIconQuad("face6");
        faces[7] = atlas.getNamedIconQuad("face7");
        faces[8] = atlas.getNamedIconQuad("face8");

        map = atlas.getNamedIconQuad("map");
        for (int i = 0; i < NUM_ISLANDS; i++) {
            islands[i] = loadMapIslandData(atlas, "island" + i);
        }
    }

    private MapIslandData loadMapIslandData(IconAtlas atlas, String name) {
        IconAtlas.Element node = atlas.getElement(name);
        ModeIconQuads quads = node.getNamedIconQuads("island");
        IconAtlas.Element n = node.getElement("island");
        int texHeight = atlas.getTexture().getHeight();
        int x = n.getInt("x");
        int y = texHeight - n.getInt("y");
        int pin_index = n.getInt("pin_index");
        int pin_x = n.getInt("pin_x");
        int pin_y = texHeight - n.getInt("pin_y");
        return new MapIslandData(quads, x, y, flags[pin_index], boats[pin_index], pin_x, pin_y);
    }

    private GUIIcon getNamedGUIIcon(IconAtlas atlas, String name) {
        IconQuad temp = atlas.getNamedIconQuad(name);
        IconAtlas.Element n = atlas.getElement(name);
        int x = n.getInt("x");
        int y = atlas.getTexture().getHeight() - n.getInt("y");
        GUIIcon gui_icon = new GUIIcon(temp);
        gui_icon.setPos(x, y);
        return gui_icon;
    }

    @Override
    public GUIIcon[] getHiddenRoutes() {
        return hidden;
    }

    @Override
    public IconQuad[] getFaces() {
        return faces;
    }

    @Override
    public IconQuad getMap() {
        return map;
    }

    @Override
    public int getNumIslands() {
        return islands.length;
    }

    @Override
    public MapIslandData getMapIslandData(int i) {
        return islands[i];
    }
}
