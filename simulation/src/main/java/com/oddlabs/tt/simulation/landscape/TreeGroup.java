package com.oddlabs.tt.simulation.landscape;

import org.jspecify.annotations.Nullable;

public final class TreeGroup extends AbstractTreeGroup {
    private static final int LANDSCAPE_TREES_MAX_LEVEL = 5;

    /*
     * child[2] | child[3]
     * -------------------
     * child[0] | child[1]
     *
     */
    private final AbstractTreeGroup[] children;

    public TreeGroup(@Nullable AbstractTreeGroup parent, int level) {
        super(parent);
        children = new AbstractTreeGroup[]{
                createChild(level),
                createChild(level),
                createChild(level),
                createChild(level)
        };
    }

    private AbstractTreeGroup createChild(int level) {
        if (level < LANDSCAPE_TREES_MAX_LEVEL) {
            return new TreeGroup(this, level + 1);
        } else {
            return new TreeLeaf(this);
        }
    }

    public AbstractTreeGroup[] children() {
        return children;
    }

    public AbstractTreeGroup child(int index) {
        return children[index];
    }


    @Override
    protected boolean initBounds() {
        boolean node_bounds = false;
        for (AbstractTreeGroup child : children) {
            boolean child_bounds = child.initBounds();
            node_bounds = checkBounds(child, child_bounds, node_bounds);
        }
        super.initBounds();
        return node_bounds;
    }

    private boolean checkBounds(AbstractTreeGroup child, boolean child_bounds, boolean node_bounds) {
        if (!child_bounds)
            return node_bounds;
        if (node_bounds)
            checkBounds(child);
        else
            setBounds(child);
        return true;
    }
}
