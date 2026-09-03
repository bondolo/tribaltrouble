package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;

/**
 * Encapsulates an icon atlas parsed from XML and its associated texture.
 */
public final class IconAtlas {
    private final Texture texture;
    private final Element root;

    private IconAtlas(Texture texture, Node rootNode) {
        this.texture = texture;
        this.root = new Element(rootNode);
    }

    public static IconAtlas load(String xmlFile, @Nullable ErrorHandler errorHandler) {
        Node rootNode = Icons.loadFile(xmlFile, errorHandler);
        Texture texture = Icons.loadTexture(rootNode);
        return new IconAtlas(texture, rootNode);
    }

    public Texture getTexture() {
        return texture;
    }

    public Element getElement(String name) {
        return root.getElement(name);
    }

    public IconQuad getNamedIconQuad(String name) {
        return root.getNamedIconQuad(name);
    }

    public ModeIconQuads getNamedIconQuads(String name) {
        return root.getNamedIconQuads(name);
    }

    public int getInt(String key) {
        return root.getInt(key);
    }

    public float getFloat(String key) {
        return root.getFloat(key);
    }

    public Color.Linear getNamedColor(String name) {
        return root.getNamedColor(name);
    }

    /**
     * An element node within an icon atlas.
     */
    public final class Element {
        private final Node node;

        Element(Node node) {
            this.node = node;
        }

        public Element getElement(String name) {
            return new Element(Icons.getNodeByName(name, node));
        }

        public int getInt(String key) {
            return Icons.getInt(node, key);
        }

        public float getFloat(String key) {
            return Icons.getFloat(node, key);
        }

        public IconQuad getIconQuad() {
            return Icons.getIconQuad(node, texture);
        }

        public ModeIconQuads getIconQuads() {
            return Icons.getIconQuads(node, texture);
        }

        public IconQuad getNamedIconQuad(String name) {
            return Icons.getNamedIconQuad(node, name, texture);
        }

        public ModeIconQuads getNamedIconQuads(String name) {
            return Icons.getNamedIconQuads(node, name, texture);
        }

        public Color.Linear getNamedColor(String name) {
            return Icons.getNamedColor(node, name);
        }

        public Color.Linear getColor() {
            return Icons.getColor(node);
        }
    }
}
