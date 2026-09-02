package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.engine.resource.TextureFile;
import com.oddlabs.util.Color;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;

/**
 * Utilities for loading icon atlases
 */
public final class Icons {
    private Icons() {
        // no instances
    }

    public static Node loadFile(String xml_file, @Nullable ErrorHandler error_handler) {
        URL url = Utils.makeURL(xml_file);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(error_handler);
            Document document = builder.parse(url.openStream());
            return document.getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("Failed to load icon atlas: " + xml_file, e);
        }
    }

    public static Texture loadTexture(Node n) {
        return loadTexture(n.getAttributes().getNamedItem("texture").getNodeValue());
    }

    public static Texture loadTexture(String tex_file) {
        return Resources.findResource(TextureFile.forUI(tex_file));
    }

    public static Node getNodeByName(String name, Node n) {
        NodeList nl = n.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeName().equals(name))
                return nl.item(i);
        }
        assert false : "Missing node: " + name;
        return null;
    }

    public static int getInt(Node n, String key) {
        String string = n.getAttributes().getNamedItem(key).getNodeValue();
        return Integer.parseInt(string);
    }

    public static float getFloat(Node n, String key) {
        String string = n.getAttributes().getNamedItem(key).getNodeValue();
        return Float.parseFloat(string);
    }

    public static ModeIconQuads getNamedIconQuads(Node n, String name,
            Texture texture) {
        return getIconQuads(getNodeByName(name, n), texture);
    }

    public static IconQuad getNamedIconQuad(Node n, String name, Texture texture) {
        return getIconQuad(getNodeByName(name, n), texture);
    }

    public static ModeIconQuads getIconQuads(Node n, Texture texture) {
        return new ModeIconQuads(
                getIconQuad(getNodeByName("normal", n), texture),
                getIconQuad(getNodeByName("active", n), texture),
                getIconQuad(getNodeByName("disabled", n), texture)
        );
    }

    public static IconQuad getIconQuad(Node n, Texture texture) {
        Node q = getNodeByName("quad", n);
        return parseIconQuad(q, texture);
    }

    public static IconQuad parseIconQuad(Node q, Texture texture) {
        int left = getInt(q, "left");
        int top = getInt(q, "top");
        int right = getInt(q, "right");
        int bottom = getInt(q, "bottom");

        float texW = texture.getWidth();
        float texH = texture.getHeight();

        // Apply a half-texel inset to UVs to prevent atlas bleeding with GL_LINEAR filtering.
        // We only apply this if the quad has sufficient size to avoid flipping UVs.
        float insetU = (right - left) >= 1 ? 0.5f : 0.0f;
        float insetV = (bottom - top) >= 1 ? 0.5f : 0.0f;

        float u1 = (left + insetU) / texW;
        float v1 = 1f - (bottom - insetV) / texH;
        float u2 = (right - insetU) / texW;
        float v2 = 1f - (top + insetV) / texH;

        return new IconQuad(u1, v1, u2, v2, right - left, bottom - top, texture);
    }

    public static Color.Linear getNamedColor(Node n, String name) {
        return getColor(getNodeByName(name, n));
    }

    public static Color.Linear getColor(Node n) {
        Node q = getNodeByName("color", n);
        float r = getFloat(q, "r");
        float g = getFloat(q, "g");
        float b = getFloat(q, "b");
        float a = getFloat(q, "a");
        return new Color.Standard(r, g, b, a).linear();
    }
}
