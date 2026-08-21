package com.oddlabs.tt.simulation.landscape;

import org.jspecify.annotations.Nullable;

public final class PatchGroup extends AbstractPatchGroup {
    /*
     * child[2] | child[3]
     * -------------------
     * child[0] | child[1]
     *
     */
    private final AbstractPatchGroup[] children;

    public PatchGroup(World world) {
        this(world, world.getHeightMap().getPatchesPerWorld(), 0, 0, 0, null);
    }

    public PatchGroup(World world, int size, int x, int y, int level, @Nullable AbstractPatchGroup parent) {
        super(world.getHeightMap(), size, x, y, parent);
        int child_size = size >> 1;
        children = new AbstractPatchGroup[]{
                createChild(world, child_size, x, y, level),
                createChild(world, child_size, x + child_size, y, level),
                createChild(world, child_size, x, y + child_size, level),
                createChild(world, child_size, x + child_size, y + child_size, level)
        };

        setBounds(children[0]);
        for (int i = 1; i < 4; i++) {
            checkBounds(children[i]);
        }
    }

    public AbstractPatchGroup[] children() {
        return children;
    }

    public AbstractPatchGroup child(int index) {
        return children[index];
    }


    private AbstractPatchGroup createChild(World world, int size, int x, int y, int level) {
        if (size == 1) {
            LandscapeLeaf leaf = new LandscapeLeaf(world, x, y, this);
            return leaf;
        } else {
            PatchGroup group = new PatchGroup(world, size, x, y, level + 1, this);
            return group;
        }
    }
}
