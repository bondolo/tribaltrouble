package com.oddlabs.tt.base.util;

import java.util.function.Supplier;

/// Functional interface for progress reporting during map loading and generation.
@FunctionalInterface
public interface ProgressListener {
  /// Listener instance that ignores progress notifications.
  ProgressListener NONE = _ -> {};

  /// Scoped value containing the active {@link ProgressListener} for the current execution context.
  ScopedValue<ProgressListener> CURRENT = ScopedValue.newInstance();

  /// Returns the active progress listener in the current scope, or {@link #NONE} if unbound.
  static ProgressListener current() {
    return CURRENT.orElse(NONE);
  }

  static void progress() {
    CURRENT.orElse(NONE).onProgress();
  }

  /// Advances progress for the current scope by `step`.
  static void progress(float step) {
    CURRENT.orElse(NONE).onAdvance(step);
  }

  static void advance(float delta) {
    CURRENT.orElse(NONE).onAdvance(delta);
  }

  /// Sets absolute progress in `[0.0, 1.0]` within the current scope.
  static void set(float fraction) {
    CURRENT.orElse(NONE).onProgress(fraction);
  }

  static void run(ProgressListener listener, Runnable operation) {
    ScopedValue.where(CURRENT, listener).run(operation);
  }

  static <T> T supply(ProgressListener listener, Supplier<T> supplier) {
    var ref =
        new Object() {
          T value;
        };
    ScopedValue.where(CURRENT, listener).run(() -> ref.value = supplier.get());
    return ref.value;
  }

  static <T, X extends Throwable> T call(ProgressListener listener, ScopedValue.CallableOp<T, X> operation) throws X {
    return ScopedValue.where(CURRENT, listener).call(operation);
  }

  /// Creates a sub-task listener allocated `weight` (in `[0.0, 1.0]`) of this task's progress.
  default ProgressListener subTask(float weight) {
    return new SubTask(this, weight);
  }

  /// Runs `operation` in a scoped sub-task allocated `weight` (in `[0.0, 1.0]`) of the current task's progress,
  /// completing the sub-task on exit.
  static void subTask(float weight, Runnable operation) {
    SubTask sub = new SubTask(current(), weight);
    try {
      ScopedValue.where(CURRENT, sub).run(operation);
    } finally {
      sub.complete();
    }
  }

  /// Evaluates `supplier` in a scoped sub-task allocated `weight` (in `[0.0, 1.0]`) of the current task's progress,
  /// completing the sub-task on exit.
  static <T> T subTask(float weight, Supplier<T> supplier) {
    SubTask sub = new SubTask(current(), weight);
    var ref =
        new Object() {
          T value;
        };
    try {
      ScopedValue.where(CURRENT, sub).run(() -> ref.value = supplier.get());
    } finally {
      sub.complete();
    }
    return ref.value;
  }

  void onProgress(float fraction);

  default void onAdvance(float delta) {
    onProgress(delta);
  }

  default void onProgress() {}

  /// Sub-task listener that scales clamped `[0.0, 1.0]` progress updates by a weight of a parent listener.
  final class SubTask implements ProgressListener {
    private final ProgressListener parent;
    private final float weight;
    private float currentFraction;

    public SubTask(ProgressListener parent, float weight) {
      this.parent = parent;
      this.weight = weight;
    }

    @Override
    public void onProgress(float fraction) {
      float clamped = Math.clamp(fraction, 0f, 1f);
      float delta = clamped - currentFraction;
      if (delta > 0f) {
        currentFraction = clamped;
        parent.onAdvance(delta * weight);
      }
    }

    @Override
    public void onAdvance(float delta) {
      if (delta > 0f) {
        onProgress(currentFraction + delta);
      }
    }

    public void complete() {
      if (currentFraction < 1f) {
        onProgress(1f);
      }
    }
  }
}
