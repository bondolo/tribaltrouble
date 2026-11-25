package com.oddlabs.tt.engine.render;

/// Typed render queue key identifying a registered shadow renderer.
public record ShadowListKey<R extends ShadowListRenderer>(int key, Class<R> type) implements RenderQueueKey {}
