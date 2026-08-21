package com.oddlabs.tt.engine.render.shader;


import java.util.List;

public final class VertexLayout<A extends Enum<A> & VertexAttribute> {

    private final List<A> attributes;
    private final int stride;

    @SafeVarargs
    public VertexLayout(A... attributes) {
        this.attributes = List.of(attributes);
        this.stride = this.attributes.stream()
                .mapToInt(VertexAttribute::getSizeBytes)
                .sum();
    }

    public int getStride() {
        return stride;
    }

    public boolean has(A attribute) {
        return attributes.contains(attribute);
    }

    public int getOffset(A attribute) {
        int offset = 0;
        for (A attr : attributes) {
            if (attr == attribute) {
                return offset;
            }
            offset += attr.getSizeBytes();
        }
        throw new IllegalArgumentException("Attribute not present in layout: " + attribute);
    }

    public void bind(ShaderProgram shader) {
        for (A attr : attributes) {
            int location = shader.getAttributeLocation(attr.getName());
            if (location >= 0) {
                attr.enable(location);
                attr.setPointer(location, stride, getOffset(attr));
            }
        }
    }

    public void unbind(ShaderProgram shader) {
        for (A attr : attributes) {
            int location = shader.getAttributeLocation(attr.getName());
            if (location >= 0) {
                attr.disable(location);
            }
        }
    }
}
