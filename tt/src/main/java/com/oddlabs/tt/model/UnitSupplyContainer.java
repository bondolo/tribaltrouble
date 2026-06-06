package com.oddlabs.tt.model;

import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * A specialized supply container for peon units to carry resources.
 */
public final class UnitSupplyContainer extends SupplyContainer {
    private final @NonNull Map<@NonNull SupplyType, @NonNull SpriteKey> supply_sprite_renderers;

    private @Nullable SupplyType type;

    public UnitSupplyContainer(int max_resource_count, @NonNull Map<@NonNull SupplyType,
            @NonNull SpriteKey> supply_sprite_renderers) {
        super(max_resource_count);
        this.supply_sprite_renderers = supply_sprite_renderers;
    }

    @Override
    public int increaseSupply(int amount) {
        throw new UnsupportedOperationException("UnitSupplyContainer requires a supply type");
    }

    public int increaseSupply(int amount, @NonNull SupplyType type) {
        if (this.type != type) {
            this.type = type;
            super.increaseSupply(-getNumSupplies());
        }
        return super.increaseSupply(amount);
    }

    public @NonNull Optional<SupplyType> getSupplyType() {
        return Optional.ofNullable(type);
    }

    public @Nullable SpriteKey getSupplySpriteRenderer(@NonNull SupplyType key) {
        return supply_sprite_renderers.get(key);
    }
}
