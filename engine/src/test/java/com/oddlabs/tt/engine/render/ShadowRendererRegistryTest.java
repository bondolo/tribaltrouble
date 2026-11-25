package com.oddlabs.tt.engine.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oddlabs.tt.engine.render.state.RenderContext;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.joml.Matrix4fc;
import org.junit.jupiter.api.Test;

/// Headless unit tests for [ShadowRendererRegistry] typed registration, lookup, and disposal.
final class ShadowRendererRegistryTest {
  /// Minimal headless [ShadowListRenderer] stub tracking close calls.
  private static final class PrimaryShadowRenderer extends ShadowListRenderer {
    private int closeCount;

    @Override
    public void renderShadows(
        RenderContext context, RenderQueues queues, float worldSize, Texture heightTexture, Matrix4fc modelView) {}

    @Override
    public void close() {
      closeCount++;
    }
  }

  /// Second distinct [ShadowListRenderer] subclass verifying type mismatch detection.
  private static final class SecondaryShadowRenderer extends ShadowListRenderer {
    private int closeCount;

    @Override
    public void renderShadows(
        RenderContext context, RenderQueues queues, float worldSize, Texture heightTexture, Matrix4fc modelView) {}

    @Override
    public void close() {
      closeCount++;
    }
  }

  @Test
  void registerAndRetrieveTypedShadowRenderers() {
    try (var registry = new ShadowRendererRegistry()) {
      Supplier<Texture[]> firstDesc = () -> new Texture[0];
      Supplier<Texture[]> secondDesc = () -> new Texture[0];

      var firstExpected = new PrimaryShadowRenderer();
      var secondExpected = new SecondaryShadowRenderer();

      ShadowListKey<PrimaryShadowRenderer> firstKey =
          registry.register(firstDesc, PrimaryShadowRenderer.class, () -> firstExpected);
      ShadowListKey<SecondaryShadowRenderer> secondKey =
          registry.register(secondDesc, SecondaryShadowRenderer.class, () -> secondExpected);

      PrimaryShadowRenderer firstActual = registry.get(firstKey);
      SecondaryShadowRenderer secondActual = registry.get(secondKey);

      assertSame(firstExpected, firstActual);
      assertSame(secondExpected, secondActual);
    }
  }

  @Test
  void duplicateDescriptorReusesExistingKeyWithoutInvokingFactory() {
    try (var registry = new ShadowRendererRegistry()) {
      Supplier<Texture[]> desc = () -> new Texture[0];
      var factoryCalls = new AtomicInteger();

      ShadowListKey<PrimaryShadowRenderer> firstKey =
          registry.register(
              desc,
              PrimaryShadowRenderer.class,
              () -> {
                factoryCalls.incrementAndGet();
                return new PrimaryShadowRenderer();
              });
      ShadowListKey<PrimaryShadowRenderer> secondKey =
          registry.register(
              desc,
              PrimaryShadowRenderer.class,
              () -> {
                factoryCalls.incrementAndGet();
                return new PrimaryShadowRenderer();
              });

      assertEquals(firstKey, secondKey);
      assertEquals(1, factoryCalls.get());
    }
  }

  @Test
  void duplicateDescriptorWithDifferentRendererTypeThrowsIllegalStateException() {
    try (var registry = new ShadowRendererRegistry()) {
      Supplier<Texture[]> desc = () -> new Texture[0];
      registry.register(desc, PrimaryShadowRenderer.class, PrimaryShadowRenderer::new);

      var secondFactoryCalls = new AtomicInteger();
      assertThrows(
          IllegalStateException.class,
          () ->
              registry.register(
                  desc,
                  SecondaryShadowRenderer.class,
                  () -> {
                    secondFactoryCalls.incrementAndGet();
                    return new SecondaryShadowRenderer();
                  }));
      assertEquals(0, secondFactoryCalls.get());
    }
  }

  @Test
  void renderersPreservesRegistrationOrder() {
    try (var registry = new ShadowRendererRegistry()) {
      var first = new PrimaryShadowRenderer();
      var second = new SecondaryShadowRenderer();

      registry.register(() -> new Texture[0], PrimaryShadowRenderer.class, () -> first);
      registry.register(() -> new Texture[0], SecondaryShadowRenderer.class, () -> second);

      assertEquals(List.of(first, second), registry.renderers());
    }
  }

  @Test
  void closeClosesRegisteredShadowRenderersAndClearsState() {
    var primary = new PrimaryShadowRenderer();
    var secondary = new SecondaryShadowRenderer();
    var replacement = new PrimaryShadowRenderer();
    Supplier<Texture[]> primaryDesc = () -> new Texture[0];
    try (var registry = new ShadowRendererRegistry()) {
      registry.register(primaryDesc, PrimaryShadowRenderer.class, () -> primary);
      registry.register(() -> new Texture[0], SecondaryShadowRenderer.class, () -> secondary);

      registry.close();
      assertEquals(1, primary.closeCount);
      assertEquals(1, secondary.closeCount);
      assertTrue(registry.renderers().isEmpty());

      registry.close();
      assertEquals(1, primary.closeCount);
      assertEquals(1, secondary.closeCount);

      ShadowListKey<PrimaryShadowRenderer> reRegisteredKey =
          registry.register(primaryDesc, PrimaryShadowRenderer.class, () -> replacement);
      assertSame(replacement, registry.get(reRegisteredKey));
    }
    assertEquals(1, replacement.closeCount);
  }
}
