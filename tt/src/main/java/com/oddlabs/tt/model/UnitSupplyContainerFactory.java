package com.oddlabs.tt.model;

import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * Factory class for constructing UnitSupplyContainers.
 */
public final class UnitSupplyContainerFactory extends SupplyContainerFactory {
    private final @NonNull Map<@NonNull SupplyType, @NonNull SpriteKey> supply_sprite_lists;

    public UnitSupplyContainerFactory(int max_resource_count, @NonNull Map<@NonNull SupplyType,
            @NonNull SpriteKey> supply_sprite_lists) {
        super(max_resource_count);
        this.supply_sprite_lists = supply_sprite_lists;
    }

    @Override
    public @NonNull SupplyContainer createContainer(Selectable<?> selectable) {
        return new UnitSupplyContainer(getMaxResourceCount(), supply_sprite_lists);
    }
}
