package com.oddlabs.tt.content.campaign.natives;

import com.oddlabs.tt.content.campaign.CampaignIcons;
import com.oddlabs.tt.content.campaign.MapIslandData;
import com.oddlabs.tt.gui.GUIErrorHandler;
import com.oddlabs.tt.gui.GUIIcon;
import com.oddlabs.tt.gui.IconAtlas;
import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;

import java.util.stream.IntStream;

/**
 * Loads and provides icon resources for the Native campaign map.
 */
public final class NativeCampaignIcons implements CampaignIcons {
    private static final int NUM_FLAGS = 3;
    private static final int NUM_BOATS = 3;
    private static final int NUM_HIDDEN = 1;
    private static final int NUM_FACES = 9;
    private static final int NUM_ISLANDS = 8;

    private final IconQuad map;
    private final MapIslandData[] islands;
    private final IconQuad[] flags;
    private final IconQuad[] boats;
    private final GUIIcon[] hidden;
    private final IconQuad[] faces;

    NativeCampaignIcons(String xml_file) {
        IconAtlas atlas = IconAtlas.load(xml_file, new GUIErrorHandler());

        flags = IntStream.range(0, NUM_FLAGS)
                .mapToObj(i -> "flag" + i)
                .map(atlas::getNamedIconQuad)
                .toArray(IconQuad[]::new);
        boats = IntStream.range(0, NUM_BOATS)
                .mapToObj(i -> "boat" + i)
                .map(atlas::getNamedIconQuad)
                .toArray(IconQuad[]::new);
        hidden = IntStream.range(0, NUM_HIDDEN)
                .mapToObj(i -> "hidden" + i)
                .map(name -> getNamedGUIIcon(atlas, name))
                .toArray(GUIIcon[]::new);
        faces = IntStream.range(0, NUM_FACES)
                .mapToObj(i -> "face" + i)
                .map(atlas::getNamedIconQuad)
                .toArray(IconQuad[]::new);
        map = atlas.getNamedIconQuad("map");
        islands = IntStream.range(0, NUM_ISLANDS)
                .mapToObj(i -> "island" + i)
                .map(name -> loadMapIslandData(atlas, name))
                .toArray(MapIslandData[]::new);
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
