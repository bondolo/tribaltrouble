package com.oddlabs.tt.effects.particle;

import com.oddlabs.util.Color;

/// Maps a spectrum position and base color to a Linear Color.
@FunctionalInterface
public interface ColorSpectrum {
  /// Resolves the linear color for a spectrum position (typically 0.0 to 1.0) from a linear-space base color.
  Color.Linear getColor(float spectrum, Color.Linear baseColor);
}
