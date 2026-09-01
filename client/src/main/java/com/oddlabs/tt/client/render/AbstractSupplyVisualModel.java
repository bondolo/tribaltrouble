package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.geom.BoundsProvider;
import com.oddlabs.tt.engine.render.SpriteKey;
import com.oddlabs.tt.engine.resource.AssetRegistry;
import com.oddlabs.tt.simulation.model.IronSupply;
import com.oddlabs.tt.simulation.model.RockSupply;
import com.oddlabs.tt.simulation.model.RubberSupply;
import com.oddlabs.tt.simulation.model.SupplyModel;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Base implementation of {@link SupplyVisualModel} providing spawn progress tracking and common transforms.
 *
 * @param <S> the specific SupplyModel type
 */
@NullMarked
public abstract class AbstractSupplyVisualModel<S extends SupplyModel> extends AbstractVisualModel
        implements SupplyVisualModel {
    private static final int ROTATION_HASH_X = 73;
    private static final int ROTATION_HASH_Y = 37;
    private static final int ROTATION_MASK = 0x7FFF;
    private static final double DEGREES_TO_RADIANS = Math.PI / 180.0;

    private final S supplyModel;
    private final float rotation;
    private float spawnProgress = 1.0f;
    private boolean spawning = false;

    protected AbstractSupplyVisualModel(S supplyModel) {
        super(supplyModel);
        this.supplyModel = supplyModel;
        int hash = (supplyModel.getGridX() * ROTATION_HASH_X + supplyModel.getGridY() * ROTATION_HASH_Y)
                & ROTATION_MASK;
        this.rotation = (float) ((hash % 360) * DEGREES_TO_RADIANS);
    }

    @Override
    public S getModel() {
        return supplyModel;
    }

    @Override
    public float getRotation() {
        return rotation;
    }

    @Override
    public float getOffsetZ() {
        return 0.0f;
    }

    @Override
    public Color.@Nullable Linear getSpawnColorTint() {
        return null;
    }

    @Override
    public ShadowProperties getShadowProperties() {
        float ratio = supplyModel.getSupplyRatio();
        float diameter = supplyModel instanceof RubberSupply ? 1.2f : 7.0f * ratio;
        float opacity = spawning ? 0.0f : 0.5f * ratio;
        return new ShadowProperties(diameter, opacity, 0.3f);
    }

    @Override
    public DecalProperties getDecalProperties() {
        return new DecalProperties(null, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public SpriteKey getSpriteKey() {
        return switch (supplyModel.getSupplyType()) {
            case ROCK -> AssetRegistry.getInstance().getRockFragmentSprite(
                    supplyModel instanceof RockSupply rock ? rock.getFragmentIndex() : 0);
            case IRON -> AssetRegistry.getInstance().getIronFragmentSprite(
                    supplyModel instanceof IronSupply iron ? iron.getFragmentIndex() : 0);
            case RUBBER -> AssetRegistry.getInstance().getChickenSprite();
            case WOOD -> throw new UnsupportedOperationException("Tree supply visuals handled separately");
        };
    }

    @Override
    public BoundsProvider getBoundsProvider() {
        return supplyModel.getBoundsProvider();
    }

    @Override
    public void setSpawnProgress(float progress) {
        this.spawnProgress = progress;
        this.spawning = progress < 1.0f;
    }

    @Override
    public void completeSpawn() {
        this.spawnProgress = 1.0f;
        this.spawning = false;
    }

    @Override
    public boolean isSpawning() {
        return spawning;
    }

    @Override
    public float getSpawnProgress() {
        return spawnProgress;
    }

    @Override
    public float getSpawnDuration() {
        return switch (supplyModel.getSupplyType()) {
            case ROCK -> 6.0f;
            case IRON -> 4.5f;
            case RUBBER -> 2.0f;
            case WOOD -> 3.0f;
        };
    }
}
