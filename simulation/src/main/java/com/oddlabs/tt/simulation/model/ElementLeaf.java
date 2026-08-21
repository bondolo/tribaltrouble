package com.oddlabs.tt.simulation.model;

import com.oddlabs.tt.simulation.landscape.HeightMap;
import org.jspecify.annotations.Nullable;

public final class ElementLeaf<T extends Element<T>> extends AbstractElementNode<T> {
    public ElementLeaf(@Nullable AbstractElementNode<T> owner/*, int level*/, int size, int x, int y) {
        super(owner/*, level*/);
        setBounds(x * HeightMap.METERS_PER_UNIT_GRID, (x + size) * HeightMap.METERS_PER_UNIT_GRID, y
                * HeightMap.METERS_PER_UNIT_GRID, (y + size) * HeightMap.METERS_PER_UNIT_GRID, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY);
    }

    @Override
    protected AbstractElementNode<T> doInsertElement(T model) {
        incElementCount();
        return addElement(model);
    }
}
