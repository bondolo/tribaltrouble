/**
 * Core event queue and tick timing infrastructure.
 * <p>
 * This package establishes the boundary between:
 * <ul>
 * <li><b>Deterministic Simulation Boundary ({@code com.oddlabs.tt.simulation.*})</b>: Low-precision, fixed-frequency
 * ticks managed via {@link com.oddlabs.tt.core.event.LocalEventQueue#getManager()} for lockstep multiplayer state,
 * checksum verification, pathfinding, player economy, unit AI, and combat rules.</li>
 * <li><b>Non-Deterministic Rendering Boundary ({@code com.oddlabs.tt.effects.*}, {@code com.oddlabs.tt.client.*})</b>:
 * High-precision, frame-rate dependent ticks managed via
 * {@link com.oddlabs.tt.core.event.LocalEventQueue#getHighPrecisionManager()} for camera interpolation, particle
 * effects, UI animations, and spatial audio positioning.</li>
 * </ul>
 */
package com.oddlabs.tt.core.event;
