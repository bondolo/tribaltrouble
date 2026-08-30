package com.oddlabs.tt.client.render;

import com.oddlabs.tt.simulation.model.SupplyModel;
import org.jspecify.annotations.NullMarked;

/**
 * Generic visual model for supply models that do not have custom particle accessories.
 */
@NullMarked
public final class GenericSupplyVisualModel extends AbstractSupplyVisualModel<SupplyModel> {
    public GenericSupplyVisualModel(SupplyModel supplyModel) {
        super(supplyModel);
    }
}
