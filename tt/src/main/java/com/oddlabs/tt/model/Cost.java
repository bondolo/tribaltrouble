package com.oddlabs.tt.model;

import com.oddlabs.tt.gui.GUIIcons;
import com.oddlabs.tt.gui.IconQuad;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

/** Captures the supply types and amounts needed for a production */
public final class Cost {
    private final @NonNull SupplyType @NonNull [] supply_types;
    private final int @NonNull [] supply_amounts;

    public Cost(@NonNull SupplyType @NonNull [] supply_types, int @NonNull [] supply_amounts) {
        this.supply_types = supply_types;
        this.supply_amounts = supply_amounts;
        assert supply_types.length == supply_amounts.length;
    }

    public @NonNull SupplyType @NonNull [] getSupplyTypes() {
        return supply_types;
    }

    public int @NonNull [] getSupplyAmounts() {
        return supply_amounts;
    }

    public @NonNull IconQuad @NonNull [] toIconArray() {
        int size = Arrays.stream(supply_amounts).sum();
        IconQuad[] result = new IconQuad[size];
        int index = 0;
        for (int i = 0; i < supply_types.length; i++) {
            IconQuad icon = getIconQuad(supply_types[i]);
            for (int j = 0; j < supply_amounts[i]; j++) {
                result[index++] = icon;
            }
        }
        assert index == result.length;
        return result;
    }

    private @NonNull IconQuad getIconQuad(@NonNull SupplyType supply_type) {
        return switch (supply_type) {
            case WOOD -> GUIIcons.getIcons().getTreeStatusIcon();
            case ROCK -> GUIIcons.getIcons().getRockStatusIcon();
            case IRON -> GUIIcons.getIcons().getIronStatusIcon();
            case RUBBER -> GUIIcons.getIcons().getRubberStatusIcon();
        };
    }
}
