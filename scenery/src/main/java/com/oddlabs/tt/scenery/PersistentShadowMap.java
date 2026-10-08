package com.oddlabs.tt.scenery;

import com.oddlabs.tt.engine.render.Texture;
import com.oddlabs.tt.procedural.landscape.LandscapeConfig;
import com.oddlabs.tt.simulation.landscape.AbstractTreeGroup;
import com.oddlabs.tt.simulation.landscape.TreeGroup;
import com.oddlabs.tt.simulation.landscape.TreeLeaf;
import com.oddlabs.tt.simulation.landscape.TreeSupply;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.AbstractElementNode;
import com.oddlabs.tt.simulation.model.Building;
import com.oddlabs.tt.simulation.model.ElementNode;
import com.oddlabs.tt.simulation.model.IronSupply;
import com.oddlabs.tt.simulation.model.Model;
import com.oddlabs.tt.simulation.model.Plants;
import com.oddlabs.tt.simulation.model.RockSupply;
import com.oddlabs.tt.simulation.model.SceneryModel;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.util.function.Consumer;

/**
 * Caches static and semi-persistent island terrain shadows into a two-channel texture.
 * Channel R stores dynamic swaying tree canopy shadows, and Channel G stores stationary structure, rock, and plant
 * shadows.
 */
final class PersistentShadowMap implements AutoCloseable {
    private static final int RESOLUTION = 1024;
    private static final int CHANNELS = 2;
    private static final int CHAN_SWAY = 0;   // Channel R: dynamic swaying tree canopies
    private static final int CHAN_STATIC = 1; // Channel G: stationary structures, buildings, rocks, plants
    private static final float SHADOW_EXPAND_MARGIN = 2.0f;
    private static final float NEIGHBOR_SEARCH_MARGIN = 25.0f;
    private static final float PLANT_SHADOW_DIAMETER = 1.0f;
    private static final float PLANT_SHADOW_OPACITY = 0.35f;

    private final float[] pixels = new float[RESOLUTION * RESOLUTION * CHANNELS];
    private final Texture texture;
    private final World world;
    private final float metersToTexels;
    private final float texelsToMeters;

    PersistentShadowMap(World world) {
        this.world = world;
        float metersPerWorld = (float) world.getHeightMap().getMetersPerWorld();
        this.metersToTexels = RESOLUTION / metersPerWorld;
        this.texelsToMeters = metersPerWorld / RESOLUTION;

        bakeInitialMap();
        this.texture = new Texture(pixels, RESOLUTION, RESOLUTION, GL30.GL_RG8, GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL12.GL_CLAMP_TO_EDGE);
    }

    Texture getTexture() {
        return texture;
    }

    private record PixelBounds(int minPx, int maxPx, int minPy, int maxPy) {
        int width() {
            return maxPx - minPx + 1;
        }

        int height() {
            return maxPy - minPy + 1;
        }
    }

    private record TreeShadowInfo(
                                  float centerX,
                                  float centerY,
                                  float radius,
                                  float diameter,
                                  float opacity,
                                  PixelBounds bounds) {
    }

    private void bakeInitialMap() {
        forEachTree(world.getTreeRoot(), tree -> {
            if (!tree.isEmpty()) {
                stampTree(tree);
            }
        });
        forEachElement(world.getElementRoot(), this::stampStaticModel);
    }

    void onTreeFelled(TreeSupply tree) {
        TreeShadowInfo info = getShadowInfo(tree, SHADOW_EXPAND_MARGIN);
        PixelBounds bounds = info.bounds();
        int minPx = bounds.minPx();
        int maxPx = bounds.maxPx();
        int minPy = bounds.minPy();
        int maxPy = bounds.maxPy();

        for (int y = minPy; y <= maxPy; y++) {
            int rowOffset = y * RESOLUTION;
            for (int x = minPx; x <= maxPx; x++) {
                pixels[(rowOffset + x) * CHANNELS + CHAN_SWAY] = 0.0f;
            }
        }

        float searchMinX = minPx * texelsToMeters - NEIGHBOR_SEARCH_MARGIN;
        float searchMaxX = maxPx * texelsToMeters + NEIGHBOR_SEARCH_MARGIN;
        float searchMinY = minPy * texelsToMeters - NEIGHBOR_SEARCH_MARGIN;
        float searchMaxY = maxPy * texelsToMeters + NEIGHBOR_SEARCH_MARGIN;

        forEachOverlappingTree(world.getTreeRoot(), searchMinX, searchMaxX, searchMinY, searchMaxY, neighbor -> {
            if (neighbor != tree && !neighbor.isEmpty()) {
                stampTree(neighbor, minPx, maxPx, minPy, maxPy);
            }
        });

        uploadRegion(bounds);
    }

    void onTreeSpawned(TreeSupply tree) {
        PixelBounds bounds = stampTree(tree);
        if (bounds != null) {
            uploadRegion(bounds);
        }
    }

    private @Nullable PixelBounds stampTree(TreeSupply tree) {
        return stampTree(tree, 0, RESOLUTION - 1, 0, RESOLUTION - 1);
    }

    private @Nullable PixelBounds stampTree(TreeSupply tree, int clipMinPx, int clipMaxPx, int clipMinPy,
            int clipMaxPy) {
        TreeShadowInfo info = getShadowInfo(tree, 1.0f);
        PixelBounds b = info.bounds();
        int treeMinPx = Math.max(b.minPx(), clipMinPx);
        int treeMaxPx = Math.min(b.maxPx(), clipMaxPx);
        int treeMinPy = Math.max(b.minPy(), clipMinPy);
        int treeMaxPy = Math.min(b.maxPy(), clipMaxPy);
        if (treeMinPx > treeMaxPx || treeMinPy > treeMaxPy) {
            return null;
        }

        float radiusSq = info.radius() * info.radius();
        float invRadius = 1.0f / info.radius();
        float invDiameter = 1.0f / info.diameter();
        float treeX = tree.getPositionX();
        float treeY = tree.getPositionY();
        float centerX = info.centerX();
        float centerY = info.centerY();
        float opacity = info.opacity();

        for (int py = treeMinPy; py <= treeMaxPy; py++) {
            float wy = (py + 0.5f) * texelsToMeters;
            float dy = wy - centerY;
            float dySq = dy * dy;
            float trunkDy = wy - treeY;
            float trunkDySq = trunkDy * trunkDy;
            int rowOffset = py * RESOLUTION;

            for (int px = treeMinPx; px <= treeMaxPx; px++) {
                float wx = (px + 0.5f) * texelsToMeters;
                float dx = wx - centerX;
                float distSq = dx * dx + dySq;

                float falloff = 0.0f;
                if (distSq < radiusSq) {
                    float dist = (float) Math.sqrt(distSq);
                    float u = dist * invRadius;
                    falloff = (1.0f - u) * (1.0f - u) * (1.0f + 2.0f * u) * 0.75f;
                }

                float trunkDx = wx - treeX;
                float trunkDistSq = trunkDx * trunkDx + trunkDySq;
                float contactDist = (float) Math.sqrt(trunkDistSq) * invDiameter;
                float contactAlpha = (float) Math.exp(-contactDist * contactDist * 12.0f) * 0.40f;

                float combined = Math.max(falloff, contactAlpha);
                if (combined > 0.001f) {
                    float val = combined * opacity;
                    int index = (rowOffset + px) * CHANNELS + CHAN_SWAY;
                    float current = pixels[index];
                    pixels[index] = current + val * (1.0f - current);
                }
            }
        }
        return new PixelBounds(treeMinPx, treeMaxPx, treeMinPy, treeMaxPy);
    }

    void onStaticModelAdded(Model model) {
        PixelBounds bounds = stampStaticModel(model);
        if (bounds != null) {
            uploadRegion(bounds);
        }
    }

    void onStaticModelRemoved(Model model) {
        float diameter = getStaticModelDiameter(model);
        float verticalCenter = getStaticModelVerticalCenter(model);
        if (diameter <= 0.001f) {
            return;
        }

        float radius = 0.5f * diameter;
        float offsetScale = 0.5625f * verticalCenter * diameter;
        float posX = model.getPositionX();
        float posY = model.getPositionY();
        float centerX = posX + LandscapeConfig.LIGHT_DIR_X * offsetScale;
        float centerY = posY + LandscapeConfig.LIGHT_DIR_Y * offsetScale;

        float minX = Math.min(centerX - radius, posX) - SHADOW_EXPAND_MARGIN;
        float maxX = Math.max(centerX + radius, posX) + SHADOW_EXPAND_MARGIN;
        float minY = Math.min(centerY - radius, posY) - SHADOW_EXPAND_MARGIN;
        float maxY = Math.max(centerY + radius, posY) + SHADOW_EXPAND_MARGIN;

        int minPx = Math.clamp((int) Math.floor(minX * metersToTexels), 0, RESOLUTION - 1);
        int maxPx = Math.clamp((int) Math.ceil(maxX * metersToTexels), 0, RESOLUTION - 1);
        int minPy = Math.clamp((int) Math.floor(minY * metersToTexels), 0, RESOLUTION - 1);
        int maxPy = Math.clamp((int) Math.ceil(maxY * metersToTexels), 0, RESOLUTION - 1);

        if (minPx > maxPx || minPy > maxPy) {
            return;
        }

        for (int y = minPy; y <= maxPy; y++) {
            int rowOffset = y * RESOLUTION;
            for (int x = minPx; x <= maxPx; x++) {
                pixels[(rowOffset + x) * CHANNELS + CHAN_STATIC] = 0.0f;
            }
        }

        float searchMinX = minPx * texelsToMeters - NEIGHBOR_SEARCH_MARGIN;
        float searchMaxX = maxPx * texelsToMeters + NEIGHBOR_SEARCH_MARGIN;
        float searchMinY = minPy * texelsToMeters - NEIGHBOR_SEARCH_MARGIN;
        float searchMaxY = maxPy * texelsToMeters + NEIGHBOR_SEARCH_MARGIN;

        forEachOverlappingElement(world.getElementRoot(), searchMinX, searchMaxX, searchMinY, searchMaxY, neighbor -> {
            if (neighbor != model) {
                stampStaticModel(neighbor, minPx, maxPx, minPy, maxPy);
            }
        });

        uploadRegion(new PixelBounds(minPx, maxPx, minPy, maxPy));
    }

    private @Nullable PixelBounds stampStaticModel(Model model) {
        return stampStaticModel(model, 0, RESOLUTION - 1, 0, RESOLUTION - 1);
    }

    private @Nullable PixelBounds stampStaticModel(Model model, int clipMinPx, int clipMaxPx, int clipMinPy,
            int clipMaxPy) {
        if (model instanceof RockSupply rock) {
            float ratio = rock.getSupplyRatio();
            float diameter = 7.0f * ratio;
            float opacity = 0.5f * ratio;
            if (opacity > 0.001f && diameter > 0.001f) {
                return stampStaticShadow(rock.getPositionX(), rock.getPositionY(), diameter, opacity, 0.0f,
                        clipMinPx, clipMaxPx, clipMinPy, clipMaxPy);
            }
        } else if (model instanceof IronSupply iron) {
            float ratio = iron.getSupplyRatio();
            float diameter = 7.0f * ratio;
            float opacity = 0.5f * ratio;
            if (opacity > 0.001f && diameter > 0.001f) {
                return stampStaticShadow(iron.getPositionX(), iron.getPositionY(), diameter, opacity, 0.3f,
                        clipMinPx, clipMaxPx, clipMinPy, clipMaxPy);
            }
        } else if (model instanceof Building building) {
            float opacity = building.getShadowOpacity();
            float diameter = building.getShadowDiameter();
            if (opacity > 0.001f && diameter > 0.001f) {
                return stampStaticShadow(building.getPositionX(), building.getPositionY(), diameter, opacity,
                        building.getShadowVerticalCenter(), clipMinPx, clipMaxPx, clipMinPy, clipMaxPy);
            }
        } else if (model instanceof Plants plants) {
            return stampStaticShadow(plants.getPositionX(), plants.getPositionY(), PLANT_SHADOW_DIAMETER,
                    PLANT_SHADOW_OPACITY, 0.0f, clipMinPx, clipMaxPx, clipMinPy, clipMaxPy);
        } else if (model instanceof SceneryModel scenery) {
            float diameter = scenery.getShadowDiameter();
            if (diameter > 0.001f) {
                return stampStaticShadow(scenery.getPositionX(), scenery.getPositionY(), diameter, 0.5f, 0.0f,
                        clipMinPx, clipMaxPx, clipMinPy, clipMaxPy);
            }
        }
        return null;
    }

    private @Nullable PixelBounds stampStaticShadow(float posX, float posY, float diameter, float opacity,
            float verticalCenter, int clipMinPx, int clipMaxPx, int clipMinPy, int clipMaxPy) {
        float radius = 0.5f * diameter;
        float offsetScale = 0.5625f * verticalCenter * diameter;
        float centerX = posX + LandscapeConfig.LIGHT_DIR_X * offsetScale;
        float centerY = posY + LandscapeConfig.LIGHT_DIR_Y * offsetScale;

        float minX = Math.min(centerX - radius, posX) - 1.0f;
        float maxX = Math.max(centerX + radius, posX) + 1.0f;
        float minY = Math.min(centerY - radius, posY) - 1.0f;
        float maxY = Math.max(centerY + radius, posY) + 1.0f;

        int minPx = Math.max(Math.clamp((int) Math.floor(minX * metersToTexels), 0, RESOLUTION - 1), clipMinPx);
        int maxPx = Math.min(Math.clamp((int) Math.ceil(maxX * metersToTexels), 0, RESOLUTION - 1), clipMaxPx);
        int minPy = Math.max(Math.clamp((int) Math.floor(minY * metersToTexels), 0, RESOLUTION - 1), clipMinPy);
        int maxPy = Math.min(Math.clamp((int) Math.ceil(maxY * metersToTexels), 0, RESOLUTION - 1), clipMaxPy);

        if (minPx > maxPx || minPy > maxPy) {
            return null;
        }

        float radiusSq = radius * radius;
        float invRadius = 1.0f / radius;
        float invDiameter = 1.0f / diameter;

        for (int py = minPy; py <= maxPy; py++) {
            float wy = (py + 0.5f) * texelsToMeters;
            float dy = wy - centerY;
            float dySq = dy * dy;
            float trunkDy = wy - posY;
            float trunkDySq = trunkDy * trunkDy;
            int rowOffset = py * RESOLUTION;

            for (int px = minPx; px <= maxPx; px++) {
                float wx = (px + 0.5f) * texelsToMeters;
                float dx = wx - centerX;
                float distSq = dx * dx + dySq;

                float falloff = 0.0f;
                if (distSq < radiusSq) {
                    float dist = (float) Math.sqrt(distSq);
                    float u = dist * invRadius;
                    falloff = (1.0f - u) * (1.0f - u) * (1.0f + 2.0f * u) * 0.75f;
                }

                float trunkDx = wx - posX;
                float trunkDistSq = trunkDx * trunkDx + trunkDySq;
                float contactDist = (float) Math.sqrt(trunkDistSq) * invDiameter;
                float contactAlpha = (float) Math.exp(-contactDist * contactDist * 12.0f) * 0.40f;

                float combined = Math.max(falloff, contactAlpha);
                if (combined > 0.001f) {
                    float val = combined * opacity;
                    int index = (rowOffset + px) * CHANNELS + CHAN_STATIC;
                    float current = pixels[index];
                    pixels[index] = current + val * (1.0f - current);
                }
            }
        }
        return new PixelBounds(minPx, maxPx, minPy, maxPy);
    }

    private TreeShadowInfo getShadowInfo(TreeSupply tree, float margin) {
        AbstractTreeGroup.TreeType type = tree.getTreeType();
        float diameter = getShadowDiameter(type);
        float opacity = getShadowOpacity(type);
        float verticalCenter = getShadowVerticalCenter(type);
        float radius = 0.5f * diameter;

        float offsetScale = 0.5625f * verticalCenter * diameter;
        float centerX = tree.getPositionX() + LandscapeConfig.LIGHT_DIR_X * offsetScale;
        float centerY = tree.getPositionY() + LandscapeConfig.LIGHT_DIR_Y * offsetScale;

        float minX = Math.min(centerX - radius, tree.getPositionX()) - margin;
        float maxX = Math.max(centerX + radius, tree.getPositionX()) + margin;
        float minY = Math.min(centerY - radius, tree.getPositionY()) - margin;
        float maxY = Math.max(centerY + radius, tree.getPositionY()) + margin;

        int minPx = Math.clamp((int) Math.floor(minX * metersToTexels), 0, RESOLUTION - 1);
        int maxPx = Math.clamp((int) Math.ceil(maxX * metersToTexels), 0, RESOLUTION - 1);
        int minPy = Math.clamp((int) Math.floor(minY * metersToTexels), 0, RESOLUTION - 1);
        int maxPy = Math.clamp((int) Math.ceil(maxY * metersToTexels), 0, RESOLUTION - 1);

        return new TreeShadowInfo(centerX, centerY, radius, diameter, opacity,
                new PixelBounds(minPx, maxPx, minPy, maxPy));
    }

    private void uploadRegion(PixelBounds bounds) {
        int minPx = bounds.minPx();
        int minPy = bounds.minPy();
        int width = bounds.width();
        int height = bounds.height();

        float[] subPixels = new float[width * height * CHANNELS];
        for (int y = 0; y < height; y++) {
            System.arraycopy(pixels, ((minPy + y) * RESOLUTION + minPx) * CHANNELS, subPixels,
                    y * width * CHANNELS, width * CHANNELS);
        }
        texture.update(minPx, minPy, width, height, subPixels);
    }

    private void forEachTree(AbstractTreeGroup node, Consumer<TreeSupply> consumer) {
        switch (node) {
            case TreeLeaf leaf -> {
                for (TreeSupply tree : leaf.getTrees()) {
                    consumer.accept(tree);
                }
            }
            case TreeGroup group -> {
                for (AbstractTreeGroup child : group.children()) {
                    forEachTree(child, consumer);
                }
            }
            case TreeSupply supply -> consumer.accept(supply);
        }
    }

    private void forEachOverlappingTree(AbstractTreeGroup node, float minX, float maxX, float minY, float maxY,
            Consumer<TreeSupply> consumer) {
        if (node.bmax_x < minX || node.bmin_x > maxX || node.bmax_y < minY || node.bmin_y > maxY) {
            return;
        }
        switch (node) {
            case TreeLeaf leaf -> {
                for (TreeSupply tree : leaf.getTrees()) {
                    consumer.accept(tree);
                }
            }
            case TreeGroup group -> {
                for (AbstractTreeGroup child : group.children()) {
                    forEachOverlappingTree(child, minX, maxX, minY, maxY, consumer);
                }
            }
            case TreeSupply supply -> consumer.accept(supply);
        }
    }

    private static float getShadowDiameter(AbstractTreeGroup.TreeType type) {
        return switch (type) {
            case JUNGLE -> 16.0f;
            case PALM -> 20.0f;
            case OAK -> 20.0f;
            case PINE -> 12.0f;
        };
    }

    private static float getShadowOpacity(AbstractTreeGroup.TreeType type) {
        return switch (type) {
            case JUNGLE -> 0.50f;
            case PALM -> 0.30f;
            case OAK -> 0.50f;
            case PINE -> 0.60f;
        };
    }

    private static float getShadowVerticalCenter(AbstractTreeGroup.TreeType type) {
        return switch (type) {
            case JUNGLE -> 0.6f;
            case PALM -> 1.0f;
            case OAK -> 0.6f;
            case PINE -> 0.3f;
        };
    }

    private void forEachElement(AbstractElementNode<Model> node, Consumer<Model> consumer) {
        for (Model element = node.getModels().getFirst(); element != null; element = element.getNext()) {
            consumer.accept(element);
        }
        if (node instanceof ElementNode<Model> elementNode) {
            for (AbstractElementNode<Model> child : elementNode.children()) {
                forEachElement(child, consumer);
            }
        }
    }

    private void forEachOverlappingElement(AbstractElementNode<Model> node, float minX, float maxX, float minY,
            float maxY,
            Consumer<Model> consumer) {
        if (node.bmax_x < minX || node.bmin_x > maxX || node.bmax_y < minY || node.bmin_y > maxY) {
            return;
        }
        for (Model element = node.getModels().getFirst(); element != null; element = element.getNext()) {
            consumer.accept(element);
        }
        if (node instanceof ElementNode<Model> elementNode) {
            for (AbstractElementNode<Model> child : elementNode.children()) {
                forEachOverlappingElement(child, minX, maxX, minY, maxY, consumer);
            }
        }
    }

    private static float getStaticModelDiameter(Model model) {
        if (model instanceof RockSupply rock) {
            return 7.0f * rock.getSupplyRatio();
        } else if (model instanceof IronSupply iron) {
            return 7.0f * iron.getSupplyRatio();
        } else if (model instanceof Building building) {
            return building.getShadowDiameter();
        } else if (model instanceof Plants) {
            return PLANT_SHADOW_DIAMETER;
        } else if (model instanceof SceneryModel scenery) {
            return scenery.getShadowDiameter();
        }
        return 0.0f;
    }

    private static float getStaticModelVerticalCenter(Model model) {
        if (model instanceof IronSupply) {
            return 0.3f;
        } else if (model instanceof Building building) {
            return building.getShadowVerticalCenter();
        }
        return 0.0f;
    }

    @Override
    public void close() {
        texture.close();
    }
}
