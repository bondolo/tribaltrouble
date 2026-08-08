package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.client.render.*;
import com.oddlabs.tt.effects.render.*;

/**
 * A key representing an item in the rendering queues.
 */
interface RenderQueueKey {
    /** {@return the integer key value} */
    int key();
}
