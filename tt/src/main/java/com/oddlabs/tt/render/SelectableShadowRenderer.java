package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.snapshot.EntitySnapshot;
import com.oddlabs.tt.model.snapshot.VisualSnapshots;
import com.oddlabs.tt.render.procedural.GeneratorHalos;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.function.Supplier;

/**
 * Specialized renderer for unit and building shadows and selection rings.
 */
public final class SelectableShadowRenderer extends ShadowListRenderer {
    private final EnumMap<GeneratorHalos.HaloType, Texture> halos = new EnumMap<>(GeneratorHalos.HaloType.class);

    private final Deque<@NonNull ModelState<?>> selection_list = new ArrayDeque<>();
    private final Deque<@NonNull Shadowable> shadowed_list = new ArrayDeque<>();

    public SelectableShadowRenderer(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        Texture[] textures = Resources.findResource(desc);
        halos.put(GeneratorHalos.HaloType.SHADOWED, textures[0]);
        halos.put(GeneratorHalos.HaloType.SELECTED, textures[1]);
        setRadial(true);
    }

    void addToSelectionList(@NonNull ModelState<?> modelState) {
        if (Globals.process_shadows) {
            selection_list.add(modelState);
        }
    }

    void addToShadowList(@NonNull ModelState<?> modelState) {
        if (Globals.process_shadows) {
            Object entityObj = modelState.getEntity();
            if (entityObj instanceof EntitySnapshot entity) {
                float shadowDiameter = 0f;
                if (entity instanceof VisualSnapshots.UnitSnapshot unit) {
                    shadowDiameter = unit.shadowDiameter();
                } else if (entity instanceof VisualSnapshots.BuildingSnapshot building) {
                    shadowDiameter = building.shadowDiameter();
                } else if (entity instanceof VisualSnapshots.SupplySnapshot supply) {
                    shadowDiameter = supply.shadowDiameter();
                }

                if (shadowDiameter > 0f) {
                    final float finalShadowDiameter = shadowDiameter;
                    shadowed_list.add(new Shadowable() {
                        @Override
                        public float getPositionX() {
                            return entity.x();
                        }

                        @Override
                        public float getPositionY() {
                            return entity.y();
                        }

                        @Override
                        public float getShadowDiameter() {
                            return finalShadowDiameter;
                        }

                        @Override
                        public float getShadowOpacity() {
                            return 1.0f;
                        }

                        @Override
                        public Color.@NonNull Linear getShadowColor() {
                            return Color.Linear.BLACK;
                        }

                        @Override
                        public float getShadowVerticalCenter() {
                            return 0.5f;
                        }

                        @Override
                        public float getShadowPattern() {
                            return 0.0f;
                        }
                    });
                }
            }
        }
    }

    void addToShadowList(@NonNull Collection<? extends @NonNull Shadowable> shadowable) {
        if (Globals.process_shadows) {
            shadowed_list.addAll(shadowable);
        }
    }

    @Override
    protected void renderShadows(@NonNull RenderContext context, @NonNull RenderQueues queues,
            @NonNull LandscapeRenderer renderer, @NonNull MatrixStack modelViewStack,
            @NonNull MatrixStack projectionStack) {
        try (var _ = setupShadows(context, queues, renderer, modelViewStack, projectionStack)) {
            setShadowColor(Color.Linear.WHITE);
            setPattern(Selectable.VisualPattern.NONE);
            bindShadowTexture(halos.get(GeneratorHalos.HaloType.SHADOWED));
            while (!shadowed_list.isEmpty()) {
                var model = shadowed_list.pop();
                renderShadow(context, renderer, model);
            }

            bindShadowTexture(halos.get(GeneratorHalos.HaloType.SELECTED));
            while (!selection_list.isEmpty()) {
                var modelState = selection_list.pop();
                Object entityObj = modelState.getEntity();
                if (entityObj instanceof EntitySnapshot entity) {
                    float shadowDiameter = 0f;
                    if (entity instanceof VisualSnapshots.UnitSnapshot unit) {
                        shadowDiameter = unit.shadowDiameter();
                    } else if (entity instanceof VisualSnapshots.BuildingSnapshot building) {
                        shadowDiameter = building.shadowDiameter();
                    }
                    final float finalShadowDiameter = shadowDiameter;
                    Shadowable selectionShadowable = new Shadowable() {
                        @Override
                        public float getPositionX() {
                            return entity.x();
                        }

                        @Override
                        public float getPositionY() {
                            return entity.y();
                        }

                        @Override
                        public float getShadowDiameter() {
                            return finalShadowDiameter;
                        }

                        @Override
                        public float getShadowOpacity() {
                            return 1.0f;
                        }

                        @Override
                        public Color.@NonNull Linear getShadowColor() {
                            return Color.Linear.BLACK;
                        }

                        @Override
                        public float getShadowVerticalCenter() {
                            return 0.5f;
                        }

                        @Override
                        public float getShadowPattern() {
                            return 0.0f;
                        }
                    };
                    setShadowColor(modelState.getSelectionColor());
                    setPattern(modelState.getPattern());
                    renderShadow(context, renderer, selectionShadowable);
                }
            }
        }
    }
}
