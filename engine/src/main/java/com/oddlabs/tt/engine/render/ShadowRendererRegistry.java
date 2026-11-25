package com.oddlabs.tt.engine.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/// Manages typed registration, deduplicated lookup, and lifecycle disposal of [ShadowListRenderer] instances.
final class ShadowRendererRegistry implements AutoCloseable {
  private final List<ShadowListRenderer> renderers = new ArrayList<>();
  private final Map<Supplier<Texture[]>, ShadowListKey<?>> keysByDesc = new HashMap<>();

  <R extends ShadowListRenderer> ShadowListKey<R> register(
      Supplier<Texture[]> desc, Class<R> type, Supplier<? extends R> rendererFactory) {
    ShadowListKey<?> existing = keysByDesc.get(desc);
    if (existing != null) {
      if (existing.type() != type) {
        throw new IllegalStateException(
            "Shadow renderer already registered as " + existing.type().getName() + ", requested " + type.getName());
      }
      @SuppressWarnings("unchecked")
      ShadowListKey<R> typed = (ShadowListKey<R>) existing;
      return typed;
    }
    int index = renderers.size();
    R renderer = rendererFactory.get();
    renderers.add(renderer);
    ShadowListKey<R> key = new ShadowListKey<>(index, type);
    keysByDesc.put(desc, key);
    return key;
  }

  <R extends ShadowListRenderer> R get(ShadowListKey<R> key) {
    return key.type().cast(renderers.get(key.key()));
  }

  List<ShadowListRenderer> renderers() {
    return Collections.unmodifiableList(renderers);
  }

  @Override
  public void close() {
    renderers.forEach(ShadowListRenderer::close);
    renderers.clear();
    keysByDesc.clear();
  }
}
