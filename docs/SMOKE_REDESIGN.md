# Comprehensive Smoke, Dust, and Cloud Redesign Plan

## 1. Visual Design Goals
Replicate and enhance the legacy "Tribal Trouble" visual style using modern shaders and procedural techniques. The objective is to move from identifiable "sprites" to cohesive, billowy volumetric clouds.

*   **Volumetric Cohesion:** Particles must blend seamlessly. Banding and concentric rings must be eliminated via soft alpha curves.
*   **Organic Variety:** Uses smoothed Voronoi structures to create stylized "chunks" without repetitive patterns or harsh concentric rings.
*   **Heterogeneous Mixing:** Each cloud is a visible mix of particles (dark, light, tinted) where the cluster state represents the *likely average*, not the absolute value.
*   **Realistic Spectrum:** Tints strictly constrained to a **Yellowy-Brown (NO2/Particulate)** ↔ **Neutral Grey** ↔ **Grey-Blue (Haze)** axis.
*   **Unique Tan (Dust):** Tan is reserved exclusively for building destruction and stone debris; it does not appear in standard smoke.
*   **Terrain-Aware Dust:** Massive impacts (Meteors) pull their final dust tint from the underlying landscape ground constants (e.g., Sand, Soil).
*   **Behavioral Transitions:** First-class support for timed state changes (e.g., Smoke transitioning to Dust).

---

## 2. Phase 0: Documentation & Baseline (Current)
Establish the implementation roadmap and definitive legacy reference values.

*   **Action 1:** Write this plan to `docs/SMOKE_REDESIGN.md`. (Completed)
*   **Action 2:** Update `docs/LEGACY_RENDERER_DESIGN.md` with legacy emitter parameters for Production Smoke, Damage Smoke, and Building Collapse. (Completed)

---

## 3. Phase 1: Procedural Texture Refinement (`GeneratorSmoke`)
Create a single, high-fidelity "Perfect Puff" asset designed for real-time tinting and high-alpha blending. The design prioritizes the game's chunky "cartoon" art style while eliminating legacy artifacts.

*   **Voronoi Foundation:** Uses Voronoi noise to maintain stylized, "bubbly" structures.
*   **Ridge Smoothing:** Applies a significant smoothing pass to the noise to eliminate "concentric rings" while preserving the chunky volume.
*   **Ultra-Soft Alpha Falloff:** 
    *   `Ring` radius: **0.4** (Core density without boundary contact).
    *   Alpha Gamma: **3.0** (Extremely shallow edge gradient to eliminate banding).
    *   Dynamic Range: **[0.85, 1.0]** (Noise perturbs the alpha for stylized "chunkiness").
*   **Lighting Detail:**
    *   High Ambient Floor (**0.7**) to maintain visibility and thickness in shadows.
    *   Subtle self-shadowing (`shadow 0.15`, `light 0.4`) via smoothed bump-mapped self-shadowing.
*   **Efficiency:** Standardize on `COMPRESSED_RGBA_S3TC_DXT5`.

---

## 4. Phase 2: Emitter Engine Enhancements
Upgrade the `Emitter` base class to support clustering memory and timed sequences.

*   **4-Anchor Spectrum Walk:**
    *   **0.0:** **Tan Dust** (Reserved for debris/destruction).
    *   **0.33:** **Yellowy-Brown** (NO2 / Particulate).
    *   **0.66:** **Neutral Grey**.
    *   **1.0:** **Grey-Blue Haze**.
*   **Dynamic Clustering:** A slow random walk along the spectrum (standard smoke restricted to `0.33` to `1.0`).
*   **High-Intensity Jitter:** Each particle adds **0.15 Gaussian Jitter** to the cluster color for visible heterogeneity.
*   **Scripted Transitions:** `setTransition(delay, duration, targetSpectrum, targetBrightness)` for precise timing.
*   **Palette Constraints:** `setSpectrumRange(min, max)` to prevent drift into inappropriate tints.

---

## 5. Phase 3: Effect-Specific Implementations

### Building Production (Chimney Smoke)
*   **Palette:** Locked to Neutral/Blue range (`0.66` to `0.8`).
*   **Behavior:** Continuous, slow-evolving billows.

### Building Damage (Haze)
*   **Palette:** Constrained to `[0.66, 1.0]` (Grey to Grey-Blue). **No Tan or Yellow-Brown.**
*   **Legacy Parity:** Initial Alpha **1.0** (normalized from legacy 3.0).

### Building Destruction (Dust)
*   **Sequence:**
    1.  **Smoke Phase (0-15% duration):** Starts as Neutral Grey (`0.66`).
    2.  **Transition Phase (15-25% duration):** Rapid shift to **Tan Dust** anchor (`0.0`).
    3.  **Dust Phase (25-100% duration):** Thick Tan Dust cloud with heavy particulate mixing.

### Lava Effect / Rock Eruption (`RockSupply`)
*   **Sequence:**
    1.  **Eruption:** High-alpha, **Yellowy-Brown NO2** (`0.33`) mixed with glowing orange particles.
    2.  **Cooling:** Spectrum shifts back to Neutral Grey (`0.66`) as the rock rises.

### Meteor (`IronSupply`)
*   **Trail:** Continuous **Yellowy-Brown NO2/Soot** (`0.33`). High brightness jitter.
*   **Impact Sequence:**
    1.  Massive burst mixing **Soot** (`0.33`) and **Grey** (`0.66`).
    2.  Transition into a thick dust cloud tinted by the **Landscape Terrain Ground Color** (sampled from local layer constants).

### Stone Supply (Rock Debris)
*   **Harvesting:** Instantaneous burst of Tan Dust (`0.0`) and Grey fragments.

### Lightning Cloud
*   **Aesthetics:** Atmospheric Grey-Blue lock (`[0.8, 1.0]`).
*   **Scale:** Large puffs, high ambient floor for visibility at distance.
