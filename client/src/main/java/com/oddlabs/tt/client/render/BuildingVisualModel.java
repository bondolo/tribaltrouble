package com.oddlabs.tt.client.render;

import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.simulation.model.Building;

/**
 * {@link VisualModel} implementation for buildings managing damage and production accessories.
 */
public final class BuildingVisualModel extends AbstractVisualModel {
    public BuildingVisualModel(Building building, AudioImplementation audio) {
        super(building);
        float hitOffsetZ = building.getHitOffsetZ();
        addAccessory(new BuildingDamagedAccessory(building, hitOffsetZ, audio));
        addAccessory(new BuildingProductionAccessory(building, audio));
    }
}
