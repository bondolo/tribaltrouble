package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.client.render.*;
import com.oddlabs.tt.effects.render.*;

/**
 * A render queue key identifying a shadow list.
 */
public record ShadowListKey(int key) implements RenderQueueKey {
}
