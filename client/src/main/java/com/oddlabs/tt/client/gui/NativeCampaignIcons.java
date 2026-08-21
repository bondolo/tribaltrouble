package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.gui.GUIErrorHandler;
import com.oddlabs.tt.gui.GUIIcon;
import com.oddlabs.tt.gui.Icons;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.engine.render.Texture;
import org.w3c.dom.Node;

public final class NativeCampaignIcons implements CampaignIcons {
    private static final int NUM_ISLANDS = 8;

    private static final NativeCampaignIcons ICONS = new NativeCampaignIcons("/gui/native_campaign.xml");

    private final IconQuad map;
    private final MapIslandData[] islands = new MapIslandData[NUM_ISLANDS];
    private final IconQuad[] flags = new IconQuad[3];
    private final IconQuad[] boats = new IconQuad[3];
    private final GUIIcon[] hidden = new GUIIcon[1];
    private final IconQuad[] faces = new IconQuad[9];
    private final int offset_x;
    private final int offset_y;
    private final int width;
    private final int height;

    public static NativeCampaignIcons getIcons() {
        return ICONS;
    }

    private NativeCampaignIcons(String xml_file) {
        Node root = Icons.loadFile(xml_file, new GUIErrorHandler());
        Texture atlas = Icons.loadTexture(root);

        flags[0] = Icons.getNamedIconQuad(root, "flag0", atlas);
        flags[1] = Icons.getNamedIconQuad(root, "flag1", atlas);
        flags[2] = Icons.getNamedIconQuad(root, "flag2", atlas);
        boats[0] = Icons.getNamedIconQuad(root, "boat0", atlas);
        boats[1] = Icons.getNamedIconQuad(root, "boat1", atlas);
        boats[2] = Icons.getNamedIconQuad(root, "boat2", atlas);
        hidden[0] = getNamedGUIIcon(root, "hidden0", atlas);
        faces[0] = Icons.getNamedIconQuad(root, "face0", atlas);
        faces[1] = Icons.getNamedIconQuad(root, "face1", atlas);
        faces[2] = Icons.getNamedIconQuad(root, "face2", atlas);
        faces[3] = Icons.getNamedIconQuad(root, "face3", atlas);
        faces[4] = Icons.getNamedIconQuad(root, "face4", atlas);
        faces[5] = Icons.getNamedIconQuad(root, "face5", atlas);
        faces[6] = Icons.getNamedIconQuad(root, "face6", atlas);
        faces[7] = Icons.getNamedIconQuad(root, "face7", atlas);
        faces[8] = Icons.getNamedIconQuad(root, "face8", atlas);

        map = Icons.getNamedIconQuad(root, "map", atlas);
        for (int i = 0; i < NUM_ISLANDS; i++) {
            islands[i] = loadMapIslandData(root, "island" + i, atlas);
        }

        Node map_node = Icons.getNodeByName("map", root);
        offset_x = Icons.getInt(map_node, "offset_x");
        offset_y = Icons.getInt(map_node, "offset_y");
        width = Icons.getInt(map_node, "width");
        height = Icons.getInt(map_node, "height");
    }

    private MapIslandData loadMapIslandData(Node root, String name,
            Texture texture) {
        Node node = Icons.getNodeByName(name, root);
        ModeIconQuads quads = Icons.getNamedIconQuads(node, "island", texture);
        Node n = Icons.getNodeByName("island", node);
        int x = Icons.getInt(n, "x");
        int y = texture.getHeight() - Icons.getInt(n, "y");
        int pin_index = Icons.getInt(n, "pin_index");
        int pin_x = Icons.getInt(n, "pin_x");
        int pin_y = texture.getHeight() - Icons.getInt(n, "pin_y");
        return new MapIslandData(quads, x, y, flags[pin_index], boats[pin_index], pin_x, pin_y);
    }

    private GUIIcon getNamedGUIIcon(Node root, String name, Texture texture) {
        IconQuad temp = Icons.getNamedIconQuad(root, name, texture);
        Node n = Icons.getNodeByName(name, root);
        int x = Icons.getInt(n, "x");
        int y = texture.getHeight() - Icons.getInt(n, "y");
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

    public int getOffsetX() {
        return offset_x;
    }

    public int getOffsetY() {
        return offset_y;
    }

    public int getInternalWidth() {
        return width;
    }

    public int getInternalHeight() {
        return height;
    }

    @Override
    public MapIslandData getMapIslandData(int i) {
        return islands[i];
    }
}
