package com.oddlabs.tt.engine.render;

import com.oddlabs.tt.client.render.*;
import com.oddlabs.tt.effects.render.*;

/**
 * A render queue key identifying a texture.
 */
public record TextureKey(int key) implements RenderQueueKey {
}
